# Noxara Client

Noxara Client is a custom Fabric client fork based on [Meteor Client](https://github.com/MeteorDevelopment/meteor-client). The current development target is **Minecraft Java Edition 26.3**.

> **Development status:** the Minecraft 26.3 compatibility port is in progress. The branch has not yet been confirmed to compile or launch; see the verification workflow before treating a build as usable.

## Development target

- Minecraft: 26.3
- Mod loader: Fabric
- Java toolchain: 25
- Build system: Gradle / Fabric Loom

## Build locally

1. Install a JDK 25 distribution.
2. Clone this repository and check out the `noxara/mc-26.3-port` branch.
3. Run the Gradle build:

   - Windows: `gradlew.bat build`
   - Linux/macOS: `./gradlew build`

The output JAR is written under `build/libs`. A successful Gradle build confirms compilation and packaging, but the client should still be launched in a clean Minecraft 26.3 Fabric instance for a runtime smoke test.

## Project roadmap

1. Establish a clean Minecraft 26.3 Fabric build.
2. Rebrand and redesign the ClickGUI and HUD while keeping the existing module framework intact.
3. Port selected Wurst features individually, checking API compatibility and attribution.
4. Review Trouser-Streak integration separately. Its repository currently has no declared GitHub license, so its code must not be copied or redistributed until the applicable permission is established.

## Upstream projects and licensing

This repository contains code derived from Meteor Client, which is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. The upstream license is retained in `LICENSE`. This derivative is also distributed under GPL-3.0; modified source must remain available under the license terms, and the origin of the upstream code must be clearly credited.

- [Meteor Client source](https://github.com/MeteorDevelopment/meteor-client)
- [Wurst7 source](https://github.com/Wurst-Imperium/Wurst7) — selected features may be ported after compatibility and license review.
- [Trouser-Streak source](https://github.com/etianl/Trouser-Streak) — listed as a planned integration only; no license was declared on the repository metadata at the time of this README update.

This is an independent community fork and is not an official Meteor Development or Wurst project.
