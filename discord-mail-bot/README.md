# Fractured Island Discord Post Office

A **one-way Discord-to-Minecraft mail form** for the existing CyberNpc
courier and letterbox system. A Discord member clicks the public
**Write a Letter** button, enters a Minecraft recipient and message, and
the bot submits an addressed letter to the Minecraft server's Drop Box
queue. The letter identifies its sender by their actual Discord username;
no Discord/Minecraft account linking is required.

This is a separate Node.js bot program, not a Discord plugin inside the
Minecraft client. Never put a Discord token in your mod files.

## Where it runs

**Version 1 uses an on-disk handoff:** the Discord bot and the Minecraft
**dedicated server** must be on the same machine or have secure access to
the exact same shared mail directory.

- Dedicated Forge server reads `<minecraft-server>/config/cybernpc-discord-mail`.
- Bot environment variable `MAIL_DIR` MUST point to that exact folder.
- Bot writes atomic `incoming/<uuid>.json` files; the server imports them
  on the server thread every two seconds and writes private `results/`
  receipts. Completed inputs are preserved in `archive/` for crash-safe
  recovery, rejected entries in `rejected/`.
- A remote-only Discord host with no shared directory **cannot** send to
  the server using this version. Do **not** expose RCON or a Minecraft
  listening port publicly as a workaround. Arrange a private shared folder
  or build a separate authenticated transport later.

## Setup

1. Install Node.js 22 or newer, and have a running dedicated Forge 1.20.1
   CyberNpc **0.45.6** server with an existing registered Drop Box
   and at least one configured Courier NPC.
2. In the Discord Developer Portal, create a bot application and install it
   to your Discord server with the **bot** and **applications.commands**
   scopes. It only needs permission to **View Channels**, **Send Messages**
   and **Embed Links** in your chosen mail channel. The Message Content
   privileged gateway intent is **not** required.
3. Enable Discord Developer Mode and copy your Discord server (guild) ID.
4. Open a terminal **in this folder** and run:

       npm install
       cp .env.example .env
       # On Windows PowerShell instead: Copy-Item .env.example .env

5. Edit `.env` with `DISCORD_BOT_TOKEN`, `DISCORD_GUILD_ID`, and
   the **absolute** `MAIL_DIR` path. Do not quote values or use a
   relative path. On Windows, forward slashes work in the path, e.g.
   `MAIL_DIR=C:/MinecraftServer/config/cybernpc-discord-mail`.
6. Run `npm start`. Leave the bot process running alongside the Minecraft
   server. The terminal should report it is online.
7. A Discord member with **Manage Server** permission types `/mailpanel`
   in the desired channel. This posts the public letter-writing embed
   and button. Everyone who can click the button may submit a letter.
   The bot does not need to see or read ordinary channel messages.
8. Test sending to a Minecraft player who has previously joined this
   server. Use a valid Java Edition username (3–16 characters). The
   Discord user writes up to 256 characters. The sender will show
   their Discord username (not a fake Minecraft user).

## How delivery works

- Discord modal submission is private. Its content does **not** appear
  in a public channel or any outbound Discord notification.
- The bot rate-limits each Discord sender to **one letter per 2 minutes**
  and **six per hour**, and caps the unfinished queue at 128 files.
- If a loaded Drop Box is available, the server registers the letter as
  normal PENDING mail, with a recorded recipient player UUID and synthetic
  UUID derived from the sender's Discord user ID.
- The **existing** Courier picks it up from the Drop Box, carries it
  and delivers it directly to that player or to their active Letter Box.
  Offline delivery to a mailbox requires its chunk to be loaded and
  reachable by the Courier as with any in-game letter.
- If no registered Drop Box is loaded, the letter remains queued on disk
  until one becomes available. The bot may show a queued notice after
  ten seconds; it is **not** a delivery confirmation.
- Unknown Minecraft player names are rejected when the server processes
  the request. The immediate private response shows this if processed
  within ten seconds.
- Minecraft persistent mail records store the Discord username as sender;
  Discord IDs are used only to generate stable non-player UUIDs.
- Each submission has a unique request ID persisted in the CyberNpc
  mail SavedData, preventing the usual duplicate import after restart.
  Archived spool files permit re-import after a crash before world save.

## Privacy and maintenance

**Letters are private, but not encrypted at rest.** The Minecraft world
save and files in the server's private mail archive contain their message
texts. Restrict OS permissions on the shared mail directory and back up
the world and archive together. Avoid posting chat logs, server data
or the bot's `.env` publicly.

The bot does not fetch server data, send mail contents to a Discord
channel, or support reverse Minecraft-to-Discord messages. It also does
not require a webhook, Discord account linking or a public web service.

If the bot crashes, restart `npm start`. Incoming letters remain on
disk. If the game is stopped, they are imported once the dedicated
server resumes with an active Drop Box. If an old letter is undeliverable,
check recipient spelling, loaded box chunks, Courier placement/work
assignment, and `config/cybernpc-discord-mail/results`.

## Updating

This version is a GitHub source update. **No bot token, webhook or server
configuration is committed.** The Minecraft mod source and Discord bot
must be deployed separately. CyberNpc jar builds use the project
`build-local.bat`; do not run GitHub Actions. Matching CyberNpc version
is required on client and server even though the Discord bridge is
server-side only.
