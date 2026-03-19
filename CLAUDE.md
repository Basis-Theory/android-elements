# Android Elements

Kotlin SDK providing secure UI components for collecting sensitive data on Android.

## Build & Test

```bash
./gradlew test                       # Unit tests (Robolectric, no emulator needed)
./gradlew connectedAndroidTest       # Acceptance tests (requires running emulator)
make verify                          # Full: syncs emulator clock + unit + acceptance
```

Unit tests run with Robolectric (no device/emulator required). Always verify fixes with `./gradlew test`.

## Project Structure

- `lib/` — Library module (published artifact). Source: `lib/src/main/java/com/basistheory/elements/`
- `example/` — Example app (XML views). Acceptance tests live in `example/src/androidTest/`
- `compose-example/` — Compose example app
- Tests: `lib/src/test/java/com/basistheory/elements/` (unit, Robolectric)

## Gotchas

- **Version is in `lib/build.gradle.kts`**: `val versionName = "X.Y.Z"` — CI updates this on release via sed. Do NOT add quotes or change the format.
- **Java 17 required**: `jvmTarget = "17"`, `sourceCompatibility/targetCompatibility = 17`
- **Kotlin DSL Gradle**: All `.gradle.kts` files, not Groovy `.gradle`
- **Version catalog**: Dependencies use `libs.plugins.*` and `libs.*` aliases (check `gradle/libs.versions.toml`)
- **`local.properties`** must exist with `sdk.dir` set (copy from `local.properties.example`)
- **Acceptance tests need API key**: Set `com.basistheory.elements.example.apiKey` in `local.properties`
- **Multi-module**: Root `settings.gradle.kts` includes `:lib`, `:example`, `:compose-example`
- **Publishing**: Maven Central via `maven-publish` plugin. Group `com.basistheory`, artifact `android-elements`

## Release

Automated on push to `master`. CI bumps version tag, updates `lib/build.gradle.kts` version, `README.md`, and `CHANGELOG.md`, then pushes. No manual publish step.

## Testing Libraries

Unit tests use: JUnit, JUnitParams, Robolectric, Strikt (assertions), MockK, MockWebServer, JavaFaker, kotlinx-coroutines-test.

## Docs

- [Android Elements SDK](https://developers.basistheory.com/docs/sdks/mobile/android/)
