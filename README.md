A Minecraft Paper plugin designed for dispatching Paladins on the LoM2 server

## Placeholders

Duty placeholders are read by other plugins and MagicSpells scripts.

- `%sneakydispatch_on_duty%` — `true` only when the player is in a unit **and** currently on a paladin character (SneakyVariables character variable `paladin` = 1, read as `%sneakyvariables_paladin,0%`). Switching to a non-paladin character makes it `false` without removing the player from their unit; switching back restores `true`.
- `%sneakydispatch_unit%` — unit member names joined by `|`, or `none` (also `none` while not on duty as above).
- `%sneakydispatch_paladins_on_duty%` — number of unit members currently on a paladin character.
- `%sneakydispatch_paladins_available%`, `%sneakydispatch_paladins_ready%` — counts used by dispatch scheduling.

## Character variables

SneakyDispatch defines two SneakyVariables character variables on enable (and drops its definitions on disable), so
SneakyVariables is a required dependency:

- `paladin` (number, 0-1): 1 on a paladin character. Duty reads it (see above).
- `badge_number` (number, 0-999): the paladin's badge number.

MagicSpells YAML may define them too while it moves off them; both are character numbers, so the definitions overlap.
They are set with `/ms variable modify paladin <player> =1` and `/ms variable modify badge_number <player> =<n>`.

## Badge prefix

While a player is on duty as a paladin (role `paladin` below) and their character's `badge_number` is 1-999, their
SneakyCharacterManager character name prefix (what `/charprefix` sets) is `badge-prefix` from `config.yml`, with
`[badge]` replaced by the number padded to three digits: badge 4 gives `<font:lom:badges>\uE004</font> &6`. When they
stop being on duty as a paladin, the prefix is cleared, unless someone else changed it since. It is reapplied on
everything that recomputes duty (below), including a `badge_number` change and a character load, since SCM does not
save the prefix. Needs SneakyCharacterManager with the `CharacterNamePrefix` API. `badge-prefix: ""` turns it off.

## Effects

What players see and hear at each dispatch step is `effects.<step>` in `config.yml`: a chat `message`, a `toast` with
the emergency's icon (SneakyUtils `Toasts`), `sounds` only that player hears, and `commands` run as that player (the
server's `findlocation` waypoint). These replace the MagicSpells spells SneakyDispatch used to force-cast; it casts no
spells now.

| Step | When | Was |
| --- | --- | --- |
| `on-duty` | `/onduty`, rejoining a unit (on a paladin character) | `paladin-onduty` |
| `emergency-reported` | an emergency is reported, to the paladins assigned to it | `paladin-emergency-reported` |
| `dispatched-self` | you dispatch yourself: chat line, waypoint, sounds | `paladin-dispatch-self` |
| `dispatched-other` | another paladin, or a squire, heads to an emergency | `paladin-dispatch-other` |
| `squire-reported` | a squire hears of an emergency (besides `squire-badge.messages.reported`) | `squire-emergency-reported` (never existed) |
| `squire-dispatched-self` | a squire clicks [Respond] | `squire-dispatch-self` (never existed) |

The paladin steps only play to unit members on a paladin character, as the spells' `onduty` modifier did. Going off
duty only clears the badge prefix (above), which was all `paladin-offduty` did. The once-a-minute
`paladin-encounter-main` cast is gone: that spell had been emptied, as encounters spawn from a passive.

## Duty event

SneakyDispatch fires SneakyUtils' `com.sneakyutils.signals.DutyChangedEvent(player, onDuty, role)` when a player's
`%sneakydispatch_on_duty%` value (`UnitManager.isOnDuty`) actually changes, so listeners don't have to poll it.

- **Thread:** always the server thread, synchronously.
- **Role:** `paladin` when in a unit and on a paladin character, otherwise `squire` when a carried Paladin Squire Badge
  counts as on duty (`squire-badge.counts-as-on-duty`). Going on duty carries the new role; going off duty carries the
  role the player had. Switching between `paladin` and `squire` while staying on duty fires nothing.
- **Fires on:** forming a unit (`/onduty`), joining one (`/squire`), leaving one (`/offduty`, offline timeout, unit
  disband for the remaining members), a character switch (SneakyCharacterManager's `LoadCharacterEvent`, and its
  `CharacterLoadedEvent` for loads and both bodies of a swap; checked the tick after, once per tick), the `paladin` character variable changing (SneakyVariables' `VariableChangeEvent`; a character value
  counts for whoever wears that character), picking up or dropping a squire badge (SneakyItems'
  `CarriedItemChangeEvent`), a squire badge being voided (SneakyWishes' `LicenseVoidEvent`), and joining the server on
  duty (a join counts as coming from off duty).
- **Character loading:** a character's first load in SneakyVariables fires no `VariableChangeEvent`, so after a
  switch or a swap, while that character's variables aren't loaded yet, the player alone is checked every 2 ticks until they are
  (then recomputed once), for up to 10 seconds; a quit or another switch cancels the wait.
- **Recheck:** inputs with no event of their own (a switched-to character whose variables took longer than 10 seconds
  to load; a `paladin` change without SneakyVariables hooked; an SCM character swap on an SCM older than `CharacterLoadedEvent`; a taped badge; squire-badge config reloaded) are caught by a check
  every 5 seconds over unit members,
  players on duty and badge carriers only, so the event can arrive up to 5 seconds late for those. Changes nothing
  announces at all (`/wishes reload` remapping the squire license to an item someone already carries; badge pickups
  without SneakyItems) are caught by 8 online players being reconciled each time, in rotation, so they can take
  (online players / 8) x 5 seconds.
- **Order:** everyone one change affects (a whole new unit, say) is recomputed before any event goes out. If a
  listener changes someone's duty again, that event is delivered after the current one has reached every listener,
  each with the role from its own change. A queued event for a player who quits before it is delivered is dropped.
- **Known gap:** after `/wishes reload` maps the squire license to a different magic item, a torn badge stamped
  before the remap isn't recognised as a badge until it is taped (SneakyWishes has no API for a torn license's type), so
  the on-duty event for taping it waits for that player's turn in the rotation.
- **Not fired:** when nothing changed, when an on-duty player quits (clear per-player state on `PlayerQuitEvent`), or on
  plugin enable for players already online (they are tracked silently).
