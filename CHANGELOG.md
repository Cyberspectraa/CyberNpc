# Changelog

## 0.29.0

### Beast Tamer companion-state fix
- Reworked wolf sit/follow control to change the ordered sit state only when the desired state actually changes.
- Removed continuous sit-pose forcing from the normal command loop, preventing CyberNpc and vanilla wolf AI from fighting over the animation every tick.
- Companion command refresh now runs every 5 ticks instead of rewriting state every tick.
- Wolves still sit while their Beast Tamer sleeps, stand/follow when awake, attack the Beast Tamer's combat/hunting target, and regroup while fleeing.
- Newly tamed and newly spawned companion wolves start standing and owned by their Beast Tamer.

### Starting class companions
- Fresh Beast Tamers now spawn with one adult tamed wolf already owned by that NPC.
- Fresh Horse Tamers now spawn with one adult owned horse when Icy's Better Horses is installed.
- The starting Horse Tamer horse receives the real Better Horses Upgraded Saddle and is assigned to that NPC.
- Wild spawn eggs participate in the same one-time companion setup as natural Wild NPC spawns.
- Companion setup is persisted so loading chunks or existing worlds cannot duplicate class companions.
- Older NPCs are treated as already initialized and keep their existing companion state rather than receiving surprise duplicates.

### Mounted sprint travel
- Horse Tamers can now make their horse sprint for longer-distance mounted travel.
- Mounted navigation uses roughly player-style sprint scaling: nearby destinations use normal horse travel speed while destinations over about 10 blocks use a faster sprint speed.
- Party catch-up, long return trips and sufficiently distant roaming destinations can trigger mounted sprinting.
- Horses automatically stop sprinting when their path finishes or the rider dismounts.
- Developer Glasses now reports "Sprinting on horse" and explains when distance caused the faster travel choice.

## 0.28.0

### NPC armor cleanup
- Removed chainmail from all new Wild NPC loadouts because it clashes visually with the custom NPC appearance.
- Fine-tier Classless, Archer, Rogue, Berserker, Beast Tamer and Horse Tamer NPCs now keep leather armor with their Fine-tier enchantments.
- Knights use iron armor from Standard/Fine upward instead of chainmail.
- Existing loaded Wild NPCs wearing CyberNpc-generated chainmail are migrated automatically: Knights receive iron and lighter classes receive leather, with gear-tier enchantments reapplied.

### Beast Tamer wolf state fix
- Newly tamed wolves now explicitly clear both the ordered-sit flag and the synced sitting pose.
- The Beast Tamer command loop now keeps the visible sitting pose in sync with the actual command state.
- Wolves sit only while their Beast Tamer is sleeping.
- Waking, following, fleeing, combat and hunting explicitly clear the sitting pose, fixing wolves that appeared seated while sliding across the ground.
- A newly tamed wolf immediately begins pathing back toward its Beast Tamer instead of remaining visually parked.

### Horse Tamer riding improvements
- Mounted NPCs no longer reuse their on-foot limb swing, preventing the rider's torso and arms from visibly walking while seated.
- The rider's body stays aligned with the horse while the head remains free to look around.
- The old 15-30 second forced ride timeout has been removed.
- Mounting now creates a longer decision commitment rather than guaranteeing a timed dismount.
- Horse Tamers stay mounted while no higher-priority need exists.
- Combat, danger, sleep and zombification can still interrupt riding immediately.
- Hunger and normal chores only cause a dismount after the minimum riding commitment has elapsed.
- After dismounting, Horse Tamers have a remount cooldown so they do not repeatedly get on and off the same horse with no meaningful reason.

### Decision reasoning
- Added a synced decision-reason state alongside the existing activity state.
- Developer Glasses now shows both what the NPC is doing and why that action currently has priority.
- Reasons cover survival, infection avoidance, fleeing, combat, hunting, sleep, horse travel, horse taming, wolf taming, food work, livestock work, socialising, party regrouping and ordinary travel.
- Existing AI priority remains intact; this pass makes commitments and transitions more stable instead of replacing working systems.

### Future optional integrations
- Tinkers' Construct is noted for a later weapon/loadout integration.
- Dynamic Trees is noted for a later environment/resource interaction feature.
- Neither mod is a hard CyberNpc dependency in 0.28.0.

## 0.27.1

