package com.teleother;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("teleotherdiscord")
public interface TeleotherDiscordWebhookConfig extends Config
{
	@ConfigSection(
		name = "Discord Webhook",
		description = "Settings for Discord integration",
		position = 0
	)
	String discordSection = "discordSection";

	@ConfigItem(
		keyName = "enableWebhook",
		name = "Enable Webhook",
		description = "Toggles sending messages to Discord",
		section = discordSection
	)
	default boolean enableWebhook()
	{
		return true;
	}

	@ConfigItem(
		keyName = "webhookUrl",
		name = "Webhook URL",
		description = "Paste your Discord server webhook URL here",
		section = discordSection
	)
	default String webhookUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "includeScreenshot",
		name = "Include Screenshot",
		description = "Toggle whether to capture and upload a screenshot to Discord",
		section = discordSection
	)
	default boolean includeScreenshot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "customMessage",
		name = "Custom Message",
		description = "The message structure sent to Discord. Use %player% for your name, %target% for the target name.",
		section = discordSection
	)
	default String customMessage()
	{
		return "**%player%** successfully teleothered **%target%**!";
	}

	@ConfigSection(
		name = "Overhead",
		description = "Settings for overhead messages",
		position = 1
	)
	String overheadSection = "overheadSection";

	@ConfigItem(
		keyName = "targetMessage",
		name = "Target Message",
		description = "The message shown above the targets character. Use %player% for your name, %target% for the target name.",
		position = 6,
		section = overheadSection
	)
	default String targetMessage()
	{
		return "";
	}

	@ConfigItem(
		keyName = "playerMessage",
		name = "Player Message",
		description = "The message shown above your character. Use %player% for your name, %target% for the target name.",
		position = 7,
		section = overheadSection
	)
	default String playerMessage()
	{
		return "";
	}
}
