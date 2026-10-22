package com.teleother;

import java.time.Instant;

public class TeleotherCast {
    public final Instant castAt;
	public final String teleportDestination;

    public TeleotherCast(Instant castAt, String teleportDestination) {
        this.castAt = castAt;
        this.teleportDestination = teleportDestination;
    }
}
