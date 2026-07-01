package com.holybuckets.foundation.event.custom;

import com.holybuckets.foundation.client.core.MovingWaypoint;
import net.minecraft.world.entity.player.Player;

public class DetermineActiveWaypointEvent {

    private final MovingWaypoint.Waypoint waypoint;
    private final Player player;

    public DetermineActiveWaypointEvent(MovingWaypoint.Waypoint waypoint, Player player) {
        this.waypoint = waypoint;
        this.player = player;
    }

    public MovingWaypoint.Waypoint getWaypoint() { return waypoint; }
    public Player getPlayer() { return player; }
}
