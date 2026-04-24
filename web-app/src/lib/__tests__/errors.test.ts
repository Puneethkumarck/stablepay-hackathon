import { describe, expect, it } from "vitest";
import { getErrorMessage } from "../errors";

describe("getErrorMessage", () => {
  it("should return message for known error code", () => {
    expect(getErrorMessage("SP-0001")).toBe("Wallet not found. Please try again.");
  });

  it("should return message for auth error code", () => {
    expect(getErrorMessage("SP-0006")).toBe("Invalid ID token. Please sign in again.");
  });

  it("should return message for funding error code", () => {
    expect(getErrorMessage("SP-0022")).toBe("A funding order is already in progress.");
  });

  it("should return fallback for unknown error code", () => {
    expect(getErrorMessage("SP-9000")).toBe("Something went wrong. Please try again.");
  });

  it("should return fallback for empty string", () => {
    expect(getErrorMessage("")).toBe("Something went wrong. Please try again.");
  });
});
