## RedPlay IPTV 0.6.2

- Rebuilt the **Add IPTV provider** editor as a dedicated modal Windows dialog instead of an overlay made from child controls inside the main player window.
- The provider dialog is an owned top-level window, so mpv/video child-window z-order can no longer cover or split the form.
- The main RedPlay window is disabled while the provider dialog is open and restored when it closes.
- Added proper dialog keyboard routing for Tab navigation and Escape-to-close.
- Provider validation messages are owned by the provider dialog so they remain above the editor.
- No changes to playback, playlists, EPG, audio/subtitle selection, built-in Western/Star Wars providers, privacy controls, or updater behavior.
