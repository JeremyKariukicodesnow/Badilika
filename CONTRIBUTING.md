# Contributing to Badilika

Badilika is free and open source. Improvements to accessibility, reliability, documentation and tests are welcome.

## Ground rules

- Preserve existing timers, history, category settings and cooldowns during updates.
- Reducing app allowances must remain immediate; increasing or removing them must pass the existing cooldown.
- Keep protection descriptions honest. DNS filtering cannot inspect every page, chat, image or proxy.
- Do not add advertising, tracking SDKs, account requirements or data uploads without an explicit project decision and privacy-policy changes.
- Never commit signing keys, tokens, credentials, device logs or private usage records.
- Keep animation decorative, unobtrusive and compatible with reduced-motion preferences.
- Preserve third-party licenses and photo attribution.

## Development

For Android, see [mobile/README.md](mobile/README.md). Run the policy tests with `./mobile/test.ps1` and build with `./mobile/build.ps1`. Device behavior requires testing with appropriate permissions on actual Android versions; successful compilation does not establish device compatibility.

The website is plain HTML/CSS/JavaScript. From `website/`, run `node build.mjs` to generate the feature, protection, setup and rules routes from `pages.mjs` and the home page's shared layout. Edit those sources rather than generated route HTML. Vercel runs this build on production pushes to `main`.

For pull requests, explain the behavior changed, how it was checked, and any remaining limits. Use synthetic data and redact private information from screenshots. Report a suspected vulnerability privately to the maintainer before publishing exploit details.
