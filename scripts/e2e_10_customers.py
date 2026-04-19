#!/usr/bin/env python3
"""Full E2E run for N customers: create wallet -> fund -> remit -> claim."""
import json
import os
import subprocess
import sys
import time
from dataclasses import dataclass, field
from typing import Any
import urllib.request

BASE = "http://localhost:8080"
AMOUNT_USDC = "1.00"
N_CUSTOMERS = 10
USDC_MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"
CLAIM_AUTHORITY = "3LZh792tEakavG2FJPJKocXUZSfBgmiLtapj5hNMTZkr"
RECIPIENT_PHONE = "+919876543210"
UPI_ID = "test@upi"


def http(method: str, path: str, body: dict | None = None) -> tuple[int, dict | str]:
    url = f"{BASE}{path}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method,
                                 headers={"Content-Type": "application/json"})
    # 180s window covers up to 3 MPC DKG retries (30s each + backoff). STA-85
    # makes the server side fail-fast at ~30s per attempt; the harness must
    # wait longer than the longest legitimate server attempt.
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            raw = r.read().decode()
            code = r.status
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        code = e.code
    try:
        return code, json.loads(raw) if raw else {}
    except json.JSONDecodeError:
        return code, raw


def poll(path: str, predicate, max_secs: int = 120, interval: float = 3.0) -> tuple[int, Any]:
    start = time.time()
    while time.time() - start < max_secs:
        code, body = http("GET", path)
        if code == 200 and predicate(body):
            return code, body
        time.sleep(interval)
    return code, body


def spl_balance(owner: str) -> str:
    r = subprocess.run(
        ["spl-token", "balance", "--url", "devnet", "--owner", owner, USDC_MINT],
        capture_output=True, text=True)
    return r.stdout.strip() if r.returncode == 0 else r.stderr.strip()


def fetch_payout_row(remittance_id: str) -> tuple[str | None, str | None, str | None]:
    """Read payout_id, payout_provider_status, payout_failure_reason from the
    remittances row. STA-91 wires RemittancePayoutWriter into the disburse
    activity; the API response deliberately does NOT expose these (STA-92),
    so the e2e harness goes straight to the DB. Returns (payout_id,
    provider_status, failure_reason); any of them may be None."""
    r = subprocess.run(
        ["docker", "exec", "stablepay-hackathon-postgres-1",
         "psql", "-U", "stablepay", "-d", "stablepay", "-tAc",
         "SELECT COALESCE(payout_id,''), COALESCE(payout_provider_status,''), "
         "COALESCE(payout_failure_reason,'') FROM remittances "
         f"WHERE remittance_id='{remittance_id}';"],
        capture_output=True, text=True)
    if r.returncode != 0:
        return None, None, None
    parts = r.stdout.strip().split("|")
    if len(parts) != 3:
        return None, None, None
    pid, status, reason = parts
    return pid or None, status or None, reason or None


SENSITIVE_KEYS = {"stripeClientSecret", "clientSecret"}


def redact(value: Any) -> Any:
    """Strip Stripe client secrets from API responses before writing to the report."""
    if isinstance(value, dict):
        return {k: "***REDACTED***" if k in SENSITIVE_KEYS else redact(v) for k, v in value.items()}
    if isinstance(value, list):
        return [redact(v) for v in value]
    return value


def create_wallet(user_id: str, log_fn) -> tuple[int, str] | None:
    """Create a wallet in exactly one backend call. STA-85 guarantees the server
    either returns a fully-formed wallet (both key shares persisted, DB NOT NULL
    enforced) or fails with 5xx. Never silently retry a half-formed 201 — that
    would mask the exact regression STA-85 is designed to catch."""
    req = {"userId": user_id}
    code, body = http("POST", "/api/wallets", req)
    log_fn("1-create-wallet", "POST", "/api/wallets", req, code, body)
    if code not in (200, 201):
        return None
    return body["id"], body["solanaAddress"]


@dataclass
class RunResult:
    index: int
    user_id: str
    wallet_id: int | None = None
    solana_address: str | None = None
    funding_id: str | None = None
    payment_intent_id: str | None = None
    remittance_id: str | None = None
    claim_token_id: str | None = None
    final_status: str | None = None
    on_chain_usdc_after_fund: str | None = None
    on_chain_usdc_after_claim: str | None = None
    payout_id: str | None = None
    payout_provider_status: str | None = None
    elapsed_secs: float = 0.0
    passed: bool = False
    error: str | None = None
    detail: list[dict] = field(default_factory=list)

    def log(self, step: str, method: str, path: str, req: Any, code: int, resp: Any):
        self.detail.append({"step": step, "method": method, "path": path,
                            "request": redact(req), "status": code, "response": redact(resp)})


