package com.teleother;

public class DiscordWebhook {
    public int ticksRemaining;
    public final String targetPlayerName;
    public final String teleportDestination;

    public DiscordWebhook(int ticksRemaining,
            String targetPlayerName,
            String teleportDestination) {
        this.ticksRemaining = ticksRemaining;
        this.targetPlayerName = targetPlayerName;
        this.teleportDestination = teleportDestination;
    }
}
