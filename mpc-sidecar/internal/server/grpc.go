package server

import (
	"context"
	"fmt"
	"net"

	pb "github.com/stablepay/mpc-sidecar/gen/sidecar/v1"
	"github.com/stablepay/mpc-sidecar/internal/p2p"
	tsswrap "github.com/stablepay/mpc-sidecar/internal/tss"
	"google.golang.org/grpc"
)

type GRPCServer struct {
	pb.UnimplementedTssSidecarServer
	port     int
	p2p      *p2p.Client
	registry *p2p.CeremonyRegistry
	server   *grpc.Server
}

func NewGRPCServer(port int, p2pClient *p2p.Client, registry *p2p.CeremonyRegistry) *GRPCServer {
	return &GRPCServer{
		port:     port,
		p2p:      p2pClient,
		registry: registry,
	}
}

func (s *GRPCServer) Start() error {
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", s.port))
	if err != nil {
		return fmt.Errorf("failed to listen on port %d: %w", s.port, err)
	}

	s.server = grpc.NewServer()
	pb.RegisterTssSidecarServer(s.server, s)

	fmt.Printf("[gRPC] Server listening on port %d\n", s.port)
	return s.server.Serve(lis)
}

func (s *GRPCServer) Stop() {
	if s.server != nil {
		s.server.GracefulStop()
	}
}

func (s *GRPCServer) GenerateKey(ctx context.Context, req *pb.GenerateKeyRequest) (*pb.GenerateKeyResponse, error) {
	ceremonyID := req.CeremonyId
	inCh := s.registry.Register(ceremonyID)
	defer s.registry.Unregister(ceremonyID)

	result, err := tsswrap.RunDkg(
		ctx,
		ceremonyID,
		int(req.PartyId),
		int(req.Threshold),
		int(req.TotalParties),
		req.PeerAddresses,
		s.p2p,
		inCh,
	)
	if err != nil {
		return &pb.GenerateKeyResponse{
			CeremonyId:   ceremonyID,
			Status:       pb.Status_STATUS_FAILED,
			ErrorMessage: err.Error(),
		}, nil
	}

	return &pb.GenerateKeyResponse{
		CeremonyId:   ceremonyID,
		SolanaAddress: result.SolanaAddress,
		PublicKey:     result.PublicKey,
		KeyShareData:  result.KeyShareData,
		Status:       pb.Status_STATUS_COMPLETED,
	}, nil
}

func (s *GRPCServer) Sign(ctx context.Context, req *pb.SignRequest) (*pb.SignResponse, error) {
	ceremonyID := req.CeremonyId
	inCh := s.registry.Register(ceremonyID)
	defer s.registry.Unregister(ceremonyID)

	signingPartyIDs := make([]int, len(req.SigningPartyIds))
	for i, id := range req.SigningPartyIds {
		signingPartyIDs[i] = int(id)
	}

	result, err := tsswrap.RunSigning(
		ctx,
		ceremonyID,
		int(req.PartyId),
		int(req.Threshold),
		signingPartyIDs,
		req.KeyShareData,
		req.Message,
		req.PeerAddresses,
		s.p2p,
		inCh,
	)
	if err != nil {
		return &pb.SignResponse{
			CeremonyId:   ceremonyID,
			Status:       pb.Status_STATUS_FAILED,
			ErrorMessage: err.Error(),
		}, nil
	}

	return &pb.SignResponse{
		CeremonyId: ceremonyID,
		Signature:  result.Signature,
		Status:     pb.Status_STATUS_COMPLETED,
	}, nil
}

func (s *GRPCServer) HealthCheck(_ context.Context, _ *pb.HealthCheckRequest) (*pb.HealthCheckResponse, error) {
	return &pb.HealthCheckResponse{Ready: true}, nil
}
