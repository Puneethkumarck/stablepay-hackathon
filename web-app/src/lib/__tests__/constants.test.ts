import { describe, expect, it } from "vitest";
import {
  CLAIM_EXPIRY_HOURS,
  CORRIDOR,
  MAX_AMOUNT_USDC,
  MIN_AMOUNT_USDC,
  NETWORK_FEE,
  SETTLEMENT_TIME,
} from "../constants";

describe("constants", () => {
  it("should define USD_INR corridor", () => {
    // then
    expect(CORRIDOR).toBe("USD_INR");
  });

  it("should define network fee as string decimal", () => {
    // then
    expect(NETWORK_FEE).toBe("0.002");
  });

  it("should define settlement time", () => {
    // then
    expect(SETTLEMENT_TIME).toBe("~30 sec");
  });

  it("should define claim expiry as 48 hours", () => {
    // then
    expect(CLAIM_EXPIRY_HOURS).toBe(48);
  });

  it("should define minimum amount", () => {
    // then
    expect(MIN_AMOUNT_USDC).toBe(1);
  });

  it("should define maximum amount", () => {
    // then
    expect(MAX_AMOUNT_USDC).toBe(10000);
  });
});
