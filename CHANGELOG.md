## [2.0.0](https://github.com/Basis-Theory/android-elements/compare/1.2.0...2.0.0) (2025-10-29)


### ⚠ BREAKING CHANGES

* api key is now required to init the library

* fix: complete migration from basistheory-java to java-sdk

- Fix ProxyApi response parsing to handle raw JSON responses correctly
- Fix Token.toAndroid() conversion to handle Optional fields properly
- Remove unused imports and functions
- All tests now passing

* ci: fix security workflows

* ci: add public security workflows

* fix: remove dev key

### Features

* migrate from bt java to java-sdk ([#19](https://github.com/Basis-Theory/android-elements/issues/19)) ([49ccf22](https://github.com/Basis-Theory/android-elements/commit/49ccf22802b40f4bfbec14679321c75da3088f6e))


## [1.2.0](https://github.com/Basis-Theory/android-elements/compare/1.1.0...1.2.0) (2025-07-04)


### Features

* including token alias in the return when encrypting multiple tokens ([6015b85](https://github.com/Basis-Theory/android-elements/commit/6015b85483c7db58c4fec3e60f46e07eecb30dc3))


## [1.1.0](https://github.com/Basis-Theory/android-elements/compare/1.0.0...1.1.0) (2025-06-19)


### Features

* JWE Encryption Support ([#21](https://github.com/Basis-Theory/android-elements/issues/21)) ([e930d30](https://github.com/Basis-Theory/android-elements/commit/e930d30ad2c375b362f2a643bedc9e95a0610da4))


## [1.0.0](https://github.com/Basis-Theory/android-elements/compare/0.0.1...1.0.0) (2025-01-14)


### ⚠ BREAKING CHANGES

* releases stable 1.0.0 version to replace deprecated basistheory-android package

### chore

* Updates Dependencies and Migrates to Gradle Kotlin DSL ([#8](https://github.com/Basis-Theory/android-elements/issues/8)) ([1b99f51](https://github.com/Basis-Theory/android-elements/commit/1b99f5173d9fa45ae97250f94f36bf8ec4d1d893))


