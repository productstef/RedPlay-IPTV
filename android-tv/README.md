# RedPlay IPTV for Android TV / Google TV

Native Android TV port of RedPlay, kept in its own project so the existing Windows/Go app remains untouched.

## What is implemented

- Same dark/red RedPlay desktop layout adapted responsively for TV: provider/channel rail, central player, Now Playing/Up Next cards, and EPG/audio/subtitle rail.
- D-pad/remote focus states and large TV hit targets without redesigning the interface.
- Built-in Western and Star Wars providers from the Windows app.
- Remote M3U/M3U8 parsing, groups, search, favorites, provider add/delete/refresh and safe IPTV request headers.
- XMLTV EPG auto-detection, Now/Next and full channel guide.
- Native LibVLC playback for HLS, HEVC/H.265, MKV, MP4 and AVI, plus audio/subtitle track selection, mute/volume, VOD seeking and fullscreen.
- Western Library generated from its XMLTV schedule with the existing mixed-extension resolution order: MKV, MP4, AVI, M4V, MOV.
- Western sidecar `.srt` handling is optional/non-fatal. If present it is normalized from UTF-8/UTF-16/Windows-1251 to UTF-8 before loading; if absent the movie still plays.
- HTTP IPTV support for the current Oracle streams.

## First Windows setup

1. Install current Android Studio with Android SDK, Platform-Tools and Emulator.
2. From PowerShell in `android-tv` run once:
   `powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap-gradle.ps1 -Build`
3. Open the `android-tv` folder in Android Studio.

The bootstrap script downloads Gradle 9.6.0 and generates the official wrapper, so you do not need to install Gradle separately.

## Daily emulator loop

Create a 1080p Google TV / Android TV AVD named `RedPlay_TV_1080p` in Android Studio Device Manager, then:

`powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1`

Or select the emulator in Android Studio and press **Run**.

## Physical TV loop

Enable Developer Options + Wireless debugging on the TV, pair/connect once, then every build is:

`powershell -ExecutionPolicy Bypass -File .\scripts\install-tv.ps1`

See the scripts for pairing syntax. No APK copying or USB stick is needed.

## Notes

- The Android project intentionally uses LibVLC rather than a narrow container player because the existing Western archive includes AVI/MKV/MP4 and Star Wars uses HEVC.
- Cleartext HTTP is allowed because RedPlay supports user-supplied IPTV providers and the existing Oracle provider URLs are HTTP. HTTPS still uses normal system certificate validation.
