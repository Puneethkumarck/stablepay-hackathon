import { describe, expect, it } from "vitest";
import { formatCurrency, formatPhone, formatRelativeTime, truncateAddress } from "../format";

describe("formatCurrency", () => {
  it("should format USD amounts with two decimals", () => {
    // then
    expect(formatCurrency("100", "USD")).toBe("$100.00");
  });

  it("should format USD zero", () => {
    // then
    expect(formatCurrency("0", "USD")).toBe("$0.00");
  });

  it("should format empty string as zero", () => {
    // then
    expect(formatCurrency("", "USD")).toBe("$0.00");
  });

  it("should format USD with cents", () => {
    // then
    expect(formatCurrency("248.50", "USD")).toBe("$248.50");
  });

  it("should format large USD with commas", () => {
    // then
    expect(formatCurrency("10000", "USD")).toBe("$10,000.00");
  });

  it("should format INR amounts with Indian grouping", () => {
    // then
    expect(formatCurrency("8450", "INR")).toBe("₹8,450.00");
  });

  it("should format large INR with lakh grouping", () => {
    // then
    expect(formatCurrency("42250", "INR")).toBe("₹42,250.00");
  });

  it("should format INR zero", () => {
    // then
    expect(formatCurrency("0", "INR")).toBe("₹0.00");
  });
});

describe("formatPhone", () => {
  it("should format Indian phone number with country code", () => {
    // then
    expect(formatPhone("+919876543210")).toBe("+91 98765 43210");
  });

  it("should return non-Indian numbers unchanged", () => {
    // then
    expect(formatPhone("+12025551234")).toBe("+12025551234");
  });

  it("should handle already formatted input", () => {
    // then
    expect(formatPhone("+91 98765 43210")).toBe("+91 98765 43210");
  });
});

describe("formatRelativeTime", () => {
  it("should format seconds ago", () => {
    // given
    const thirtySecondsAgo = new Date(Date.now() - 30 * 1000).toISOString();

    // then
    expect(formatRelativeTime(thirtySecondsAgo)).toBe("30s ago");
  });

  it("should format minutes ago", () => {
    // given
    const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString();

    // then
    expect(formatRelativeTime(fiveMinutesAgo)).toBe("5m ago");
  });

  it("should format hours ago", () => {
    // given
    const threeHoursAgo = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString();

    // then
    expect(formatRelativeTime(threeHoursAgo)).toBe("3h ago");
  });

  it("should format yesterday", () => {
    // given
    const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();

    // then
    expect(formatRelativeTime(oneDayAgo)).toBe("yesterday");
  });

  it("should format days ago", () => {
    // given
    const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString();

    // then
    expect(formatRelativeTime(threeDaysAgo)).toBe("3 days ago");
  });

  it("should format weeks ago", () => {
    // given
    const twoWeeksAgo = new Date(Date.now() - 14 * 24 * 60 * 60 * 1000).toISOString();

    // then
    expect(formatRelativeTime(twoWeeksAgo)).toBe("2w ago");
  });

  it("should handle future dates", () => {
    // given
    const futureDate = new Date(Date.now() + 60 * 1000).toISOString();

    // then
    expect(formatRelativeTime(futureDate)).toBe("just now");
  });
});

describe("truncateAddress", () => {
  it("should truncate long addresses", () => {
    // then
    expect(truncateAddress("CrsMdGHJ1234567890DAd18")).toBe("CrsMd…DAd18");
  });

  it("should return short addresses unchanged", () => {
    // then
    expect(truncateAddress("short")).toBe("short");
  });

  it("should handle exactly 10 characters", () => {
    // then
    expect(truncateAddress("1234567890")).toBe("1234567890");
  });

  it("should handle 11 characters", () => {
    // then
    expect(truncateAddress("12345678901")).toBe("12345…78901");
  });
});
