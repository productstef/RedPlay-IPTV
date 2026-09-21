# Android TV emulator

Create a TV AVD in Android Studio **Device Manager** using a recent Google TV / Android TV API 35-36 x86_64 image. Name it `RedPlay_TV_1080p`.

Start/build/install/launch in one step:
`powershell -ExecutionPolicy Bypass -File .\scripts\run-emulator.ps1`

TV remote keyboard mapping in the emulator:
- Arrow keys: D-pad
- Enter: Select/OK
- Escape/Backspace: Back
- Home: Android TV Home

Reset app data:
`adb shell pm clear com.redplay.iptv`

Reinstall without clearing app data:
`adb install -r app\build\outputs\apk\debug\app-debug.apk`
