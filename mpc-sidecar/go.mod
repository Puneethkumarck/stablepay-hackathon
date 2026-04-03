module github.com/stablepay/mpc-sidecar

go 1.26

require (
	github.com/bnb-chain/tss-lib/v2 v2.0.2
	github.com/btcsuite/btcutil v1.0.2
	github.com/decred/dcrd/dcrec/edwards/v2 v2.0.4
	google.golang.org/grpc v1.80.0
	google.golang.org/protobuf v1.36.11
)

replace github.com/agl/ed25519 => github.com/binance-chain/edwards25519 v0.0.0-20200305024217-f36fc4b53d43

replace github.com/bnb-chain/tss-lib/v2 => github.com/fystack/tss-lib/v2 v2.0.3