def run_one(idx: int) -> RunResult:
    user_id = f"e2e-10x-{int(time.time()*1000)}-{idx}"
    r = RunResult(index=idx, user_id=user_id)
    t0 = time.time()
    try:
        # 1. Create wallet — STA-85 guarantees the server never returns a
        # half-formed wallet. One attempt, fail the run on 5xx.
        result = create_wallet(user_id, r.log)
        if result is None:
            r.error = "create wallet failed — expected 201, see detail log"
            return r
        r.wallet_id, r.solana_address = result

        # 2. Fund $2
        req = {"amount": float(AMOUNT_USDC)}
        code, body = http("POST", f"/api/wallets/{r.wallet_id}/fund", req)
        r.log("2-initiate-fund", "POST", f"/api/wallets/{r.wallet_id}/fund", req, code, body)
        if code != 201:
            r.error = f"fund HTTP {code}"
            return r
        r.funding_id = body["fundingId"]
        r.payment_intent_id = body["stripePaymentIntentId"]

        # 3. Poll funding order to FUNDED
        code, body = poll(
            f"/api/funding-orders/{r.funding_id}",
            lambda b: b.get("status") == "FUNDED",
            max_secs=90)
        r.log("3-poll-funded", "GET", f"/api/funding-orders/{r.funding_id}", None, code, body)
        if body.get("status") != "FUNDED":
            r.error = f"fund not FUNDED: status={body.get('status')}"
            return r

        # 4. Verify on-chain USDC after fund
        r.on_chain_usdc_after_fund = spl_balance(r.solana_address)
        if r.on_chain_usdc_after_fund != "1":
            r.error = f"on-chain USDC after fund = {r.on_chain_usdc_after_fund}, expected 1"
            return r

        # 5. Create remittance
        req = {"senderId": r.user_id, "recipientPhone": RECIPIENT_PHONE,
               "amountUsdc": float(AMOUNT_USDC)}
        code, body = http("POST", "/api/remittances", req)
        r.log("4-create-remittance", "POST", "/api/remittances", req, code, body)
        if code != 201 and code != 200:
            r.error = f"remittance HTTP {code}"
            return r
        r.remittance_id = body["remittanceId"]
        r.claim_token_id = body["claimTokenId"]

        # 6. Poll remittance to ESCROWED
        code, body = poll(
            f"/api/remittances/{r.remittance_id}",
            lambda b: b.get("status") in ("ESCROWED", "CLAIMED", "DELIVERED"),
            max_secs=120)
        r.log("5-poll-escrowed", "GET", f"/api/remittances/{r.remittance_id}", None, code, body)
        if body.get("status") not in ("ESCROWED", "CLAIMED", "DELIVERED"):
            r.error = f"remittance not ESCROWED: status={body.get('status')}"
            return r

        # 7. Get claim details
        code, body = http("GET", f"/api/claims/{r.claim_token_id}")
        r.log("6-get-claim", "GET", f"/api/claims/{r.claim_token_id}", None, code, body)
        if code != 200:
            r.error = f"get claim HTTP {code}"
            return r

        # 8. Submit claim
        req = {"upiId": UPI_ID}
        code, body = http("POST", f"/api/claims/{r.claim_token_id}", req)
        r.log("7-submit-claim", "POST", f"/api/claims/{r.claim_token_id}", req, code, body)
        if code != 200:
            r.error = f"submit claim HTTP {code}"
            return r

        # 9. Poll remittance to DELIVERED
        code, body = poll(
            f"/api/remittances/{r.remittance_id}",
            lambda b: b.get("status") == "DELIVERED",
            max_secs=180)
        r.log("8-poll-delivered", "GET", f"/api/remittances/{r.remittance_id}", None, code, body)
        r.final_status = body.get("status")
        if r.final_status != "DELIVERED":
            r.error = f"final status = {r.final_status}, expected DELIVERED"
            return r

        # 10. Final on-chain check — claim authority's ATA must show a numeric
        # balance >= the amount we just delivered. If spl-token printed an error
        # string or a value smaller than expected, the claim did not settle.
        r.on_chain_usdc_after_claim = spl_balance(CLAIM_AUTHORITY)
        try:
            claim_balance = float(r.on_chain_usdc_after_claim)
        except ValueError:
            r.error = (
                "claim authority USDC balance unreadable after claim: "
                + str(r.on_chain_usdc_after_claim)
            )
            return r
        if claim_balance < float(AMOUNT_USDC):
            r.error = (
                f"claim authority USDC balance = {claim_balance}, "
                f"expected >= {AMOUNT_USDC}"
            )
            return r

        # 11. STA-91 — verify RemittancePayoutWriter persisted payout_id +
        # provider_status during disburseInr. Logging adapter returns
        # "log_<uuid>" + "SIMULATED"; Razorpay (real or WireMock-stubbed)
        # returns "pout_..." + "processing". Either shape proves the writer
        # path ran; any other combination is a regression.
        payout_id, provider_status, _ = fetch_payout_row(r.remittance_id)
        r.payout_id = payout_id
        r.payout_provider_status = provider_status
        if not payout_id or not (payout_id.startswith("log_") or payout_id.startswith("pout_")):
            r.error = f"payout_id not persisted or wrong format: {payout_id!r}"
            return r
        if provider_status not in ("SIMULATED", "processing"):
            r.error = f"payout_provider_status = {provider_status!r}, expected 'SIMULATED' or 'processing'"
            return r

        r.passed = True
    finally:
        r.elapsed_secs = round(time.time() - t0, 1)
    return r


