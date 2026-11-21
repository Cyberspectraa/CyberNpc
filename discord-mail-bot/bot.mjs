import {
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  Client,
  EmbedBuilder,
  Events,
  GatewayIntentBits,
  MessageFlags,
  ModalBuilder,
  PermissionFlagsBits,
  SlashCommandBuilder,
  TextInputBuilder,
  TextInputStyle
} from 'discord.js';
import {
  mkdir, readdir, readFile, rename, stat, writeFile, unlink
} from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import path from 'node:path';

const BOT_TOKEN = process.env.DISCORD_BOT_TOKEN?.trim();
const GUILD_ID = process.env.DISCORD_GUILD_ID?.trim();
const MAIL_DIR = process.env.MAIL_DIR?.trim();
if (!BOT_TOKEN || !/^\d{15,22}$/.test(GUILD_ID ?? '')
    || !MAIL_DIR || !path.isAbsolute(MAIL_DIR)) {
  throw new Error('Set DISCORD_BOT_TOKEN, DISCORD_GUILD_ID and absolute MAIL_DIR in .env');
}

const INCOMING = path.join(MAIL_DIR, 'incoming');
const RESULTS = path.join(MAIL_DIR, 'results');
const MIN_INTERVAL_MS = 2 * 60_000;
const MAX_HOURLY = 6;
const MAX_QUEUED = 128;
const sendTimes = new Map();

const PANEL_COMMAND = new SlashCommandBuilder()
  .setName('mailpanel')
  .setDescription('Post the Fractured Island letter-writing panel in this channel')
  .setDefaultMemberPermissions(PermissionFlagsBits.ManageGuild)
  .setDMPermission(false);

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));
const escapeCode = (s) => s.replace(/[\r\n\u0000-\u001f\u007f\u00a7]/gu, '');

function validateRecipient(value) {
  const name = value.trim();
  return /^[A-Za-z0-9_]{3,16}$/.test(name) ? name : null;
}

function validateMessage(value) {
  // Match the Minecraft 256-character Letter Paper limit. Do not silently
  // alter the contents of someone's personal letter.
  const text = value.trim();
  return text.length >= 1 && text.length <= 256
    && !/[\u0000-\u001f\u007f\u00a7]/u.test(text) ? text : null;
}

function senderName(user) {
  // Always derive identity from the authenticated Discord interaction.
  // No user-facing field can impersonate another sender.
  const name = escapeCode(user.username ?? '');
  return name.length <= 32 ? name : name.slice(0, 32);
}

function checkRateLimit(id) {
  const now = Date.now();
  const recent = (sendTimes.get(id) ?? []).filter(t => now - t < 60 * 60_000);
  if (recent.length >= MAX_HOURLY) return 'You have reached the six-letter hourly limit.';
  if (recent.length && now - recent.at(-1) < MIN_INTERVAL_MS) {
    const seconds = Math.ceil((MIN_INTERVAL_MS - now + recent.at(-1)) / 1000);
    return 'Please wait ' + seconds + ' seconds before sending another letter.';
  }
  return null;
}

async function enqueue(request) {
  await mkdir(INCOMING, { recursive: true, mode: 0o700 });
  await mkdir(RESULTS, { recursive: true, mode: 0o700 });
  // Fail closed if the queue is already too large. Do not flood Minecraft's
  // main server thread when Discord is busy or the world is offline.
  const count = (await readdir(INCOMING)).filter(f => f.endsWith('.json')).length;
  if (count >= MAX_QUEUED) throw new Error('The island post office is full right now.');
  const temporary = path.join(INCOMING, request.requestId + '.tmp');
  const destination = path.join(INCOMING, request.requestId + '.json');
  try {
    await writeFile(temporary, JSON.stringify(request), {
      encoding: 'utf8', flag: 'wx', mode: 0o600
    });
    // On a single filesystem, rename is atomic: the mod never reads half a letter.
    await rename(temporary, destination);
  } catch (e) {
    await unlink(temporary).catch(() => {});
    throw e;
  }
}

async function awaitReceipt(id) {
  for (let i = 0; i < 10; i++) {
    await sleep(1000);
    try {
      const raw = await readFile(path.join(RESULTS, id + '.json'), 'utf8');
      const receipt = JSON.parse(raw);
      if (receipt.requestId !== id) continue;
      if (receipt.status === 'accepted' || receipt.status === 'rejected') {
        return receipt;
      }
    } catch (error) {
      if (error.code !== 'ENOENT') {
        // Bad status data is not a reason to erase the queued letter.
        console.warn('Could not read postal receipt:', error.message);
      }
    }
  }
  return null;
}

client.once(Events.ClientReady, async () => {
  const guild = await client.guilds.fetch(GUILD_ID);
  // Guild-command create acts as an upsert for the same name and does not
  // replace unrelated commands belonging to this Discord application.
  await guild.commands.create(PANEL_COMMAND.toJSON());
  console.log('CyberNpc mail bot online. Use /mailpanel to place its public letter panel.');
  console.log('Minecraft inbox directory: ' + MAIL_DIR);
});

