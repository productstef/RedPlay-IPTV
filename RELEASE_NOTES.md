## RedPlay IPTV 0.6.3

- Restyled the **Add IPTV provider** dialog to closely match the new dark RedPlay mockup while preserving the existing owned-modal architecture.
- Added the new Name and M3U/M3U8 URL presentation plus the dedicated M3U file upload / drag-and-drop area.
- Kept the provider dialog separate from the mpv/video surface, so the existing overlay and z-order protections remain intact.
- Existing provider save/load behavior, validation flow, playlist handling, playback, EPG, audio/subtitle selection, built-in providers, privacy controls, and updater behavior remain unchanged.
- Editing an existing provider preserves stored advanced values that are no longer shown in the simplified dialog.
