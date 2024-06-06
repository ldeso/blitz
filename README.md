<!-- Copyright 2024 Léo de Souza -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<p align="center">
  <a href="https://blitz.leodesouza.net">
    <img src="src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="96" height="96" alt="App icon"></a>
</p>

<p align="center">
  <a href="https://f-droid.org/packages/net.leodesouza.blitz/">
    <img src="https://img.shields.io/f-droid/v/net.leodesouza.blitz?logo=F-Droid&label=F-Droid" width="113" height="20" alt="F-Droid badge"></a>
  <a href="https://accrescent.app/app/net.leodesouza.blitz">
    <img src="https://img.shields.io/badge/Accrescent-v1.9.1-blue.svg?logo=data:image/svg+xml;base64,PHN2ZyBmaWxsPSJ3aGl0ZXNtb2tlIiByb2xlPSJpbWciIHZpZXdCb3g9IjAgMCAyNCAyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48dGl0bGU+QWNjcmVzY2VudDwvdGl0bGU+PHBhdGggZD0iTTEyIDBBMTIgMTIgMCAwIDAgMCAxMmExMiAxMiAwIDAgMCAxMiAxMiAxMiAxMiAwIDAgMCAxMi0xMkExMiAxMiAwIDAgMCAxMiAwem0tLjMgMi45YTkgOSAwIDAgMSA4LjggNi43IDkgOSAwIDAgMS02LjMgMTFBOSA5IDAgMCAxIDQgMTYuM2E3LjggNy44IDAgMCAwIDUuNi42QTcuOCA3LjggMCAwIDAgMTUgNy40IDcuOCA3LjggMCAwIDAgMTEuNyAzeiIvPjwvc3ZnPg==" width="131" height="20" alt="Accrescent badge"></a>
  <a href="https://play.google.com/store/apps/details?id=net.leodesouza.blitz">
    <img src="https://img.shields.io/badge/Play%20Store-v1.9.1-blue?logo=Google-Play" width="129" height="20" alt="Play Store badge"></a>
  <a href="https://github.com/ldeso/blitz/releases/latest">
    <img src="https://img.shields.io/github/release/ldeso/blitz.svg?logo=github&label=GitHub" width="109" height="20" alt="GitHub badge"></a>
</p>

# Blitz: Fischer Chess Clock

A minimalist [Fischer chess clock](https://en.wikipedia.org/wiki/Fischer_clock) for Android.

This app is free software.
Its [source code](https://github.com/ldeso/blitz) is available under the [Apache License 2.0](LICENSES/Apache-2.0.md).

## Features

**A chess clock made for blitz**

  - Defaults to 5+3 Fischer timing: 5 minutes + 3 seconds per move
  - FIDE-compliant: initial time for 5+3 is 5:03, not 5:00
  - Privacy-friendly: no ads, no permissions

**Fast and intuitive controls**

  - Touching anywhere on the screen switches to the next player
  - Time and increment are set by horizontal and vertical dragging
  - Haptic feedback is enabled by setting the ringtone to vibrate
  - Back gestures pause and reset the clock

**Robust time implementation**

  - Uses the most precise time source available on the device
  - Clock implementation tested with 100% code coverage

**Free and secure**

  - Open source with reproducible builds
  - Enables memory tagging on compatible devices
  - Meets Android 14 security requirements

## Install

The app can be installed from four different sources:

  1. [F-Droid](https://f-droid.org/packages/net.leodesouza.blitz/): built and signed by the developer and [verified by F-Droid](https://f-droid.org/docs/Reproducible_Builds/) to correspond to the available source code.
  2. [Accrescent](https://accrescent.app/app/net.leodesouza.blitz): built and signed by the developer.
  3. [Play Store](https://play.google.com/store/apps/details?id=net.leodesouza.blitz): built and signed by Google who can [modify the app](https://play.google/play-app-signing-terms/) to optimize its performance, security and/or size.
  4. [GitHub](https://github.com/ldeso/blitz/releases/latest): built and signed by the developer.

As the app is signed using the same private signing key on all sources, it can be updated using a different source to the one it was installed from.
For example, an app installed from F-Droid can be updated to a newer version downloaded from the Play Store.

<details style="margin-bottom: 15px">
  <summary>Public key certificate fingerprint</summary>
  <pre><code>6d7fd2715ed21cff64086dc5fcf8820a685a793ebd07d972163d86172babba75</code></pre>
</details>

## About

This is a native Android app for Android 5.0+.
It is built with [Jetpack Compose](https://developer.android.com/jetpack/compose) and implements:

  - Predictive back gestures ([Android 14+](https://developer.android.com/about/versions/13/features/predictive-back-gesture))
  - Per-app language preferences ([Android 13+](https://developer.android.com/about/versions/13/features/app-languages))
  - Hardware memory tagging ([Android 13+](https://developer.android.com/ndk/guides/arm-mte))
  - Support for right-to-left languages
  - Precise handling of orientation
  - Keyboard navigation

## Links

[License: Apache-2.0](LICENSES/Apache-2.0.md) — [Website](https://blitz.leodesouza.net) — [Issue Tracker](https://github.com/ldeso/blitz/issues) — [Source Code](https://github.com/ldeso/blitz) — [Changelog](CHANGELOG.md) — [Privacy Policy](PRIVACY_POLICY.md)

## Screenshots

<p align="center">
  &nbsp;<img src="metadata/en-US/images/phoneScreenshots/1.png" alt="Screenshot of the initial view" width="360" height="640">&nbsp;&#8203;
  &nbsp;<img src="metadata/en-US/images/phoneScreenshots/2.png" alt="Screenshot when time is over" width="360" height="640">&nbsp;
</p>
