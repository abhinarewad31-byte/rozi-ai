# Rozi Android prototype

Native Android starter project for Rozi. Open this folder in Android Studio and let Gradle sync. The project uses Android Gradle Plugin 8.13.2, Kotlin 2.2.20, compile SDK 36, min SDK 26, and target SDK 35.

## Prototype features in this increment

- Foreground push-to-talk using Android's speech recognition service, with English, Hindi, and Marathi locale selection.
- Spoken replies through Android TextToSpeech, plus a simple conversational fallback.
- Local assistant display-name preference.
- Local time and alarm intents.
- Strictly explicit SMS command parsing (`send message to NAME: TEXT`, or the corresponding explicit send phrasing in Hindi/Marathi). The app resolves contacts, asks when a match is missing or ambiguous, and uses Android runtime permission before sending. A casual message mention does not match the send-command grammar.
- No always-on microphone, call recording, password capture, payment feature, backend credentials, or admin surface.

## Build

Open this directory in Android Studio with an Android SDK installed, then run the `app` configuration on a device or emulator. This environment has no Java, Gradle, ADB, or Android SDK command available, so I could not produce or verify an APK here. Android SMS permission is sensitive and subject to Google Play policy; this prototype is intended for local development and needs a policy review before distribution.

## Current prototype limits

This is a local interaction prototype, not the complete product. It does not yet include a hosted conversational model, durable opt-in memory controls, voice-sample enrollment, the admin console/authentication infrastructure, relationship labels/calling, or broad app launching. Conversational responses are simple local rules and are not a substitute for a full Hindi/English/Marathi assistant. Speech recognition and text-to-speech depend on the device's installed services and language models. The app does not claim perfect transcription or spelling.

The main product and security requirements remain in [the specification](outputs/rozi-android-prototype-spec.md).

