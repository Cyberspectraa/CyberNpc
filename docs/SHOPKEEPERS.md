# Shopkeepers — CyberNpc 0.45.5

Shopkeepers now have a direct Buy/Sell UI, individually configured catalogues
and server-authoritative physical coin transactions. This replaces the old
unreachable global seven-item dialogue. Other NPCs and systems are untouched.

## Player experience

- Right-click a living Shopkeeper NPC to open their merchant screen.
- Buy tab: the items that Shopkeeper sells to players.
- Sell tab: the items that Shopkeeper accepts from player inventory.
- Each row displays a Minecraft item sprite, item name, bundle quantity,
  total price and transaction button. The screen also displays the player's
  physical coin wallet, a Done button, pages and transaction feedback.
- Sales check the player's exact item and NBT, within the main inventory
  rather than taking equipped armour/offhand. All prices and offers come
  from the server and cannot be chosen or forged by a client.
- Server checks a 6-block range, session NPC UUID, expiry, active role,
  price and inventory before each trade. A stale shop closes cleanly.
- Payments use only the existing CyberNpc copper, silver, gold, platinum and
  dragon coins. Larger coins return physical change. No virtual money or bank
  balance. Failed transactions cannot take partial payment.
- Stock is a merchant-specific catalogue, not a finite physical warehouse:
  a configured offer is repeatable. A new shop starts empty rather than using
  unrelated default items.

## Operator commands (permission level 2)

Hold the example item in your main hand; configuring it does not consume
that item. A target can be a direct entity selector, e.g.
@e[type=cybernpc:cyber_npc,sort=nearest,limit=1].

    /cybernpc shop addbuy <target> <priceInCopper> <bundleCount>
    /cybernpc shop addsell <target> <priceInCopper> <bundleCount>
    /cybernpc shop list <target>
    /cybernpc shop removebuy <target> <index>
    /cybernpc shop removesell <target> <index>

Example, while holding bread near a Shopkeeper:

    /cybernpc shop addbuy @e[type=cybernpc:cyber_npc,sort=nearest,limit=1] 20 3

This sells a bundle of 3 bread to the player for 20 copper-equivalent coins.
The addsell command lists items that players sell TO the Shopkeeper, paying
the specified amount of physical coins for the whole bundle. Index is the
zero-based slot shown by list, separately numbered for each tab. Removing an
offer shifts subsequent indices; re-run list between removals.

Price limit: 1 to 100,000 copper. Bundle size: 1 to 64, additionally
limited by the item's max stack size. Max 24 entries per tab per merchant.
Denominations: copper 1, silver 10, gold 100, platinum 1,000, dragon 10,000.
The cap avoids a single sale spawning thousands of coin stacks.

## Persistence and compatibility

Offers are saved per-NPC in Forge entity persistent compound CyberNpcShopV2,
under Buy and Sell lists with the item NBT, bundle count and coin price.
No legacy registry IDs, currency denominations or NPC save IDs were changed.
The Shopkeeper spawn egg and Shopkeeper role remain valid.
The old global test stock was not saved on individual NPCs and is retired.

Network protocol 10 appends three messages to the end of the existing packet
list, without renumbering intro, quest or Rift messages. Both client and
server must use matching CyberNpc 0.45.5 on Forge 1.20.1 / Java 17. Only
the development-no-actions branch is changed. GitHub Actions must not run.

## Manual tests

1. Create two Shopkeepers, list different items on each, save/reopen world;
   their goods and prices must stay different.
2. Buy with no currency, exact coins and a larger coin requiring change.
   Check inventory overflow. No unpaid goods should appear.
3. Sell a correct bundle from main inventory; try insufficient count and
   mismatched NBT, and verify nothing was partially removed.
4. Move away during a trade and confirm the screen closes without freezing.
   Attempt rapid clicks and simultaneous purchases by separate players.
5. Test unchanged Pope, Guild Receptionist, town character dialogue,
   CyberQuest/CyberClasses, mail and Rift outro.

## Discord mail follow-up

The current CyberNpc tree retains existing in-game letter writing, drop box,
letterbox and courier saved-mail features. There is no current Discord bot or
webhook implementation in CyberNpc or CyberServer's main tree.
Whether the owner's old Discord feature sent notifications, forwarded letter
contents or supported writing letters from Discord must be clarified before
restoring it. Never embed Discord webhook URLs or bot tokens in the mod jar
or GitHub source; prefer optional server-side configuration.
