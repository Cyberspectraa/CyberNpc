# Island affliction and Guild onboarding (0.44.68)

The Pope knows that everyone on this island was once human. The island's
unexplained affliction changes their bodies over time but does not erase
their memories or personality. He **did not summon** the player and
**does not know** the cause of the affliction or the summoning.

He explains this in a new page of the first encounter. His warning about
the dangerous northern Shipbreaker remains. The final pages direct the
player to register at the Adventurers' Guild before adventuring.

Guild Receptionist has a new **"I'd like to register my class"** option.
It opens the CyberClasses class selector through an optional, server-side
reflection hook; the class selection belongs to CyberClasses, not CyberNpc.
All other NPC spoken dialogue stays intentionally blank.

Summoning no longer waits for class selection: the player chooses a race,
sees the existing cinematic, meets the Pope, and then selects a class at
the Adventurers' Guild. Existing characters and saved class IDs survive.

## Optional, admin-configured town departure gate

The class requirement is compulsory for leaving town, but there is no
automatic arbitrary-size boundary. **An operator must configure the actual
town boundary**. Stand near the town centre and run:

`/cybernpc intro setclassgate <radius>`

The gate checks the horizontal radius in blocks around the configured
centre, across the town's dimension, after the first introduction.
Classless survival/adventure players are returned to their last safe
position if they cross the boundary; the same check also prevents portal
or modded-teleport escapes into another dimension.

Operators and creative/spectator players are exempt so map-building and
admin recovery remain available. With no gate configured, movement is
not restricted (safe upgrade behaviour for old worlds).

Disable and reconfigure with:

`/cybernpc intro disableclassgate`

Guild class selection and the starter kit are separate decisions.
**A class is mandatory; the starter kit is optional**. Set up a Guild
Receptionist NPC and the boundary before testing the new-character path.

Requires the updated companion CyberClasses build for the Guild flow.
