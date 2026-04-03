package main

import (
	"fmt"
	"os"
	"os/signal"
	"syscall"

	"github.com/stablepay/mpc-sidecar/internal/config"
	"github.com/stablepay/mpc-sidecar/internal/p2p"
	"github.com/stablepay/mpc-sidecar/internal/server"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to load config: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("[MAIN] Starting MPC sidecar: partyID=%d, gRPC=%d, P2P=%d\n",
		cfg.PartyID, cfg.GRPCPort, cfg.P2PPort)

	registry := p2p.NewCeremonyRegistry()
	p2pClient := p2p.NewClient()
	defer p2pClient.Close()

	// Start P2P server (receives round messages from peers)
	p2pServer := p2p.NewServer(registry, cfg.P2PPort)
	go func() {
		if err := p2pServer.Start(); err != nil {
			fmt.Fprintf(os.Stderr, "[P2P] Server error: %v\n", err)
			os.Exit(1)
		}
	}()

	// Start gRPC server (receives requests from Java backend)
	grpcServer := server.NewGRPCServer(cfg.GRPCPort, p2pClient, registry)
	go func() {
		if err := grpcServer.Start(); err != nil {
			fmt.Fprintf(os.Stderr, "[gRPC] Server error: %v\n", err)
			os.Exit(1)
		}
	}()

	// Wait for shutdown signal
	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	<-sigCh

	fmt.Println("[MAIN] Shutting down...")
	grpcServer.Stop()
	p2pServer.Stop()
}
