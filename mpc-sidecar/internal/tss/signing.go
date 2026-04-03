package tss

import (
	"context"
	"encoding/json"
	"fmt"
	"math/big"
	"time"

	"github.com/bnb-chain/tss-lib/v2/common"
	"github.com/bnb-chain/tss-lib/v2/eddsa/keygen"
	"github.com/bnb-chain/tss-lib/v2/eddsa/signing"
	"github.com/bnb-chain/tss-lib/v2/tss"
	"github.com/decred/dcrd/dcrec/edwards/v2"
	"github.com/stablepay/mpc-sidecar/internal/p2p"
)

type SignResult struct {
	Signature []byte // 64-byte Ed25519 signature (R || S)
}

func RunSigning(
	ctx context.Context,
	ceremonyID string,
	partyID int,
	threshold int,
	signingPartyIDs []int,
	keyShareData []byte,
	message []byte,
	peerAddresses map[int32]string,
	p2pClient *p2p.Client,
	inCh chan *p2p.InboundMessage,
) (*SignResult, error) {
	// Deserialize key shares from DKG
	var saveData keygen.LocalPartySaveData
	if err := json.Unmarshal(keyShareData, &saveData); err != nil {
		return nil, fmt.Errorf("failed to unmarshal key share data: %w", err)
	}

	// Reconstruct party IDs from saved DKG data
	allPartyIDs := make(tss.UnSortedPartyIDs, len(saveData.Ks))
	for i, k := range saveData.Ks {
		allPartyIDs[i] = tss.NewPartyID(
			fmt.Sprintf("party-%d", i),
			fmt.Sprintf("Party %d", i),
			k,
		)
	}
	sortedAllIDs := tss.SortPartyIDs(allPartyIDs)

	// Select subset of parties for this signing ceremony
	signingSet := make(tss.UnSortedPartyIDs, 0, len(signingPartyIDs))
	for _, sid := range signingPartyIDs {
		for _, pid := range sortedAllIDs {
			if pid.KeyInt().Int64() == int64(sid+1) {
				signingSet = append(signingSet, pid)
				break
			}
		}
	}
	sortedSigningIDs := tss.SortPartyIDs(signingSet)

	// Find self in signing set
	var selfPartyID *tss.PartyID
	for _, pid := range sortedSigningIDs {
		if pid.KeyInt().Int64() == int64(partyID+1) {
			selfPartyID = pid
			break
		}
	}
	if selfPartyID == nil {
		return nil, fmt.Errorf("party ID %d not found in signing set", partyID)
	}

	// Create parameters for signing — Edwards curve
	peerCtx := tss.NewPeerContext(sortedSigningIDs)
	params := tss.NewParameters(tss.Edwards(), peerCtx, selfPartyID, len(sortedSigningIDs), threshold-1)

	// Convert message to big.Int for tss-lib signing
	msgBigInt := new(big.Int).SetBytes(message)

	outCh := make(chan tss.Message, len(sortedSigningIDs)*10)
	endCh := make(chan *common.SignatureData, 1)

	party := signing.NewLocalParty(msgBigInt, params, saveData, outCh, endCh)

	go func() {
		if err := party.Start(); err != nil {
			fmt.Printf("[SIGN] Error starting party: %v\n", err)
		}
	}()

	timeout := time.After(5 * time.Minute)

	for {
		select {
		case <-ctx.Done():
			return nil, ctx.Err()

		case <-timeout:
			return nil, fmt.Errorf("signing ceremony timed out after 5 minutes")

		case msg := <-outCh:
			if err := p2pClient.SendToParty(ctx, ceremonyID, partyID, msg, peerAddresses); err != nil {
				return nil, fmt.Errorf("failed to send P2P message: %w", err)
			}

		case inMsg := <-inCh:
			parsed, err := tss.ParseWireMessage(inMsg.Payload, sortedSigningIDs[inMsg.FromPartyID], inMsg.IsBroadcast)
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

		case sigData := <-endCh:
			return finalizeSigning(sigData, &saveData, message)
		}
	}
}

func finalizeSigning(sigData *common.SignatureData, saveData *keygen.LocalPartySaveData, message []byte) (*SignResult, error) {
	// Verify the signature before returning
	publicKey := saveData.EDDSAPub
	pk := edwards.PublicKey{
		Curve: tss.Edwards(),
		X:     publicKey.X(),
		Y:     publicKey.Y(),
	}

	r := new(big.Int).SetBytes(sigData.R)
	s := new(big.Int).SetBytes(sigData.S)

	ok := edwards.Verify(&pk, message, r, s)
	if !ok {
		return nil, fmt.Errorf("signature verification failed")
	}

	// Ed25519 signature = R (32 bytes) || S (32 bytes) = 64 bytes
	sig := sigData.Signature
	if len(sig) != 64 {
		// Pad/construct from R and S if Signature field isn't set
		rBytes := padTo32(sigData.R)
		sBytes := padTo32(sigData.S)
		sig = append(rBytes, sBytes...)
	}

	return &SignResult{
		Signature: sig,
	}, nil
}

func padTo32(b []byte) []byte {
	if len(b) >= 32 {
		return b[len(b)-32:]
	}
	padded := make([]byte, 32)
	copy(padded[32-len(b):], b)
	return padded
}
