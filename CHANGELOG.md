# Changelog

## 0.6.0

- Replaced the simulated cooking timer with real furnace/smoker/campfire interaction.
- Wild NPCs now insert raw food into the real cooking block inventory and wait for Minecraft to cook it.
- Furnace/smoker cooking requires available fuel or existing burn time.
- Campfire cooking uses the real campfire cooking slots and waits for the cooked item drop.
- NPCs retrieve cooked food, hold it, and eat it before hunger is restored.
- Added saved pending cooking/eating state.
- Added a sustainable livestock/corral system using existing fenced pens.
- NPCs detect enclosed fence-gate pens instead of building their own.
- NPCs can choose a livestock species, hold the correct lure food, open the gate, and lead missing adults into the pen.
- Corrals preserve at least two adult breeders.
- NPCs put suitable breeding pairs into love mode.
- Babies are never selected as food.
- Hungry NPCs can hunt surplus adults once a corral has at least three adults.
- Critically hungry NPCs fall back to ordinary hunting instead of waiting for herd setup.

## 0.5.0

- Reworked Wild NPC ranged combat into a visible weapon-use state machine.
- Bow users visibly draw before firing.
- Crossbow users visibly charge, hold, and fire.
- Added adaptive projectile prediction and drop compensation.
- Added sneaking while stalking prey and improved player-like movement.
- Enabled swimming, wooden-door navigation, and climbable-block movement.

## 0.4.0

- Added two-weapon Wild NPC combat.
- Added hunger, hunting, food pickup, cooking, eating, and starvation.
