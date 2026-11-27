# Main Seven personal quest finishing pass

CyberNpc **0.45.10** + CyberQuest **0.3.0-alpha.24**, Forge 1.20.1, Java 17.

These are authored in-world friendship stories, not biographical claims about the people who inspired any NPC. No new lore about the origin of the fracture or the next series is established.

| Character | Quest | Existing gameplay objective | Personal revelation |
| --- | --- | --- | --- |
| Kai | A Piece of Home | Find and show the Choso Plush | An ordinary keepsake makes his memories of home feel real again. |
| Katie | A Little Yarn | Craft 64 White Yarn | Crochet gives her something she chooses for herself; the finished project remains a surprise. |
| Elara | The Lost Route | Craft an empty map and discuss uncertain paths | She wants to admit uncertainty instead of pretending every route is safe. |
| Rowan | A Table for Strangers | Craft 8 bread and discuss welcoming newcomers | She remembers feeling that she had to explain herself before being welcome. |
| Felix | A Fair Hand | Craft 12 paper and discuss the old deck | He misses company without feeling obliged to entertain everyone. |
| Nadia | The Last Failsafe | Craft 2 repeaters and discuss a failure test | She needs to trust other people to review her work. |
| Orin | A Sky Worth Remembering | Craft a spyglass and discuss observations | He wants to keep memories separate from evidence about the unfamiliar sky. |

Offers still require Trust >= 2 and Affection >= 1. Katie has an explicit eligible root option; her existing crochet dialogue still works. The five crafting/discussion quests offer a choice of two meaningful responses rather than a generic research button. Crafting and discussion can occur in either order. The existing quests and objective IDs, counts, completion flags, stage IDs, XP rewards, and NPC story IDs are retained. Existing completed quests access their revised follow-up; no resets required.

Craft objectives prove crafting, not physical delivery. The dialogue does not promise an actual constructed mechanism, filled map, astronomical measurement, or additional custom item. Kai's plush must still be placed by the series owner. His quest only shows the item, without consuming it. CyberQuest's new tryDeliverItemsForQuest API isolates the explicit hand-in to the personal quest; CyberNpc does not fall back to the old generic handler. The old CyberServer Mason quest is retained pending the wider unused-systems audit.

## Verification

- Build matching CyberNpc and CyberQuest JARs locally; keep existing CyberQuest prebuilt texture archive in tools/prebuilt-quest-textures.zip if using the Python-free build.
- Check untrusted dialogue offers are absent; earn the required relationship values.
- Test all seven start, progress, turn-in, and completed dialogues; try incomplete turn-ins and repeated choices.
- For five crafting/discussion quests, test either order and verify both conversation answers record the same single discovery.
- For Katie, check 64 actual post-acceptance crafts and completion via both direct and crochet conversation paths.
- For Kai, test with and without the plush in main hand. With both the legacy Mason quest and personal quest active, verify the personal action does not consume the plush or advance the legacy quest.
- Check existing active/completed quests and two different players; restart and repeat.
- Verify no changes to the intro, credits or NPC departure behaviour.

Source/data checks only. Local Forge compilation and in-world testing are still required. NPC Rift departure and the developer ledger are a separate next update. Housing/jobs remain deferred.
