# Ripple Android documentation-to-code map

**Created:** 2026-09-25  
**Implementation status updated:** 2026-09-25

**Target root:** `/Users/urkman/Development/projects/ripple/android`

This map is the implementation index for the 22 stable surface IDs in the
shared catalog. The Android handoff recommends an `Android/` repository folder;
the user explicitly made the current directory the Android target, so the same
module names are rooted here rather than under a second `Android/` directory.

## Shared ownership map

| Concern | Android owner | Contract |
|---|---|---|
| Domain values, invariants, use cases, read models | `core:domain` | `Docs/shared/Ripple_DATA_MODEL.md:53-107`, `:351-387` |
| Room entities, DAOs, transactions, repositories | `core:storage` | `Docs/shared/Ripple_DATA_MODEL.md:310-326`, `:389-422` |
| Named colors, typography, metrics, motion, reusable primitives | `core:designsystem` | `Docs/shared/Ripple_DESIGN_SYSTEM.md:40-57`, `:59-221` |
| Preferences and authorization status | `core:preferences`, `core:health` | `Docs/shared/Ripple_DATA_MODEL.md:171-217`; `Docs/shared/Android/ANDROID_ARCHITECTURE.md:675-706` |
| Phone/watch envelope and projection | `core:wear-sync` | `Docs/shared/Ripple_DATA_MODEL.md:342-349`; `Docs/shared/Android/ANDROID_ARCHITECTURE.md:945-1025` |
| Phone app composition and adaptive navigation | `app` | `Docs/shared/Android/ANDROID_ARCHITECTURE.md:357-395`, `:708-784` |
| Phone feature presentation | `feature:*` modules | `Docs/shared/Android/ANDROID_ARCHITECTURE.md:278-322` |
| Widgets, tile, notifications, shortcuts, export, complication | `system:*` modules | `Docs/shared/Android/ANDROID_ARCHITECTURE.md:883-944` |
| Wear app composition and screens | `wear` | `Docs/shared/Android/ANDROID_ARCHITECTURE.md:945-1025` |

## Surface map

The implementation paths below are relative to the target root. “Build/data”
means the surface is represented by the documented operation and compiles with
the current project. “Phone runtime” means it was exercised on the API 36
phone AVD; Wear and tablet runtime evidence remain separate below.

