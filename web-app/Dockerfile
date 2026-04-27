FROM oven/bun:1 AS base
WORKDIR /app

FROM base AS deps
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

FROM base AS builder
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN mkdir -p public
ARG STABLEPAY_BACKEND_URL=http://backend:8080
ARG NEXT_PUBLIC_API_URL=http://localhost:3000
ARG NEXT_PUBLIC_GOOGLE_CLIENT_ID=placeholder
ENV STABLEPAY_BACKEND_URL=${STABLEPAY_BACKEND_URL}
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
ENV NEXT_PUBLIC_GOOGLE_CLIENT_ID=${NEXT_PUBLIC_GOOGLE_CLIENT_ID}
RUN if [ "$NEXT_PUBLIC_GOOGLE_CLIENT_ID" = "placeholder" ]; then \
      echo "ERROR: NEXT_PUBLIC_GOOGLE_CLIENT_ID is 'placeholder'. Set GOOGLE_CLIENT_ID in .env" >&2; \
      exit 1; \
    fi
RUN bun run build

FROM base AS runner
ENV NODE_ENV=production
RUN groupadd --system --gid 1001 appgroup && \
    useradd --system --uid 1001 --gid appgroup --no-create-home appuser
COPY --from=builder --chown=appuser:appgroup /app/.next/standalone ./
COPY --from=builder --chown=appuser:appgroup /app/.next/static ./app/.next/static
COPY --from=builder --chown=appuser:appgroup /app/public ./app/public
USER appuser
EXPOSE 3000
CMD ["bun", "app/server.js"]
