package config

import (
	"fmt"
	"os"
	"strconv"
)

type Config struct {
	PartyID  int
	GRPCPort int
	P2PPort  int
}

func Load() (*Config, error) {
	partyID, err := intEnv("PARTY_ID", 0)
	if err != nil {
		return nil, fmt.Errorf("invalid PARTY_ID: %w", err)
	}
	grpcPort, _ := intEnv("GRPC_PORT", 50051)
	p2pPort, _ := intEnv("P2P_PORT", 7000)

	return &Config{
		PartyID:  partyID,
		GRPCPort: grpcPort,
		P2PPort:  p2pPort,
	}, nil
}

func intEnv(key string, defaultVal int) (int, error) {
	val := os.Getenv(key)
	if val == "" {
		return defaultVal, nil
	}
	return strconv.Atoi(val)
}
