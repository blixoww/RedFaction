package fr.redfaction.data;

import com.google.gson.*;
import fr.redfaction.entity.*;
import fr.redfaction.main.RedFaction;
import fr.redfaction.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles all JSON persistence for RedFaction.
 * Faction data: plugins/RedFaction/data/factions/<uuid>.json (one per faction)
 * Player data:  plugins/RedFaction/data/players.json (all players in one file)
 */
public class DataManager {

    private final RedFaction plugin;
    private final Gson gson;
    private final File factionsDir;
    private final File playersFile;

    public DataManager(RedFaction plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        File dataDir = new File(plugin.getDataFolder(), "data");
        this.factionsDir = new File(dataDir, "factions");
        this.playersFile = new File(dataDir, "players.json");
        factionsDir.mkdirs();
    }

    // ================================================================
    //  LOAD
    // ================================================================

    /** Loads all factions and players from disk into the managers. */
    public void loadAll() {
        loadFactions();
        loadPlayers();
        // Rebuild ClaimManager index from faction claim sets
        rebuildClaimIndex();
    }

    private void loadFactions() {
        FactionManager fm = plugin.getFactionManager();
        File[] files = factionsDir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            try {
                Faction f = readFactionFile(file);
                if (f != null) fm.addFaction(f);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load faction file: " + file.getName(), e);
            }
        }
        // Ensure special factions exist even if no file loaded
        fm.ensureSpecialFactions();
    }

    private Faction readFactionFile(File file) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = gson.fromJson(reader, JsonObject.class);
            if (obj == null) return null;
            UUID id = UUID.fromString(obj.get("id").getAsString());
            String name = obj.get("name").getAsString();
            Faction faction = new Faction(id, name);
            if (obj.has("description")) faction.setDescription(obj.get("description").getAsString());
            if (obj.has("motd"))        faction.setMotd(obj.get("motd").getAsString());
            // Members
            if (obj.has("members")) {
                for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("members").entrySet()) {
                    faction.getMembersInternal().put(UUID.fromString(e.getKey()), Role.valueOf(e.getValue().getAsString()));
                }
            }
            // Ally
            if (obj.has("ally") && !obj.get("ally").isJsonNull()) {
                faction.setAlly(UUID.fromString(obj.get("ally").getAsString()));
            }
            // Enemies
            if (obj.has("enemies")) {
                for (JsonElement e : obj.getAsJsonArray("enemies")) {
                    faction.getEnemiesInternal().add(UUID.fromString(e.getAsString()));
                }
            }
            // Claims
            if (obj.has("claims")) {
                for (JsonElement e : obj.getAsJsonArray("claims")) {
                    JsonObject c = e.getAsJsonObject();
                    faction.getClaimsInternal().add(new FLocation(
                            c.get("world").getAsString(),
                            c.get("chunkX").getAsInt(),
                            c.get("chunkZ").getAsInt()
                    ));
                }
            }
            // Spawn
            if (obj.has("spawn") && !obj.get("spawn").isJsonNull()) {
                JsonObject s = obj.getAsJsonObject("spawn");
                faction.setSpawnWorld(s.get("world").getAsString());
                faction.setSpawnX(s.get("x").getAsDouble());
                faction.setSpawnY(s.get("y").getAsDouble());
                faction.setSpawnZ(s.get("z").getAsDouble());
                faction.setSpawnYaw(s.get("yaw").getAsFloat());
                faction.setSpawnPitch(s.get("pitch").getAsFloat());
                faction.setHasSpawn(true);
            }
            return faction;
        }
    }

    private void loadPlayers() {
        if (!playersFile.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(playersFile), StandardCharsets.UTF_8)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("players")) return;
            FPlayerManager fpm = plugin.getFPlayerManager();
            for (JsonElement el : root.getAsJsonArray("players")) {
                JsonObject obj = el.getAsJsonObject();
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                String name = obj.get("name").getAsString();
                FPlayer fp = new FPlayer(uuid, name);
                fp.setPower(obj.get("power").getAsDouble());
                if (obj.has("factionId") && !obj.get("factionId").isJsonNull()) {
                    fp.setFactionId(UUID.fromString(obj.get("factionId").getAsString()));
                }
                fpm.addFPlayer(fp);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load players.json", e);
        }
    }

    private void rebuildClaimIndex() {
        ClaimManager cm = plugin.getClaimManager();
        cm.getClaimsInternal().clear();
        for (Faction f : plugin.getFactionManager().getAllFactions()) {
            for (FLocation loc : f.getClaims()) {
                cm.getClaimsInternal().put(loc, f.getId());
            }
        }
    }

    // ================================================================
    //  SAVE
    // ================================================================

    /** Saves all data (factions + players) to disk. */
    public void saveAll() {
        for (Faction f : plugin.getFactionManager().getAllFactions()) {
            saveFaction(f);
        }
        savePlayers();
    }

    /** Saves a single faction to its JSON file. */
    public void saveFaction(Faction faction) {
        File file = new File(factionsDir, faction.getId().toString() + ".json");
        JsonObject obj = new JsonObject();
        obj.addProperty("id", faction.getId().toString());
        obj.addProperty("name", faction.getName());
        obj.addProperty("description", faction.getDescription());
        obj.addProperty("motd", faction.getMotd());
        // Members
        JsonObject members = new JsonObject();
        for (Map.Entry<UUID, Role> e : faction.getMembersInternal().entrySet()) {
            members.addProperty(e.getKey().toString(), e.getValue().name());
        }
        obj.add("members", members);
        // Ally
        obj.addProperty("ally", faction.getAlly() != null ? faction.getAlly().toString() : null);
        // Enemies
        JsonArray enemies = new JsonArray();
        for (UUID id : faction.getEnemiesInternal()) enemies.add(id.toString());
        obj.add("enemies", enemies);
        // Claims
        JsonArray claims = new JsonArray();
        for (FLocation loc : faction.getClaimsInternal()) {
            JsonObject c = new JsonObject();
            c.addProperty("world", loc.getWorld());
            c.addProperty("chunkX", loc.getChunkX());
            c.addProperty("chunkZ", loc.getChunkZ());
            claims.add(c);
        }
        obj.add("claims", claims);
        // Spawn
        if (faction.hasSpawn()) {
            JsonObject spawn = new JsonObject();
            spawn.addProperty("world", faction.getSpawnWorld());
            spawn.addProperty("x", faction.getSpawnX());
            spawn.addProperty("y", faction.getSpawnY());
            spawn.addProperty("z", faction.getSpawnZ());
            spawn.addProperty("yaw", faction.getSpawnYaw());
            spawn.addProperty("pitch", faction.getSpawnPitch());
            obj.add("spawn", spawn);
        } else {
            obj.add("spawn", JsonNull.INSTANCE);
        }
        writeJson(file, obj);
    }

    /** Deletes the faction's save file (on disband). */
    public void deleteFactionFile(UUID factionId) {
        new File(factionsDir, factionId.toString() + ".json").delete();
    }

    /** Saves all players to players.json. */
    public void savePlayers() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (fr.redfaction.entity.FPlayer fp : plugin.getFPlayerManager().getAllFPlayers()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", fp.getUuid().toString());
            obj.addProperty("name", fp.getName());
            obj.addProperty("power", fp.getPower());
            obj.addProperty("factionId", fp.getFactionId() != null ? fp.getFactionId().toString() : null);
            arr.add(obj);
        }
        root.add("players", arr);
        writeJson(playersFile, root);
    }

    // ================================================================
    //  Utility
    // ================================================================

    private void writeJson(File file, JsonElement json) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write JSON to " + file.getName(), e);
        }
    }
}

