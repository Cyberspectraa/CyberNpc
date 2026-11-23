# Cyber Command Hub — CyberNpc 0.45.9

The five existing Cyber mods now share **one command entry point** on
Forge 1.20.1:

    /cyber

This is NOT a rewrite of command handlers. CyberNpc registers aliases
to the real already-registered Brigadier command nodes supplied by the
other installed mods. Commands that modify races, classes, quests, NPCs,
world settings and the player's balance still use their original
permission requirements, argument parsers, auto-completion and functions.

## Consolidated names

| New command prefix | Existing command | Owner | Access |
|---|---|---|---|
| `/cyber npc` | `/cybernpc` | CyberNpc | Operator |
| `/cyber quest` | `/cyberquest` | CyberQuest | Player (admin subcommands require OP) |
| `/cyber races` | `/cyberraces` | CyberRaces | Player (admin subcommands require OP) |
| `/cyber classes` | `/cyberclasses` | CyberClasses | Player (admin subcommands require OP) |
| `/cyber server` | `/cyberserver` | CyberServer | Operator |
| `/cyber arrival` | `/arrival` | CyberServer | Operator |
| `/cyber reset` | `/cyberresetall` | CyberRaces | Operator; destructive testing reset |
| `/cyber balance` | `/bal` | CyberNpc | Player only (bank balance) |

Every command nested below those existing roots is available through
the new prefix as well. Examples:

    /cyber npc shop list <target>
    /cyber npc intro setarrival
    /cyber npc discordmail status
    /cyber quest journal
    /cyber quest npc inspect <target>
    /cyber races race list
    /cyber races character status
    /cyber classes info
    /cyber server status
    /cyber server discordmail config
    /cyber arrival test
    /cyber reset
    /cyber balance

The console can run operator commands, but **player-only** actions
such as opening a journal or checking a personal bank balance require
a player. This is unchanged from the original commands. Do not run
`/cyber reset` on a player whose character should be preserved.

## Browse and diagnose

- `/cyber`: Show available command groups for the current caller. No
  admin-only groups are listed to non-operators.
- `/cyber help`: Same groups in a short list.
- `/cyber help <section>`: Show the first-level subcommands of that
  section, respecting their permission checks. Further levels appear
  through normal Minecraft tab completion as the command is typed.
- `/cyber doctor`: Operator-only, non-destructive. Checks that each
  installed Cyber mod's original roots and important children exist,
  and that every `/cyber` alias points to the exact original command
  node. It **does not execute** commands that spawn NPCs, reset data,
  assign roles or change quests.

The aliases are generated on every `RegisterCommandsEvent` at
`EventPriority.LOWEST`. Other Cyber mods install their roots at the
default NORMAL priority first, so no compiled or reflective dependency
on CyberQuest, CyberRaces, CyberClasses or CyberServer is needed.
Optional mods not installed are simply omitted from the hub. A mod
that is installed but fails to register its command is flagged by
`/cyber doctor` rather than replaced with a fake command.

## Fixed bug

`BankMailCommands.register` already defined the legacy `/bal`
command, but CyberNpc's mod constructor never registered it on the
Forge event bus. CyberNpc 0.45.9 now registers this existing handler.
Both `/bal` and `/cyber balance` therefore route to the same bank
balance code. No bank balances or save data were modified.

## Testing (required before marking the feature complete)

1. Build CyberNpc 0.45.9 locally; install the new JAR on all clients
   and the server. No GitHub Actions and no changes to main.
2. Install the current CyberQuest, CyberRaces, CyberClasses and
   CyberServer build versions used by your pack. The hub adds no
   dependency or requirement to rebuild those mods.
3. On a dedicated server, grant the tester OP and run `/cyber doctor`.
   Expect all eight roots and the listed important children to register.
4. Verify player commands: `/cyber quest journal`,
   `/cyber races race list`, `/cyber classes info`,
   and `/cyber balance`. Repeat using the old prefixes.
5. Verify safe operator read-only commands:
   `/cyber npc discordmail status`, `/cyber server status`,
   `/cyber server discordmail status`, and `/cyber races character
   status`. Use Minecraft tab completion to check nested arguments.
6. Only in a backed-up **test world**, verify write commands such as
   `/cyber npc intro setarrival` and `/cyber arrival test`, then
   confirm the intro and quest progress still work. Test entity
   targeting with `/cyber npc shop list <target>`.
7. Remove OP from the test account. It must not be offered the
   `npc`, `server`, `arrival` or `reset` sections, and
   permissions on nested quest/race/class admin actions must remain.
8. Remove one optional Cyber mod, reload a test world and verify its
   alias vanishes without preventing the others from loading.
9. Restart/reload datapacks; `/cyber doctor` must still succeed
   without registering duplicate or broken nodes.

IMPORTANT: Source structural checks are not a replacement for a real
Forge compile and in-game tests. This update does not claim that any
world-changing command has been run successfully.
