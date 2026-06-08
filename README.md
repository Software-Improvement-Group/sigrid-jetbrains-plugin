# Sigrid Plugin for JetBrains IDEs

A JetBrains IDE plugin that lets you view and manage [Sigrid](https://www.softwareimprovementgroup.com/sigrid/) findings — maintainability, security, and open source health — without leaving your IDE.

## Features

- **Maintainability** — browse refactoring candidates surfaced by Sigrid, with severity icons and file locations.
- **Security** — review security findings with risk levels directly in your project context.
- **Open Source Health** — inspect third-party dependency risks flagged by Sigrid.
- **File navigation** — double-click any finding to jump to the exact file and line in the editor; a picker appears when a finding has multiple locations.
- **Edit findings** — update a finding's status and remark directly from the table; supports single and batch edits (up to 25 findings at once).
- **Search** — filter findings across all three panels with a real-time search bar.
- **Settings** — configure your Sigrid API key, customer name, and API base URL globally (IDE-level), with per-project overrides.

## Requirements

- One of the following JetBrains IDEs, version 2026.1 or later:
  - IntelliJ IDEA (Community & Ultimate)
  - PyCharm (Community & Professional)
  - WebStorm
  - GoLand
  - PhpStorm
  - RubyMine
  - CLion
  - Rider
  - Android Studio
  - Aqua
- A [Sigrid](https://www.softwareimprovementgroup.com/sigrid/) account with API access

## Installation

Build from source:

```bash
./gradlew buildPlugin
```

The plugin ZIP is written to `build/distributions/`. Then install it from **Settings → Plugins → Install Plugin from Disk**.

## Configuration

1. Open **Settings → Tools → Sigrid**.
2. Enter your **API Key**, **Customer** name, and optionally a custom **Sigrid API URL** (defaults to the SIG-hosted instance).
3. For per-project overrides (e.g. a different system name or API key), open **Settings → Tools → Sigrid → Project Settings**.

## Usage

Once configured, open the **Sigrid** tool window at the bottom of the IDE. The window has three tabs:

| Tab | What it shows |
|-----|--------------|
| Maintainability | Refactoring candidates grouped by category and severity |
| Security | Security findings with risk level and file location |
| Open Source Health | OSH findings for your project's dependencies |

Use the search bar at the top of each tab to filter findings by any text. Double-click a row to open the file at the finding's location; if the finding has multiple locations a picker lets you choose. To edit a finding's status or remark, select one or more rows and click the edit button in the toolbar, use the keyboard shortcut (**F2**), or right-click and choose **Edit…** from the context menu; batch edits are supported for up to 25 findings at a time.

## Development

```bash
./gradlew build          # Compile, test, and assemble
./gradlew test           # Run tests only
./gradlew runIde         # Launch a sandboxed IDE with the plugin loaded
./gradlew verifyPlugin   # Verify plugin compatibility
```

## License

[Apache License 2.0](./LICENSE)