### Beast Tamer wolf follow fix
- Beast Tamer wolves now actively path back to their owner instead of relying only on vanilla wolf follow AI.
- Following is the default companion state during ordinary travel, socialising, idling and other normal activities.
- Wolves only receive the normal sit command while their Beast Tamer is actually sleeping.
- Wolves immediately receive an unsit/follow command after the Beast Tamer wakes.
- Combat and hunting still override following and send the pack after the Beast Tamer's current target.
- Fleeing recalls the pack to the Beast Tamer instead of allowing wolves to stay behind.
- A wolf with a live self-defence target is allowed to finish defending itself before resuming active follow.

## 0.27.0

### Horse Tamer class
- Added the rare Horse Tamer Wild NPC class at a 3% class weight while keeping the full class-weight table at 100%.
- Horse Tamer is only available when Icy's Better Horses is installed; CyberNpc still has no hard dependency on that mod.
- Horse Tamers spawn with the Better Horses Upgraded Saddle rather than a duplicate CyberNpc saddle.
- A Horse Tamer searches for an adult unowned horse, approaches it, uses its Upgraded Saddle to tame/claim it, equips that saddle and makes the horse persistent.
- Horse Tamers prefer their already-owned horse before attempting to tame another one.
- Horse ownership is now strict: ordinary Wild NPCs no longer borrow saddled horses, and Horse Tamers never take player-owned or another NPC's horse.
- Existing combat, hunger, sleep and emergency behaviour still makes a mounted Horse Tamer dismount when the NPC has something more important to do.

### Beast Tamer companion commands
- Beast Tamer wolves now receive explicit owner state commands instead of only being targeted at enemies.
- Wolves sit while their Beast Tamer is sleeping or stopped for a social conversation.
- Wolves automatically unsit to follow their owner again when the Beast Tamer resumes normal activity.
- Combat orders unsit every owned wolf and give the whole nearby pack the Beast Tamer's current target.
- Hunting uses the same coordinated target order, so the wolves help bring down the prey selected by their Beast Tamer rather than selecting unrelated animals.
- A fleeing Beast Tamer recalls the pack instead of leaving wolves sitting behind.
- Existing gear-tier companion limits, wolf taming, ownership and cleanup on death/zombification are preserved.

## 0.26.0

### Expanded reaction-bubble language
- Expanded Wild NPC icon-only communication with dedicated symbols for greeting, food/help, sleep, mounting, beast taming, danger and combat.
- Existing friend/group/emotion reactions remain intact.
- Friends can greet each other with a distinct greeting bubble rather than looking like strangers having the same generic interaction.
- NPCs show a food reaction when hunger reaches the hunting threshold.
- NPCs show a sleep reaction when they successfully claim a bed for the night.
- NPCs show a mount reaction when mounting a horse.
- Beast Tamers show a beast reaction when they tame a wolf.
- Normal non-hunt combat can show a combat warning reaction.
- Threatening chat that does not immediately trigger retaliation can produce a danger or scared reaction depending on personality.
- HOME remains reserved for the future home/settlement system.

### Friendship now unlocks behaviour
- Persistent mutual Friends now qualify as guaranteed combat helpers when healthy enough, even when they are not in the same party.
- Friends react more strongly when a player attacks or kills one of their friends.
- Non-party friends loosely seek each other out while idle instead of behaving as unrelated strangers.
- Friends can share one prepared food item when one friend is well-fed and the other is very hungry.
- Sharing food strengthens friendship/trust/respect and produces visible reaction bubbles.
- Existing party formation still requires mutual friendship and remains persistent.

### Beast Tamer class
- Added the new rare Beast Tamer Wild NPC class at a 3% class weight, keeping total class weighting at 100%.
- Beast Tamers use melee weapons and practical light-to-medium armour appropriate to their gear tier.
- Beast Tamers spawn with a supply of bones.
- An idle Beast Tamer searches for nearby adult untamed wolves, approaches them and tames them.
- Wolf companion limit scales with gear tier: Standard 1, Fine/Rare 2, Elite 3.
- Tamed wolves are owned by the Beast Tamer NPC, follow normal wolf-owner behaviour and are ordered into the Beast Tamer's fights.
- Beast Tamer companions are released back to wild status if their NPC owner dies or zombifies, preventing permanently orphaned wolf ownership.

