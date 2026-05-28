# TikTok Patches for Morphe

[![License: GPL v3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)
[![Morphe](https://img.shields.io/badge/morphe-patch%20source-00b894.svg)](https://github.com/MorpheApp/morphe-cli)
[![Platform](https://img.shields.io/badge/platform-Android-3ddc84.svg)](https://www.android.com/)
[![Target](https://img.shields.io/badge/TikTok-43.8.3-ff0050.svg)](https://www.tiktok.com/)

<p>
  <a href="https://ko-fi.com/P5P5YOUU7">
    <img height="56" src="https://storage.ko-fi.com/cdn/kofi2.png?v=3" alt="Support my work on Ko-fi" />
  </a>
</p>

Development is done in my spare time. If you want to support it, my Ko-fi is here: [ko-fi.com/P5P5YOUU7](https://ko-fi.com/P5P5YOUU7).

This is a Morphe patch source for TikTok `43.8.3`.

## Add Source

Add this source to Morphe:

```text
https://morphe.software/add-source?github=icysymmetra/tiktok-patches-for-morphe
```

Manual source URL:

```text
https://github.com/icysymmetra/tiktok-patches-for-morphe
```

## Supported Target

- App: TikTok
- Package: `com.ss.android.ugc.trill`
- Version: `43.8.3`
- Build code: `430803`

Only this target is listed in the patch metadata because it is the version this source was adapted and tested against.

## Available Patches

- `Settings`: Adds the Morphe settings screen inside TikTok.
- `Enable Open Debug`: Adds a TikTok settings row that opens the Morphe settings screen reliably.
- `Disable login requirement`: Lets supported TikTok flows load without the normal login wall first.
- `Fix Google login`: Restores Google sign-in behavior after patching.
- `Feed filter`: Filters feed items such as ads, livestreams, stories, shop content, image videos, and videos outside configured view or like ranges.
- `Downloads`: Adds download support and download-related controls.
- `Playback speed`: Adds playback speed controls.
- `Remember clear display`: Keeps the clear-display state across videos.
- `SIM spoof`: Spoofs SIM-related values used by TikTok region logic.
- `Sanitize sharing links`: Cleans TikTok share links before they leave the app.
- `Show seekbar`: Shows the video seekbar where TikTok would normally hide it.

## Planned Work

- [/] Drama mini series feed video filter
- [/] Custom offline video download limit
- [ ] Remove create button
- [ ] Remove Tako AI
- [ ] Copy comments without the original commenter's username
- [ ] Feed tab navigation toggles for Friends, Explore, Following, and For You

## Building

Build the Morphe patch bundle and metadata:

```bash
./gradlew :patches:buildAndroid :patches:generatePatchesList
```

The generated bundle is written to:

```text
patches/build/libs/patches-0.1.0.mpp
```

Morphe reads `patches-bundle.json` from this repository, downloads the `.mpp` release asset listed there, and loads the patch metadata from that bundle.

## Project Structure

- `patches/`: Kotlin patch definitions, fingerprints, and shared patch utilities.
- `extensions/`: Java extension code injected into TikTok by the patches.
- `patches-list.json`: Generated patch metadata.
- `patches-bundle.json`: Morphe source metadata for the published release bundle.

## Attribution

This project is built from [RookieEnough/De-Vanced](https://github.com/RookieEnough/De-Vanced) and uses the [Morphe patches template](https://github.com/MorpheApp/morphe-patches-template). Some patch code also traces back to Morphe and ReVanced sources where noted in file headers.

## Notes

- This is an unofficial patch source.
- It is not affiliated with TikTok, ByteDance, Morphe, ReVanced, or De-Vanced.
- TikTok changes often, so compatibility is intentionally tied to the exact version listed above.

## License

This project reuses the GPLv3 licensing from the projects it was built on.

See [LICENSE](LICENSE) and [NOTICE](NOTICE).
