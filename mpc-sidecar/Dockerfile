# Stage 1: Build
FROM golang:1.26-alpine AS builder

RUN apk add --no-cache git

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /mpc-sidecar ./cmd/sidecar

# Stage 2: Runtime
FROM alpine:3.21

RUN apk add --no-cache ca-certificates

COPY --from=builder /mpc-sidecar /usr/local/bin/mpc-sidecar

EXPOSE 50051 7000

ENTRYPOINT ["mpc-sidecar"]
