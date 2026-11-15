# NOVA JARVIS v1 - Android starter

This is source code, not a compiled APK. It is an early prototype and has not been compiled or device-tested.

## Included
- Native Kotlin Android app
- Text commands and tap-to-talk speech input
- Spoken replies
- Firebase AI Logic cloud chat
- Confirmation before supported phone actions
- Starter actions: launch selected apps, open Android Settings, open Wi-Fi settings, and web search

## Firebase setup
1. Create a project at https://console.firebase.google.com/
2. Add an Android app with package name `com.nova.assistant`.
3. Download `google-services.json` into the `app/` folder.
4. In Firebase Console, open AI Services > AI Logic and enable Gemini Developer API.
5. Register the App Check debug token if Firebase asks.
6. Never ship a release build using the debug App Check provider; configure production App Check first.

## Build
Open in current Android Studio with Android SDK 36 and JDK 17, sync Gradle, then build an APK. Plugin versions may need adjustment to your installed environment.

## Phone-only note
A code editor alone cannot compile a native Android app. If you only have a phone, use a reputable cloud Android build environment or a remote computer with Android Studio.

## Limits and safety
- No always-listening wake word yet.
- Android does not allow unrestricted device control.
- Account recovery must use official platform processes and ownership verification.
- Never enter passwords, OTPs, recovery codes, or identity documents into AI prompts.
