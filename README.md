<p align="center">
  <img src="metadata/en-US/images/icon.png" height="128" width="128">
</p>

<p align="center">
  <a href="https://github.com/ldeso/blitz/releases/latest">
    <img src="https://img.shields.io/github/release/ldeso/blitz.svg?logo=github&label=GitHub" alt="GitHub" /></a>
  <a href="https://f-droid.org/packages/net.leodesouza.blitz/">
    <img src="https://img.shields.io/f-droid/v/net.leodesouza.blitz?logo=F-Droid&label=F-Droid" alt="F-Droid" /></a>
  <a href="https://play.google.com/store/apps/details?id=net.leodesouza.blitz">
    <img src="https://img.shields.io/badge/Play%20Store-v1.7.0-blue?logo=Google-Play" alt="Play Store" /></a>
</p>

A minimalist [Fischer chess clock](https://en.wikipedia.org/wiki/Fischer_clock) for Android.

This app is free software.
Its [source code](https://github.com/ldeso/blitz) is available under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Features

  - Defaults to 5+3 Fischer timing (5 minutes + 3 seconds per move).
  - Total time and increment can be set by horizontal and vertical dragging.
  - The back action pauses or resets the clock.

## Install

The app can be installed from three different sources:

  1. [GitHub](https://github.com/ldeso/blitz/releases/latest): built and signed by the developer.
  2. [F-Droid](https://f-droid.org/packages/net.leodesouza.blitz/): built and signed by the developer and [verified by F-Droid](https://f-droid.org/docs/Reproducible_Builds/) to correspond to the available source code.
  3. [Play Store](https://play.google.com/store/apps/details?id=net.leodesouza.blitz): built and signed by Google, who is [allowed to modify](https://play.google/play-app-signing-terms/) the app to optimise its performance, security and/or size.

All sources are using the same app signing key, so the app can be updated using a different source to the one it was installed from.
For example, an app installed from F-Droid can be updated to a newer version downloaded from the Play Store.

## About

This is a native Android app that requires no permission and is compatible with Android 5.0+.
It is built entirely with [Jetpack Compose](https://developer.android.com/develop/ui/compose) and implements:

  - Predictive back gestures ([Android 14+](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture))
  - Per-app language preferences ([Android 13+](https://developer.android.com/guide/topics/resources/app-languages))
  - Right-to-left layout support
  - Precise orientation support
  - Keyboard navigation

## Links

[License: Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) — [Website](https://blitz.leodesouza.net) — [Issue Tracker](https://github.com/ldeso/blitz/issues) — [Source Code](https://github.com/ldeso/blitz) — [Changelog](CHANGELOG.md) — [Privacy Policy](PRIVACY_POLICY.md)

## Screenshots

<p align="center">
  &nbsp;<img src="metadata/en-US/images/phoneScreenshots/1.png" alt="Portrait screenshot of the initial view" height="622" width="350">&nbsp;&#8203;
  &nbsp;<img src="metadata/en-US/images/phoneScreenshots/2.png" alt="Portrait screenshot of when time is over" height="622" width="350">&nbsp;
</p>
