# docker/

Builds this repo's `sv-improvements` branch into an image that's a drop-in
replacement for `erikvl87/languagetool:6.8`, which is what `lt.katalyst.now`
(CT 106/107 on terra, `/opt/languagetool/docker-compose.yml`) ran before this
existed.

## Why this exists instead of just using the stock image

The stock `erikvl87/languagetool` image only documents one lightweight
customization path (`spelling.txt`/`ignore.txt`/`prohibited.txt` via a custom
Dockerfile layer), and even that requires a rebuild. It doesn't support
overriding grammar rules, `segment.srx`, or the hunspell dictionary/affix
files without one. Since the Swedish improvements touch all of those, there
was no realistic way to stay on the stock image; building our own is
the smaller deviation from it, not a bigger one; still Docker, still Compose,
still covered by `update-stack.sh`'s snapshot/health-check/rollback cycle on
terra, just pointing at a different image.

## What's different from erikvl87/docker-languagetool

Only the source: this repo's own tree (via `COPY`) instead of an upstream
release tag. `start.sh` and `config.properties` are reused verbatim, so the
existing `docker-compose.yml`'s `langtool_languageModel`, `Java_Xms`,
`Java_Xmx` environment section and the `/ngrams` volume mount keep working
completely unchanged, only the `image:` line needs to change. ARM64
workarounds were dropped (terra is x86_64).

The build source is whatever's passed as the Docker build *context*, not
something the Dockerfile hardcodes:

```bash
# Local working tree (including uncommitted changes), for fast iteration:
docker build -f docker/Dockerfile -t languagetool-sv:local .

# A specific remote branch, no local checkout needed, e.g. to build
# directly on the LXC:
docker build -f docker/Dockerfile -t languagetool-sv:remote \
  'https://github.com/katalystnord/languagetool.git#sv-improvements'
```

An earlier version did `git clone` *inside* the Dockerfile instead. That
caused a real bug: BuildKit cached that `RUN` layer by command text, not by
the branch's actual current commit, so rebuilding after a new push silently
reused stale source, an image that looked freshly built but wasn't. Building
straight from the context doesn't have this failure mode; the context
itself is either the live local tree or freshly re-resolved from the remote
on every build. If a build ever looks suspiciously fast or an old bug
reappears after a fix was pushed, suspect stale cache and rebuild with
`--no-cache` before assuming the fix didn't work.

## Building and testing locally

```bash
docker build -f docker/Dockerfile -t languagetool-sv:local .
docker run --rm -p 8010:8010 languagetool-sv:local
curl --data 'language=sv&text=Det var en mörk kväll och ett mörk kväll.' \
  http://localhost:8010/v2/check
```

To speed up a from-scratch build using an already-warm local `~/.m2`, add
`--build-context hostm2=$HOME/.m2` (falls back to normal resolution for
anything not already cached).

## Deploying to terra

This changes a live service other things depend on (OnlyOffice integration
among them), so it's a deliberate, staged process, not a blind swap:

1. Build directly on the LXC (`david@192.168.50.16`) from the remote branch,
   no local checkout needed there:
   ```bash
   docker build -f docker/Dockerfile -t languagetool-sv:sv-improvements \
     'https://github.com/katalystnord/languagetool.git#sv-improvements'
   ```
2. Run it alongside the current production container, on a spare port
   (8011), and verify there before touching anything live:
   ```bash
   docker run -d --name languagetool-sv-staging -p 8011:8010 \
     -e langtool_languageModel=/ngrams -e Java_Xms=512m -e Java_Xmx=2g \
     -v /opt/languagetool/ngrams:/ngrams:ro --restart unless-stopped \
     languagetool-sv:sv-improvements
   curl --data 'language=sv&text=...' http://localhost:8011/v2/check
   ```
3. Cut over: stop and rename the current production container (kept, not
   removed, for rollback), then bring up the new one on port 8010, matching
   `docker-compose.yml`'s existing config exactly.
   ```bash
   docker stop languagetool && docker rename languagetool languagetool-<prev>-backup
   docker run -d --name languagetool --restart unless-stopped -p 8010:8010 \
     -e langtool_languageModel=/ngrams -e Java_Xms=512m -e Java_Xmx=2g \
     -v /opt/languagetool/ngrams:/ngrams:ro \
     --health-cmd "curl -sf --max-time 10 -d 'text=test' -d 'language=en-US' http://localhost:8010/v2/check >/dev/null || exit 1" \
     --health-interval 60s --health-timeout 15s --health-retries 3 \
     languagetool-sv:sv-improvements
   ```
4. Update `/opt/languagetool/docker-compose.yml` itself (`image:` line) so
   the nightly `update-stack.sh` automation doesn't see a mismatch, this
   needs `sudo` on that box. Verify end to end through
   `https://lt.katalyst.now` (through Caddy, exactly what OnlyOffice hits)
   before considering it done, not just against the container directly.
5. Remove the old staging container once the cutover is confirmed good.

Rollback, if ever needed: `docker rm -f languagetool && docker rename
languagetool-<prev>-backup languagetool && docker start languagetool`.

See `implementation-plan.md` in the notes repo for the full decision
history and what's actually been deployed so far.
