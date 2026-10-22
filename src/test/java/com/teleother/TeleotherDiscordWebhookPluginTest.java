package com.teleother;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TeleotherDiscordWebhookPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TeleotherDiscordWebhookPlugin.class);
		RuneLite.main(args);
	}
}