### Horse riding
- Wild NPCs can now locate and ride adult tamed saddled horses while otherwise idle.
- Horse use is intentionally occasional rather than making every nearby NPC mount immediately.
- Mounted NPCs can travel around and mounted party members can use the horse to catch up with their party leader.
- NPCs remember where a temporary ride started and avoid carrying a borrowed horse indefinitely away from that area.
- NPCs dismount for combat, fleeing, sleep, zombification, urgent hunger/food work or when the riding period ends.
- Added temporary per-horse NPC claims so multiple NPCs do not race for the same mount.
- Stale claims automatically clean themselves up after NPC unload/death/reload situations.
- Developer Glasses now reports Approaching horse and Riding horse states.

### Icy's Better Horses compatibility
- Added optional soft compatibility for the `icys_better_horses` mod with no hard dependency.
- CyberNpc recognises Better Horses ownership and cart state reflectively.
- NPCs do not mount horses currently fitted as cart horses.
- NPCs never overwrite Better Horses owner, bond or gear data.
- Player-owned horses are treated as borrowed mounts rather than reassigned to the NPC.
- If the horse's player owner returns nearby, the NPC dismounts and releases its temporary claim.
- Better Horses custom breed horses are naturally included through the shared AbstractHorse riding logic.

### Player chat affects NPC relationships
- Wild NPCs can now understand a small conservative set of positive, negative and threatening chat intents.
- Chat only affects an NPC when its name is explicitly mentioned near the relevant phrase, or when the player says "you" while visibly addressing a nearby NPC.
- Named/direct intent detection uses a local word window so unrelated sentiment elsewhere in the message is much less likely to be misattributed.
- Examples such as "I hate Leo" lower Leo's opinion of that player and produce an upset/annoyed reaction.
- Positive phrases such as liking, thanking or praising an NPC improve player reputation.
- Threat phrases such as kill, hurt, attack, fight, beat, destroy or murder reduce reputation heavily.
- Simple negations such as "don't kill Leo", "do not attack Leo", "never hurt Leo" are not treated as threats.
- Nearby friends of a named target can also take offence or react positively because the player is talking about their friend.
- A threatened NPC has a personality/aggression/reputation-dependent chance to retaliate immediately.
- Aggressive, reckless, brave and protective personalities are more willing to retaliate; cautious, skittish and patient personalities are less likely.
- A retaliating NPC uses the normal CyberNpc combat/help systems rather than a special scripted attack.
- Chat reactions have a cooldown to prevent reputation spam from a single burst of messages.
- Normal Minecraft chat and the 0.25.0 player speech-bubble system remain unchanged.

### Debugging and lifecycle
- Developer Glasses can now expose Taming wolf, Approaching horse and Riding horse activity states.
- New chat, mount and Beast Tamer task state is transient and resets cleanly when the NPC reloads.
- Horse claims are coordination hints only and do not become ownership data.


## 0.25.0

- Removed the text/icon drop shadow from Wild NPC reaction bubbles for a cleaner Minecraft-style look.
- Added a shared speech-bubble renderer so NPC reaction bubbles and player chat bubbles use the same stable white, dark-bordered pixel bubble geometry.
- Added player speech bubbles for normal player chat messages.
- When a player sends a chat message, clients show that message above the speaking player's character while the normal chat window remains unchanged.
- Player bubbles show message text only; the player's name is not repeated inside the bubble because the bubble is attached to their character.
- Player speech bubbles wrap long messages automatically and are bounded to a maximum of four visible lines.
- Very long chat remains complete in the normal chat window while the world-space bubble stays a manageable size.
- New messages replace a player's older active speech bubble rather than stacking multiple bubbles.
- Bubble lifetime scales with message length, from roughly 3.5 seconds up to 9 seconds.
- Player speech text has no drop shadow.
- Player chat bubbles use the same true-white fill, dark pixel border, stepped corners and speech tail as NPC bubbles.
- Player speech bubbles render only within 32 blocks.
- Invisible players do not render speech bubbles, preventing the bubble from revealing an invisible player's position.
- Player chat filtering is respected before text is shown in a world-space bubble.
- Player speech-bubble state is client-side and is cleared when leaving the server/world, so old messages cannot carry into another session.
- Wild NPCs remain icon-only and never use player-style text dialogue.


## 0.24.1

