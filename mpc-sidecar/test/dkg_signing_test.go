package test

import (
	"context"
	stded25519 "crypto/ed25519"
	"encoding/json"
	"fmt"
	"math/big"
	"sync"
	"testing"
	"time"

	"github.com/bnb-chain/tss-lib/v2/common"
	"github.com/bnb-chain/tss-lib/v2/eddsa/keygen"
	"github.com/bnb-chain/tss-lib/v2/eddsa/signing"
	"github.com/bnb-chain/tss-lib/v2/tss"
	"github.com/btcsuite/btcutil/base58"
	"github.com/decred/dcrd/dcrec/edwards/v2"
)

// TestEdDSAKeygen validates that the fystack/tss-lib EdDSA module
// produces valid Ed25519 key pairs via distributed key generation.
func TestEdDSAKeygen(t *testing.T) {
	threshold := 1 // tss-lib: t means t+1 needed (so 2-of-2)
	totalParties := 2

	keyShareDatas := runDKG(t, threshold, totalParties)

	// Verify all parties derived the same public key / Solana address
	var addresses []string
	for i := 0; i < totalParties; i++ {
		var saveData keygen.LocalPartySaveData
		if err := json.Unmarshal(keyShareDatas[i], &saveData); err != nil {
			t.Fatalf("Party %d unmarshal error: %v", i, err)
		}
		pk := edwards.PublicKey{
			Curve: tss.Edwards(),
			X:     saveData.EDDSAPub.X(),
			Y:     saveData.EDDSAPub.Y(),
		}
		addr := base58.Encode(pk.Serialize())
		addresses = append(addresses, addr)
		t.Logf("Party %d Solana address: %s", i, addr)
	}

	if addresses[0] != addresses[1] {
		t.Fatalf("Parties derived different addresses: %s vs %s", addresses[0], addresses[1])
	}
	t.Logf("PASS: All parties agree on Solana address: %s", addresses[0])
}

// TestEdDSAKeygenAndSigning validates the full DKG → Sign → Verify cycle.
func TestEdDSAKeygenAndSigning(t *testing.T) {
	threshold := 1
	totalParties := 2

	// Phase 1: DKG
	t.Log("=== Phase 1: EdDSA Distributed Key Generation ===")
	keyShareDatas := runDKG(t, threshold, totalParties)

	// Phase 2: Signing
	t.Log("=== Phase 2: EdDSA Threshold Signing ===")
	message := []byte("test solana transaction data")

	sigData := runSigning(t, threshold, totalParties, keyShareDatas, message)

	// Phase 3: Verify signature
	t.Log("=== Phase 3: Signature Verification ===")
	var saveData keygen.LocalPartySaveData
	if err := json.Unmarshal(keyShareDatas[0], &saveData); err != nil {
		t.Fatalf("Unmarshal error: %v", err)
	}

	pk := edwards.PublicKey{
		Curve: tss.Edwards(),
		X:     saveData.EDDSAPub.X(),
		Y:     saveData.EDDSAPub.Y(),
	}

	r := new(big.Int).SetBytes(sigData.R)
	s := new(big.Int).SetBytes(sigData.S)

	ok := edwards.Verify(&pk, message, r, s)
	if !ok {
		t.Fatal("FAIL: Ed25519 signature verification failed (dcrd/edwards)")
	}
	t.Logf("PASS: dcrd/edwards signature verified")

	// Cross-verify with Go's standard crypto/ed25519 (same as Solana uses)
	pubKeyBytes := pk.Serialize() // 32-byte Ed25519 public key
	t.Logf("Public key (hex): %x", pubKeyBytes)
	t.Logf("Signature (hex):  %x", sigData.Signature)
	t.Logf("Message (hex):    %x", message)

	stdOk := stded25519.Verify(stded25519.PublicKey(pubKeyBytes), message, sigData.Signature)
	if !stdOk {
		t.Logf("FAIL: crypto/ed25519 verification FAILED — signature not Solana-compatible")
		t.Logf("Trying with R||S constructed from big-endian fields...")

		// Try constructing signature from R/S fields
		rBytes := padTo32(sigData.R)
		sBytes := padTo32(sigData.S)
		altSig := append(rBytes, sBytes...)
		t.Logf("Alt signature (hex): %x", altSig)

		altOk := stded25519.Verify(stded25519.PublicKey(pubKeyBytes), message, altSig)
		if altOk {
			t.Log("PASS: crypto/ed25519 verified with manually constructed R||S")
		} else {
			t.Fatal("FAIL: Neither sigData.Signature nor R||S passes crypto/ed25519 verify")
		}
	} else {
		t.Log("PASS: crypto/ed25519 (standard Ed25519) signature verified — Solana compatible!")
	}

	t.Logf("Solana address: %s", base58.Encode(pubKeyBytes))
	t.Log("SPIKE RESULT: EdDSA keygen + signing with fystack/tss-lib PASSED")
}

