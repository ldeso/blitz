# Changelog

## [1.1.3] - 2024-02-22

This release adds metadata and slightly simplifies the source code.

### Enhancements

- Add metadata
- Remove an if block to simplify code
- Further simplify if block

## [1.1.2] - 2024-02-22

This release improves compatibility and fixes a bug preventing a reset when both times are equal to their initial values.

### Bug Fix

- Allow reset when times are equal to their initial values

### Enhancements

- Set minimum SDK version to 21 (Android 5.0)
- Enable Gradle configuration cache

## [1.1.1] - 2024-02-22

This release prevents adjusting the time after it reaches zero and reduces horizontal dragging sensitivity.

### Bug Fixes

- Disallow time adjustments when a time is up
- Reduce horizontal dragging sensitivity

### Note

- Slightly change code structure

## [1.1.0] - 2024-02-21

This release adds the possibility to select the time period using vertical dragging and the time increment using horizontal dragging.

### New Feature

- Implement time input by dragging

## [1.0.7] - 2024-02-21

This release updates and reduces dependencies, bringing the size of the APK down to 686 KiB.

### Enhancements

- Use Compose Foundation instead of Material 3
- Update dependencies

## [1.0.6] - 2024-02-21

This release fixes a bug where it would not be possible to reset if one of the timers did not start.

### Bug Fix

- Allow a reset when one of the timers did not start

### Note

- Use 0L (Long) instead of 0 (Int)

## [1.0.5] - 2024-02-21

This release fixes some bugs introduced with the reset functionality.

### Bug Fixes

- Make it possible to leave the app
- Set first player to white on reset

## [1.0.4] - 2024-02-21

This release makes it possible to reset the clock with the "back" action when the clock is paused.

### Enhancement

- Make the "back" action reset a paused clock

### Notes

- Force portrait orientation
- Move most logic out of LaunchedEffect

## [1.0.3] - 2024-02-20

This release enables resource shrinking and app optimization to bring the size of the APK below one megabyte.

### Enhancement

- Enable resource shrinking and app optimization

## [1.0.2] - 2024-02-20

This release improves the app icon.

### Enhancements

- Implement adaptive icon
- Remove circular icon
- Reduce vector path from 1909 to 674 characters

## [1.0.1] - 2024-02-20

This release fixes a bug where the time could still be incremented after reaching zero.

### Bug Fixes

- Do not increment when time is up
- Handle negative

## [1.0.0] - 2024-02-20

Initial release.

[1.1.3]: https://github.com/ldeso/blitz/releases/tag/v1.1.3
[1.1.2]: https://github.com/ldeso/blitz/releases/tag/v1.1.2
[1.1.1]: https://github.com/ldeso/blitz/releases/tag/v1.1.1
[1.1.0]: https://github.com/ldeso/blitz/releases/tag/v1.1.0
[1.0.7]: https://github.com/ldeso/blitz/releases/tag/v1.0.7
[1.0.6]: https://github.com/ldeso/blitz/releases/tag/v1.0.6
[1.0.5]: https://github.com/ldeso/blitz/releases/tag/v1.0.5
[1.0.4]: https://github.com/ldeso/blitz/releases/tag/v1.0.4
[1.0.3]: https://github.com/ldeso/blitz/releases/tag/v1.0.3
[1.0.2]: https://github.com/ldeso/blitz/releases/tag/v1.0.2
[1.0.1]: https://github.com/ldeso/blitz/releases/tag/v1.0.1
[1.0.0]: https://github.com/ldeso/blitz/releases/tag/v1.0.0