- Fixed reaction bubbles still appearing black in 0.24.0.
- Root cause: the dark outline was actually rendered as a complete filled bubble silhouette, while the white interior was rendered as a second depth layer above it. Depending on camera-facing depth ordering, the dark silhouette could win the depth test and cover the fill.
- Rebuilt reaction-bubble geometry so the dark border and white interior occupy completely separate XY regions on the same Z plane.
- There is now no black surface underneath the white center, so a depth-order reversal cannot turn the whole bubble black.
- Removed the layered bubble shadow/highlight surfaces that could contribute to depth-order ambiguity.
- Kept the stepped Minecraft-style corners, shaped speech tail, full-bright rendering, larger scale, icon colours and subtle bob animation.
- The bubble interior is now true opaque white rather than a translucent cream tone.
- The social/friendship/group-invitation systems from 0.24.0 are unchanged.


## 0.24.0

- Reworked reaction-bubble rendering so the bubble body uses Minecraft's white texture with a normal translucent entity render path instead of the dark name-tag background shader.
- Reaction bubbles now stay genuinely bright cream/white even in darker environments and use full-bright lighting for reliable readability.
- Removed all remaining overlapping coplanar bubble rectangles. The outline, fill and speech tail are now built from non-overlapping pixel strips, fixing the angle-dependent flicker/depth glitches seen in 0.23.1.
- Kept the stepped pixel corners, shadow, highlight, shaped tail, larger scale and subtle bobbing introduced in 0.23.1.
- Added explicit persistent Friend status to NPC relationships. Friendship is saved in NBT and treated as a permanent social milestone rather than something that silently decays away.
- Existing Friendship, Trust, Respect, Fear and Rivalry values continue to exist underneath the Friend milestone.
- Friends keep minimum Friendship/Trust values, and being a friend gives a support-score bonus. Friends can still become annoyed or frightened without randomly ceasing to be friends.
- Nearby NPCs no longer gain friendship merely by standing near one another. Relationship progress now happens through actual social interactions.
- Social interactions are visible: NPCs pause, face one another and exchange icon-only reaction bubbles for a short period.
- Added Group Invite (+?) and Group Accept (+!) reactions. These are symbols only; Wild NPCs remain completely non-verbal.
- Party formation now requires mutual Friend status first.
- A party invitation is a multi-step social interaction: a friend proposes grouping, the other NPC visibly thinks about it, then both show an accept reaction if the party is formed.
- Party invitations have cooldowns so NPCs do not repeatedly spam group requests.
- Existing parties remain persistent and do not dissolve because relationship values drift or time passes.
- Party leaders can invite a friend into an existing party when there is space. Two ungrouped friends can also create a new party, with leadership decided by the existing class/personality/gear leadership scoring.
- Added natural-spawn social seeding in preparation for full natural Wild NPC spawning. Natural/Chunk Generation NPCs can spawn with believable pre-existing friendships and, sometimes, pre-existing parties with nearby Wild NPCs.
- Natural-spawn social history intentionally bypasses the visible introduction sequence because those relationships represent history from before the player encountered the NPCs.
- Natural-spawned NPCs are not forced into groups: some remain solitary, some gain one or more prior friends, and only a portion of those friendships seed a party.
- Developer Glasses relationship debug now marks explicit Friend relationships.
- Socializing and considering a party invite are exposed through the NPC activity debug state.
- Combat, fleeing, zombification and urgent behaviour interrupt face-to-face social interactions so conversation never overrides survival.


## 0.23.1

- Completely redesigned Wild NPC reaction bubbles so they no longer look like plain rectangles.
- Increased the world-space bubble scale from 0.025 to 0.032 and raised the bubble slightly higher above NPC heads for easier reading.
- Added stepped pixel-art corners to give each bubble a chunky rounded Minecraft-style silhouette.
- Added a two-step shaped speech tail instead of the old straight rectangular tail.
- Added an offset translucent shadow behind the entire bubble for visibility against bright skies, blocks and foliage.
- Replaced the dark translucent fill with a bright cream/white interior and a strong dark pixel outline.
- Added a subtle top highlight for depth while keeping the design deliberately pixel-art rather than glossy UI.
- Increased minimum bubble width and icon padding so short symbols such as ! and ? no longer look cramped.
- Added an icon drop shadow and brighter, more saturated reaction colours for much stronger contrast.
- Added a very small idle bob animation so reaction bubbles feel attached to a living NPC rather than a static debug label.
- Existing icon-only/non-verbal reaction behaviour is unchanged.


## 0.23.0

