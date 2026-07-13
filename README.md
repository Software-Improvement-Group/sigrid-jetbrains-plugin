# Sigrid Plugin for JetBrains IDEs

A JetBrains IDE plugin that lets you view and manage [Sigrid](https://www.softwareimprovementgroup.com/sigrid/) findings — maintainability, security, and open source health — without leaving your IDE.

## Features

- **Maintainability** — browse refactoring candidates surfaced by Sigrid, with severity icons and file locations.
- **Security** — review security findings with risk levels directly in your project context.
- **Open Source Health** — inspect third-party dependency risks flagged by Sigrid.
- **File navigation** — double-click any finding to jump to the exact file and line in the editor; a picker appears when a finding has multiple locations.
- **Filter by active file** — toggle the active-file filter in the panel toolbar to see only findings for the file currently open in the editor; the filter state is remembered per tab.
- **Column filters** — click the filter icon in any column header to narrow findings by risk level, status, or dependency type; active filters are highlighted and combine across columns.
- **Open in Sigrid** — click the **Open in Sigrid** button in the toolbar (or press **F3**, or use the right-click context menu) to open a finding directly in the Sigrid web app.
- **Edit findings** — update a finding's status and remark directly from the table; supports single and batch edits (up to 25 findings at once).
- **Issue tracker integration** — create a Jira issue or Azure DevOps work item from any selected finding via the **Create Issue** split button in the toolbar or the right-click context menu; a dialog lets you edit the title and preview the auto-generated description before submitting.
- **Search** — filter findings across all three panels with a real-time search bar.
- **Settings** — configure your Sigrid API key, portfolio name, and API base URL globally (IDE-level), with per-project overrides; the tool window refreshes automatically when settings change. A gear icon in the tool window title bar opens project settings directly.

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

Install from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32479-sigrid) (search for **Sigrid**), or build from source::

```bash
./gradlew buildPlugin
```

The plugin ZIP is written to `build/distributions/`. Then install it from **Settings → Plugins → Install Plugin from Disk**.

## Configuration

1. Open **Settings → Tools → Sigrid**.
2. Enter your **API Key**, **Portfolio Name**, and optionally a custom **Sigrid API URL** (defaults to the SIG-hosted instance).
3. For per-project overrides (e.g. a different system name or API key), click **Configure system and other per-project Sigrid settings…** at the bottom of the global settings page, or open **Settings → Tools → Sigrid → Project Settings** directly. You can also reach project settings via the gear icon in the Sigrid tool window title bar.
4. To enable Jira integration, open **Settings → Tools → Sigrid → Issue Trackers → Jira** and enter your Jira base URL, project key, username, and API token.
5. To enable Azure DevOps integration, open **Settings → Tools → Sigrid → Issue Trackers → Azure DevOps** and enter your organization URL, personal access token, and project name. Per-project overrides are available under **Settings → Tools → Sigrid → Issue Trackers → Azure DevOps → Project Settings**.

## Usage

Once configured, open the **Sigrid** tool window at the bottom of the IDE. The window has three tabs:

| Tab | What it shows |
|-----|--------------|
| Maintainability | Refactoring candidates grouped by category and severity |
| Security | Security findings with risk level and file location |
| Open Source Health | OSH findings for your project's dependencies |

Use the search bar at the top of each tab to filter findings by any text. Use the segmented button in the toolbar to switch between **All findings** and **Active file** — the latter shows only findings for the file currently open in the editor. Click the filter icon in a column header to open a dropdown and select one or more values to filter by (risk level, status, or dependency type); active filters are highlighted and stack across columns. Double-click a row to open the file at the finding's location; if the finding has multiple locations a picker lets you choose. To edit a finding's status or remark, select one or more rows and click the edit button in the toolbar, use the keyboard shortcut (**F2**), or right-click and choose **Edit…** from the context menu; batch edits are supported for up to 25 findings at a time. To open a finding in the Sigrid web app, select a row and click **Open in Sigrid** in the toolbar, press **F3**, or choose **Open in Sigrid** from the right-click context menu. Right-clicking also exposes **Navigate to location** for quick file navigation. When one or more issue trackers are configured, a **Create Issue** split button appears in the toolbar and a **Create Issue** item appears in the right-click context menu; clicking it opens a dialog where you can edit the issue title and preview the auto-generated description before submitting. The dropdown arrow on the split button lets you choose a specific tracker or open Issue Tracker Settings.

## Development

```bash
./gradlew build          # Compile, test, and assemble
./gradlew test           # Run tests only
./gradlew runIde         # Launch a sandboxed IDE with the plugin loaded
./gradlew verifyPlugin   # Verify plugin compatibility
```

## License

[Apache License 2.0](./LICENSE)

## References
* [Plugin documentation](https://docs.sigrid-says.com/integrations/jetbrains-extension.html)
* [Plugin page on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32479-sigrid)
