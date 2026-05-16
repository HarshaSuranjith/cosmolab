# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

RUN corepack enable && corepack prepare pnpm@10 --activate

WORKDIR /app

COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm run build:prod

# ── Stage 2: serve ────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine

COPY --from=builder /app/dist/cosmolab-frontend /usr/share/nginx/html
# nginx image runs envsubst on /etc/nginx/templates/*.template at startup
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Default backend URL — override at runtime via docker-compose environment
ENV API_BASE_URL=http://cosmolab-backend:8080
# Limit substitution to this variable so nginx vars ($host, $uri, etc.) are untouched
ENV NGINX_ENVSUBST_TEMPLATE_VARS=API_BASE_URL

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
