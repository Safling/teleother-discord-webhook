package com.teleother;
import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
@Slf4j
@PluginDescriptor(name = "Teleother Discord Webhook", description = "Sends a message / screenshot to a discord webhook when you teleother someone", tags = {
		"teleport", "other", "discord", "webhook" })
public class TeleotherDiscordWebhookPlugin extends Plugin {
	private static final int ACCEPT_TELEOTHER = 1816;
	private static final long TELEPORT_OFFER_TIMEOUT_SECONDS = 60;
	@Inject
	private Client client;

	@Inject
	private TeleotherDiscordWebhookConfig config;

	@Provides
	TeleotherDiscordWebhookConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(TeleotherDiscordWebhookConfig.class);
	}

	private final Map<String, TeleotherCast> pendingTeleothers = new HashMap<>();

	@Override
	protected void shutDown() throws Exception {
		pendingTeleothers.clear();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_PLAYER) {
			if (client.getSelectedWidget() == null) {
				return;
			}

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
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (pendingTeleothers.isEmpty()) {
			return;
		}

		Instant now = Instant.now();
		pendingTeleothers.entrySet().removeIf(entry -> {
			return Duration.between(entry.getValue().castAt, now).toSeconds() > TELEPORT_OFFER_TIMEOUT_SECONDS;
		});
	}
