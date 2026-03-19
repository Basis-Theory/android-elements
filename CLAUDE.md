# Android Elements

Android SDK for Basis Theory Elements — Kotlin library providing secure UI components for collecting sensitive data.

## Development Workflow

```bash
./gradlew build       # Build the library
```

## Testing

```bash
make verify           # Full verification (unit + acceptance tests)
./gradlew test        # Unit tests
./gradlew connectedAndroidTest   # Acceptance tests (requires emulator)
```

## Feedback Loops

Run `./gradlew test` for fast unit test feedback. For acceptance tests, ensure an emulator is running first.

When a failing test is discovered, always verify it passes using the appropriate feedback loop before considering the fix complete.

## Standards & Conventions

- Kotlin, Gradle (Kotlin DSL)
- Library published to Maven Central

## Links

- [Elements docs](https://developers.basistheory.com/docs/sdks/mobile/android/)
