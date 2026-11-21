# Discord-to-Minecraft Post Office — CyberNpc 0.45.6

This restores the owner's intended **public Discord embed → Write a Letter
modal → Minecraft Drop Box → NPC courier → player/letterbox** flow. Discord
members use their Discord usernames as senders; no account linking.

Implementation:
- `discord-mail-bot/`: standalone Node 22 / discord.js bot. Operator posts
  the embed using `/mailpanel`. A button opens a private two-field modal
  for recipient Minecraft username and up to 256 characters of message.
- `DiscordMailInboxService`: server-only (dedicated Forge) importer. Reads
  atomic local JSON files from `config/cybernpc-discord-mail/incoming`,
  checks malformed requests, rejects unknown recipients, waits if no
  registered loaded Drop Box, and posts to the existing MailSavedData.
- `MailSavedData`: selects a genuine loaded registered Drop Box with fewer
  than 64 pending letters and saves a requestId→mailId mapping.
  On restart, the importer scans archived submissions, safely deduplicates
  accepted letters and replays ones not preserved before a sudden crash.

No public HTTP/RCON listener is used; no Discord token is in the JAR.
The bot must be hosted on the same machine or a secure shared filesystem.
Do not advertise the bot as working across unrelated remote hosts without
a transport connection.

Testing:
1. Install the new CyberNpc jar on client + server and confirm server starts.
2. Place a Drop Box and assign an existing Courier near it; load the chunk.
3. Run `discord-mail-bot` alongside the dedicated server, set its local
   shared folder and Discord application credentials in an untracked `.env`.
4. In Discord, use `/mailpanel`, click **Write a Letter**, enter a player
   who has joined the world and a message.
5. Inspect the private success response and let the Courier collect.
   Confirm the displayed sender is the true Discord username and that the
   letter arrives at the recipient or their registered letterbox.
6. Test with the Minecraft server offline: queued letter imports when
   the server returns and a Drop Box is loaded.
7. Test invalid recipient, full Drop Box, rapid submissions, repeated request
   ID after restart and mail privacy.
8. Confirm unchanged in-game Letter Paper/Drop Box functionality, Shopkeeper
   trading (0.45.5), intro, Rift finale and other Cyber mods.

See `discord-mail-bot/README.md` for full setup and permission details.
