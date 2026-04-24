import { act, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "@/test-utils/render";
import SendingSendingPage from "../page";

const mockPush = vi.fn();
const mockReplace = vi.fn();

let mockSearchParams = new URLSearchParams();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
  useSearchParams: () => mockSearchParams,
}));

function setSearchParams(params: Record<string, string>) {
  mockSearchParams = new URLSearchParams(params);
}

const DEFAULT_PARAMS = {
  remittanceId: "rem-uuid-123",
  amount: "100",
  phone: "+919876543210",
  fxRate: "84.50",
};

describe("SendingSendingPage", () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    mockPush.mockClear();
    mockReplace.mockClear();
    mockSearchParams = new URLSearchParams();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("should redirect to /home when remittanceId is missing", () => {
    // given
    setSearchParams({});

    // when
    renderWithProviders(<SendingSendingPage />);

    // then
    expect(mockReplace).toHaveBeenCalledWith("/home");
  });

  it("should show initial sending state with all steps pending", () => {
    // given
    setSearchParams(DEFAULT_PARAMS);

    // when
    renderWithProviders(<SendingSendingPage />);

    // then
    expect(screen.getByText("Sending…")).toBeInTheDocument();
    expect(screen.getByText("Sending")).toBeInTheDocument();
    expect(screen.getByText("Authorising transfer")).toBeInTheDocument();
    expect(screen.getByText("Locking funds")).toBeInTheDocument();
    expect(screen.getByText("Notifying recipient")).toBeInTheDocument();
  });

  it("should display amount and recipient info", () => {
    // given
    setSearchParams(DEFAULT_PARAMS);

    // when
    renderWithProviders(<SendingSendingPage />);

    // then
    expect(screen.getByText("100.00")).toBeInTheDocument();
    expect(screen.getByText(/to \+919876543210 · ₹8,450\.00/)).toBeInTheDocument();
  });

  it("should show disabled Processing button during animation", () => {
    // given
    setSearchParams(DEFAULT_PARAMS);

    // when
    renderWithProviders(<SendingSendingPage />);

    // then
    expect(screen.getByRole("button", { name: "Processing…" })).toBeDisabled();
  });

  it("should progress through all three steps", () => {
    // given
    setSearchParams(DEFAULT_PARAMS);
    renderWithProviders(<SendingSendingPage />);

    // when
    act(() => vi.advanceTimersByTime(900));

    // then
    expect(screen.getByText("Sending…")).toBeInTheDocument();

    // when
    act(() => vi.advanceTimersByTime(900));

    // then
    expect(screen.getByText("Sending…")).toBeInTheDocument();

    // when
    act(() => vi.advanceTimersByTime(900));

    // then
    expect(screen.getByText("Transfer sent")).toBeInTheDocument();
  });

  it("should show completion state after all steps", () => {
    // given
    setSearchParams(DEFAULT_PARAMS);
    renderWithProviders(<SendingSendingPage />);

    // when
    act(() => vi.advanceTimersByTime(2700));

    // then
    expect(screen.getByText("Transfer sent")).toBeInTheDocument();
    expect(screen.getByText("Sent")).toBeInTheDocument();
    expect(screen.getByText("Sent — awaiting claim")).toBeInTheDocument();
    expect(screen.getByText("48h claim window")).toBeInTheDocument();
  });

  it("should show Done button after completion", () => {
    // given
    setSearchParams(DEFAULT_PARAMS);
    renderWithProviders(<SendingSendingPage />);

    // when
    act(() => vi.advanceTimersByTime(2700));

    // then
    expect(screen.getByRole("button", { name: "Done" })).toBeEnabled();
    expect(screen.queryByRole("button", { name: "Processing…" })).not.toBeInTheDocument();
  });

  it("should navigate to /home when Done is clicked", async () => {
    // given
    setSearchParams(DEFAULT_PARAMS);
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderWithProviders(<SendingSendingPage />);
    act(() => vi.advanceTimersByTime(2700));

    // when
    await user.click(screen.getByRole("button", { name: "Done" }));

    // then
    expect(mockPush).toHaveBeenCalledWith("/home");
  });

  it("should show claim link text with recipient phone", () => {
    // given
    setSearchParams(DEFAULT_PARAMS);

    // when
    renderWithProviders(<SendingSendingPage />);

    // then
    expect(screen.getByText("Claim link sent to +919876543210")).toBeInTheDocument();
  });

  it("should display step subtitles", () => {
    // given
    setSearchParams(DEFAULT_PARAMS);

    // when
    renderWithProviders(<SendingSendingPage />);

    // then
    expect(screen.getByText("Securely signing your transaction")).toBeInTheDocument();
    expect(screen.getByText("Held safely until recipient claims")).toBeInTheDocument();
  });
});