- Added Minecraft-style world-space reaction bubbles for Wild NPCs.
- Wild NPCs remain non-verbal: reaction bubbles contain compact icon/emoticon symbols only and never generate dialogue lines.
- Added reaction states for Happy, Thinking, Confused, Friendly, Surprised, Sad, Annoyed, Scared and Angry.
- Reaction state is synchronized to clients but intentionally not saved to NBT; persistent relationship/reputation values remain the long-term memory underneath.
- Higher-priority reactions such as Angry or Scared are not immediately overwritten by low-priority casual reactions.
- Reaction bubbles are camera-facing, blocky white panels with a dark pixel border and tail, rendered only within 32 blocks.
- Player attacks now produce Annoyed or Angry reactions depending on severity and prior reputation.
- NPCs witnessing attacks against friends/party members react visibly, with stronger reactions for fatal attacks and party bonds.
- NPCs show Scared reactions when they decide to flee a dangerous threat.
- NPCs can show Confused reactions when they become suspicious of a nearby zombifying NPC.
- Helping an NPC in combat can produce Happy/Friendly reactions, especially when the player kills the threat.
- Forming or joining a party produces Friendly/Happy reactions.
- Peaceful nearby NPC interactions have a deliberately low chance to produce paired reactions, so towns feel socially alive without filling the screen with bubbles.
- The reaction system is exposed through a reusable showReaction API so future social-conversation logic can drive the same icon-only bubbles without adding text dialogue.


## 0.22.1

- Converted CyberNpc zombies are now permanently adult. Baby rolls and chicken-jockey conversion data are suppressed during spawn, setBaby(true) is ignored, and old saved custom zombies with baby NBT are corrected when loaded.
- Converted zombies now reconsider targets every 5 ticks and prefer the closest valid visible target instead of staying locked onto a distant target.
- Converted zombie target candidates include survival players, living non-zombifying CyberNpc NPCs, villagers, iron golems, and unusual mobs that are actively attacking the zombie; zombie-on-zombie infighting is excluded.
- Wild NPCs now periodically reconsider active combat targets and switch to a closer hostile mob that is actually threatening them.
- Hunting NPCs now search around themselves for the closest valid prey rather than staying anchored to prey near the old target position.
- Generic hostile mobs may now switch from a farther living target to a substantially closer visible CyberNpc, preventing mobs from ignoring a newly arrived NPC beside them.
- Added Just Expressions-aware facial character for converted zombie NPCs without moving them back to PlayerModel.
- When Just Expressions' player_face resource is detected, converted zombies use cached native blink and focused-eye texture variants based on the NPC's existing Low/Middle/High eye placement.
- Zombie facial variants are cached per appearance, so the expression feature does not regenerate textures every render frame.
- Fresh Animations remains authoritative for the zombie body/limb model and animations; the Just Expressions compatibility layer only changes the custom zombie face texture state.


## 0.22.0

- Added separate male and female Wild NPC name pools. New Wild NPCs now choose their persistent appearance gender before receiving a name, so names always come from the matching pool.
- Existing named NPCs keep their saved names; this change only affects NPCs that still need a generated default name.
- Female converted zombies now bake Minecraft's normal ModelLayers.ZOMBIE just like male converted zombies, giving EMF/Fresh Animations the same vanilla zombie model entry point for both genders.
- Female zombies keep an Alex-like slimmer arm silhouette by narrowing the zombie arm parts after normal ZombieModel animation setup rather than using a PlayerModel/custom player-derived layer.
- Removed the obsolete female custom zombie model-layer registration from the active renderer path.
- Fixed crossbow charge audio duplication by removing CyberNpc's manual CROSSBOW_LOADING_START and CROSSBOW_LOADING_END sounds. Vanilla CrossbowItem now owns the loading sequence.
- Crossbow charging now tolerates short line-of-sight interruptions instead of immediately cancelling and restarting the use action, preventing repeated loading sounds when a target briefly passes behind another entity or block.
- Charged crossbows will wait for line of sight before firing rather than releasing through an obstruction.


## 0.21.0

