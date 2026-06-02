# Sigrid Plugin for JetBrains IDEs

A JetBrains IDE plugin that lets you view and manage [Sigrid](https://www.softwareimprovementgroup.com/sigrid/) findings — maintainability, security, and open source health — without leaving your IDE.

## Features

- **Maintainability** — browse refactoring candidates surfaced by Sigrid, with severity icons and file locations.
- **Security** — review security findings with risk levels directly in your project context.
- **Open Source Health** — inspect third-party dependency risks flagged by Sigrid.
- **Search** — filter findings across all three panels with a real-time search bar.
- **Settings** — configure your Sigrid API key, customer name, and API base URL globally (IDE-level), with per-project overrides.

## Requirements

- One of the following JetBrains IDEs, version 2025.2 or later:
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

Use the search bar at the top of each tab to filter findings by any text.

## Development

```bash
./gradlew build          # Compile, test, and assemble
./gradlew test           # Run tests only
./gradlew runIde         # Launch a sandboxed IDE with the plugin loaded
./gradlew verifyPlugin   # Verify plugin compatibility
```

## License

[MIT](LICENSE)