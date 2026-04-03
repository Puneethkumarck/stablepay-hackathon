package p2p

import (
	"sync"
)

// CeremonyRegistry routes inbound P2P messages to the correct ceremony goroutine.
// Messages arriving before the ceremony registers are buffered.
type CeremonyRegistry struct {
	mu       sync.RWMutex
	channels map[string]chan *InboundMessage
	buffers  map[string][]*InboundMessage
}

func NewCeremonyRegistry() *CeremonyRegistry {
	return &CeremonyRegistry{
		channels: make(map[string]chan *InboundMessage),
		buffers:  make(map[string][]*InboundMessage),
	}
}

// Register creates a channel for a ceremony and flushes any buffered messages.
func (r *CeremonyRegistry) Register(ceremonyID string) chan *InboundMessage {
	r.mu.Lock()
	defer r.mu.Unlock()

	ch := make(chan *InboundMessage, 100)
	r.channels[ceremonyID] = ch

	// Flush buffered messages
	if buffered, ok := r.buffers[ceremonyID]; ok {
		for _, msg := range buffered {
			ch <- msg
		}
		delete(r.buffers, ceremonyID)
	}

	return ch
}

// Unregister removes a ceremony channel.
func (r *CeremonyRegistry) Unregister(ceremonyID string) {
	r.mu.Lock()
	defer r.mu.Unlock()
	delete(r.channels, ceremonyID)
}

// Deliver routes a message to the ceremony channel, buffering if not yet registered.
func (r *CeremonyRegistry) Deliver(msg *InboundMessage) {
	r.mu.RLock()
	ch, ok := r.channels[msg.CeremonyID]
	r.mu.RUnlock()

	if ok {
		ch <- msg
		return
	}

	// Buffer for later
	r.mu.Lock()
	r.buffers[msg.CeremonyID] = append(r.buffers[msg.CeremonyID], msg)
	r.mu.Unlock()
}