- Added persistent NPC-to-NPC relationships with Friendship, Trust, Respect, Fear and Rivalry values.
- Relationships are stored per NPC in NBT and kept to a bounded history so long-running worlds do not accumulate unlimited social data.
- Added per-NPC player reputation from -100 to +100.
- Players gain reputation with an NPC by damaging or killing a mob the NPC is actively fighting or fleeing from.
- Players lose reputation by attacking an NPC; nearby party members and strongly bonded NPCs also remember witnessed attacks and kills.
- Existing player provocation now considers remembered reputation, so NPCs are more tolerant of trusted players and react faster to players with a bad history.
- Added small persistent parties of up to 4 Wild NPCs.
- Peaceful nearby contact slowly builds relationships; sufficiently positive mutual relationships can create a party or allow an NPC to join an existing party.
- Parties persist in NBT, choose a leader using class/personality/gear traits and regroup around that leader during idle time.
- If a loaded party leader dies, loaded party members choose a replacement leader instead of immediately losing the group.
- Combat-help selection now prioritizes party members and trusted relationships instead of simply choosing the nearest healthy strangers.
- Different personalities have different willingness to help: Protective/Loyal/Brave NPCs assist readily while Cautious/Skittish NPCs require stronger relationships.
- Party/trusted backup contributes more realistically to combat confidence, and helping another NPC in combat strengthens mutual Trust and Respect.
- Clerics now prioritize injured party members and trusted allies when choosing whom to support.
- Added a Developer Glasses Relationships tab showing party role/leader/loaded size, strongest NPC relationships and remembered player reputation.
- Converted CyberNpc zombies no longer use PlayerModel animation layers, preventing FA: Player Extension from treating them as players.
- Male converted zombies now use Minecraft's ZombieModel layer; female zombies keep slim/Alex-width arm geometry through a dedicated CyberNpc layer while still using ZombieModel animation logic.
- Zombie armor rendering now uses zombie armor layers.
- This makes vanilla zombie animations authoritative for converted zombies. EMF supports modded entity models, so Fresh Animations can interact with CyberNpc's model path, but a full Fresh Animations CEM override for the custom entity may still require an explicit resource-pack compatibility entry.


## 0.20.1

- Fixed the top-left Starlight/Lunar export artifact by clearing Minecraft's unused 8x8 skin corner after composition, so stray creator/editor pixels cannot render on NPCs or converted zombies.
- Verified the revised Lunarskins pack remains compatible with the existing appearance slots and 64x64 layout.
- Replaced the bundled female zombie appearance with the corrected slim/Alex-compatible ZombieFemale texture from the latest Lunarskins pack.
- Female converted zombies now explicitly load that corrected raw resource before applying their preserved eyes and hairstyle.
- No living-NPC appearance combinations changed from 0.20.0.


## 0.20.0

- Added persistent randomized visual identities for CyberNpc entities.
- Added Male and Female appearance variants. Female NPCs use the slim/Alex player model while Male NPCs use the classic/Steve player model.
- Bundled the supplied Lunar/Starlight skin components: 7 skin tones per gender, 3 eye-alignment variants and 6 brown hairstyles.
- Each NPC rolls its appearance once and stores the result in synchronized entity data and NBT, so the same NPC keeps its identity after world reloads.
- Added a cached client-side skin compositor. Each unique combination is assembled from the bundled 64x64 layers once, registered as a dynamic texture and then reused.
- Added an Appearance page to Developer Glasses showing gender/model, skin tone, eye alignment, hairstyle, hair colour and appearance ID.
- Converted CyberNpc zombies now use player-proportioned classic/slim models rather than the generic zombie renderer.
- Added the corrected slim female zombie base and preserved the NPC's hairstyle and eyes after zombification.
- Zombie conversion now transfers the NPC's name, gender, appearance, armour, held equipment and carried inventory.
- Converted zombies persist their inherited carried inventory in NBT and release it when they are killed.
- Zombie-kill conversion suppresses the original NPC equipment/inventory drop so transferred items are not duplicated.
- Updated the custom zombie dimensions to match CyberNpc's 1.75-block player-like body.


## 0.19.0

