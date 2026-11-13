# EyesOpen 👁️

**EyesOpen** is an Android assistant for blind and visually impaired people:
the camera recognises the objects around the user in real time and
**describes them out loud** — what the object is, **where** it is
(on the left, ahead, on the right) and whether it is **very close**.

Everything runs **on the phone** (TensorFlow Lite): no image ever leaves the device,
and it works offline.

## Features

- **Real-time object detection** — 80 everyday objects (people, chairs, furniture,
  vehicles, cups, phones…) with an SSD MobileNet model.
- **Smart voice announcements** — an object is spoken when it appears or changes side,
  not on every frame; position and proximity are included.
- **“What do I see?” button** — describes the whole scene on demand.
- **Accessible by design** — very large buttons (72–88 dp), high-contrast yellow-on-black
  theme, TalkBack labels, live region for the detected objects, vibration feedback,
  spoken welcome message.
- **Settings** — voice on/off, position hints, vibration, minimum confidence.
- **Torch** for dark places, screen kept awake during detection.
- **French & English** — interface and object names follow the phone language.

## Architecture

```
app/src/main/java/com/ferdaoues/eyesopen/
├── detection/
│   ├── Detection.kt             detection model + spatial helpers (left / ahead / right, close)
│   ├── ObjectDetector.kt        interface (the UI does not depend on TensorFlow)
│   └── TfliteObjectDetector.kt  SSD MobileNet v1 implementation
├── speech/
│   ├── Announcer.kt             decides what to say and when (anti-repetition) — pure Kotlin
│   └── SpeechOutput.kt          interface + Android TextToSpeech implementation
├── settings/AppSettings.kt      persisted user preferences
└── ui/
    ├── HomeActivity.kt          accessible welcome screen + settings
    ├── DetectorActivity.kt      CameraX preview, background analysis, announcements
    └── DetectionOverlayView.kt  boxes and labels drawn over the preview
```

- **CameraX** (`Preview` + `ImageAnalysis`, keep-only-latest strategy): analysis runs on a
  background thread, the UI stays smooth.
- **Single responsibility & dependency inversion**: detection, speech decisions, speech
  engine and UI are separate; `ObjectDetector` and `SpeechOutput` are interfaces.
- **Unit tests** for the announcement logic and spatial helpers (`app/src/test`).

## What changed in v2

| v1 | v2 |
|---|---|
| Spoke every label on every frame (endless speech queue) | Speaks new objects only, with position and proximity |
| Model ran on the UI thread (lag) | Background analysis with CameraX |
| Crash if the camera permission was refused | Clear permission screen with a retry button |
| English object names only | French and English labels |
| Small “Start” button | Accessible home screen with settings |
| Camera2 boilerplate, no architecture | Layered code with interfaces and unit tests |

## Build

Requirements: Android Studio (Hedgehog or newer), JDK 17, Android SDK 34.

```bash
git clone https://github.com/FerdawesAbdessalem/EyesOpen.git
cd EyesOpen
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
```

## Tech

Kotlin · CameraX · TensorFlow Lite (SSD MobileNet v1, COCO) · Android TextToSpeech ·
Material Design 3 · View Binding
