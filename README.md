A Minecraft Paper plugin designed for dispatching Paladins on the LoM2 server

## Placeholders

Duty placeholders are read by other plugins and MagicSpells scripts.

- `%sneakydispatch_on_duty%` — `true` only when the player is in a unit **and** currently on a paladin character (`%sneakycharacters_character_hastag_paladin%`). Switching to a non-paladin character makes it `false` without removing the player from their unit; switching back restores `true`.
- `%sneakydispatch_unit%` — unit member names joined by `|`, or `none` (also `none` while not on duty as above).
- `%sneakydispatch_paladins_on_duty%` — number of unit members currently on a paladin character.
- `%sneakydispatch_paladins_available%`, `%sneakydispatch_paladins_ready%` — counts used by dispatch scheduling.
