# Hub Companion POC

A Kotlin Multiplatform sketch for helping State Farm employees after they arrive at an unfamiliar hub.

This directory is intentionally self-contained so the Flask parking service can live in a sibling directory without sharing build files or source paths.

The shared Compose UI demonstrates the primary POC story:

1. Detect the current hub.
2. Recommend parking using simulated availability.
3. Save the parked-car location.
4. Find rooms and amenities.
5. Ask Jake campus-logistics questions.

## Project structure

- `composeApp/src/commonMain`: shared UI, models, and demo state
- `composeApp/src/androidMain`: Android entry point
- `composeApp/src/iosMain`: iOS Compose controller
- `composeApp/src/desktopMain`: desktop demo entry point

## Run

Open the `mobile-app` directory—not the repository root—in a current Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin.

Once the Gradle wrapper is available:

```shell
./gradlew :composeApp:run
./gradlew :composeApp:assembleDebug
```

Expected repository layout once the API is added:

```text
the-interns-hack-day-27/
├── mobile-app/       # Kotlin Multiplatform app
├── api/              # FastAPI inference and lot-analysis service
└── parking-lot-bot/  # Model training code
```

The `/api/v1/parking-lot/analyze` endpoint returns per-space availability for a
complete sample-layout image. The mobile app still uses local availability data
until that endpoint is connected to a stable lot image source.

For iOS, open the project in the Kotlin Multiplatform IDE and create an iOS application target that embeds the generated `ComposeApp` framework. The shared controller is exposed as `MainViewController()`.

## POC boundaries

- Dallas Hub and parking availability are simulated.
- Parking recommendations are explainable ranking results, not guaranteed reservations.
- Indoor directions use a small landmark-based route.
- Jake is backed by local approved demo answers and is limited to campus logistics.
- Lunch matching is intentionally left as a stretch feature.

## Hub selection

Employees select their active hub from the dropdown on the home screen. The app
does not request GPS permission or automatically change hubs.