def write_report(results: list[RunResult], out_path: str):
    lines = [
        f"# 10-Customer Full E2E Run — ${AMOUNT_USDC} USDC per customer",
        "",
        f"Generated: {time.strftime('%Y-%m-%d %H:%M:%S UTC', time.gmtime())}",
        "",
        "## Summary",
        "",
        f"- Customers attempted: {len(results)}",
        f"- Passed: {sum(1 for r in results if r.passed)}",
        f"- Failed: {sum(1 for r in results if not r.passed)}",
        f"- Per-customer amount: ${AMOUNT_USDC} USDC",
        "",
        "| # | userId | walletId | address | fundingId | remittanceId | final | USDC after fund | payout_id | elapsed (s) | PASS |",
        "|---|--------|----------|---------|-----------|--------------|-------|-----------------|-----------|-------------|------|",
    ]
    for r in results:
        lines.append(
            f"| {r.index} | `{r.user_id}` | {r.wallet_id} | `{r.solana_address}` | "
            f"`{r.funding_id}` | `{r.remittance_id}` | {r.final_status or '-'} | "
            f"{r.on_chain_usdc_after_fund or '-'} | `{r.payout_id or '-'}` | "
            f"{r.elapsed_secs} | "
            f"{'✅' if r.passed else '❌ ' + (r.error or '')} |"
        )
    lines += ["", "## Per-customer request / response log", ""]
    for r in results:
        lines += [
            f"### Customer {r.index} — `{r.user_id}`",
            "",
            f"- walletId: `{r.wallet_id}`  solanaAddress: `{r.solana_address}`",
            f"- fundingId: `{r.funding_id}`  paymentIntentId: `{r.payment_intent_id}`",
            f"- remittanceId: `{r.remittance_id}`  claimTokenId: `{r.claim_token_id}`",
            f"- final status: **{r.final_status}**  elapsed: {r.elapsed_secs}s  "
            f"{'**PASS**' if r.passed else '**FAIL: ' + (r.error or '') + '**'}",
            f"- On-chain USDC after fund: {r.on_chain_usdc_after_fund}",
            f"- Claim authority USDC after claim: {r.on_chain_usdc_after_claim}",
            f"- Payout: id=`{r.payout_id or '-'}` status=`{r.payout_provider_status or '-'}`",
            "",
        ]
        for d in r.detail:
            lines += [
                f"#### {d['step']}",
                "",
                f"`{d['method']} {d['path']}`  → **HTTP {d['status']}**",
                "",
                "Request:",
                "```json",
                json.dumps(d["request"], indent=2) if d["request"] is not None else "(no body)",
                "```",
                "",
                "Response:",
                "```json",
                json.dumps(d["response"], indent=2) if isinstance(d["response"], (dict, list)) else str(d["response"]),
                "```",
                "",
            ]
        lines.append("---\n")
    with open(out_path, "w") as f:
        f.write("\n".join(lines))


