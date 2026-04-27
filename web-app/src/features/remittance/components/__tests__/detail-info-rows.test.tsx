import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test-utils/render";
import { DetailInfoRows } from "../detail-info-rows";

describe("DetailInfoRows", () => {
  it("should display truncated remittance ID with copy button", () => {
    // given
    // when
    renderWithProviders(
      <DetailInfoRows
        remittanceId="8ce3abcd-1234-5678-9012-dc2a3456bcde"
        escrowPda="7C2zAbCdEfGhIjKlMnOpQrStUvWxYzWij"
        fxRate="84.50"
      />,
    );

    // then
    expect(screen.getByText("8ce3a…6bcde")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /^Copy / })).toHaveLength(2);
  });

  it("should render copy buttons for remittance ID and escrow PDA", async () => {
    // given
    const user = userEvent.setup();
    renderWithProviders(
      <DetailInfoRows
        remittanceId="8ce3abcd-1234-5678-9012-dc2a3456bcde"
        escrowPda="7C2zAbCdEfGhIjKlMnOpQrStUvWxYzWij"
        fxRate="84.50"
      />,
    );

    // when
    const buttons = screen.getAllByRole("button", { name: /^Copy / });
    await user.click(buttons[0]!);

    // then
    expect(buttons).toHaveLength(2);
  });

  it("should display truncated escrow PDA", () => {
    // given
    // when
    renderWithProviders(
      <DetailInfoRows
        remittanceId="8ce3abcd-1234-5678-9012-dc2a3456bcde"
        escrowPda="7C2zAbCdEfGhIjKlMnOpQrStUvWxYzWij"
        fxRate="84.50"
      />,
    );

    // then
    expect(screen.getByText("7C2zA…YzWij")).toBeInTheDocument();
  });

  it("should display on-chain fee and FX rate", () => {
    // given
    // when
    renderWithProviders(
      <DetailInfoRows remittanceId="rem-id" escrowPda="pda-addr" fxRate="84.50" />,
    );

    // then
    expect(screen.getByText("$0.002")).toBeInTheDocument();
    expect(screen.getByText("84.50")).toBeInTheDocument();
  });

  it("should display all four row labels", () => {
    // given
    // when
    renderWithProviders(
      <DetailInfoRows remittanceId="rem-id" escrowPda="pda-addr" fxRate="84.50" />,
    );

    // then
    expect(screen.getByText("Remittance ID")).toBeInTheDocument();
    expect(screen.getByText("Escrow PDA")).toBeInTheDocument();
    expect(screen.getByText("On-chain fee")).toBeInTheDocument();
    expect(screen.getByText("FX rate")).toBeInTheDocument();
  });
});
