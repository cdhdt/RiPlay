# Packaging the desktop app

Native installers are built with Compose Desktop's jpackage integration. Each command bundles a
trimmed JRE, so the installed app needs no system Java.

## Prerequisites

- JDK 21 (`JAVA_HOME` must point at it), e.g. `export JAVA_HOME=~/.jdks/jdk-21.0.11+10`
- Build on the target OS: jpackage produces a package for the host platform only.

## Commands

| Target | Command | Output |
|--------|---------|--------|
| Linux .deb | `./gradlew :composeApp:packageDeb` | `composeApp/build/compose/binaries/main/deb/*.deb` |
| Linux .rpm | `./gradlew :composeApp:packageRpm` | `.../main/rpm/*.rpm` |
| Windows .msi | `./gradlew :composeApp:packageMsi` | `.../main/msi/*.msi` (run on Windows) |
| Current OS | `./gradlew :composeApp:packageDistributionForCurrentOS` | matching format |

Install the Linux package with `sudo dpkg -i riplay_<version>_amd64.deb` (or `sudo rpm -i ...`); the
app then launches from `/opt/riplay/bin/RiPlay` and appears under the Audio menu group.

## First run

The player embeds Chromium (KCEF) to drive music.youtube.com. That runtime (~150 MB) is **not** in
the installer — it downloads once on first launch to `~/.riplay/kcef`, so the first start needs
internet and is slower. Subsequent starts are offline-capable for the engine itself.

The launcher carries the JCEF `--add-opens` flags and `-Xmx512m` automatically (see the
`nativeDistributions` block in `composeApp/build.gradle.kts`); no manual JVM tuning is needed.
