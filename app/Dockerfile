# syntax=docker/dockerfile:1

# Web client: build the Vite bundle with bun, then serve the static output via nginx.
# Build context is ./app (see docker-compose.yml).

# ---- Stage 1: build the static production bundle ----
FROM oven/bun:1-alpine AS build
WORKDIR /build
# Install deps first for better layer caching.
COPY package.json bun.lock ./
RUN --mount=type=cache,target=/root/.bun \
    bun install --frozen-lockfile
COPY . .
RUN bun run build

# ---- Stage 2: serve with nginx ----
FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
