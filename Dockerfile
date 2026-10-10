# syntax=docker/dockerfile:1.6
FROM ubuntu:24.04 AS base

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates zlib1g \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/data

EXPOSE 8000

FROM base AS branch-amd64
COPY --chmod=755 target/push-server-amd64 /app/push-server

FROM base AS branch-arm64
COPY --chmod=755 target/push-server-arm64 /app/push-server

FROM branch-${TARGETARCH} AS final
ENTRYPOINT ["/app/push-server"]
