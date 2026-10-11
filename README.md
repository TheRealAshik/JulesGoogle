# Jules for Android

A native Android client for Google Jules, with a Gemini-powered coding copilot. The app lets you start and manage Jules coding sessions from an Android device.

## Features

- Create coding sessions for a connected GitHub source and branch.
- Browse sessions, inspect session activity, send follow-up messages, and delete sessions.
- Review and approve a Jules-generated plan when plan approval is enabled.
- Use a Gemini-powered copilot with Jules-related tool actions.
- Sign in with Google through Firebase Authentication or continue in guest mode.
- Save and sync prompt-gallery entries with Cloud Firestore when Firebase is configured.
- Adjust app theme and Gemini model preferences in Settings.

## Tech stack

- Kotlin and Jetpack Compose with Material 3
- Android Gradle Plugin and Gradle Kotlin DSL
- Retrofit, OkHttp, and Moshi for REST APIs and JSON
- Google Jules REST API
- Gemini API
- Firebase Authentication and Cloud Firestore
- Kotlin coroutines and Flow

## Architecture

The main source code is under `app/src/main/java/com/example/`.

| Package or file | Responsibility |
| --- | --- |
| `MainActivity.kt` | Android activity and Compose app entry point |
| `ui/screens/` | Welcome/authentication, chat home, session details, settings, and prompt gallery screens |
| `ui/components/` and `ui/theme/` | Reusable Compose UI, theme, colors, and typography |
| `viewmodel/JulesAppViewModel.kt` | UI state, screen navigation, and coordination between UI and data/services |
| `jules/sdk/` | Jules API client, request/response models, and session/source operations |
| `gemini/` | Gemini-powered copilot logic, API service, and Gemini request/response models |
| `data/auth/` | Firebase Authentication and Google sign-in |
| `data/firestore/` | Cloud Firestore synchronization for saved prompts |
| `data/preferences/` | Local preferences such as API key, selected model, theme, and session defaults |

At a high level, Compose screens observe state exposed by the ViewModel. The ViewModel coordinates Jules API requests through the Jules SDK, Gemini copilot requests through the Gemini service, and authentication or saved-prompt synchronization through Firebase-backed data classes. Local UI preferences are stored with Android SharedPreferences.

## Prerequisites

- Android Studio with support for the Android SDK Platform 36
- JDK 17 for the Gradle build (the GitHub Actions workflow uses Temurin JDK 17)
- Git and network access to download Gradle and Android dependencies
- A Google Jules API key to use Jules sessions
- A Gemini API key to use the Gemini-powered copilot
- A Firebase project with Google sign-in configured if you want Google authentication and cloud-synced saved prompts

The project uses the Gradle wrapper, configured for Gradle 9.3.1. You do not need to install Gradle separately.

## Configuration

### 1. Gemini API key

The Secrets Gradle Plugin reads `.env` and uses `.env.example` as its default properties file. From the repository root, create a local environment file:

```bash
cp .env.example .env
```

Open `.env` and replace the placeholder with a valid Gemini API key:

```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```

Do not leave the placeholder value in place if you want Gemini requests to work. Keep `.env` local; it is listed in `.gitignore`. Never commit a real API key. If you are working in Google AI Studio, follow its Secrets configuration flow instead of copying a real key into source-controlled files.

