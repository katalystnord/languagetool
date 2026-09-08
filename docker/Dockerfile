# syntax=docker/dockerfile:1
#
# Builds this repo's own branch instead of an upstream release tag.
# Adapted from erikvl87/docker-languagetool
# (https://github.com/Erikvl87/docker-languagetool, LGPL-2.1), which is what
# lt.katalyst.now ran before this image existed. Kept close to that original
# (same runtime layout, same start.sh/config.properties mechanism) so
# docker-compose.yml needs no changes beyond the image itself, and so future
# upstream Dockerfile changes stay easy to diff against and pull in.
#
# Dropped: the ARM64 workarounds upstream carries (terra is x86_64) and the
# version ARG (we build a git ref, not a release tag).
#
# Builds from the Docker build context directly (COPY), not an internal
# `git clone`: pass a git URL as context to build a specific remote ref
# without transferring files (docker build -f docker/Dockerfile
# 'https://github.com/katalystnord/languagetool.git#sv-improvements'), or a
# local checkout to build uncommitted changes. An internal git clone was
# tried first and caused a real bug: BuildKit cached that RUN layer by
# command text, not by the remote branch's actual commit, so a rebuild
# after a new push silently reused stale source. COPY from the context
# doesn't have this problem, the context itself is freshly resolved (or
# is the live local tree) on every build.

FROM debian:bookworm AS build

ENV DEBIAN_FRONTEND=noninteractive

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update -y && \
    apt-get install -y --no-install-recommends \
        locales bash libgomp1 openjdk-17-jdk-headless maven unzip xmlstarlet

RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    dpkg-reconfigure --frontend=noninteractive locales && \
    update-locale LANG=en_US.UTF-8
ENV LANG=en_US.UTF-8

WORKDIR /languagetool
COPY . .

# Seeds the build cache from the host's own ~/.m2 (already warm from building
# this same project locally) instead of re-downloading everything; falls back
# to normal resolution for anything not already cached. Pass
# --build-context hostm2=$HOME/.m2 on `docker build` to use this; without it,
# the mount is simply empty and the build behaves as a normal cold cache.
#
# Deliberately skips the upstream Dockerfile's separate `dependency:go-offline`
# pre-step: that resolves every plugin bound anywhere in the POM, including
# ones (e.g. a security-scanning plugin) not actually needed for `package`,
# which pulls in a large legacy dependency tree for no benefit here, we don't
# need the Docker-layer-caching split it was for.
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=bind,from=hostm2,target=/root/.m2-seed,ro \
    cp -an /root/.m2-seed/. /root/.m2/ 2>/dev/null; \
    mvn --projects languagetool-standalone --also-make package -DskipTests --quiet

RUN unzip /languagetool/languagetool-standalone/target/LanguageTool-*.zip -d /dist && \
    mv /dist/LanguageTool-* /dist/LanguageTool

FROM alpine:3.24.0

RUN apk add --no-cache \
    bash curl fasttext libc6-compat libstdc++ openjdk17-jre-headless

RUN addgroup -S languagetool && adduser -S languagetool -G languagetool

COPY --chown=languagetool:languagetool --from=build /dist/LanguageTool /LanguageTool

WORKDIR /LanguageTool

RUN mkdir /nonexistent && touch /nonexistent/.languagetool.cfg

COPY --chown=languagetool:languagetool docker/start.sh docker/config.properties ./

RUN install -d -m 755 /fastText \
    && curl -L https://dl.fbaipublicfiles.com/fasttext/supervised-models/lid.176.bin -o /fastText/lid.176.bin \
    && chown -R languagetool:languagetool /fastText

RUN set -eux; \
    echo "fasttextModel=/fastText/lid.176.bin" >> config.properties; \
    echo "fasttextBinary=$(command -v fasttext)" >> config.properties; \
    chown languagetool:languagetool config.properties

USER languagetool

HEALTHCHECK --timeout=10s --start-period=5s CMD curl --fail --data "language=sv&text=Det var en enkel test." http://localhost:8010/v2/check || exit 1

CMD ["bash", "start.sh"]

EXPOSE 8010
