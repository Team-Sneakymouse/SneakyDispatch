# Agent instructions (SneakyDispatch)

Paper plugin in Team-Sneakymouse / Lords of Minecraft. Follow the shared rules in EmptyPlugin's `AGENTS.md` and the team development policy.

## Repo-specific rules

- Sources live in `src/main/kotlin` and resources (`config.yml`, `paper-plugin.yml`) in `src/resources`. Do not move them without editing the `srcDir` lines in `build.gradle.kts`.
- Duty state is exposed through PlaceholderAPI (`Placeholders.kt`) and read by other plugins such as SneakySecrets. Keep placeholder names and values stable.
- `UnitManager` starts offline timers when a unit logs out; cancel them on rejoin and on disable.
- On/off-duty commands in `CommandOnDuty`/`CommandOffDuty` are hardcoded MagicSpells casts (`cast forcecast <player> paladin-onduty`); check the spell names exist on the server before renaming.