| Stable ID | Canonical contract | Android entry point | State/data/domain owner | Design tokens/components | Verification status |
|---|---|---|---|---|---|
| `today` | `Docs/shared/screens/today.md` v1.2.3; `Docs/shared/Android/ANDROID_UI_SPEC.md` §4.4 | `feature/today/src/main/kotlin/com/stefansturm/ripple/feature/today/TodayScreen.kt`; `TodayViewModel.kt`; `TodayHeroMotion.kt`; `AndroidTiltController.kt` | `ObserveToday`; `LogIntake`; `UndoLastIntake`; UI-only gravity adapter | `RippleTheme`, `RippleHero` Canvas/Path primitives, `GlassCard`, `QuickAddCluster`, `LogButton`; named geometry/motion tokens | Build/data pass; phone runtime pass: tapered tumbler/zero state, single pour stream, coalesced taps, Undo, reduced-motion setting, localized semantics; tablet runtime pass: adaptive wide hero/row |
| `custom-amount` | `Docs/shared/screens/custom-amount.md` | `feature/today/src/main/kotlin/com/stefansturm/ripple/feature/today/TodayScreen.kt` amount sheet | draft amount/container; `LogIntake` on Add | named sheet, `Slider`, `ContainerChip`, `AmountReadout` | Build/data pass; phone runtime pass: sheet, 250/200/500 container choices, Add |
| `history` | `Docs/shared/screens/history.md` | `feature/history/src/main/kotlin/com/stefansturm/ripple/feature/history/HistoryScreens.kt`; `HistoryViewModels.kt` | `ObserveMonth`; `ObserveHistory` | `DayRing`, `GlassCard`, calendar primitives | Build/data pass; phone runtime pass: month grid and logged date |
| `day-detail` | `Docs/shared/screens/day-detail.md` | `feature/history/src/main/kotlin/com/stefansturm/ripple/feature/history/HistoryScreens.kt` detail route | day snapshot; `EditIntake`, `DeleteIntake`, `RestoreIntake`, `UndoLastIntake` | day header, intake rows, `EmptyState`, Snackbar | Build/data pass; phone runtime pass: September 25 detail and Edit/Delete actions |
| `edit-intake` | `Docs/shared/screens/edit-intake.md` | `feature/history/src/main/kotlin/com/stefansturm/ripple/feature/history/HistoryScreens.kt` edit sheet | draft; `EditIntake` with stable ID | named form components and native amount controls | Build/data pass; direct runtime flow pending |
| `stats` | `Docs/shared/screens/stats.md` | `feature/stats/src/main/kotlin/com/stefansturm/ripple/feature/stats/StatsScreen.kt` | `ObserveStats` | summary cards, accessible chart descriptions, named typography | Build/data pass; phone runtime pass: Stats route and Week/Month/Year controls |
| `settings` | `Docs/shared/screens/settings.md` | `feature/settings/src/main/kotlin/com/stefansturm/ripple/feature/settings/SettingsScreen.kt`; `SettingsViewModel.kt` | profile/goal/container/reminder repositories and use cases | settings rows, `GlassCard`, native switches/pickers | Build/data pass; phone runtime pass: Settings entry and Profile/Units/Containers/Reminders groups |
| `add-container` | `Docs/shared/screens/add-container.md` | `feature/settings/src/main/kotlin/com/stefansturm/ripple/feature/settings/SettingsScreen.kt` add sheet | draft; `UpsertContainer` | named form components, native range control | Build/data pass; direct runtime flow pending |
| `edit-container` | `Docs/shared/screens/edit-container.md` | `feature/settings/src/main/kotlin/com/stefansturm/ripple/feature/settings/SettingsScreen.kt` edit sheet | stable container draft; `UpsertContainer`/`DeleteContainer` | same named form components and destructive action | Build/data pass; direct runtime flow pending |
| `edit-reminder` | `Docs/shared/screens/edit-reminder.md` | `feature/settings/src/main/kotlin/com/stefansturm/ripple/feature/settings/SettingsScreen.kt` reminder sheet | reminder draft; `RescheduleReminders` and `ReminderScheduler` | switches, native time picker, interval controls | Build/data pass; direct runtime flow pending |
| `onboarding` | `Docs/shared/screens/onboarding.md` | `feature/onboarding/src/main/kotlin/com/stefansturm/ripple/feature/onboarding/Onboarding.kt` | `UpdateProfile`, `UpdateGoal`, auth handoff, finish flag | page container and named tokens | Build/data pass; phone runtime pass: all six pages and Start Ripple |
| `watch-today` | `Docs/shared/screens/watch-today.md` | `wear/src/main/kotlin/com/stefansturm/ripple/wear/WearScreens.kt`; `WearViewModel.kt` | local Wear snapshot; open amount sheet | Wear Material3, flat level/readout, single `+` | Build/data pass; Wear runtime pass on `ripple_wear`: readable Today values, one `+`, local log and sync-pending status |
| `watch-custom-amount` | `Docs/shared/screens/watch-custom-amount.md` | `wear/src/main/kotlin/com/stefansturm/ripple/wear/WearScreens.kt` amount dialog | draft; `LogIntake` source `watch` on confirm | three presets, rotary-aware adjustment, explicit confirm | Build/data pass; Wear runtime pass: current amount, preset selection, scroll to Confirm, confirmed local log; physical rotary input not exercised |
| `watch-history` | `Docs/shared/screens/watch-history.md` | `wear/src/main/kotlin/com/stefansturm/ripple/wear/WearScreens.kt` history page | seven-day history flow | Wear `ScalingLazyColumn`, native scroll | Build/data pass; Wear runtime pass: seven-day list and logged day semantics |
| `watch-day-detail` | `Docs/shared/screens/watch-day-detail.md` | `wear/src/main/kotlin/com/stefansturm/ripple/wear/WearScreens.kt` detail page | day snapshot; `DeleteIntake`, `RestoreIntake` | Wear rows and undo feedback | Build/data pass; Wear runtime pass: selected day and logged entry/delete affordance |
| `watch-stats` | `Docs/shared/screens/watch-stats.md` | `wear/src/main/kotlin/com/stefansturm/ripple/wear/WearScreens.kt` stats page | ISO-week `ObserveStats` | Wear summary and single accessible chart | Build/data pass; Wear runtime pass: ISO-week summary and accessible chart description |
| `widget` | `Docs/shared/screens/widget.md` | `system/widgets/src/main/kotlin/com/stefansturm/ripple/system/widgets/RippleWidget.kt` | DataStore projection; action delegates to app entry | Glance layout and localized stale state | Build/data pass; widget host runtime pending |
| `quick-log-control` | `Docs/shared/screens/quick-log-control.md` | `system/quicksettings/src/main/kotlin/com/stefansturm/ripple/system/quicksettings/QuickLogTileService.kt` | resolve default; `LogIntake` source `control` | native Quick Settings tile | Build/data pass; system-panel runtime pending |
| `notification-actions` | `Docs/shared/screens/notification-actions.md` | `system/notifications/src/main/kotlin/com/stefansturm/ripple/system/notifications/ReminderReceiver.kt` and scheduler | reminder projection; `LogIntake` source `notification` | native localized notification action | Build/data pass; notification runtime pending |
| `shortcuts-and-intents` | `Docs/shared/screens/shortcuts-and-intents.md` | `system/appactions/src/main/kotlin/com/stefansturm/ripple/system/appactions/RippleIntentContract.kt`; `app/src/main/kotlin/com/stefansturm/ripple/MainActivity.kt`; `app/src/main/res/xml/shortcuts.xml` | parse amount/unit/container; `LogIntake` source `intent` | launcher shortcuts and deep-link intents | Build/data pass; external launcher/Assistant runtime pending |
| `complication` | `Docs/shared/screens/complication.md` | `wear/src/main/kotlin/com/stefansturm/ripple/wear/complications/RippleComplicationService.kt` | local projected snapshot; tap-to-open | Wear short-text complication data | Build/data pass; complication host runtime pending |
| `share-export` | `Docs/shared/screens/share-export.md` | `system/export/src/main/kotlin/com/stefansturm/ripple/system/export/ExportDocumentAdapter.kt`; Settings share actions | `ExportData` with no mutation | native Android share/save affordances | Build/data pass; share target runtime pending |

