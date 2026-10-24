package com.teleother;

import com.google.inject.Provides;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.Player;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import okhttp3.HttpUrl;

@Slf4j
@PluginDescriptor(name = "Teleother Discord Webhook", description = "Sends a message / screenshot to a discord webhook when you teleother someone", tags = {
		"teleport", "other", "discord", "webhook" })
public class TeleotherDiscordWebhookPlugin extends Plugin {
	private static final int ACCEPT_TELEOTHER = 1816;
	private static final int CAST_TELEOTHER = 1818;
	// Delay the screenshot by 2 ticks, so we capture the teleport animation
	private static final int SCREENSHOT_DELAY_TICKS = 2;
	private static final long TELEPORT_OFFER_TIMEOUT_SECONDS = 60;

	@Inject
	private Client client;

	@Inject
	protected EventBus eventBus;

	@Inject
	private TeleotherDiscordWebhookConfig config;

	@Provides
	TeleotherDiscordWebhookConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(TeleotherDiscordWebhookConfig.class);
	}

	private final Map<String, Instant> offersQueue = new HashMap<>();
	private final Map<String, Integer> notificationQueue = new HashMap<>();
	private String playerName = null;

	@Override
	protected void shutDown() throws Exception {
		offersQueue.clear();
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) {
		Actor actor = event.getActor();
		if (actor == null || actor.getName() == null)
			return;

		switch (actor.getAnimation()) {
			case CAST_TELEOTHER:
				onCastTeleother(actor);
				break;
			case ACCEPT_TELEOTHER:
				onAcceptTeleother(actor);
				break;
		}
	}

	private void onCastTeleother(Actor actor) {
		if (actor == null || !isLocalPlayer(actor))
			return;

		Actor target = actor.getInteracting();
		if (target != null && target instanceof Player) {
			log.debug("{} cast teleother on {}", getLocalPlayerName(), target.getName());
			offersQueue.put(target.getName(), Instant.now());
		}
	}

	private void onAcceptTeleother(Actor actor) {
		if (actor == null || isLocalPlayer(actor))
			return;

		if (offersQueue.containsKey(actor.getName())) {
			Instant castTime = offersQueue.get(actor.getName());

			if (Duration.between(castTime, Instant.now()).toSeconds() < TELEPORT_OFFER_TIMEOUT_SECONDS) {
				log.debug("{} accepted teleother from {}", actor.getName(), getLocalPlayerName());
				queueNotification(actor.getName());
			}
		}
	}

	private boolean isLocalPlayer(Actor actor) {
		return actor != null && actor.getName() != null && actor.getName().equals(getLocalPlayerName());
	}

	private void queueNotification(String actorName) {
		offersQueue.remove(actorName);
		if (!config.enableWebhook() || config.webhookUrl().isEmpty())
			return;

		notificationQueue.put(actorName, getDiscordDelay());
	}

	private int getDiscordDelay() {
		return config.includeScreenshot() ? SCREENSHOT_DELAY_TICKS : 0;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		Iterator<Map.Entry<String, Integer>> iterator = notificationQueue.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Integer> entry = iterator.next();
			String targetName = entry.getKey();
			int ticksRemaining = entry.getValue() - 1;

			if (ticksRemaining <= 0) {
				sendToDink(targetName);
				iterator.remove();
			} else {
				entry.setValue(ticksRemaining);
			}
		}
		if (offersQueue.isEmpty()) {
			return;
		}

		Instant now = Instant.now();
		offersQueue.entrySet().removeIf(entry -> {
			return Duration.between(entry.getValue(), now).toSeconds() > TELEPORT_OFFER_TIMEOUT_SECONDS;
		});
	}

	private String getLocalPlayerName() {
		if (playerName != null)
			return playerName;

		Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null)
			return "Someone";

		playerName = player.getName();
		return playerName;
	}

	private String replacePlaceholders(String message, String targetName) {
		if (message == null || message.isEmpty())
			return "";

		return message.replace("%player%", getLocalPlayerName()).replace("%target%", targetName);
	}

	private void sendToDink(String targetName) {
		HashMap<String, Object> data = new HashMap<>();
		data.put("text",  replacePlaceholders(config.customMessage(), targetName));
		data.put("title", "Teleother");
		data.put("sourcePlugin", "Teleother");
		data.put("imageRequested", config.includeScreenshot());
		if (config.webhookUrl() != null && !config.webhookUrl().isEmpty()) {
			List<HttpUrl> httpUrls = Arrays.stream(config.webhookUrl().split(";")).map(HttpUrl::parse)
					.collect(Collectors.toList());
			data.put("urls", httpUrls);
		}

		log.debug("sending notification to dink");

		eventBus.post(new PluginMessage("dink", "notify", data));
	}
}