client.on(Events.InteractionCreate, async interaction => {
  try {
    if (interaction.guildId !== GUILD_ID) return;

    if (interaction.isChatInputCommand() && interaction.commandName === 'mailpanel') {
      if (!interaction.memberPermissions?.has(PermissionFlagsBits.ManageGuild)) {
        await interaction.reply({
          content: 'Only server staff can place the postal panel.',
          flags: MessageFlags.Ephemeral
        });
        return;
      }
      const embed = new EmbedBuilder()
        .setColor(0x795938)
        .setTitle('Fractured Island Post Office')
        .setDescription(
          'Want to send someone on the island a letter?\n\n' +
          'Choose **Write a Letter**, enter their Minecraft username, and write your message. ' +
          'The letter will be posted into Minecraft for a courier to collect and deliver ' +
          'to the player or their letterbox.\n\n' +
          'Your Discord username will appear as the sender. ' +
          'Messages are private and must be 256 characters or fewer.'
        )
        .setFooter({ text: 'Island Mail • Letters may take time to arrive' });
      const button = new ButtonBuilder()
        .setCustomId('cybernpc:mail:compose')
        .setStyle(ButtonStyle.Secondary)
        .setEmoji('✉️')
        .setLabel('Write a Letter');
      await interaction.reply({
        embeds: [embed],
        components: [new ActionRowBuilder().addComponents(button)]
      });
      return;
    }

    if (interaction.isButton() && interaction.customId === 'cybernpc:mail:compose') {
      const modal = new ModalBuilder()
        .setCustomId('cybernpc:mail:send')
        .setTitle('Write a Letter');
      const recipient = new TextInputBuilder()
        .setCustomId('recipient')
        .setLabel('Minecraft recipient username')
        .setPlaceholder('Their Minecraft username')
        .setRequired(true)
        .setMinLength(3)
        .setMaxLength(16)
        .setStyle(TextInputStyle.Short);
      const message = new TextInputBuilder()
        .setCustomId('message')
        .setLabel('Your letter (up to 256 characters)')
        .setRequired(true)
        .setMinLength(1)
        .setMaxLength(256)
        .setStyle(TextInputStyle.Paragraph);
      modal.addComponents(
        new ActionRowBuilder().addComponents(recipient),
        new ActionRowBuilder().addComponents(message)
      );
      await interaction.showModal(modal);
      return;
    }

    if (!interaction.isModalSubmit() || interaction.customId !== 'cybernpc:mail:send') return;

    const recipient = validateRecipient(interaction.fields.getTextInputValue('recipient'));
    const message = validateMessage(interaction.fields.getTextInputValue('message'));
    if (!recipient || !message) {
      await interaction.reply({
        content: 'Use a valid Minecraft username and a letter of 1–256 characters without control codes.',
        flags: MessageFlags.Ephemeral
      });
      return;
    }

    const limit = checkRateLimit(interaction.user.id);
    if (limit) {
      await interaction.reply({ content: limit, flags: MessageFlags.Ephemeral });
      return;
    }

    await interaction.deferReply({ flags: MessageFlags.Ephemeral });
    const request = {
      requestId: randomUUID(),
      discordUserId: interaction.user.id,
      senderName: senderName(interaction.user),
      recipientName: recipient,
      message
    };

    await enqueue(request);
    sendTimes.set(interaction.user.id, [
      ...(sendTimes.get(interaction.user.id) ?? []).filter(t => Date.now() - t < 60 * 60_000),
      Date.now()
    ]);
    const receipt = await awaitReceipt(request.requestId);
    if (receipt?.status === 'rejected') {
      await interaction.editReply('The letter could not be posted: ' + receipt.message);
    } else if (receipt?.status === 'accepted') {
      await interaction.editReply(
        'Your letter to **' + recipient + '** is in the island Drop Box. ' +
        'A courier will collect it when available.'
      );
    } else {
      await interaction.editReply(
        'Your letter to **' + recipient + '** is queued for the Minecraft post office. ' +
        'It will be checked when the server and a Drop Box are available. ' +
        'If the recipient is unknown, the server will reject it.'
      );
    }
  } catch (error) {
    console.error('Discord mail interaction error:', error);
    const message = error.message === 'The island post office is full right now.'
      ? error.message
      : 'The post office could not process your letter. Please try again later.';
    try {
      if (interaction.deferred || interaction.replied) {
        await interaction.editReply(message);
      } else {
        await interaction.reply({ content: message, flags: MessageFlags.Ephemeral });
      }
    } catch (replyError) {
      console.warn('Could not send postal failure response:', replyError.message);
    }
  }
});

client.login(BOT_TOKEN);
