package fr.redfaction.entity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a named teleportation warp within a faction.
 * Stored as raw components to avoid Bukkit dependency at serialization time.
 */
public class FactionWarp {

    private final String name;
    private String world;
    private double x, y, z;
    private float yaw, pitch;

    public FactionWarp(String name, String world, double x, double y, double z, float yaw, float pitch) {
        this.name  = name;
        this.world = world;
        this.x     = x;
        this.y     = y;
        this.z     = z;
        this.yaw   = yaw;
        this.pitch = pitch;
    }

    public FactionWarp(String name, Location location) {
        this.name  = name;
        this.world = location.getWorld().getName();
        this.x     = location.getX();
        this.y     = location.getY();
        this.z     = location.getZ();
        this.yaw   = location.getYaw();
        this.pitch = location.getPitch();
    }

    /** Returns a Bukkit Location, or null if the world is not loaded. */
    public Location getLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public String getName()  { return name; }
    public String getWorld() { return world; }
    public double getX()     { return x; }
    public double getY()     { return y; }
    public double getZ()     { return z; }
    public float  getYaw()   { return yaw; }
    public float  getPitch() { return pitch; }

    @Override
    public String toString() {
        return name + " @ " + world + " " + String.format("%.1f,%.1f,%.1f", x, y, z);
    }
}

