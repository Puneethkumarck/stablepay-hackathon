package tss

import (
	"context"
	"encoding/json"
	"fmt"
	"math/big"
	"time"

	"github.com/bnb-chain/tss-lib/v2/eddsa/keygen"
	"github.com/bnb-chain/tss-lib/v2/tss"
	"github.com/btcsuite/btcutil/base58"
	"github.com/decred/dcrd/dcrec/edwards/v2"
	"github.com/stablepay/mpc-sidecar/internal/p2p"
)

type DkgResult struct {
	SolanaAddress string
	PublicKey     []byte
	KeyShareData  []byte
}

func RunDkg(
	ctx context.Context,
	ceremonyID string,
	partyID int,
	threshold int,
	totalParties int,
	peerAddresses map[int32]string,
	p2pClient *p2p.Client,
	inCh chan *p2p.InboundMessage,
) (*DkgResult, error) {
	// Build party IDs (0-indexed, converted to 1-based BigInt internally by tss-lib)
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

	// Find our own party in the sorted list
	var selfPartyID *tss.PartyID
	for _, pid := range sortedIDs {
		if pid.KeyInt().Int64() == int64(partyID+1) {
			selfPartyID = pid
			break
		}
	}
	if selfPartyID == nil {
		return nil, fmt.Errorf("party ID %d not found in party list", partyID)
	}

	// Create peer context and parameters — use Edwards curve for Ed25519
	peerCtx := tss.NewPeerContext(sortedIDs)
	// tss-lib threshold convention: t means t+1 parties needed to sign
	params := tss.NewParameters(tss.Edwards(), peerCtx, selfPartyID, totalParties, threshold-1)

	outCh := make(chan tss.Message, totalParties*10)
	endCh := make(chan *keygen.LocalPartySaveData, 1)

	// No pre-params needed for EdDSA (unlike ECDSA which needs Paillier keys)
	party := keygen.NewLocalParty(params, outCh, endCh)

	go func() {
		if err := party.Start(); err != nil {
			fmt.Printf("[DKG] Error starting party: %v\n", err)
		}
	}()

	timeout := time.After(5 * time.Minute)

	for {
		select {
		case <-ctx.Done():
			return nil, ctx.Err()

		case <-timeout:
			return nil, fmt.Errorf("DKG ceremony timed out after 5 minutes")

		case msg := <-outCh:
			// Send outbound TSS message to peers via P2P
			if err := p2pClient.SendToParty(ctx, ceremonyID, partyID, msg, peerAddresses); err != nil {
				return nil, fmt.Errorf("failed to send P2P message: %w", err)
			}

		case inMsg := <-inCh:
			// Route inbound message to the TSS party
			parsed, err := tss.ParseWireMessage(inMsg.Payload, sortedIDs[inMsg.FromPartyID], inMsg.IsBroadcast)
			if err != nil {
				return nil, fmt.Errorf("failed to parse wire message: %w", err)
			}
			ok, tssErr := party.Update(parsed)
			if tssErr != nil {
				return nil, fmt.Errorf("TSS update error: %v", tssErr)
			}
			if !ok {
				return nil, fmt.Errorf("TSS update returned false")
			}

		case saveData := <-endCh:
			return finalizeDkg(saveData)
		}
	}
}

func finalizeDkg(saveData *keygen.LocalPartySaveData) (*DkgResult, error) {
	// Extract the Ed25519 public key from DKG output
	publicKey := saveData.EDDSAPub
	pk := edwards.PublicKey{
		Curve: tss.Edwards(),
		X:     publicKey.X(),
		Y:     publicKey.Y(),
	}

	// Serialize the public key (compressed format, 33 bytes for Edwards)
	pubKeyBytes := pk.SerializeCompressed()

	// Solana address = base58 encode of raw 32-byte Ed25519 public key
	// The compressed Edwards key is 33 bytes (prefix + 32 bytes X coord)
	// For Solana, we need the raw 32-byte representation
	rawPubKey := pk.Serialize()
	// Edwards Serialize() returns 32 bytes (just the Y coordinate with sign bit)
	solanaAddress := base58.Encode(rawPubKey)

	// Serialize key shares for storage
	keyShareData, err := json.Marshal(saveData)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal key share data: %w", err)
	}

	return &DkgResult{
		SolanaAddress: solanaAddress,
		PublicKey:     pubKeyBytes,
		KeyShareData:  keyShareData,
	}, nil
}