Get a key and read the setup guide in the [Gemini API documentation](https://ai.google.dev/gemini-api/docs/api-key).

### 2. Firebase and Google sign-in

Firebase configuration is not included in this repository. To configure Google sign-in and cloud-synced saved prompts:

1. Create or select a project in the [Firebase console](https://console.firebase.google.com/).
2. Register an Android app using the application ID `com.aistudio.jules.ashik` from `app/build.gradle.kts`.
3. Enable the Google sign-in provider in Firebase Authentication.
4. Configure the OAuth client for your app and ensure the generated configuration includes the web OAuth client ID used by Google sign-in.
5. Download `google-services.json` and place it at `app/google-services.json`.
6. Enable and configure Cloud Firestore for the Firebase project if you need saved-prompt synchronization.

Use the official [Firebase Android setup guide](https://firebase.google.com/docs/android/setup), [Google Services configuration guide](https://firebase.google.com/docs/android/google-services-plugin-and-file), and [Credential Manager Google sign-in guide](https://developer.android.com/identity/sign-in/credential-manager-siwg).

The app's Gradle configuration allows a build to proceed when `google-services.json` is missing, but Google sign-in will not be configured. The authentication code specifically looks for the web OAuth client ID generated from the Firebase configuration. If you only need to compile the app, Firebase setup may not block the debug build; Firebase-backed features require a valid project configuration and service setup.

Firebase configuration files contain project-specific identifiers. Follow your team's repository policy for them, and never commit private credentials, service-account keys, passwords, or API keys.

### 3. Jules API key

Run the app, open **Settings**, and enter your Jules API key in the API-key setting. The app stores this value in local app preferences. The preference value is Base64-encoded, which is not encryption; protect access to your device and do not share the key or include it in screenshots, logs, or commits.

Create or manage a key through the [official Jules API documentation](https://jules.google/docs/api/reference/). The Jules API also requires the relevant GitHub source to be connected to Jules before the app can create sessions for that source.

## Build locally

### Android Studio

1. Clone the repository and open its root directory in Android Studio.
2. Install the required Android SDK Platform 36 if Android Studio prompts you.
3. Set the Gradle JDK to JDK 17 in Android Studio's Gradle settings.
4. Complete the configuration steps above for the services you intend to use.
5. Wait for Gradle sync to finish, then select **Build > Make Project** or **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The debug APK is normally written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Command line

From the repository root, build the debug APK with the Gradle wrapper.

**macOS / Linux:**

```bash
chmod +x ./gradlew
./gradlew assembleDebug
```

**Windows (Command Prompt):**

```bat
gradlew.bat assembleDebug
```

The output APK is at `app/build/outputs/apk/debug/app-debug.apk`. These commands build the debug variant; they do not claim that tests or lint checks have passed.

## Continuous integration

The [GitHub Actions build workflow](.github/workflows/build.yml) runs on pushes and pull requests targeting `main` or `master`, and can also be started manually. It uses JDK 17 and runs `./gradlew assembleDebug`; the resulting debug APK is uploaded as a workflow artifact. The workflow currently verifies the debug build rather than running automated test tasks. Open the workflow run in the repository's **Actions** tab to inspect its result and download the artifact.

## Troubleshooting

- **Gemini requests fail:** Check that `.env` exists in the repository root, `GEMINI_API_KEY` contains a valid key rather than the example placeholder, and Gradle sync/build completed after the change.
- **Google sign-in reports that it is not configured:** Check that `app/google-services.json` matches application ID `com.aistudio.jules.ashik`, Google sign-in is enabled in Firebase Authentication, and the configuration provides a web OAuth client ID.
- **Firebase features fail at runtime:** Confirm that the Firebase project and the required Authentication/Firestore services are configured. A successful debug build alone does not verify Firebase runtime configuration.
- **Gradle cannot find a Java runtime or fails during sync:** Configure JDK 17 as the Gradle JDK, then sync again.
- **Android SDK or platform errors appear:** Install Android SDK Platform 36 in Android Studio's SDK Manager and retry the build.
- **Jules requests fail:** Check the API key entered in app Settings and make sure the intended GitHub repository is connected to Jules.

## Contributing

Bug reports and focused improvements are welcome. When opening an issue, include the steps to reproduce the problem, the expected behavior, and relevant error output with secrets and personal data removed. For a change proposal, describe the intended behavior and keep the pull request focused.

No root-level license file was found in the repository when this README was prepared. This README does not make any claim about the project's license; check with the maintainers before reusing or redistributing the code.

## Official references

- [Android Studio](https://developer.android.com/studio)
- [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [Gemini API](https://ai.google.dev/gemini-api/docs)
- [Firebase for Android](https://firebase.google.com/docs/android/setup)
- [Google sign-in with Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg)
- [Jules API quickstart and reference](https://jules.google/docs/api/reference/)
