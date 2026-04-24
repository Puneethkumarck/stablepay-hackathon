import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { useSendFlowStore } from "@/stores/send-flow";
import { renderWithProviders } from "@/test-utils/render";
import SendRecipientPage from "../page";

const mockPush = vi.fn();
const mockReplace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
}));

describe("SendRecipientPage", () => {
  beforeEach(() => {
    mockPush.mockClear();
    mockReplace.mockClear();
    useSendFlowStore.getState().reset();
  });

  it("should redirect to /send when amount is not set", () => {
    // given
    renderWithProviders(<SendRecipientPage />);

    // then
    expect(mockReplace).toHaveBeenCalledWith("/send");
  });

  it("should render phone input and recent contacts when amount is set", () => {
    // given
    useSendFlowStore.getState().setAmount("100");

    // when
    renderWithProviders(<SendRecipientPage />);

    // then
    expect(screen.getByLabelText("Recipient phone number")).toBeInTheDocument();
    expect(screen.getByText("Raj Patel")).toBeInTheDocument();
    expect(screen.getByText("Meera Iyer")).toBeInTheDocument();
    expect(screen.getByText("Vikram Shah")).toBeInTheDocument();
    expect(screen.getByText("Ananya Rao")).toBeInTheDocument();
  });

  it("should show step indicator", () => {
    // given
    useSendFlowStore.getState().setAmount("100");

    // when
    renderWithProviders(<SendRecipientPage />);

    // then
    expect(screen.getByText("Step 2 of 3")).toBeInTheDocument();
  });

  it("should disable continue button when phone is empty", () => {
    // given
    useSendFlowStore.getState().setAmount("100");

    // when
    renderWithProviders(<SendRecipientPage />);

    // then
    expect(screen.getByRole("button", { name: "Continue" })).toBeDisabled();
  });

  it("should enable continue button when phone is entered", async () => {
    // given
    useSendFlowStore.getState().setAmount("100");
    const user = userEvent.setup();
    renderWithProviders(<SendRecipientPage />);

    // when
    await user.type(screen.getByLabelText("Recipient phone number"), "+919876543210");

    // then
    expect(screen.getByRole("button", { name: "Continue" })).toBeEnabled();
  });

  it("should store phone and navigate to review on continue", async () => {
    // given
    useSendFlowStore.getState().setAmount("100");
    const user = userEvent.setup();
    renderWithProviders(<SendRecipientPage />);

    // when
    await user.type(screen.getByLabelText("Recipient phone number"), "+919876543210");
    await user.click(screen.getByRole("button", { name: "Continue" }));

    // then
    expect(useSendFlowStore.getState().recipientPhone).toBe("+919876543210");
    expect(mockPush).toHaveBeenCalledWith("/send/review");
  });

  it("should fill phone and navigate to review when tapping a contact", async () => {
    // given
    useSendFlowStore.getState().setAmount("100");
    const user = userEvent.setup();
    renderWithProviders(<SendRecipientPage />);

    // when
    await user.click(screen.getByText("Raj Patel"));

    // then
    expect(useSendFlowStore.getState().recipientPhone).toBe("+91 98765 43210");
    expect(useSendFlowStore.getState().recipientName).toBe("Raj Patel");
    expect(mockPush).toHaveBeenCalledWith("/send/review");
  });

  it("should navigate to /send on back", async () => {
    // given
    useSendFlowStore.getState().setAmount("100");
    const user = userEvent.setup();
    renderWithProviders(<SendRecipientPage />);

    // when
    await user.click(screen.getByRole("button", { name: "Go back" }));

    // then
    expect(mockPush).toHaveBeenCalledWith("/send");
  });

  it("should show avatar initials for contacts", () => {
    // given
    useSendFlowStore.getState().setAmount("100");

    // when
    renderWithProviders(<SendRecipientPage />);

    // then
    expect(screen.getByText("RP")).toBeInTheDocument();
    expect(screen.getByText("MI")).toBeInTheDocument();
    expect(screen.getByText("VS")).toBeInTheDocument();
    expect(screen.getByText("AR")).toBeInTheDocument();
  });

  it("should show phone numbers for contacts", () => {
    // given
    useSendFlowStore.getState().setAmount("100");

    // when
    renderWithProviders(<SendRecipientPage />);

    // then
    expect(screen.getByText("+91 98765 43210")).toBeInTheDocument();
    expect(screen.getByText("+91 99887 66554")).toBeInTheDocument();
    expect(screen.getByText("+91 99001 23456")).toBeInTheDocument();
    expect(screen.getByText("+91 90000 12345")).toBeInTheDocument();
  });
});
