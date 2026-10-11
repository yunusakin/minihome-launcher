# miniHome Launcher

miniHome Launcher is a home screen for Android TV. It shows a grid of the installed apps and a clock. It has no recommendation rows, no advertisements, no animations and no network access.

## Why this project exists

Low-cost TVs have 1.5 to 2 GB of RAM and a slow CPU. The stock Google TV home screen loads content rows and recommendations. It also keeps two processes in memory.

We measured both launchers on one TV. The TV has 1.9 GB of RAM and four ARM cores.

| Measurement | Stock Google TV launcher | miniHome Launcher |
|---|---|---|
| RAM (PSS) | 115 to 145 MB | 45 to 55 MB |
| Frame time while the user moves the selection | 30 to 65 ms | 20 ms (median) |

These values come from one device. Your values can be different.

miniHome Launcher does one job. It lets the user open an app quickly.

## Features

- Grid with 5 columns. Each tile shows the app banner. If an app has no banner, the tile shows the app icon.
- Clock and date at the top of the screen.
- Settings button at the top right.
- Move mode. Press and hold OK on an app. Move the app with the arrow keys. Press OK to drop it. The launcher saves the order.
- Hide list. The launcher does not show the apps in the list. The apps stay installed.
- Selection highlight. The selected tile is 10% larger and shows its label. The other tiles are dimmer. The change is immediate.
- The APK is approximately 21 KB. The launcher needs no permissions.
- Interface text in English and Turkish. The launcher selects the language from the system language.

## Requirements

- A TV with Android 5.0 or later (API level 21 or later).
- ADB access to the TV over the network. On most TVs, enable it in Settings > Developer options > Wireless debugging.

## Build

The build does not use Gradle or Android Studio.

Install these items:

- JDK 17. The `d8` tool failed with JDK 21 in our tests.
- The Android command-line tools, with the packages `build-tools;34.0.0` and `platforms;android-34`.

Run the build script:

```bash
ANDROID_HOME=/path/to/sdk ./build.sh
```

The script creates `minihome.apk`. It signs the APK with a debug key. The script creates the key in the `build/` directory. Use your own key if you give the APK to other people.

## Install

1. Connect to the TV:

   ```bash
   adb connect <tv-ip>:<port>
   ```

2. Install the APK:

   ```bash
   adb install -r minihome.apk
   ```

3. Start the launcher:

   ```bash
   adb shell am start -n com.local.minihome/.Home
   ```

The project does not publish a prebuilt APK. Build it yourself, so you control the signing key.

## Set as the default home screen

Run this command:

```bash
adb shell cmd package set-home-activity com.local.minihome/.Home
```

To go back, run the same command with the component of your original launcher. On Google TV, the component is `com.google.android.apps.tv.launcherx/.home.HomeActivity`.

## Known limits

Read this section before you install the launcher.

- On some TVs, the Home button opens the stock launcher. The TV that we tested (Onvo Apollo Premium 4K, Android 14, Google TV) has this problem. The system lists miniHome Launcher as the home app, but it sends the Home button and the cold boot to the Google launcher. When the TV wakes from standby, it shows the last app that was open. If miniHome Launcher was open, it stays on the screen.
- Do not disable the stock launcher on Google TV. On the TV that we tested, the command `pm disable-user` caused a black screen. The TV opened the setup recovery activity and no window had focus. We enabled the launcher again with `pm enable`.
- Google TV has a setting named "Apps only mode". It is in Settings > Accounts & Profiles. The setting removes the recommendations from the stock launcher and reduces its memory use. You can use it together with this project.
- We tested the launcher on one TV only. Reports for other TVs are welcome.

## Customize

All settings are in `src/com/local/minihome/Home.java`.

- `COLS` sets the number of columns.
- `HIDE`, `HIDE_SUB` and `HIDE_PKG` set the apps to hide. `HIDE` matches the exact label. `HIDE_SUB` matches part of a label in lowercase. `HIDE_PKG` matches the package name.
- Colors and sizes are numbers in `onCreate` and `style()`.

The banner and the icon come from `tools/GenBanner.java`. The tool uses only the Java standard library and the system font "Arial". To change the text, the font or the colors, edit the constants at the top of the file. Then run:

```bash
javac -d /tmp/gen tools/GenBanner.java
java -Djava.awt.headless=true -cp /tmp/gen GenBanner res/drawable-nodpi
```

## Change the launcher for your TV

You do not need to know software development or Android development to change this project. You can give the project to an AI coding agent. Ask the agent to change the launcher for your TV.

Use this procedure:

1. Download the project files to your computer.
2. Open the project directory in the AI agent.
3. Tell the agent what you want to change. For example, tell the agent to change the number of columns, to hide other apps or to use other colors.
4. Tell the agent the model of your TV and the Android version.
5. Ask the agent to build the APK and to install it on your TV with ADB.

Check the result on your TV. An AI agent can make mistakes. Keep the original launcher enabled, so you can go back if a change does not work.

## How it works

The launcher has one activity with a `GridView`. The manifest declares the activity as `HOME` and `LEANBACK_LAUNCHER`.

1. `PackageManager.queryIntentActivities` gets the list of apps.
2. `loadBanner` gets the banner of each app.
3. A normal launch intent starts the selected app.
4. The `style()` method changes the selected tile.

## License

MIT. See [LICENSE](LICENSE).
