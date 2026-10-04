# K PARK BAR TV

K PARK BAR TV is an Android TV / Fire TV Stick player for M3U playlists, Xtream Codes, and Stalker portals, with EPG and catch-up. It ships no channels or content. Users add their own licensed sources.

**Brand palette:** gold `#B79B6A`, navy `#2B3553`, cream `#FAFBF3`, black `#0F0F0F`.

## Build
Cloud: Actions tab → **Build APK** → Run workflow → download the `KParkBarTV-debug-apk` artifact. The cloud build does **not** include your provider login (the repo is public); add the source in the app, or build locally with the auto-login file below.

Local auto-login: copy `kparkbar.credentials.properties.example` to `kparkbar.credentials.properties` and fill it in (git-ignored). The built APK then adds your Xtream source on first launch.

Needs JDK 17+ and the Android SDK (not included in the cloud session). Then:

    ./gradlew assembleRelease

Sideload the APK on a Fire TV Stick with Downloader or `adb install`.

## Credits and license
Based on [OwnTV](https://github.com/ahXN00/OwnTV) by ahXN00, released under the GNU GPLv3 (see `LICENSE`, `NOTICE.md`). Any distributed build must stay under GPLv3 and offer its source. Not affiliated with or endorsed by the OwnTV project.