- Added three new Wild NPC classes: Berserker, Cleric and Spellblade.
- Rebalanced class spawn weights to keep Classless dominant while making the new specialist classes uncommon/rare.
- Berserkers use real axes, lighter armor, high melee pressure and a low-health rage state that increases attack tempo and chase pressure.
- Clerics require Iron's Spells 'n Spellbooks, always use the Holy school and carry a real spellbook that guarantees both Guiding Bolt and Healing Circle.
- Clerics detect wounded nearby Wild NPC allies and temporarily stop attacking to cast a real Iron's Healing Circle on them.
- Spellblades require Iron's, carry both a real sword and a randomized school spellbook, use magic at range/for defensive needs and switch to melee once they close distance or useful spells are unavailable.
- Mage-school persistence and Developer Glasses spell diagnostics now work for Mage, Cleric and Spellblade.
- Expanded Wild NPC personalities from 5 to 12: Balanced, Brave, Cautious, Aggressive, Tactical, Reckless, Protective, Loyal, Opportunistic, Skittish, Stubborn and Patient.
- Personalities now drive aggression, combat confidence, emergency-health thresholds, willingness to seek help, attack/recovery tempo, combat spacing and strafing behaviour.
- Protective and Loyal NPCs seek/benefit from group support earlier, while Reckless/Stubborn NPCs stay committed longer and Skittish NPCs disengage much sooner.


## 0.18.0

- Fixed repeated Archer/Classless bow shots missing the bow-draw pose by rendering CyberNpc's synced aiming state directly.
- Slightly reduced CyberNpc height from 1.80 to 1.75 blocks and scaled the humanoid render to match.
- Retuned Wild NPC movement toward player-like walking pace and changed idle wandering from 60% to full navigation speed.
- Added strong route avoidance for lava, fire, campfires and other damaging terrain, plus an emergency escape if an NPC is pushed or spawned onto a hazard.
- Added proactive Warden awareness: nearby uncommitted Wardens cause quiet crouched retreat; a Warden that targets or gets dangerously close causes immediate full-speed escape.
- Refined Mage spell ranges from Iron's actual spell implementations, including true melee spacing for Flaming Strike/Divine Smite and self-AoE spacing for Frostwave/Shockwave.
- Long Mage casts now obey Iron's own shouldAIStopCasting rule where provided, improving spell-specific range/target behaviour.
- Added rarity-aware death loot: common items drop more often than uncommon/rare/epic items, enchanted items count as one rarity tier higher, and stronger gear tiers improve drop odds.
- Stackable inventory loot now drops a gear-tier-scaled fraction instead of always dropping the whole carried stack.
- Mage spellbooks remain guaranteed death loot and still drop empty.


## 0.17.0

- Reduced gap jumping to a conservative 1-block walking gap and up to 3 blocks while sprinting; larger/unsafe gaps remain pathfinding detours.
- Reduced hostile threat reevaluation interval from 10 ticks to 4 ticks so NPCs react much faster when combat conditions suddenly change.
- Wardens are treated as exceptional threats in confidence calculations and now overwhelm ordinary backup confidence much more strongly.
- Starting a flee now immediately cancels an active Mage cast and switches to sprint escape behaviour.
- Wardens are excluded from CyberNpc's generic hostile-target injection so their normal vanilla vibration/anger/smell behaviour remains authoritative.
- Added visible-sculk awareness during normal movement: Wild NPCs crouch only when a nearby sculk sensor/calibrated sensor is actually visible rather than through walls.
- CyberNpc crouching explicitly counts as careful stepping, allowing vanilla vibration mechanics to suppress appropriate movement vibrations; normal/noisy movement still uses vanilla entity game events.
- Wild NPC ranged attacks now detect friendly NPCs in the firing corridor and reposition instead of firing through them.
- Friendly Wild NPC damage is ignored as a final safety net for arrows and Iron's spell/AoE crossfire.
- Mage ranged/control spells re-check the friendly-fire lane immediately before casting.
- Mage casting visuals now reuse the existing rendered offhand spellbook instead of replacing it every tick, reducing unnecessary modded-model render churn.
- Hunting prey now also passes through the combat-confidence system rather than using health alone.

## 0.16.0

- Added combat confidence scoring for Wild NPCs using mob aggression class, current health, estimated damage, NPC health/damage and nearby backup.
- Passive, neutral and aggressive mobs contribute different threat weight; actively targeting mobs become more dangerous.
- NPCs now ask nearby Wild NPCs for help when confidence is uncertain and only flee when the odds are genuinely poor or health is critically low.
- Fleeing NPCs can re-evaluate the fight and re-engage if backup arrives or the threat becomes manageable.
- Added player-scale gap jumping: up to 2-block walking gaps and up to 4-block sprint gaps, with clear-body/landing checks; unsafe or oversized gaps remain normal pathfinding detours.
- Mage spell cooldowns are now tracked per spell instead of one global spell cooldown.
- A Mage can rotate to another ready spell while one cools down; if no useful spell is ready it sprints/kites away until a spell becomes available.
- Every newly generated Mage spellbook now requires at least 2 usable spells and guarantees at least 1 direct attack spell.
- Existing invalid Mage books with fewer than 2 useful spells or no attack spell are automatically rerolled by the existing repair path.
- Developer Glasses now show combat confidence and add a dedicated Spells tab with the Mage's actual spells, levels, tactical roles and live cooldowns.

