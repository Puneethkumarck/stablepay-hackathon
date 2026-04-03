package p2p

type InboundMessage struct {
	CeremonyID  string
	FromPartyID int
	ToPartyID   int
	Payload     []byte
	IsBroadcast bool
}
