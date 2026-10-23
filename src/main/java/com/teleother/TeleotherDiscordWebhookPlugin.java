package com.teleother;

import com.google.inject.Provides;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
import net.runelite.api.MenuAction;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.Player;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import okhttp3.*;

@Slf4j
@PluginDescriptor(name = "Teleother Discord Webhook", description = "Sends a message / screenshot to a discord webhook when you teleother someone", tags = {
		"teleport", "other", "discord", "webhook" })
public class TeleotherDiscordWebhookPlugin extends Plugin {
	private static final int ACCEPT_TELEOTHER = 1816;
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

	private final Map<String, TeleotherCast> pendingTeleothers = new HashMap<>();
	private final List<DiscordWebhook> pendingScreenshots = new ArrayList<>();

	@Override
	protected void shutDown() throws Exception {
		pendingTeleothers.clear();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_PLAYER) {
			if (client.getSelectedWidget() == null)
				return;

			String spellName = client.getSelectedWidget().getName();
			if (spellName == null || !spellName.contains("Teleother")) {
				return;
			}

			String cleanSpellName = Text.sanitize(Text.removeTags(spellName));
			String destination = cleanSpellName.replace("Teleother ", "").trim();

			String rawTarget = Text.sanitize(Text.removeTags(event.getMenuTarget()));
			String targetName = rawTarget;

			if (rawTarget.contains("->")) {
				String rightSide = rawTarget.split("->")[1].trim();

				if (rightSide.contains("(")) {
					targetName = rightSide.split("\\(")[0].trim();
				} else {
					targetName = rightSide;
				}
			}

			pendingTeleothers.put(targetName, new TeleotherCast(Instant.now(), destination));
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) {
		Actor actor = event.getActor();
		if (actor == null || actor.getName() == null) {
			return;
		}

		String actorName = Text.sanitize(Text.removeTags(actor.getName()));

		if (pendingTeleothers.containsKey(actorName)) {
			if (actor.getAnimation() == ACCEPT_TELEOTHER) {
				TeleotherCast offer = pendingTeleothers.get(actorName);

				if (Duration.between(offer.castAt, Instant.now()).toSeconds() < 60) {
					handleTeleother(actorName, offer);
				}
			}
		}
	}

	private void handleTeleother(String actorName, TeleotherCast offer) {
		pendingTeleothers.remove(actorName);
		if (!config.enableWebhook() || config.webhookUrl().isEmpty())
			return;

		pendingScreenshots.add(
				new DiscordWebhook(getDiscordDelay(), actorName,
						offer.teleportDestination));
	}

	private int getDiscordDelay() {
		return config.includeScreenshot() ? SCREENSHOT_DELAY_TICKS : 0;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		Iterator<DiscordWebhook> iterator = pendingScreenshots.iterator();
		while (iterator.hasNext()) {
			DiscordWebhook pending = iterator.next();

			if (--pending.ticksRemaining <= 0) {
				sendToDink(
						pending.targetPlayerName,
						pending.teleportDestination);
				iterator.remove();
			}
		}
		if (pendingTeleothers.isEmpty()) {
			return;
		}

		Instant now = Instant.now();
		pendingTeleothers.entrySet().removeIf(entry -> {
			return Duration.between(entry.getValue().castAt, now).toSeconds() > TELEPORT_OFFER_TIMEOUT_SECONDS;
		});
	}

	private void sendToDink(String targetName, String destination) {
		HashMap<String, Object> data = new HashMap<>();
        data.put("text", config.customMessage());
		data.put("title", "Teleother");
        data.put("sourcePlugin", "Teleother");
        data.put("imageRequested", config.includeScreenshot());
		data.put("replacements", Map.of("%player%", Map.of("value", getPlayerName()), "%target%", Map.of("value", targetName), "%destination%", Map.of("value", destination)));
        if (config.webhookUrl() != null && !config.webhookUrl().isEmpty()) {
            List<HttpUrl> httpUrls = Arrays.stream(config.webhookUrl().split(";")).map(HttpUrl::parse).collect(Collectors.toList());
            data.put("urls", httpUrls);
        }

        log.debug("Sending dink notification with data: {}", data);

        eventBus.post(new PluginMessage("dink", "notify", data));
	}

	private String getPlayerName(){
		Player localPlayer = client.getLocalPlayer();
		String localPlayerName = (localPlayer != null && localPlayer.getName() != null)
				? Text.sanitize(Text.removeTags(localPlayer.getName()))
				: "Someone";
		return localPlayerName;
	}
}
