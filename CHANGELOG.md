# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.7] - 2026-10-29

### Changed

- **"Fix it" terminal launch** — Claude Code now opens through the terminal tab builder instead of the deprecated `createShellWidget`, and is started directly with its arguments rather than through a shell command line.
- Removed the deprecated `DynamicBundle` constructor usage and compiled with `-jvm-default=no-compatibility`, so the Marketplace verifier no longer reports deprecated API overrides and calls.

## [1.0.6] - 2026-10-28

### Added

- **Prioritized tab** — a new first tab merges Maintainability, Security, Reliability, and Open Source Health findings into a single cross-capability ranked list, reusing the existing table, search, and Scope UI. Each row has a searchable "Why" column explaining its rank.
- **Reliability findings** — the plugin now fetches Reliability findings from the Sigrid API; they are surfaced in the Prioritized tab.
- **Ranking rules** — Security findings and Open Source Health vulnerabilities are ranked first, followed by Reliability findings, then the remaining findings by severity and size (duplication by lines of code, unit size by weight, unit complexity by McCabe, unit interfacing by parameter count, module coupling by fan-in). Duplication findings are sequenced first within the remaining ones when their files overlap, and findings with the same priority, category, and location are ordered by start line.
- **Open Source Health in the Prioritized tab** — only dependencies with a known vulnerability are listed, ranked by vulnerability risk (real CVE/CVSS data), with duplicate vulnerabilities and dependencies sharing a library name collapsed into one finding.
- **Deprioritization of dormant files** — Maintainability findings in files with no Git activity drop one severity tier, with the reason in the "Why" column. Sigrid's change history only covers a short window, so the plugin also checks the local Git history over the last 12 months (needs the Git plugin; without it only Sigrid's window is used). Dates only are shown in the reason.
- **Objectives awareness** — findings in a capability whose quality objective is already met are dampened by one severity tier (Security and Reliability findings are always prioritized). Maintainability compares its live rating against the configured target, or a 3.0-star market benchmark if none is set; Open Source Health compares against the market benchmark. The Maintainability rating is fetched once at project open and refreshed when Sigrid settings change.
- **"Your files" section** — findings in files you changed on the current branch (committed since it left the default branch, or uncommitted) are pinned at the top ("Prioritized because you modified this file") and update as you edit. Needs the Git plugin for the branch diff; uncommitted changes work with any VCS.
- **"Show all"** — the Prioritized tab opens with the top 50 findings; "Show all" reveals the rest and "Show top 50" collapses it again. Searching or filtering always covers every finding.
- **Group by file** — a toggle nests Prioritized findings under expandable file rows (each finding under its first file location); expand/collapse state is kept across searches, filters, and refreshes.
- **Exclusion and test-code lanes** — findings in vendored, generated, or config files are dropped from the Prioritized tab, and findings in test code are routed to a separate lane.
- **Line numbers for Security and Reliability findings** — the Prioritized tab's location column shows `file:line`; the line is searchable and included in the fix prompt title.

### Changed

- **Architecture tab loads with the other tabs** — it is no longer loaded lazily on first open. It shares a single raw architecture graph request with the Prioritized tab, and responses are cached between tabs until you press Refresh.
- All tabs now render in an IntelliJ `TreeTable` instead of a plain table. Flat tabs look the same; the first left-aligned column hosts the expander.

### Fixed

- **Duplicate security findings** — when the API returns the same finding several times (same location, rule, and severity, differing only in id and first-seen date), it is now shown once in the Security tab and the Prioritized list. A triaged copy (non-RAW status or a remark) is kept over untriaged ones.
- **Prioritized tab reloads when Git repositories appear after startup** — on IDE start, Git could still be registering its repositories when the tab first loaded, so the local-history check was skipped until a manual refresh.

## [1.0.5] - 2026-10-27

### Added

- **Onboard from the IDE** — when the configured system does not exist in Sigrid yet, the tool window explains this and offers an "Onboard" button. It uploads the repository (respecting `.gitignore`) to Sigrid; onboarding takes a few minutes, so use Refresh to check back.

## [1.0.4] - 2026-10-21

### Added

- **Architecture tab** — lists undesirable and cyclic dependencies between components, derived from the raw architecture graph, with "Fix with AI" support. The architecture graph is large, so the tab is only loaded once you first open it.

### Changed

- **Findings toolbar layout** — the Scope control is left-aligned with a "Scope:" label (the "Active" option is now "Active file"), the action buttons sit directly left of the search bar, and all toolbar controls share the same height.
- **Column order** — Description now comes before Location on the Maintainability and Security tabs.

## [1.0.3] - 2026-10-05

### Changed

- **Sigrid Axis support** — "Fix with AI" now uses the Sigrid Axis plugin skills (`/axis:autofix maintainability|security|open-source`), including for security findings. Note that `/axis:autofix` commits its fixes on a local branch, and for security findings updates their status in Sigrid.
- **Legacy fallback** — when only the old `sigrid-ai-toolkit` plugin is enabled, the prompt keeps using its skills (`/sigrid:sigrid-improve`, `/sigrid:resolve-security-findings`, `/sigrid:fix-osh-risk`).
- The install hint now links to the [Sigrid Axis installation guide](https://docs.sigrid-says.com/axis/installation.html).

## [1.0.2] - 2026-09-07

### Fixed

- **Duplication finding descriptions** — descriptions now show the correct metric: the number of duplicated lines of code (`loc`) instead of the duplication frequency count (`weight`).

## [1.0.1] - 2026-08-12

### Added

- **"Fix with AI" (Claude Code) integration** — a **Fix with AI** toolbar button and a **Fix it with Claude Code** right-click context menu item are available on all three panels (Maintainability, Security, Open Source Health) whenever Claude Code is detected. Selecting one or more findings and triggering the action builds a natural-language prompt — including a Customer/System context line, a numbered list of findings with severities and file locations, and per-category Sigrid slash commands (`/sigrid:sigrid-improve`, `/sigrid:fix-osh-risk`) when the Sigrid MCP plugin is detected — then hands it off to the `claude` CLI in a terminal.
- **Sigrid MCP detection** — the plugin checks `~/.claude/settings.json` to confirm the Sigrid MCP plugin is installed and enabled; when it isn't, the generated prompt falls back to plain instructions and a once-per-session notification offers an **Install Sigrid Plugin** link.
- **AI agent abstraction** (`AiAgentProvider` / `AiAgentRegistry` / `ClaudeCodeProvider`) — a pluggable registry for AI coding agents so additional agents can be added alongside Claude Code in the future.
- **Injection-safe terminal handoff** — the prompt is written to a temporary Markdown file and referenced via `claude "... Read that file first." --add-dir <dir>`, keeping Sigrid-sourced finding text off the shell command line.

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