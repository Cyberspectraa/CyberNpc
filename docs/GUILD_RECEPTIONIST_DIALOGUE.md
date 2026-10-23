# Guild Receptionist spoken dialogue (0.44.71)

- The Guild Receptionist again speaks when right-clicked, with different greetings before and after choosing a class.
- Player class registration remains delegated to CyberClasses, and the registration choice is removed when `CyberClasses.ClassChosen` is set.
- The class starter kit is optional; the receptionist does not grant items directly.
- The `guild_info` answer explains board notices, holding the notice to register, the existing limit of two guild contracts, and collecting rewards.
- `guild_register` and `guild_claim` display the server-side result in the NPC dialogue instead of leaving a blank speech area.
- `guild_card` continues opening CyberQuest's membership screen. No second dialogue packet is sent on success.
- Class-registration failure now gets a spoken explanation, rather than a blank bubble.
- Pope and first-arrival dialogue are unchanged. Shopkeepers, other service NPCs and unfinished quest NPCs remain non-interactable until their dialogue is authored.
- Existing action IDs, quest IDs, save keys, packet IDs, and progression rules are unchanged.

## Quick in-game check

1. Right-click a Guild Receptionist with no class. Confirm the first-class welcome and registration option.
2. Select a class, including the choice to decline starter gear; reopen the receptionist. The registration option must be gone.
3. Ask how the guild works. Confirm spoken instructions and available options.
4. Take a contract from the board, hold it, register with the receptionist, and check the journal.
5. Try registering without holding an eligible contract and claiming when none is ready. Both must return spoken guidance.
6. Complete a contract, collect payment, and open the guild card.
7. Right-click an unconfigured citizen or shopkeeper: no generic blank dialogue menu should appear.
