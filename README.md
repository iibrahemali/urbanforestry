# MyCelia

An Android app for the City of Lancaster's urban forestry department. You walk around town, find trees on the map, read about them, post photos, and pick up achievements along the way.

Course: CPS360:MAD (F&M, Spring) with Prof. Ed Novak. Client: [City of Lancaster Urban Forestry](https://www.cityoflancasterpa.gov/services/trees/).

## What it does

- Map of every street tree in Lancaster, loaded from a CSV the city publishes. Tap a marker for species info.
- Compost bin locations on the same map.
- Social feed: post a photo or text, comment on others.
- User profiles with editable bios and avatars.
- Missions and achievements tied to walking routes through the city.
- Seasonal theming, so the look changes through the year.

## Stack

- Java, Android Views (no Compose), ViewBinding
- OSMDroid + osmbonuspack for maps (no Google Maps key required)
- Firebase Auth, Firestore, Realtime DB, and Storage for the social side
- Glide for image loading
- Play Services Location for GPS
- A bit of native C++ via CMake
- minSdk 24, targetSdk 36

## Running it

You need a `google-services.json` from the Firebase project. Drop it in `app/`. Without it the app builds, but login and the feed will not work.

1. Clone the repo.
2. Open in Android Studio (Iguana or newer).
3. Put `google-services.json` in `app/`.
4. Sync Gradle and run on a device with API 24+.

The CSVs in `app/src/main/assets/` (`trees.csv`, `compostBins.csv`) populate the map. Replace them to refresh the data.

## Project layout

```
app/src/main/
  java/com/example/urbanforestry/   # activities, adapters, models
  assets/                            # trees.csv, compostBins.csv
  cpp/                               # native bits
  res/                               # layouts, drawables, navigation graphs
```

`WelcomeActivity` is the launcher. `MainActivity` holds the map. `MenuActivity` is the side menu. `FeedActivity`, `CreatePostActivity`, and `PostImageActivity` run posting and the feed. `TreeInfoActivity` is the popup for a tapped marker. `Missions.java`, `Routes.java`, `Achievement.java`, and `SeasonManager.java` handle the gamified parts.

## Team

Will, Ibrahem, Aiden, John
