# Tolgee Android starters

Two minimal, complete apps that integrate the Tolgee Android SDK the fastest correct way. They are the
reference for the documentation on docs.tolgee.io: every snippet there is a file here, and CI builds both
apps on every push so a breaking SDK change shows up before it reaches a customer.

| Starter | Stack | Copy these into your app |
|---|---|---|
| [`android-views/`](./android-views) | Activity + ViewModel, XML layouts, ViewBinding | `App.kt`, `TolgeeBinding.kt`, `BaseActivity.kt`, `res/xml/network_security.xml` |
| [`jetpack-compose/`](./jetpack-compose) | Jetpack Compose, Material 3 | `App.kt`, `res/xml/network_security.xml`, the three `io.tolgee.*` imports |

Both apps show the same screen: a static string, a parameterised string, a plural driven by state, a
string array, and a locale switch. Out of the box they load translations from Tolgee's public demo
project, so the first run already demonstrates over-the-air text.

## Playbook: clone to first over-the-air string

Times are for someone who has never used Tolgee. Steps 1–3 happen in the Tolgee Platform once per project.

| # | Step | How | Check |
|---|---|---|---|
| 1 | **Create the Content Delivery** | Tolgee Platform → your project → *Developer settings* → *Content Delivery* → format **Android SDK**, auto-publish on. Copy the URL prefix. | The prefix looks like `https://cdn.tolg.ee/<hash>` |
| 2 | **Push the strings** | In the starter directory: `npm i -g @tolgee/cli`, `tolgee login <project API key>`, `tolgee push` (uses `.tolgeerc`, format `ANDROID_XML`). Set `projectId` to yours first. | Keys in the platform equal the `name` attributes in `res/values/strings.xml` |
| 3 | **Verify the CDN** | `curl -sSf "<prefix>/en.json" \| head -c 200` | A JSON object. XML or 404 means the delivery format or prefix is wrong; fix it here, not in code |
| 4 | **Point the app at it** | Add to the repo-root `local.properties`: `TOLGEE_CDN_DEV=<prefix>` (and `TOLGEE_CDN_PROD=<prefix>` for release) | `BuildConfig.TOLGEE_CDN_URL` carries it; no source file changes |
| 5 | **Build and run** | `./gradlew :starters:android-views:installDebug` (or `:starters:jetpack-compose:installDebug`) | First frame shows bundled text, CDN text replaces it within a second |
| 6 | **Prove it is over the air** | Change one translation in the platform, wait for publish (auto-publish is quick; the CDN edge caches up to ~15 min), reopen the screen | New text, same APK |
| 7 | **Switch locale** | Tap a language button | Static, parameterised, plural and array texts all change; nothing recreates |
| 8 | **Port to your app** | Copy the files listed above; replace `project(":core")` with `io.tolgee.mobile-kotlin-sdk:core:<version>` (or `:compose`). Migration guide: https://docs.tolgee.io/android-sdk/agents/migrate-strings | `./gradlew assembleDebug` green, one string from the CDN, a locale switch without a crash |

## Content Delivery configuration, in one place

The SDK parses **JSON only**. Everything below follows from that.

- **Format:** create the delivery with format **Android SDK**. It publishes `<prefix>/<languageTag>.json` with
  Android-style placeholders (`%1$s`, `%d`). The SDK defaults `path = { "$it.json" }` and
  `Formatter.Sprintf` match it, so `App.kt` sets neither. Do not point `path` at `strings.xml`: the app
  would silently fall back to bundled resources.
- **URL per environment:** `build.gradle.kts` resolves `TOLGEE_CDN_DEV` / `TOLGEE_CDN_PROD` from
  `local.properties`, then `-P` or `gradle.properties`, then the environment, then the demo project. The
  prefix is public; API keys never enter the app.
- **Locales:** `availableLocaleTags("en", "cs", "fr", "sv")` lists what the delivery publishes. It
  enables BCP 47 fallback (`en-US` → `en`) and `preloadAll()` even when there is no `manifest.json`.
- **Cache:** `TolgeeStorageProviderAndroid(app, BuildConfig.VERSION_CODE)` persists the last fetch under
  `filesDir`; a new version code starts clean. Requires `buildFeatures { buildConfig = true }`.
- **Network:** `res/xml/network_security.xml` allows `tolgee.io` and `tolg.ee`; add your own host for a
  self-hosted CDN. The manifest references it and declares `INTERNET`.
- **Keys:** the SDK looks a resource up by its entry name (`R.string.description` → `description`).
  Keep `strings.xml` as the offline fallback and the source pushed to the platform.

## Reactivity: Compose is declarative, Views are not

Translations arrive asynchronously and change on locale switch. Compose handles that with the three
`io.tolgee` composables. Views need a delegate that re-reads the string; `TolgeeBinding.kt` provides it.

| Where the string is used | Jetpack Compose | Views + ViewModel |
|---|---|---|
| Static text in a layout or composable | `stringResource(id)` | automatic at inflation via `TolgeeContextWrapper`; refreshed by `retranslate()` in `BaseActivity` |
| Parameterised or plural text set from code | `stringResource(id, args)`, `pluralStringResource(id, qty, args)` | `textView.bindTolgee(id, args)`, `bindTolgeePlural(id, qty, args)` |
| String array | `stringArrayResource(id)` | `bindTolgeeArray(id)` |
| Text a ViewModel must own (also used outside a view) | ViewModel exposes ids; composable resolves | `tolgeeText(app, id)` → `StateFlow<String>`; uses the application context, no wrapper needed |
| Window title, notifications, other imperative strings | `tFlow(activity, id).collect { title = it }` | same, or override `onTranslationsChanged()` |
| Locale switch | `setLocale(tag)`; recomposition follows | `switchLocale(tag)` = `setLocale` + `preload`; bound views update, `changeFlow` re-translates the rest |

The one rule that keeps the imperative track as short as the declarative one: **UI state carries the
inputs to strings (numbers, names), never resolved strings and never a Context.** The view layer resolves
them, so a locale or translation change re-renders text without the ViewModel knowing.

A detail worth knowing when you write your own bindings: the SDK's `tPlural` / `tPluralFlow` already pass
`quantity` as the first format argument (`%1$d`), mirroring `Resources.getQuantityString(id, qty, qty, …)`.
Pass only the remaining arguments.

## What the starters deliberately leave out

Dependency injection, navigation, multi-module layout, and product theming. Each would make the
starter look like *an* app instead of *your* app. The four files listed at the top are the entire
integration; everything else is a plain Android project.

## Related

- Android SDK docs and recipes for humans and coding agents: https://docs.tolgee.io/android-sdk/agents
- API reference (Markdown twin and `llms.txt` for agents): https://tolgee.github.io/tolgee-mobile-kotlin-sdk/
- Agent Skill packaging the same rules: [`../skills/tolgee-android-sdk/SKILL.md`](../skills/tolgee-android-sdk/SKILL.md)
