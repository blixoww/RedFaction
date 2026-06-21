package fr.redfaction.entity;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Objects;

/**
 * Represents a chunk location identified by world name, chunk X and chunk Z.
 * Used as the key for claim ownership in ClaimManager.
 */
public class FLocation {

    private final String world;
    private final int chunkX;
    private final int chunkZ;

    public FLocation(String world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /** Creates an FLocation from a Bukkit Location by extracting its chunk. */
    public static FLocation fromLocation(Location location) {
        return new FLocation(
                location.getWorld().getName(),
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4
        );
    }

    /** Creates an FLocation from a Bukkit Chunk. */
    public static FLocation fromChunk(Chunk chunk) {
        return new FLocation(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public String getWorld() { return world; }
    public int getChunkX()   { return chunkX; }
    public int getChunkZ()   { return chunkZ; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FLocation)) return false;
        FLocation other = (FLocation) o;
        return chunkX == other.chunkX && chunkZ == other.chunkZ && Objects.equals(world, other.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, chunkX, chunkZ);
    }

    @Override
    public String toString() {
        return world + ":" + chunkX + "," + chunkZ;
    }
}

