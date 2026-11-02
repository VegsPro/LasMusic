# LasMusic

Modern Android music player with **continuous infinite playback** and **progressive streaming**.

Inspired by SimpMusic, but with two key improvements:

1. **Progressive loading** — music starts playing almost immediately (does not wait for full download).
2. **Infinite playlist** — when the current queue is ending, the player automatically finds and adds similar tracks so music never stops.

## Features (v1.0.0)

- Media3 (ExoPlayer) with tuned buffering for smooth streaming
- Automatic similar-track recommendations when queue is low
- Modern dark UI + glass-style bottom navigation
- Dedicated release keystore for seamless updates

## Tech

- Kotlin + Jetpack Compose + Material 3
- Media3 ExoPlayer
- Coil
- Kyant Backdrop (liquid glass ready)

## Build

Open in Android Studio or:

```bash
./gradlew assembleRelease
```

## Author

VegsPro
