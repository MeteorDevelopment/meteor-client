# Meteor Client
Look [upstream](https://github.com/MeteorDevelopment/meteor-client) for a proper description.

You can get a build from [GitHub Actions](https://github.com/JFronny/meteor-client/actions)

## Changes
- Use GitHub Actions for CI -> no dev build numbers
- JiJ for compat
- Utilize LibJF for ASM
- Pretend to be fabric-api-base
- Remove discord presence, capes, player count
- Add module to disable custom rendering
- Use FAPI resource loader, client commands and keybinds instead of custom systems (requires FAPI now)
- Add optional UI rounding + relevant API additions ([#619](https://github.com/MeteorDevelopment/meteor-client/pull/619))
- Remove credit/spash/title/prefix customization
- Add keywords to modules for discoverability ([#1600](https://github.com/MeteorDevelopment/meteor-client/pull/1600))
- Use browser user agent in Http Utils instead of "Meteor Client"
- Add default target entities to KillAura and BowAimbot
- KillAura: ignore passive targets ([#1589](https://github.com/MeteorDevelopment/meteor-client/pull/1589))

## Credits
[Cabaletta](https://github.com/cabaletta) for [Baritone](https://github.com/cabaletta/baritone)  
The [Fabric Team](https://github.com/FabricMC) for [Fabric](https://github.com/FabricMC/fabric-loader) and [Yarn](https://github.com/FabricMC/yarn)

## Meteor Licensing Notice
This project is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html).

If you use **ANY** code from the source:
- You must disclose the source code of your modified work, and the source code you took from this project. This means you are not allowed to use code from this project (even partially) in a closed-source and/or obfuscated application.
- You must state clearly and obviously to all end users that you are using code from this project.
- Your application must also be licensed under the same license.


*If you have any other questions, check our [FAQ](https://github.com/MeteorDevelopment/meteor-client/wiki).*