func runDKG(t *testing.T, threshold, totalParties int) [][]byte {
	t.Helper()

	partyIDs := make(tss.UnSortedPartyIDs, totalParties)
	for i := 0; i < totalParties; i++ {
		key := big.NewInt(int64(i + 1))
		partyIDs[i] = tss.NewPartyID(
			fmt.Sprintf("party-%d", i),
			fmt.Sprintf("Party %d", i),
			key,
		)
	}
	sortedIDs := tss.SortPartyIDs(partyIDs)

	outChs := make([]chan tss.Message, totalParties)
	endChs := make([]chan *keygen.LocalPartySaveData, totalParties)
	parties := make([]tss.Party, totalParties)

	for i := 0; i < totalParties; i++ {
		outChs[i] = make(chan tss.Message, 100)
		endChs[i] = make(chan *keygen.LocalPartySaveData, 1)

		peerCtx := tss.NewPeerContext(sortedIDs)
		params := tss.NewParameters(tss.Edwards(), peerCtx, sortedIDs[i], totalParties, threshold)
		parties[i] = keygen.NewLocalParty(params, outChs[i], endChs[i])
	}

	// Start all parties
	for i := 0; i < totalParties; i++ {
		go func(idx int) {
			if err := parties[idx].Start(); err != nil {
				t.Errorf("Party %d start error: %v", idx, err)
			}
		}(i)
	}

	// Route messages and collect results
	keyShareDatas := make([][]byte, totalParties)
	var wg sync.WaitGroup
	wg.Add(totalParties)

	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	for i := 0; i < totalParties; i++ {
		go func(idx int) {
			defer wg.Done()
			for {
				select {
				case <-ctx.Done():
					t.Errorf("Party %d DKG timed out", idx)
					return
				case msg := <-outChs[idx]:
					routeMessage(t, msg, idx, totalParties, sortedIDs, parties)
				case saveData := <-endChs[idx]:
					data, _ := json.Marshal(saveData)
					keyShareDatas[idx] = data
					t.Logf("Party %d DKG complete", idx)
					return
				}
			}
		}(i)
	}

	wg.Wait()

	for i := 0; i < totalParties; i++ {
		if keyShareDatas[i] == nil {
			t.Fatalf("Party %d did not complete DKG", i)
		}
	}

	return keyShareDatas
}

func runSigning(t *testing.T, threshold, totalParties int, keyShareDatas [][]byte, message []byte) *common.SignatureData {
	t.Helper()

	// Reconstruct party IDs from saved DKG data
	var saveData0 keygen.LocalPartySaveData
	if err := json.Unmarshal(keyShareDatas[0], &saveData0); err != nil {
		t.Fatalf("Unmarshal error: %v", err)
	}

	signPartyIDs := make(tss.UnSortedPartyIDs, len(saveData0.Ks))
	for i, k := range saveData0.Ks {
		signPartyIDs[i] = tss.NewPartyID(
			fmt.Sprintf("party-%d", i),
			fmt.Sprintf("Party %d", i),
			k,
		)
	}
	sortedIDs := tss.SortPartyIDs(signPartyIDs)

	msgBigInt := new(big.Int).SetBytes(message)

	outChs := make([]chan tss.Message, totalParties)
	endChs := make([]chan *common.SignatureData, totalParties)
	parties := make([]tss.Party, totalParties)

	for i := 0; i < totalParties; i++ {
		outChs[i] = make(chan tss.Message, 100)
		endChs[i] = make(chan *common.SignatureData, 1)

		var sd keygen.LocalPartySaveData
		json.Unmarshal(keyShareDatas[i], &sd)

		peerCtx := tss.NewPeerContext(sortedIDs)
		params := tss.NewParameters(tss.Edwards(), peerCtx, sortedIDs[i], totalParties, threshold)
		parties[i] = signing.NewLocalParty(msgBigInt, params, sd, outChs[i], endChs[i])
	}

	// Start all parties
	for i := 0; i < totalParties; i++ {
		go func(idx int) {
			if err := parties[idx].Start(); err != nil {
				t.Errorf("Party %d signing start error: %v", idx, err)
			}
		}(i)
	}

	// Route messages and collect signature
	var result *common.SignatureData
	var mu sync.Mutex
	var wg sync.WaitGroup
	wg.Add(totalParties)

	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	for i := 0; i < totalParties; i++ {
		go func(idx int) {
			defer wg.Done()
			for {
				select {
				case <-ctx.Done():
					t.Errorf("Party %d signing timed out", idx)
					return
				case msg := <-outChs[idx]:
					routeMessage(t, msg, idx, totalParties, sortedIDs, parties)
				case sigData := <-endChs[idx]:
					mu.Lock()
					if result == nil {
						result = sigData
					}
					mu.Unlock()
					t.Logf("Party %d signing complete", idx)
					return
				}
			}
		}(i)
	}

	wg.Wait()

	if result == nil {
		t.Fatal("No party completed signing")
	}

	return result
}

func routeMessage(t *testing.T, msg tss.Message, fromIdx, totalParties int, sortedIDs tss.SortedPartyIDs, parties []tss.Party) {
	t.Helper()

	wireBytes, routing, err := msg.WireBytes()
	if err != nil {
		t.Errorf("Wire bytes error from party %d: %v", fromIdx, err)
		return
	}

	if routing.IsBroadcast {
		for j := 0; j < totalParties; j++ {
			if j == fromIdx {
				continue
			}
			parsed, _ := tss.ParseWireMessage(wireBytes, sortedIDs[fromIdx], true)
			if ok, tssErr := parties[j].Update(parsed); tssErr != nil {
				t.Errorf("Broadcast update error at party %d: %v", j, tssErr)
			} else if !ok {
				t.Errorf("Broadcast update returned false at party %d", j)
			}
		}
	} else {
		for _, to := range routing.To {
			toIdx := int(to.KeyInt().Int64() - 1)
			parsed, _ := tss.ParseWireMessage(wireBytes, sortedIDs[fromIdx], false)
			if ok, tssErr := parties[toIdx].Update(parsed); tssErr != nil {
				t.Errorf("P2P update error at party %d: %v", toIdx, tssErr)
			} else if !ok {
				t.Errorf("P2P update returned false at party %d", toIdx)
			}
		}
	}
}

func padTo32(b []byte) []byte {
	if len(b) >= 32 {
		return b[len(b)-32:]
	}
	padded := make([]byte, 32)
	copy(padded[32-len(b):], b)
	return padded
}
