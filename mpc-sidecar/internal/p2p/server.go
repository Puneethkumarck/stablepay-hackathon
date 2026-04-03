package p2p

import (
	"context"
	"fmt"
	"net"

	pb "github.com/stablepay/mpc-sidecar/gen/sidecar/v1"
	"google.golang.org/grpc"
)

type Server struct {
	pb.UnimplementedP2PTransportServer
	registry *CeremonyRegistry
	port     int
	server   *grpc.Server
}

func NewServer(registry *CeremonyRegistry, port int) *Server {
	return &Server{
		registry: registry,
		port:     port,
	}
}

func (s *Server) Start() error {
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", s.port))
	if err != nil {
		return fmt.Errorf("failed to listen on port %d: %w", s.port, err)
	}

	s.server = grpc.NewServer()
	pb.RegisterP2PTransportServer(s.server, s)

	fmt.Printf("[P2P] Server listening on port %d\n", s.port)
	return s.server.Serve(lis)
}

func (s *Server) Stop() {
	if s.server != nil {
		s.server.GracefulStop()
	}
}

func (s *Server) SendMessage(_ context.Context, msg *pb.P2PMessage) (*pb.P2PAck, error) {
	s.registry.Deliver(&InboundMessage{
		CeremonyID:  msg.CeremonyId,
		FromPartyID: int(msg.FromPartyId),
		ToPartyID:   int(msg.ToPartyId),
		Payload:     msg.Payload,
		IsBroadcast: msg.IsBroadcast,
	})

	return &pb.P2PAck{Accepted: true}, nil
}
