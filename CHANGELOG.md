# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2026-07-13

### Added

- **Azure DevOps integration** — create Azure DevOps work items directly from selected findings in any of the three panels (Maintainability, Security, Open Source Health). A dialog lets you choose the work item type (loaded dynamically from your project), edit the title, and preview the auto-generated description (listing each finding's title, severity, and file locations) before submitting. A success notification includes the work item number and an **Open in Browser** link.
- **Unified "Create Issue" split button** — the separate Jira and Azure DevOps toolbar buttons are consolidated into a single **Create Issue** split button. The primary action targets the first configured tracker; the dropdown arrow exposes all configured trackers plus an **Issue Tracker Settings** shortcut.
- **Azure DevOps settings pages** — a new **Settings → Tools → Sigrid → Issue Trackers → Azure DevOps** global settings page for organization URL, personal access token, and project name; a corresponding per-project override page for URL and PAT.
- **Issue Trackers settings group** — Jira and Azure DevOps settings are now grouped under **Settings → Tools → Sigrid → Issue Trackers** for clearer organization.

## [0.0.11] - 2026-06-30

### Added

- **Jira integration** — create Jira issues directly from selected findings in any of the three panels (Maintainability, Security, Open Source Health). A toolbar button and context menu item appear when Jira is configured; a dialog lets you edit the issue title and preview the auto-generated description (listing each finding's title, severity, and file locations) before submitting.
- **Jira settings page** — a new **Settings → Tools → Sigrid → Jira** project-level settings page consolidates Jira credentials (base URL, project key, username, API token) in one place.
- **Settings shortcut in tool window** — a gear icon in the Sigrid tool window title bar opens the project-scoped Sigrid settings dialog directly, avoiding the need to navigate through **Settings → Tools** manually.

## [0.0.10] - 2026-06-29

### Fixed

- **Internal API removed from `FileFilterPanel`** — `SegmentedButton.getComponent()` (flagged by Marketplace verification as an internal API) is replaced with a `setFocusableRecursively()` helper that walks the Swing component tree using only public API, keeping the filter button out of the focus traversal cycle.

## [0.0.9] - 2026-06-23

### Added

- **"Open in Sigrid" button and context menu item** — each panel toolbar now has an **Open in Sigrid** button that opens the selected finding in the Sigrid web app; the same action is available via the right-click context menu (**F3**). The button is enabled only when the selected finding carries a URL from the API.

### Fixed

- **Focus traversal** — Tab now cycles only between the findings table and the search field; toolbar buttons (filter toggles, Edit, Open in Sigrid) are excluded from the focus cycle.

## [0.0.8] - 2026-06-22

### Fixed

- **HTTPS enforcement** — Sigrid URLs that use `http://` are now rejected at validation time, preventing the Bearer token from being sent over a plain-text connection.
- **API key isolation** — the global API key is no longer forwarded when a project overrides the Sigrid URL without providing its own key; the request is blocked and an error is surfaced instead.
- **Path traversal prevention** — file paths resolved when navigating to a finding location are now checked to stay within the project root, blocking `../` escape sequences.
- **HTML escaping in descriptions** — finding description text is HTML-escaped before being rendered, preventing injected markup from affecting the UI.
- **URL-encoded API path segments** — customer, system, and subsystem names in API request URLs are now percent-encoded, fixing requests that would fail or misroute for names containing special characters.
- **Settings name validation** — customer and system names are validated against an allowlist of safe characters before use, providing a clear error message for invalid input.
- **Edit Finding error surfacing** — HTTP errors returned by the PATCH endpoint are now detected and displayed as an inline error in the dialog; a success balloon notification is shown when the save completes.
- **HTTP client timeouts** — the `HttpClient` now sets explicit connect and request timeouts, preventing the plugin from hanging indefinitely when the Sigrid API is unreachable.
- **EDT visibility of credential cache** — the `loadAsync` credential cache is now safely published to the Event Dispatch Thread, eliminating a potential race where a stale `null` was observed on first use.

### Changed

- **Gradle wrapper checksum** — the Gradle wrapper JAR is now verified with a SHA-256 checksum, hardening the build against a compromised wrapper binary.

## [0.0.7] - 2026-06-17

### Added

- **"Not configured" panel** — when Sigrid is not yet set up, each tab now shows a centered message with a clickable link that opens the Settings dialog directly, instead of a plain text label.
- **Settings link in global settings** — the global Sigrid settings page now shows a link to the per-project settings page, making it easier to navigate to system-specific configuration.
- **Settings change listener** — `SigridSettingsListener` reacts to credential/configuration changes and triggers a panel refresh automatically, so the tool window updates without a manual refresh after saving settings.

### Changed

- **"Customer" renamed to "Portfolio Name"** — the label and help text in the global settings panel now use "Portfolio Name" to match Sigrid's current terminology; the error message for a not-found system was updated accordingly.
- **Sigrid panel visible during indexing** — `SigridWindowFactory` now implements `DumbAware`, so the tool window renders immediately when a project opens, even while the IDE is still indexing.

## [0.0.6] - 2026-06-16

### Added

- **Usage statistics** — a startup activity (`UsageStatisticsActivity`) fires an HTTP request on project open to record anonymous plugin usage; includes unit tests covering the HTTP client logic.

### Fixed

- **Nullable `purl` field** — `OpenSourceHealth.purl` is now nullable to handle API responses that omit the field, preventing JSON deserialization errors.
- **Huge `Tool Windows` menu icon** — added 16x16 and 32x32 icons to solve the issue.

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