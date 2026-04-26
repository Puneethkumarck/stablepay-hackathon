import { afterEach, describe, expect, it } from "vitest";
import { useSendFlowStore } from "../send-flow";

describe("useSendFlowStore", () => {
  afterEach(() => {
    useSendFlowStore.getState().reset();
  });

  it("should start with empty initial state", () => {
    // given
    const state = useSendFlowStore.getState();

    // then
    expect(state.amountUsdc).toBe("");
    expect(state.recipientPhone).toBe("");
    expect(state.recipientName).toBe("");
    expect(state.fxRate).toBeNull();
    expect(state.fxRateExpiresAt).toBeNull();
  });

  it("should set amount", () => {
    // when
    useSendFlowStore.getState().setAmount("100.50");

    // then
    expect(useSendFlowStore.getState().amountUsdc).toBe("100.50");
  });

  it("should set recipient with name", () => {
    // when
    useSendFlowStore.getState().setRecipient("+919876543210", "Raj Patel");

    // then
    expect(useSendFlowStore.getState().recipientPhone).toBe("+919876543210");
    expect(useSendFlowStore.getState().recipientName).toBe("Raj Patel");
  });

  it("should set recipient without name", () => {
    // when
    useSendFlowStore.getState().setRecipient("+919876543210");

    // then
    expect(useSendFlowStore.getState().recipientPhone).toBe("+919876543210");
    expect(useSendFlowStore.getState().recipientName).toBe("");
  });

  it("should set fx rate with expiry", () => {
    // given
    const expiresAt = "2026-04-24T12:00:00Z";

    // when
    useSendFlowStore.getState().setFxRate("84.50", expiresAt);

    // then
    expect(useSendFlowStore.getState().fxRate).toBe("84.50");
    expect(useSendFlowStore.getState().fxRateExpiresAt).toBe(expiresAt);
  });

  it("should reset all state to initial values", () => {
    // given
    useSendFlowStore.getState().setAmount("250");
    useSendFlowStore.getState().setRecipient("+919876543210", "Raj");
    useSendFlowStore.getState().setFxRate("84.50", "2026-04-24T12:00:00Z");

    // when
    useSendFlowStore.getState().reset();

    // then
    const state = useSendFlowStore.getState();
    expect(state.amountUsdc).toBe("");
    expect(state.recipientPhone).toBe("");
    expect(state.recipientName).toBe("");
    expect(state.fxRate).toBeNull();
    expect(state.fxRateExpiresAt).toBeNull();
  });
});
