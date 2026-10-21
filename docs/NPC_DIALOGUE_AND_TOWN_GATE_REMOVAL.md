# NPC dialogue and town exit cleanup (0.44.69)

- Only Pope (written story dialogue) and Guild Receptionist
  (required class and guild service dialogue) respond to normal
  right-click with a dialogue screen.
- Citizens, guards, couriers, unfinished quest characters, banker
  and shopkeepers with no authored dialogue **do not open blank menus**.
  Their dialogue will be restored as each role is written.
- Wild NPC emoji greetings are unchanged. Admin tools (Town Register,
  NPC removal) continue to work on all NPCs.
- Guild class registration option disappears as soon as the player's
  `CyberClasses.ClassChosen` save flag is true. Existing guild contract
  handling remains.
- The town class gate has been **removed completely**: no teleport
  boundary, no background enforcement, and no class gate commands.
  Old world gate NBT is ignored (no destructive migration required).
- Pope now recommends guild class registration as a first task instead
  of falsely claiming the player physically cannot leave town.

The replacement guidance is a first Main Quest delivered by the matching
CyberQuest build. The class stays an important story milestone; equipment
remains optional and saved.
