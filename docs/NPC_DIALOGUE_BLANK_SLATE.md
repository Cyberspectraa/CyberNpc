# NPC speech blank slate (0.44.66)

This update intentionally **removes all pre-written NPC spoken lines** from the main CyberNpc dialogue controller, including citizen greetings, service-role greetings, the Pope summoning introduction, role responses, rumours, and quest NPC dialogue slots.

Only the dialogue text is cleared. The **player's choice/button labels and IDs remain**, so the existing server-validated conversation flow, Pope introduction progress, guild registration/claims, CyberQuest integration, mail guidance actions, shop buy/sell transactions, and leave / return controls remain connected.

Shop action feedback is presented as **neutral system notifications**, not spoken NPC text, to avoid silently taking or paying coins. These notices are not future NPC dialogue lines.

The dialogue UI still renders the NPC portrait, empty speech area, and choices. Blank speech is intentional until new lines are authored.

Wild NPC icons and short non-verbal vocalisations are unaffected: they have no existing written dialogue to clear.

No save data, quest IDs, or packet protocols changed.