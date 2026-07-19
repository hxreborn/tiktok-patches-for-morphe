<p align="center">
  <img src="assets/readme-header.png" alt="TikTok Patches for Morphe" width="290"/>
</p>

<p align="center">
  <a href="https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-43-8-3-release/tiktok-43-8-3-2-android-apk-download/"><img alt="TikTok 43.8.3" src="https://img.shields.io/badge/TikTok-43.8.3-ff0050?style=flat-square" /></a>
  <a href="https://github.com/hxreborn/tiktok-patches-for-morphe/releases/latest"><img alt="release" src="https://img.shields.io/github/v/release/hxreborn/tiktok-patches-for-morphe?style=flat-square&color=ff0050&label=release" /></a>
  <a href="https://github.com/hxreborn/tiktok-patches-for-morphe/commits/main"><img alt="commits since release" src="https://img.shields.io/github/commits-since/hxreborn/tiktok-patches-for-morphe/latest?style=flat-square&color=00b894&label=since%20release" /></a>
  <a href="LICENSE"><img alt="license" src="https://img.shields.io/badge/license-GPLv3-blue?style=flat-square" /></a>
</p>

# TikTok Patches for Morphe

<br>

Personal TikTok patches for [Morphe](https://github.com/MorpheApp/morphe-cli), based on [icysymmetra](https://github.com/icysymmetra)'s work. Targets `com.zhiliaoapp.musically` on [TikTok `43.8.3`](https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-43-8-3-release/tiktok-43-8-3-2-android-apk-download/).

<br>

## Install

On the device where Morphe is installed: [add to Morphe](https://morphe.software/add-source?github=hxreborn/tiktok-patches-for-morphe), or paste the repo URL as a source: `https://github.com/hxreborn/tiktok-patches-for-morphe`.

Or try a prebuilt APK (at your own risk): [hxreborn/Morphe-AutoBuilds](https://github.com/hxreborn/Morphe-AutoBuilds/releases/latest).

<br>

## Patches

Everything from [icysymmetra's upstream](https://github.com/icysymmetra/tiktok-patches-for-morphe), plus:

- Disable AI-generated posts
- Disable telemetry, ByteDance/AppsFlyer/Firebase
- Hide the in-feed playlist bar
- Hide the floating promotional event badge, e.g. FIFA World Cup
- Separate image and video download folders

<br>

## Building

```bash
./gradlew :patches:buildAndroid :patches:generatePatchesList
```

Outputs `patches/build/libs/patches-<version>.mpp`.

<br>

## Credits

Built on [icysymmetra/tiktok-patches-for-morphe](https://github.com/icysymmetra/tiktok-patches-for-morphe) and everyone upstream of it. See [NOTICE](NOTICE) for full attribution. Not affiliated with TikTok, ByteDance, or Morphe.

<br>

## License

GPLv3. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
