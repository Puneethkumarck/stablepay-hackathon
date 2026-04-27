.PHONY: up down restart restart-clean logs ps infra urls clean build-backend help stripe-listen

COMPOSE := docker compose
STRIPE_LOG := /tmp/stablepay-stripe-listen.log
STRIPE_PID := /tmp/stablepay-stripe-listen.pid

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build-backend: ## Build backend JAR (required before 'make up')
	@echo "Building backend JAR..."
	@cd backend && ./gradlew assemble -q

define start_stripe
	@echo "Starting Stripe webhook listener..."
	@if command -v stripe >/dev/null 2>&1; then \
		if [ -f $(STRIPE_PID) ] && kill -0 $$(cat $(STRIPE_PID)) 2>/dev/null; then \
			echo "  Stripe listener already running (PID $$(cat $(STRIPE_PID)))"; \
		else \
			nohup stripe listen --forward-to localhost:8080/webhooks/stripe > $(STRIPE_LOG) 2>&1 & \
			echo $$! > $(STRIPE_PID); \
			echo "  Stripe listener PID: $$!"; \
			echo "  Stripe logs: $(STRIPE_LOG)"; \
		fi; \
	else \
		echo "  ⚠ stripe CLI not found — install it or run 'make stripe-listen' separately"; \
	fi
endef

define stop_stripe
	@if [ -f $(STRIPE_PID) ] && kill -0 $$(cat $(STRIPE_PID)) 2>/dev/null; then \
		kill $$(cat $(STRIPE_PID)) && echo "Stripe listener stopped (PID $$(cat $(STRIPE_PID)))."; \
		rm -f $(STRIPE_PID); \
	else \
		rm -f $(STRIPE_PID); \
		echo "No Stripe listener running."; \
	fi
endef

up: build-backend ## Start full stack + Stripe listener
	@echo "Starting StablePay stack..."
	@$(COMPOSE) up -d --build
	@echo ""
	@echo "Waiting for services to become healthy..."
	@$(COMPOSE) up -d --wait 2>/dev/null || true
	$(start_stripe)
	@$(MAKE) --no-print-directory urls

down: ## Stop all services + Stripe listener
	$(stop_stripe)
	@$(COMPOSE) down
	@echo "StablePay stack stopped."

restart: ## Restart app services only — preserves Postgres data
	$(stop_stripe)
	@echo "Rebuilding and restarting app services..."
	@$(COMPOSE) up -d --build --no-deps backend web-app
	@echo ""
	@echo "Waiting for services to become healthy..."
	@$(COMPOSE) up -d --wait 2>/dev/null || true
	$(start_stripe)
	@$(MAKE) --no-print-directory urls

restart-clean: ## Restart full stack from scratch (wipes all data)
	$(stop_stripe)
	@$(COMPOSE) down -v
	@echo "Volumes removed. Starting fresh..."
	@$(MAKE) --no-print-directory up

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
	@echo "  Web App:        http://localhost:3000"
	@echo "  Backend API:    http://localhost:8080"
	@echo "  Swagger UI:     http://localhost:8080/swagger-ui.html"
	@echo "  Health:         http://localhost:8080/actuator/health"
	@echo "  Temporal UI:    http://localhost:8088"
	@echo "  PostgreSQL:     localhost:5432"
	@echo "  Redis:          localhost:6379"
	@echo "  MPC Sidecar 0:  localhost:50051 (gRPC)"
	@echo "  MPC Sidecar 1:  localhost:50052 (gRPC)"
	@echo "  Stripe logs:    $(STRIPE_LOG)"
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
