package com.teleother;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
@Slf4j
@PluginDescriptor(name = "Teleother Discord Webhook", description = "Sends a message / screenshot to a discord webhook when you teleother someone", tags = {
		"teleport", "other", "discord", "webhook" })
public class TeleotherDiscordWebhookPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private TeleotherDiscordWebhookConfig config;

	@Provides
	TeleotherDiscordWebhookConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(TeleotherDiscordWebhookConfig.class);
	}
