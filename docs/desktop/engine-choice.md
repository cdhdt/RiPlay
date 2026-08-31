# Why the desktop player embeds Chromium

Two other engines were tried first and abandoned. Both are recorded here so neither gets
re-attempted from scratch.

## JavaFX WebView — rejected, no MSE

`checkWebPlayback` and `checkWebMusic` (removed) loaded YouTube's IFrame player inside a JavaFX
`WebView` and watched the player clock. The page loads and the API responds, but the clock never
advances past zero: the WebKit build JavaFX ships has no Media Source Extensions, and YouTube
serves every stream through MSE. There is no flag or workaround — the codec support simply is not
compiled in.

Cost of keeping the spike around: 91 MB of the installed package, 16 jars, over a quarter of the
total. Removed once the CEF path proved itself.

## VLC / vlcj — rejected, stream URLs are not durable

The earlier route resolved a stream URL server-side and handed it to libvlc. It plays, but the URLs
carry a per-URL byte budget and a PO-token requirement that shifts, so playback dies partway through
a track and the whole resolution chain has to be re-derived every time YouTube changes it. Dropped
along with the `stageWindowsVlc` build wiring (~3 MB, and a VLC install to manage on Windows).

## KCEF — chosen

Embedded Chromium drives the real `music.youtube.com` page, so MSE, DRM negotiation, ad handling and
stream resolution are Google's problem rather than ours. `CefPlayerController` talks to the page
through JS (`document.querySelector('video')`, `#movie_player`).

The trade-off is the runtime: ~150 MB of Chromium fetched on first launch to `~/.riplay/kcef`, not
shipped in the installer. It also means no access to the PCM stream, which is why the equalizer and
visualizer stay out of scope (see BACKLOG.md, K8 and K14).
