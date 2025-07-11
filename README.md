# android-elements

[Basis Theory Elements](https://developers.basistheory.com/docs/sdks/mobile/android/) are simple, secure, developer-friendly inputs that enable you to collect
sensitive data within your application and securely store this data within Basis Theory’s certified 
token vault.

Sensitive data entered into elements by your end users can be indirectly referenced within API
calls, enabling you to tokenize this data without needing to directly marshall or access raw
sensitive data. Your application code will only receive access to the created tokens, which can 
safely be sent to your backend systems and persisted for later use.

## Installation

### Requirements

- Android 5.0+ (API level 21+)
- AndroidX

### Gradle

Add this dependency to your project's build file:

```groovy
  repositories {
      maven { url 'https://jitpack.io' }
  }

  dependencies {
      implementation 'com.github.basis-theory:android-elements:1.2.0'
  }
```

Add the ProGuard rules found in example/proguard-rules.pro to your project's rules if you build 
enables code shrinking or obfuscation

## Features

- [TextElement](https://developers.basistheory.com/docs/sdks/mobile/android/types#textelement) to securely collect text input
- [CardNumberElement](https://developers.basistheory.com/docs/sdks/mobile/android/types#cardnumberelement) to securely collect credit card numbers
- [CardExpirationDateElement](https://developers.basistheory.com/docs/sdks/mobile/android/types#cardexpirationdateelement) to securely collect card expiration dates
- [CardVerificationCodeElement](https://developers.basistheory.com/docs/sdks/mobile/android/types#cardverificationcodeelement) to securely collect card verification codes
- [Services](https://developers.basistheory.com/docs/sdks/mobile/android/services) to tokenize sensitive data entered into Elements
- [Styling](https://developers.basistheory.com/docs/sdks/mobile/android/options#styling) - custom styles and branding are fully supported
- [Events](https://developers.basistheory.com/docs/sdks/mobile/android/events) - subscribe to events raised by Elements

## Example Usage

Simply include one or more elements within your application's views:

```xml
<com.basistheory.elements.view.CardNumberElement
    android:id="@+id/card_number"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />

<com.basistheory.elements.view.CardExpirationDateElement
    android:id="@+id/expiration_date"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />

<com.basistheory.elements.view.CardVerificationCodeElement
    android:id="@+id/cvc"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Then tokenize the user input by referencing these elements. This can be wired up in response to a 
button click, or any other user action.

```kotlin
val cardNumberElement = findViewById(R.id.card_number)
val cardExpirationDateElement = findViewById(R.id.expiration_date)
val cardVerificationCodeElement = findViewById(R.id.cvc)

val bt = BasisTheoryElements.builder()
    .apiKey(myPublicApiKey)
    .build()

runBlocking {
    val tokenizeResponse = bt.tokenize(object {
        val type = "card"
        val data = object {
            val number = cardNumberElement
            val expiration_month = cardExpirationDateElement.month()
            val expiration_year = cardExpirationDateElement.year()
            val cvc = cardVerificationCodeElement
        }
    })
    
    // send the tokens within tokenizeResponse to your backend
}
```

Note that the Android SDK requires the use of a public API key during initialization 
(an API key issued to a `public` [Application](https://developers.basistheory.com/docs/concepts/access-controls#what-are-applications)).
Click [here](https://portal.basistheory.com/applications/create?permissions=token%3Acreate&type=public.) 
to create one in the Basis Theory portal.

A full example Android app is included within the [example](example) module within this repo.

## Security Notice

This SDK includes various measures to protect sensitive data entered into secure elements and to
prevent unauthorized access. However, it is important to note that it is not possible to fully
prevent all forms of data access by a developer with malicious intent. We strongly recommend that
our customers ensure they only integrate our SDK with applications and other SDKs from vetted and
trusted developers. Unauthorized access to sensitive data can occur if the developer of the wrapping
SDK or application is motivated to do so.
