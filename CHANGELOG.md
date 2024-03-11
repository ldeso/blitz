# Changelog

## [1.5.1] - 2024-03-11

This release fixes a bug introduced in version [1.5.0] where changing the language would change the default duration and time increment.

### Bug Fix

- Fix bug where changing the language would change the default duration and increment

## [1.5.0] - 2024-03-11

This release is a big refactor of the code that tries to follow the principle of separation of concerns, and introduces a better support of the orientation on all devices.

### New Feature

- Properly handle orientation on all devices

### Notes

- Separate chess clock model, view and controller
- Change order of declarations
- Use callbacks instead of passing Window to composable

## [1.4.9] - 2024-03-06

This release reverts the behavior of rounding up to nearest second when above one hour that was introduced in version [1.4.7].

### Enhancement

- Revert rounding up to nearest second above one hour

## [1.4.8] - 2024-03-06

This release fixes a bug introduced in the last release where, above one hour, the displayed time would sometimes be off by up to one second.

### Bug Fix

- Fix wrong time displayed above one hour

## [1.4.7] - 2024-03-06

This release improves the precision of the counter and of the time display.

### Enhancement

- Round up to show the current (thenth of a) second

### Bug Fix

- Update current time on pause

## [1.4.6] - 2024-03-06

This release is another refactor that makes triggering the next turn more consistent between tapping and dragging actions.

### Bug Fix

- Trigger next turn from the end of a drag action instead of its start

### Notes

- Refactor chess clock policy into an interface
- Pass an immutable orientation state to its consumers

## [1.4.5] - 2024-03-05

This release vastly increases the sensitivity to touch events when counting down.

### Enhancement

- Also trigger next turn from drag events

### Note

- Use application ID in build filename

## [1.4.4] - 2024-03-05

This release increases the precision and readability of the clock.

### Enhancement

- Use a bold font to increase readability

### Bug Fix

- Fix a bug that skipped the last tenth of a second

## [1.4.3] - 2024-03-04

This release is a big refactor of the code handling dragging events, fixing two issues and improving the dragging experience.

### Enhancement

- Improve single player's time settings ([#3])

### Bug Fixes

- Use monospace font to prevent digits from jumping ([#2])
- Fix D-Pad events being counted twice

### Notes

- Do not repeat short description in full description
- Add missing documentation

## [1.4.2] - 2024-02-29

This release restores the orientation sensor when using reverse portrait mode while avoiding issues when the orientation is locked.

### Enhancement

- Restore orientation sensor when in reverse portrait

## [1.4.1] - 2024-02-29

This release fixes possible wrong orientations by disabling the orientation sensor when using reverse portrait mode.

### Bug Fix

- Disable orientation sensor when in reverse portrait

### Note

- Add icon and badges to README.md

## [1.4.0] - 2024-02-29

This release adds support for landscape mode and for devices without a touchscreen.

### New Features

- Add true landscape support
- Add support for devices with only a D-pad controller

### Enhancement

- Enable per-app language support

### Note

- Add documentation for isBlackRightHanded

## [1.3.2] - 2024-02-29

This release fixes a bug where, when correcting a player’s time, the drag orientation would not flip based on the orientation of the clock.

### Bug Fix

- Fix frozen drag orientation when correcting time

## [1.3.1] - 2024-02-29

This release fixes a bug where the time could not be restarted after reaching zero.

### Bug Fix

- Fix bug where time could not be restarted after ending

## [1.3.0] - 2024-02-28

This release allows flipping the clock orientation for left-handed players and flips horizontal dragging for a right-to-left layout direction.

### New Feature

- Flip clock based on orientation for left-handed players

### Enhancements

- Support right-to-left layout direction
- Add support for the predictive back gesture

### Bug Fix

- Make state survive activity or process recreation

### Notes

- Add privacy_policy.txt
- Add featureGraphic.png
- Add short description in title
- Shorten changelogs in metadata
- Shorten changelogs even more
- Set screen orientation in AndroidManifest.xml
- Improve code clarity by adding variables and imports
- Give default values to arguments of ChessClock
- Make BasicTime more basic
- Update documentation
- Replace ' with ’
- Replace - with *
- Minor code formatting changes
- Formatting

## [1.2.0] - 2024-02-25

This releases keeps the screen turned on and increases time precision by tracking system time instead of recompositions.

### New Feature

- Keep screen on during countdown

### Enhancement

- Increase precision by updating time independently from refresh rate

### Notes

- Update dependencies
- Update JVM bytecode target version
- Update graddle wrapper
- Remove themes.xml
- Remove tools:targetApi="31" from manifest
- Add documentation
- Change order of function definitions
- Apply auto-formatting
- Make changelogs valid markdown
- Fix link to last version

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

[1.5.1]: https://github.com/ldeso/blitz/releases/tag/v1.5.1
[1.5.0]: https://github.com/ldeso/blitz/releases/tag/v1.5.0
[1.4.9]: https://github.com/ldeso/blitz/releases/tag/v1.4.9
[1.4.8]: https://github.com/ldeso/blitz/releases/tag/v1.4.8
[1.4.7]: https://github.com/ldeso/blitz/releases/tag/v1.4.7
[1.4.6]: https://github.com/ldeso/blitz/releases/tag/v1.4.6
[1.4.5]: https://github.com/ldeso/blitz/releases/tag/v1.4.5
[1.4.4]: https://github.com/ldeso/blitz/releases/tag/v1.4.4
[1.4.3]: https://github.com/ldeso/blitz/releases/tag/v1.4.3
[1.4.2]: https://github.com/ldeso/blitz/releases/tag/v1.4.2
[1.4.1]: https://github.com/ldeso/blitz/releases/tag/v1.4.1
[1.4.0]: https://github.com/ldeso/blitz/releases/tag/v1.4.0
[1.3.2]: https://github.com/ldeso/blitz/releases/tag/v1.3.2
[1.3.1]: https://github.com/ldeso/blitz/releases/tag/v1.3.1
[1.3.0]: https://github.com/ldeso/blitz/releases/tag/v1.3.0
[1.2.0]: https://github.com/ldeso/blitz/releases/tag/v1.2.0
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
[#3]: https://github.com/ldeso/blitz/issues/3
[#2]: https://github.com/ldeso/blitz/issues/2
