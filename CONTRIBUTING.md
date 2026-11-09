# Contributing Guidelines

For feature ideas, [open a discussion](https://github.com/ddev/ddev-intellij-plugin/discussions/new?category=ideas). 
For bug fixes, please open an issue first, then submit a pull request. For optimizations or test cases that don't 
change behavior, you can submit a [pull request](https://github.com/ddev/ddev-intellij-plugin/pulls) directly.

## Issues

Ensure issues are related to this plugin, not [DDEV](https://github.com/ddev/ddev) itself. DDEV issues should be 
reported [here](https://github.com/ddev/ddev/issues).

When [opening an issue](https://github.com/ddev/ddev-intellij-plugin/issues/new), include:
- Your IDE (PHPStorm, WebStorm, etc.)
- Operating system (including WSL for Windows users)
- Steps to reproduce
- Error report ID (if applicable)

## Error Reports

When a critical error occurs, you'll see a flashing red exclamation mark in your IDE. Click it to file a bug report 
and receive an error report ID. These reports include your OS, DDEV, Docker, and IDE versions, helping us diagnose 
and fix issues efficiently.

## Development Setup

1. Open IntelliJ IDEA, select `File > New > Project from Version Control` and check out this repository
2. Select JDK 21 as project JDK
3. Run `runIde` to build and start a development version with the plugin
4. Consult the [IntelliJ Platform SDK documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html)

Any IntelliJ-based IDE can be used for development. To use a local installation as the development target, add to 
[build.gradle.kts](build.gradle.kts):

```kotlin
tasks {
    runIde {
        ideDir.set(File("C:\\Users\\<User>\\AppData\\Local\\Programs\\PhpStorm"))
    }
}
```

## Releases

Every push to the `beta` or `alpha` branch publishes a prerelease to the JetBrains Marketplace channel of the same 
name and creates a matching GitHub prerelease. The version is the next patch after the latest stable release, 
numbered per channel, e.g. `1.2.11-beta.3`. To ship a beta, merge or fast-forward `main` into `beta`.

Stable releases are published by creating a GitHub release with an `X.Y.Z` tag. Add the version's section to 
[CHANGELOG.md](CHANGELOG.md) first.

To test prereleases, add the channel's repository in `Settings > Plugins > ⚙ > Manage Plugin Repositories`:
- Beta: `https://plugins.jetbrains.com/plugins/beta/list`
- Alpha: `https://plugins.jetbrains.com/plugins/alpha/list`
