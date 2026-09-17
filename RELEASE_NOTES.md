## RedPlay IPTV 0.6.8

- Added the Western Channel on-demand Library while keeping the existing live channel architecture unchanged.
- Library movie playback now resolves the actual Oracle media extension across MKV, MP4, AVI, M4V and MOV files instead of assuming MKV.
- Added single-click selection and double-click immediate playback for Library movie cards.
- Added a VLC-style seek bar for Library movies with current time, total duration, click-to-seek and drag scrubbing.
- The Library seek overlay now auto-hides after 2 seconds of mouse inactivity and reappears immediately when the mouse moves, including over the embedded mpv video surface.
- Fixed Bulgarian Library subtitles by detecting legacy Windows-1251 sidecars and converting them to temporary UTF-8 files before handing them to mpv. UTF-8/UTF-16 subtitles remain supported.
- Existing live TV playback, provider UI, owned overlays, TV Guide, audio/subtitle selectors, fullscreen behavior and updater flow remain unchanged.
