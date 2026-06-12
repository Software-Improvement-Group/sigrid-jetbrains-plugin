# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.0.5] - 2026-06-12

### Added

- **Column filters** — click the filter icon in any filterable column header to narrow findings by risk level, status, or dependency type; multiple values can be selected and filters on different columns combine; active filters are indicated by a highlighted filter icon.
- **Dependency type** — the Transitive column in the Open Source Health tab now shows a typed `DependencyType` value (`Direct`, `Transitive`, or `Unknown`) instead of a raw API string, and can be filtered.
- **"No results" inline label** — when active column filters or a search query produce no matches, the table stays visible and a red message is shown below it, keeping the toolbar accessible.

### Changed

- **Filter and search controls disabled when unconfigured** — the file-filter toggle and search field are now grayed out until the plugin is configured with valid credentials.

## [0.0.4] - 2026-06-11

### Added

- **Filter by active file** — a segmented button in each panel toolbar lets you switch between showing all findings and showing only findings for the file currently open in the editor.
- **Filter state preserved across tabs** — the active-file filter toggle is remembered independently per tab when switching between Maintainability, Security, and Open Source Health.
- **Unit tests** — coverage for `FileFilterPanel` including path matching, toggle behavior, and edge cases for nested file paths.

### Changed

- **Context menu** — `FindingEditPopupHandler` refactored into `FindingContextMenuHandler`; the context menu now also exposes the **Navigate to location** action alongside **Edit…**.
- **Blank-state messages** — empty-table messages are more specific when the active-file filter is on and no findings exist for the current file.

## [0.0.3] - 2026-06-09

### Added

- **Edit findings** — click the edit button (or press the keyboard shortcut) on any finding to open a dialog for changing its status and adding a remark; the change is sent to the Sigrid API immediately.
- **Batch editing** — select multiple rows in the findings table and edit all of them at once; supports mixed statuses and remarks, capped at 25 findings per batch.
- **Multi-row selection** — findings tables now support multi-row selection via standard keyboard and mouse gestures.
- **Global refresh** — a single Refresh action in the tool window title bar replaces the per-panel refresh buttons; one click reloads data across all tabs.
- **Unit tests** — coverage for `EditFindingDialog` and `FindingEditPopupHandler`, including status/remark resolution logic for mixed-selection batches.

### Changed

- **Search field** — upgraded from `JBTextField` to `SearchTextField` for a more native look and built-in clear button.
- **Edit dialog title** — now includes the finding's file location for easier identification.
- **Row selection preserved on refresh** — the selected finding row is restored after data is reloaded.
- **Edit button tooltip** — tooltip text added to the edit toolbar button.

### Fixed

- Security findings displayed incorrect file locations in the table.

## [0.0.2] - 2026-06-05

### Added

- **File navigation** — double-clicking a finding in any panel opens the corresponding file at the exact line in the editor. When a finding spans multiple locations a popup lets you choose which one to jump to.
- **`FindingNavigator`** — dedicated class encapsulating navigation logic (location filtering, line mapping, multi-location popup) extracted from the panel layer for reuse and testability.
- **Unit tests** for `FindingNavigator` covering single-location navigation, multi-location popup, invalid/missing file handling, line-number edge cases, and popup item text formatting.

## [0.0.1] - 2026-06-02

### Added

- **Maintainability panel** — displays refactoring candidates fetched from the Sigrid API, with severity icons and file locations.
- **Security panel** — displays security findings fetched from the Sigrid API, with risk level indicators.
- **Open Source Health panel** — displays OSH findings fetched from the Sigrid API.
- **Search** — real-time text filtering across all three panels.
- **Severity icons** (`RiskIcon`, `RiskIconCellRenderer`) — color-coded icons rendered in table cells for each risk/severity level.
- **Data models** — `RefactoringCandidate`, `SecurityFinding`, `OpenSourceHealth`, `FileLocation`, `FindingStatus`, `MaintainabilitySeverity`, `RiskSeverity`.
- **Mappers** — `RefactoringCandidateMapper`, `SecurityFindingMapper`, `OpenSourceHealthMapper`, and shared `MapperUtils` (`normalizePath`, `toDisplayFilePath`).
- **Sigrid API service** — HTTP client (`SigridApiService`) fetching refactoring candidates, security findings, and OSH findings; supports `PATCH` for updating finding status.
- **Credential management** — `PasswordSafeCredential` abstraction for storing API keys and tokens securely via the IDE's `PasswordSafe`.
- **IDE settings** — application-level settings (API key, customer, Sigrid URL) via `SigridSettingsConfigurable`; per-project overrides via `SigridProjectSettingsConfigurable`.
- **Localization** — all user-facing strings moved to `Sigrid.properties` resource bundle.
- **Tool window** — `SigridWindowFactory` / `SigridWindow` with tabbed layout anchored to the IDE bottom bar, with Sigrid branding icons.
- **Test coverage** — unit tests for mappers (`RefactoringCandidateMapper`, `SecurityFindingMapper`, `OpenSourceHealthMapper`, `MapperUtils`) and models (`FindingStatus`, `MaintainabilitySeverity`, `RiskSeverity`).