def run_salvage(idx: int, wallet_id: int, user_id: str, amount_usdc: str) -> RunResult:
    """Complete remittance + claim for an already-funded wallet from an aborted prior run."""
    r = RunResult(index=idx, user_id=user_id, wallet_id=wallet_id)
    r.funding_id = "(salvaged — fund completed in prior aborted run)"
    t0 = time.time()
    try:
        # Look up solana address
        dbr = subprocess.run(
            ["docker", "exec", "stablepay-hackathon-postgres-1",
             "psql", "-U", "stablepay", "-d", "stablepay", "-tAc",
             f"SELECT solana_address FROM wallets WHERE id={wallet_id};"],
            capture_output=True, text=True)
        r.solana_address = dbr.stdout.strip()
        r.on_chain_usdc_after_fund = spl_balance(r.solana_address)

        # Remittance
        req = {"senderId": user_id, "recipientPhone": RECIPIENT_PHONE,
               "amountUsdc": float(amount_usdc)}
        code, body = http("POST", "/api/remittances", req)
        r.log("4-create-remittance", "POST", "/api/remittances", req, code, body)
        if code not in (200, 201):
            r.error = f"remittance HTTP {code}"
            return r
        r.remittance_id = body["remittanceId"]
        r.claim_token_id = body["claimTokenId"]

        # Poll to ESCROWED
        code, body = poll(
            f"/api/remittances/{r.remittance_id}",
            lambda b: b.get("status") in ("ESCROWED", "CLAIMED", "DELIVERED"),
            max_secs=180)
        r.log("5-poll-escrowed", "GET", f"/api/remittances/{r.remittance_id}", None, code, body)
        if body.get("status") not in ("ESCROWED", "CLAIMED", "DELIVERED"):
            r.error = f"remittance not ESCROWED: status={body.get('status')}"
            return r

        # Get claim
        code, body = http("GET", f"/api/claims/{r.claim_token_id}")
        r.log("6-get-claim", "GET", f"/api/claims/{r.claim_token_id}", None, code, body)

        # Submit claim
        req = {"upiId": UPI_ID}
        code, body = http("POST", f"/api/claims/{r.claim_token_id}", req)
        r.log("7-submit-claim", "POST", f"/api/claims/{r.claim_token_id}", req, code, body)

        # Poll DELIVERED
        code, body = poll(
            f"/api/remittances/{r.remittance_id}",
            lambda b: b.get("status") == "DELIVERED",
            max_secs=180)
        r.log("8-poll-delivered", "GET", f"/api/remittances/{r.remittance_id}", None, code, body)
        r.final_status = body.get("status")
        if r.final_status != "DELIVERED":
            r.error = f"final status = {r.final_status}"
            return r
        r.on_chain_usdc_after_claim = spl_balance(CLAIM_AUTHORITY)
        payout_id, provider_status, _ = fetch_payout_row(r.remittance_id)
        r.payout_id = payout_id
        r.payout_provider_status = provider_status
        if not payout_id or not (payout_id.startswith("log_") or payout_id.startswith("pout_")):
            r.error = f"payout_id not persisted or wrong format: {payout_id!r}"
            return r
        if provider_status not in ("SIMULATED", "processing"):
            r.error = f"payout_provider_status = {provider_status!r}, expected 'SIMULATED' or 'processing'"
            return r
        r.passed = True
    finally:
        r.elapsed_secs = round(time.time() - t0, 1)
    return r


def parse_salvage_from_env() -> list[tuple[int, str, str]]:
    """Optional opt-in via STABLEPAY_SALVAGE_WALLETS environment variable.
    Format: "walletId:userId:amount,walletId:userId:amount,..."
    Example: STABLEPAY_SALVAGE_WALLETS="12:e2e-10x-...:2.00,13:e2e-10x-...:1.00"
    """
    raw = os.environ.get("STABLEPAY_SALVAGE_WALLETS", "").strip()
    if not raw:
        return []
    entries = []
    for chunk in raw.split(","):
        parts = chunk.strip().split(":", 2)
        if len(parts) != 3:
            print(f"  ignoring malformed salvage entry: {chunk!r}", flush=True)
            continue
        entries.append((int(parts[0]), parts[1], parts[2]))
    return entries


def main():
    salvage = parse_salvage_from_env()
    results: list[RunResult] = []
    for i, (wid, uid, amt) in enumerate(salvage, start=1):
        print(f"[{i}/{N_CUSTOMERS}] SALVAGE wallet={wid} userId={uid} amount=${amt}...", flush=True)
        r = run_salvage(i, wid, uid, amt)
        results.append(r)
        mark = "PASS" if r.passed else f"FAIL ({r.error})"
        print(f"[{i}/{N_CUSTOMERS}] {mark} elapsed={r.elapsed_secs}s "
              f"wallet={r.wallet_id} remittance={r.remittance_id}", flush=True)

    for i in range(len(salvage) + 1, N_CUSTOMERS + 1):
        print(f"[{i}/{N_CUSTOMERS}] starting fresh...", flush=True)
        r = run_one(i)
        results.append(r)
        mark = "PASS" if r.passed else f"FAIL ({r.error})"
        print(f"[{i}/{N_CUSTOMERS}] {mark} elapsed={r.elapsed_secs}s "
              f"wallet={r.wallet_id} funding={r.funding_id} "
              f"remittance={r.remittance_id}", flush=True)

    out = "docs/E2E_10_CUSTOMER_RUN.md"
    write_report(results, out)
    passed = sum(1 for r in results if r.passed)
    print(f"\n{passed}/{N_CUSTOMERS} passed. Report written to {out}", flush=True)
    sys.exit(0 if passed == N_CUSTOMERS else 1)


if __name__ == "__main__":
    main()
