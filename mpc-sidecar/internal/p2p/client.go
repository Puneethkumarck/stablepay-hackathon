package p2p

import (
	"context"
	"fmt"
	"sync"
	"time"

	"github.com/bnb-chain/tss-lib/v2/tss"
	pb "github.com/stablepay/mpc-sidecar/gen/sidecar/v1"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

type Client struct {
	mu      sync.RWMutex
	conns   map[string]*grpc.ClientConn
	clients map[string]pb.P2PTransportClient
}

func NewClient() *Client {
	return &Client{
		conns:   make(map[string]*grpc.ClientConn),
		clients: make(map[string]pb.P2PTransportClient),
	}
}

func (c *Client) SendToParty(
	ctx context.Context,
	ceremonyID string,
	fromPartyID int,
	msg tss.Message,
	peerAddresses map[int32]string,
) error {
	wireBytes, routing, err := msg.WireBytes()
	if err != nil {
		return fmt.Errorf("failed to get wire bytes: %w", err)
	}

	isBroadcast := routing.IsBroadcast

	if isBroadcast {
		// Send to all peers except self
		for peerID, addr := range peerAddresses {
			if int(peerID) == fromPartyID {
				continue
			}
			if err := c.sendMessage(ctx, addr, &pb.P2PMessage{
				CeremonyId:  ceremonyID,
				FromPartyId: int32(fromPartyID),
				ToPartyId:   peerID,
				Payload:     wireBytes,
				IsBroadcast: true,
			}); err != nil {
				return fmt.Errorf("broadcast to party %d failed: %w", peerID, err)
			}
		}
	} else {
		// Point-to-point: send to specific targets
		for _, to := range routing.To {
			// tss-lib uses 1-based keys internally
			toID := int32(to.KeyInt().Int64() - 1)
			addr, ok := peerAddresses[toID]
			if !ok {
				return fmt.Errorf("no address for party %d", toID)
			}
			if err := c.sendMessage(ctx, addr, &pb.P2PMessage{
				CeremonyId:  ceremonyID,
				FromPartyId: int32(fromPartyID),
				ToPartyId:   toID,
				Payload:     wireBytes,
				IsBroadcast: false,
			}); err != nil {
				return fmt.Errorf("send to party %d failed: %w", toID, err)
			}
		}
	}

	return nil
}

func (c *Client) sendMessage(ctx context.Context, addr string, msg *pb.P2PMessage) error {
	client, err := c.getOrCreateClient(addr)
	if err != nil {
		return err
	}

	sendCtx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()

	_, err = client.SendMessage(sendCtx, msg)
	return err
}

func (c *Client) getOrCreateClient(addr string) (pb.P2PTransportClient, error) {
	c.mu.RLock()
	client, ok := c.clients[addr]
	c.mu.RUnlock()
	if ok {
		return client, nil
	}

	c.mu.Lock()
	defer c.mu.Unlock()

	// Double-check after acquiring write lock
	if client, ok := c.clients[addr]; ok {
		return client, nil
	}

	conn, err := grpc.NewClient(addr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return nil, fmt.Errorf("failed to connect to %s: %w", addr, err)
	}

	client = pb.NewP2PTransportClient(conn)
	c.conns[addr] = conn
	c.clients[addr] = client

	return client, nil
}

func (c *Client) Close() {
	c.mu.Lock()
	defer c.mu.Unlock()
	for _, conn := range c.conns {
		conn.Close()
	}
}