## Implementation and acceptance evidence

- Domain, Room storage, DataStore projection, Health Connect gateway, Wear
  envelope, named design system, phone features, Wear features, and system
  adapters are implemented in the modules listed in the shared ownership map.
- `:app:assembleDebug`, `:wear:assembleDebug`, and
  `:app:assembleDebugAndroidTest` pass. Domain and Wear-sync unit tests pass.
- `:app:lintDebug` and `:wear:lintDebug` pass with 0 errors. App has 10
  dependency-update warnings; Wear has 2 `PluralsCandidate` localization
  heuristics. These are recorded in the completion report.
- `MainActivitySmokeTest` passes directly on the API 36 phone AVD. Manual
  phone evidence covers six-page onboarding, Today, the updated tapered
  tumbler zero state, quick add, one active pour stream, coalesced quick taps,
  Undo, reduced-motion logging, custom amount, History, day detail, Stats, and
  Settings. The Today UI tree exposes the localized date and one coherent
  consumed/goal/remaining/percentage accessibility summary.
- The 2026-09-25 Today contract update is implemented without changing shared
  documentation: the Android design-system hero now owns the documented
  `GlassShape`/`WaterFill`/`PourStreamShape` geometry, while the Today feature
  owns the finite shared-clock animation and lifecycle-scoped tilt adapter.
- Tablet runtime is verified on `medium_tablet` (2560×1600) for the updated
  Today empty state and quick add. Wear runtime is verified on `ripple_wear`
  (450×450) for Today, the amount sheet and confirmed local logging, History,
  Day Detail, and Stats. The physical rotary interaction itself was not
  exercised.
- Widget host, notification/Quick Settings host runtime, Health Connect
  provider behavior, complication host behavior, and two-device Data Layer
  sync remain unverified.
- The shared handoff contains no Android runtime captures
  (`Docs/shared/Android/UI/README.md:7-24`), so the phone captures created
  during verification are implementation evidence rather than shared-contract
  evidence.
