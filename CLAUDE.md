# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build          # Compile, test, and assemble the plugin
./gradlew test           # Run tests only
./gradlew runIde         # Launch a sandboxed IDE instance with the plugin loaded
./gradlew buildPlugin    # Build the distributable plugin ZIP
./gradlew verifyPlugin   # Verify plugin compatibility
./gradlew clean          # Clean build outputs
```

To run a single test class:
```bash
./gradlew test --tests "com.softwareimprovementgroup.plugins.sigrid.mappers.RefactoringCandidateMapperTest"
```

## Architecture

The plugin targets IntelliJ IDEA 2026.1+ and is written in Kotlin 2.3.21. Package root: `com.softwareimprovementgroup.plugins.sigrid`.

**Entry points registered in `plugin.xml`:**
- `toolWindow` → `SigridWindowFactory` — creates a bottom tool window with 3 tabs (Maintainability, Security, Open Source Health)
- `postStartupActivity` → `MyProjectActivity` — runs on every project open (currently unused)
- `applicationConfigurable` → `SigridSettingsConfigurable` — global settings under Settings → Tools → Sigrid
- `projectConfigurable` → `SigridProjectSettingsConfigurable` — per-project settings

**Data flow:**

1. `SigridWindowFactory` creates one panel per tab and wires up cross-panel search callbacks
2. Each panel extends `SigridPanel` (abstract base with search, table, loading/error states)
3. On refresh, the panel calls `SigridApiService.get*Findings(project)` on a pooled thread
4. `SigridApiService` uses the effective credentials from `SigridProjectConfiguration` to make an HTTP GET with a Bearer token; JSON is parsed via GSON
5. A mapper class transforms the API response into domain models, filtering by subsystem and computing display fields
6. The panel updates the table on the EDT via `ApplicationManager.getApplication().invokeLater {}`

**Configuration hierarchy:**

- **Global** (`SigridConfiguration`, app-scoped service): `apiKey` (PasswordSafe), `customer`, `sigridUrl`
- **Project** (`SigridProjectConfiguration`, project-scoped service): `system` (required), `subsystem` (optional), plus optional per-project overrides for apiKey/customer/sigridUrl
- Effective value resolution: project override takes precedence over global; `isConfigurationValid` checks apiKey + customer + system are non-blank
- Credentials are stored in the platform's PasswordSafe (not in XML); `PasswordSafeCredential` wraps async loading

**Panels:**
- `SigridPanel` — abstract base; subclasses implement `fetchData()`, `getColumnNames()`, and `toRow()`
- `MaintainabilityPanel`, `SecurityPanel`, `OpenSourceHealthPanel` — one per tab
- `RiskIcon` / `RiskIconCellRenderer` — severity-to-color mapping and table cell rendering

**Mappers** (`mappers/` package) contain most of the business logic and are comprehensively unit-tested:
- Filter findings by subsystem
- Map API string enums to typed enums (`MaintainabilitySeverity`, `RiskSeverity`, `FindingStatus`)
- Compute display fields: `displayLocation` (abbreviated path), `normalizedPath` (strips subsystem prefix)
- Sort results (severity descending, then name/path ascending)
- OSH mapper uses `maxOf()` across 7 risk dimensions to derive an overall risk level

**Localization:** All user-facing strings go through `SigridBundle` (wraps `DynamicBundle`) backed by `src/main/resources/messages/Sigrid.properties`. Access via `SigridBundle["key"]` or `SigridBundle["key", param1]`.

**Icons:** `src/main/resources/icons/` — `sigrid.png` (light) and `sigrid_dark.png` (dark); the platform selects the dark variant automatically.

## Key Configuration

- `gradle.properties` — plugin version, group ID, GitHub repo URL
- `src/main/resources/META-INF/plugin.xml` — plugin ID (`com.softwareimprovementgroup.plugins.sigrid`), extension registrations, dependencies
- Gradle configuration cache and build cache are enabled; avoid imperative Gradle scripts that break caching