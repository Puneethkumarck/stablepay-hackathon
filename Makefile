.PHONY: up down restart logs ps infra urls clean build-backend help stripe-listen

COMPOSE := docker compose

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build-backend: ## Build backend JAR (required before 'make up')
	@echo "Building backend JAR..."
	@cd backend && ./gradlew assemble -q

up: build-backend ## Start the full stack (build + docker compose up)
	@echo "Starting StablePay stack..."
	@$(COMPOSE) up -d --build
	@echo ""
	@echo "Waiting for services to become healthy..."
	@$(COMPOSE) up -d --wait 2>/dev/null || true
	@$(MAKE) --no-print-directory urls

down: ## Stop all services
	@$(COMPOSE) down
	@echo "StablePay stack stopped."

restart: down up ## Restart the full stack

logs: ## Follow logs from all services
	@$(COMPOSE) logs -f

ps: ## Show running services
	@$(COMPOSE) ps

infra: ## Start infrastructure only (for local backend dev via ./gradlew bootRun)
	@echo "Starting infrastructure services..."
	@$(COMPOSE) up -d postgres redis temporal temporal-ui mpc-sidecar-0 mpc-sidecar-1
	@echo ""
	@echo "Waiting for services..."
	@sleep 5
	@$(MAKE) --no-print-directory urls-infra
	@echo ""
	@echo "Run the backend locally:"
	@echo "  cd backend && ./gradlew bootRun"

urls: ## Print all service URLs
	@echo ""
	@echo "============================================"
	@echo "  StablePay Dev Stack"
	@echo "============================================"
	@echo "  Backend API:    http://localhost:8080"
	@echo "  Swagger UI:     http://localhost:8080/swagger-ui.html"
	@echo "  Health:         http://localhost:8080/actuator/health"
	@echo "  Temporal UI:    http://localhost:8088"
	@echo "  PostgreSQL:     localhost:5432"
	@echo "  Redis:          localhost:6379"
	@echo "  MPC Sidecar 0:  localhost:50051 (gRPC)"
	@echo "  MPC Sidecar 1:  localhost:50052 (gRPC)"
	@echo "============================================"
	@echo ""

urls-infra: ## Print infrastructure URLs (no backend)
	@echo ""
	@echo "============================================"
	@echo "  StablePay Infrastructure"
	@echo "============================================"
	@echo "  Temporal UI:    http://localhost:8088"
	@echo "  PostgreSQL:     localhost:5432"
	@echo "  Redis:          localhost:6379"
	@echo "  MPC Sidecar 0:  localhost:50051 (gRPC)"
	@echo "  MPC Sidecar 1:  localhost:50052 (gRPC)"
	@echo "============================================"
	@echo ""

e2e: ## Run E2E API tests via Newman (requires running stack)
	@cd e2e-tests && npm install --silent && npm test

e2e-ci: ## Run E2E tests with JUnit output for CI
	@cd e2e-tests && npm install --silent && npm run test:ci

e2e-full: up ## Start stack + wait + run E2E tests
	@cd e2e-tests && npm install --silent && npm run wait-and-test

clean: ## Stop all services and remove volumes
	@$(COMPOSE) down -v
	@echo "StablePay stack stopped and volumes removed."

stripe-listen: ## Forward Stripe webhook events to the local backend (requires `stripe` CLI)
	stripe listen --forward-to localhost:8080/webhooks/stripe
