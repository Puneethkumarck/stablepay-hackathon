import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { describe, expect, it, vi } from "vitest";
import { useSendFlowStore } from "@/stores/send-flow";
import { server } from "@/test-utils/msw-server";
import { renderWithProviders } from "@/test-utils/render";
import type { RecentRecipient } from "@/types/api";
import SendRecipientPage from "../page";

const mockPush = vi.fn();
const mockReplace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
}));

const RECENT_RECIPIENTS: RecentRecipient[] = [
  { name: "Raj Patel", phone: "+91 98765 43210", lastSentAt: "2026-04-25T10:00:00Z" },
  { name: "Meera Iyer", phone: "+91 99887 66554", lastSentAt: "2026-04-24T10:00:00Z" },
];

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

  it("should render phone and name inputs when amount is set", () => {
    // given
    useSendFlowStore.getState().setAmount("100");

    // when
    renderWithProviders(<SendRecipientPage />);

    // then
    expect(screen.getByLabelText("Recipient phone number")).toBeInTheDocument();
    expect(screen.getByLabelText("Recipient name")).toBeInTheDocument();
  });

  it("should show recent recipients from API", async () => {
    // given
    useSendFlowStore.getState().setAmount("100");
    server.use(
      http.get("/api/recipients/recent", () => HttpResponse.json(RECENT_RECIPIENTS)),
    );

    // when
    renderWithProviders(<SendRecipientPage />);

    // then
    await waitFor(() => {
      expect(screen.getByText("Raj Patel")).toBeInTheDocument();
    });
    expect(screen.getByText("Meera Iyer")).toBeInTheDocument();
  });

  it("should not show recent section when no recipients", () => {
    // given
    useSendFlowStore.getState().setAmount("100");
    server.use(http.get("/api/recipients/recent", () => HttpResponse.json([])));

    // when
    renderWithProviders(<SendRecipientPage />);

    // then
    expect(screen.queryByText("Recent")).not.toBeInTheDocument();
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

  it("should store phone and name and navigate to review on continue", async () => {
    // given
    useSendFlowStore.getState().setAmount("100");
    const user = userEvent.setup();
    renderWithProviders(<SendRecipientPage />);

    // when
    await user.type(screen.getByLabelText("Recipient name"), "Test User");
    await user.type(screen.getByLabelText("Recipient phone number"), "+919876543210");
    await user.click(screen.getByRole("button", { name: "Continue" }));

    // then
    expect(useSendFlowStore.getState().recipientPhone).toBe("+919876543210");
    expect(useSendFlowStore.getState().recipientName).toBe("Test User");
    expect(mockPush).toHaveBeenCalledWith("/send/review");
  });

  it("should fill phone and name and navigate when tapping a recent contact", async () => {
    // given
    useSendFlowStore.getState().setAmount("100");
    server.use(
      http.get("/api/recipients/recent", () => HttpResponse.json(RECENT_RECIPIENTS)),
    );
    const user = userEvent.setup();
    renderWithProviders(<SendRecipientPage />);
    await waitFor(() => {
      expect(screen.getByText("Raj Patel")).toBeInTheDocument();
    });

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
});
