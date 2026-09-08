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

Only the source: `git clone` pulls `REPO`/`REF` (this fork's branch) instead
of an upstream release tag. `start.sh` and `config.properties` are reused
verbatim, so the existing `docker-compose.yml`'s `langtool_languageModel`,
`Java_Xms`, `Java_Xmx` environment section and the `/ngrams` volume mount
keep working completely unchanged, only the `image:`/`build:` line needs to
change. ARM64 workarounds were dropped (terra is x86_64).

## Building and testing locally

```bash
docker build -f docker/Dockerfile -t languagetool-sv:local .
docker run --rm -p 8010:8010 languagetool-sv:local
curl --data 'language=sv&text=Det var en mörk kväll och ett mörk kväll.' \
  http://localhost:8010/v2/check
```

## Deploying to terra

Not done automatically, this changes a live service other things depend on
(OnlyOffice integration among them). Proposed steps, for review before
running:

1. On the LXC (`david@192.168.50.16`, `/opt/languagetool`):
   `git clone` isn't needed there, only the compose file changes.
2. Update `docker-compose.yml`: replace
   `image: erikvl87/languagetool:6.8` with a `build:` block pointing at this
   `docker/Dockerfile` (context = a checkout of this repo on that box, or an
   image built elsewhere and pushed to a registry, decide which before
   proceeding), keep everything else (`ports`, `environment`, `volumes`,
   `healthcheck`) unchanged.
3. `docker compose build && docker compose up -d`, then rerun the check above
   against the live host before considering it done.
4. Consider running the new container on a spare port alongside the old one
   first, to test before cutting over, rather than replacing in place.

See `implementation-plan.md` in the notes repo for the full decision
history.