## 0.15.0

- Added tactical spell awareness to Wild Mage NPCs.
- Mage AI now profiles its rolled Iron's spells as close-range, ranged, control, healing or defensive abilities.
- Mages choose only from spells actually present in their own randomized spellbook.
- Spell choice is now scored by target distance, Mage health and spell role instead of random selection.
- Close-range spellbooks make the Mage close distance; long-range spellbooks make the Mage create space.
- Healing is prioritized when the Mage is injured, while defensive magic becomes more attractive under pressure.
- Long casts are cancelled if the target escapes far outside that spell's tactical range.
- Unknown future Iron's spells use a conservative mid-range fallback rather than breaking Mage AI.

## 0.14.3

- Fixed Mage spellbooks appearing empty and leaving Mages unable to cast.
- Mage spellbooks are now populated through Iron's real 1.20.1 ISpellContainer API instead of relying on CyberNpc-written legacy NBT.
- Mage AI reads the same real spell container that Iron's tooltips and spellbook systems read.
- Added automatic reroll/repair when an existing Mage owns a book with no usable combat spells.
- Kept compatibility reading for spellbooks created by earlier CyberNpc 0.14.x builds.
- Mage death loot still drops the same Iron's spellbook type empty; only the death copy is emptied.

## 0.14.2

- Mage NPC spellbooks remain populated with a randomized set of usable spells while the NPC is alive.
- Mage combat continues to read the NPC's actual spellbook, so a Mage can only cast spells that were rolled into that individual book.
- Killing a Mage now drops its real Iron's spellbook item as an empty book instead of leaking the NPC's preloaded combat spells.
- Empty Mage book drops have CyberNpc-only spellbook and school markers removed so they behave like normal player-owned Iron's spellbooks.

## 0.14.1

- Fixed a server crash when an Evocation Mage cast Iron's Spectral Hammer as a non-player entity.
- Added an explicit NPC-unsafe Iron's spell guard to both spellbook generation and live casting, so existing saved 0.14.0 Mage books are protected too.
- Replaced Spectral Hammer in the Evocation Mage pool with Fang Swirl while keeping Spectral Hammer available to normal players.

## 0.14.0

- Added persistent Wild NPC gear tiers: Standard 70%, Fine 20%, Rare 8%, Elite 2%.
- Added armor loadouts to every Wild NPC class, including Classless.
- Added progressively stronger weapons, armor, enchantments and base-stat bonuses for rarer gear tiers.
- Added class-specific Archer, Knight and Rogue equipment progression.
- Added persistent Mage magic schools: Fire, Ice, Lightning, Nature, Holy, Ender, Blood and Evocation.
- Added matching Iron's school armor for every Mage school.
- Added real persistent Iron's spellbooks to Mage NPC inventories.
- Added school-specific Iron's spellbooks where Iron's provides them, with generic Iron's books used for schools without dedicated books.
- Added actual Iron's spell-container NBT with randomly selected school spells.
- Gear tier now controls Mage spell count and starting spell level.
- Reworked Iron's compatibility to follow the real MOB casting lifecycle, including MagicData, SyncedSpellData, pre-cast, cast ticks, cast and cast completion.
- Added support for Iron's INSTANT and LONG spells.
- Continuous Iron's spells remain excluded until safe repeated-channel support is implemented.
- Added spellbook-in-offhand rendering while a Mage is casting.
- Added separate instant-cast and long-cast player-model animations based on the real Iron's cast type/timing.
- Added humanoid armor rendering to CyberNpc.
- Developer Glasses now show gear tier, Mage school and current spell.
- Curios support was not required for this implementation; the Mage's spellbook remains authoritative in the real NPC inventory.

## 0.13.1

- Made Mage conditional on Iron's Spells 'n Spellbooks.
- Removed fake CyberNpc Mage magic.
- Added optional soft Iron's spell casting integration.

## 0.13.0

- Added Wild NPC classes, personalities and tabbed Developer Glasses.

## 0.12.0

- Added the persistent 18-slot Wild NPC inventory.
