# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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