# Mehrstufiger Build: SvelteKit-SPA erzeugen und mit nginx ausliefern.
FROM node:22-alpine AS build
WORKDIR /app

# pnpm über Corepack, passend zur packageManager-Angabe.
RUN corepack enable

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .

# Statisches SPA für nginx, API kommt zur Laufzeit über denselben Ursprung.
ENV ADAPTER=static
RUN pnpm build

FROM nginx:1.27-alpine
# Reverse-Proxy für /api und SPA-Fallback.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html
# Leere API-Basis-URL: Browser spricht denselben Host an, nginx leitet /api weiter.
COPY docker/api-config.json /usr/share/nginx/html/api-config.json
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
	CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
