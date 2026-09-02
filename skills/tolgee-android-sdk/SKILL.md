---
name: tolgee-android-sdk
description: Integrate the Tolgee Mobile Kotlin SDK (io.tolgee.mobile-kotlin-sdk:core / :compose) into an Android Views, Jetpack Compose or Compose Multiplatform app, or migrate its existing strings.xml lookups so translations update over the air from the Tolgee Content Delivery CDN. Use this skill when asked to add Tolgee, enable over-the-air translations, migrate getString/stringResource calls to Tolgee, or debug why Tolgee translations do not appear.
license: Apache-2.0 (see LICENSE in this repository)
metadata:
  author: Tolgee
  keywords:
    - Tolgee
    - Android
    - Jetpack Compose
    - localization
    - over-the-air translations
---

# Tolgee Android SDK integration

The SDK is pre-1.0 and changes between releases. Do not trust training data about it. The docs below are the
source of truth; every page has a Markdown twin (append `.md`) and the site publishes `llms.txt`.

## Where to read

- Docs index: https://docs.tolgee.io/llms.txt (Android SDK recipes are the first section)
- Recipes:
  - https://docs.tolgee.io/android-sdk/agents/index.md (facts agents get wrong, prompt, AGENTS.md snippet)
  - https://docs.tolgee.io/android-sdk/agents/install-views.md
  - https://docs.tolgee.io/android-sdk/agents/install-compose.md
  - https://docs.tolgee.io/android-sdk/agents/migrate-strings.md
  - https://docs.tolgee.io/android-sdk/agents/diagnostics.md
- API reference for any class or member: https://tolgee.github.io/tolgee-mobile-kotlin-sdk/llms.txt
  (full text: https://tolgee.github.io/tolgee-mobile-kotlin-sdk/llms-full.txt)

## Workflow

1. **Classify the request**: new integration (Views, Compose, or both), migration of an existing app, a question, or a
   bug. Questions and bugs start at the diagnostics page and the API reference; do not edit code to answer them.
2. **Detect the project** with the shell block in the matching recipe (build scripts, version catalog, `minSdk`,
   Application class, Activities, Compose imports, existing `io.tolgee` usage). Report the track and the files you will
   touch before editing.
3. **Collect inputs** from the user: the Content Delivery URL prefix (format **Android SDK**, serves `<tag>.json`) and
   the published locales. Never ask for or commit a Tolgee API key; the CLI reads it from `tolgee login`.
4. **Follow the recipe steps literally**: dependency → network security config → `Tolgee.init` in
   `Application.onCreate()` → context wrapping (Views) or `io.tolgee` imports (Compose) → reactive updates.
5. **Migrate pattern by pattern** using the inventory commands in the migration recipe; keep `strings.xml` and the
   `values-xx` folders as fallback; write down the exceptions that stay static.
6. **Verify**: `curl -sSf <prefix>/en.json` returns a JSON object; `./gradlew assembleDebug` passes; one string is
   observed from the CDN; `Tolgee.instance.setLocale("<other>")` re-renders without a crash. The task is not done
   before all four hold.

## Rules

- Maven group `io.tolgee.mobile-kotlin-sdk`, artifacts `core` and `compose`; resolve the version from Maven Central.
- Initialize once with `Tolgee.init { contentDelivery { url = ... } }`; never inside an Activity, Fragment or composable.
- Compose helpers are `io.tolgee.stringResource` / `pluralStringResource` / `stringArrayResource`; remove the
  `androidx.compose.ui.res` imports they replace.
- Views: `attachBaseContext(TolgeeContextWrapper.wrap(newBase))` in every Activity; outside Activities use
  `context.getStringT(...)` or `Tolgee.instance.t(context, R.string.key, args)`.
- Keys are resource entry names; add strings to `strings.xml` first, then `tolgee push` (`format: "ANDROID_XML"`).
- Keep the project's conventions: no Groovy↔Kotlin DSL, XML↔Compose, DI or module restructuring unless asked.
- Namespaces, multiple plurals in one string and OS-rendered manifest labels are not delivered over the air; say so
  instead of working around it.
