package fr.redfaction.data;

import com.google.gson.*;
import fr.redfaction.entity.*;
import fr.redfaction.main.RedFaction;
import fr.redfaction.managers.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles all JSON persistence for RedFaction.
 * Faction data: plugins/RedFaction/data/factions/<uuid>.json
 * Player data:  plugins/RedFaction/data/players.json
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

    public void loadAll() {
        loadFactions();
        loadPlayers();
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
        fm.ensureSpecialFactions();
    }

    private Faction readFactionFile(File file) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject obj = gson.fromJson(reader, JsonObject.class);
            if (obj == null) return null;

            UUID id   = UUID.fromString(obj.get("id").getAsString());
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

            // Allies (array)
            if (obj.has("allies")) {
                for (JsonElement e : obj.getAsJsonArray("allies")) {
                    faction.getAlliesInternal().add(UUID.fromString(e.getAsString()));
                }
            }

            // Truces
            if (obj.has("truces")) {
                for (JsonElement e : obj.getAsJsonArray("truces")) {
                    faction.getTrucesInternal().add(UUID.fromString(e.getAsString()));
                }
            }

            // Enemies
            if (obj.has("enemies")) {
                for (JsonElement e : obj.getAsJsonArray("enemies")) {
                    faction.getEnemiesInternal().add(UUID.fromString(e.getAsString()));
                }
            }

            // Pending ally requests
            if (obj.has("pendingAllyRequests")) {
                for (JsonElement e : obj.getAsJsonArray("pendingAllyRequests")) {
                    faction.getPendingAllyRequestsInternal().add(UUID.fromString(e.getAsString()));
                }
            }

            // Pending truce requests
            if (obj.has("pendingTruceRequests")) {
                for (JsonElement e : obj.getAsJsonArray("pendingTruceRequests")) {
                    faction.getPendingTruceRequestsInternal().add(UUID.fromString(e.getAsString()));
                }
            }

            // Banned players
            if (obj.has("bannedPlayers")) {
                for (JsonElement e : obj.getAsJsonArray("bannedPlayers")) {
                    faction.getBannedPlayersInternal().add(UUID.fromString(e.getAsString()));
                }
            }

            // Permission grid (rank/relation rows)
            if (obj.has("permGrid")) {
                for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("permGrid").entrySet()) {
                    PermTarget target = PermTarget.fromString(e.getKey());
                    if (target == null) continue;
                    faction.getPermGridInternal().put(target, readPermSet(e.getValue()));
                }
            }

            // Per-player permission overrides
            if (obj.has("playerPerms")) {
                for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("playerPerms").entrySet()) {
                    EnumSet<FactionPermission> set = readPermSet(e.getValue());
                    if (!set.isEmpty()) faction.getPlayerPermsInternal().put(UUID.fromString(e.getKey()), set);
                }
            }

            // Per-faction permission overrides
            if (obj.has("factionPerms")) {
                for (Map.Entry<String, JsonElement> e : obj.getAsJsonObject("factionPerms").entrySet()) {
                    EnumSet<FactionPermission> set = readPermSet(e.getValue());
                    if (!set.isEmpty()) faction.getFactionPermsInternal().put(UUID.fromString(e.getKey()), set);
                }
            }

            // Legacy access grants -> migrate to full-territory overrides
            if (obj.has("accessPlayers")) {
                for (JsonElement e : obj.getAsJsonArray("accessPlayers")) {
                    faction.grantPlayerAccess(UUID.fromString(e.getAsString()));
                }
            }
            if (obj.has("accessFactions")) {
                for (JsonElement e : obj.getAsJsonArray("accessFactions")) {
                    faction.grantFactionAccess(UUID.fromString(e.getAsString()));
                }
            }

            // Warps
            if (obj.has("warps")) {
                for (JsonElement el : obj.getAsJsonArray("warps")) {
                    JsonObject w = el.getAsJsonObject();
                    FactionWarp warp = new FactionWarp(
                            w.get("name").getAsString(),
                            w.get("world").getAsString(),
                            w.get("x").getAsDouble(),
                            w.get("y").getAsDouble(),
                            w.get("z").getAsDouble(),
                            w.get("yaw").getAsFloat(),
                            w.get("pitch").getAsFloat()
                    );
                    faction.getWarpsInternal().put(warp.getName().toLowerCase(), warp);
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

            // Last announce
            if (obj.has("lastAnnouncementTime")) {
                faction.setLastAnnouncementTime(obj.get("lastAnnouncementTime").getAsLong());
            }
            if (obj.has("chestEnabled")) {
                faction.setChestEnabled(obj.get("chestEnabled").getAsBoolean());
            }
            if (obj.has("lastAllOfflineEpoch")) {
                faction.setLastAllOfflineEpoch(obj.get("lastAllOfflineEpoch").getAsLong());
            }
            if (obj.has("tag") && !obj.get("tag").isJsonNull()) {
                faction.setTag(obj.get("tag").getAsString());
            }
            if (obj.has("open")) {
                faction.setOpen(obj.get("open").getAsBoolean());
            }
            if (obj.has("foundedDate")) {
                faction.setFoundedDate(obj.get("foundedDate").getAsLong());
            }

            return faction;
        }
    }

    /** Serialises a permission set into a JSON array of names. */
    private JsonArray writePermSet(EnumSet<FactionPermission> set) {
        JsonArray arr = new JsonArray();
        for (FactionPermission p : set) arr.add(new JsonPrimitive(p.name()));
        return arr;
    }

    /** Parses a JSON array of permission names into an EnumSet. */
    private EnumSet<FactionPermission> readPermSet(JsonElement element) {
        EnumSet<FactionPermission> set = EnumSet.noneOf(FactionPermission.class);
        if (element != null && element.isJsonArray()) {
            for (JsonElement pe : element.getAsJsonArray()) {
                FactionPermission p = FactionPermission.fromString(pe.getAsString());
                if (p != null) set.add(p);
            }
        }
        return set;
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
                if (obj.has("powerRegenAnchor")) fp.setPowerRegenAnchor(obj.get("powerRegenAnchor").getAsLong());
                if (obj.has("factionId") && !obj.get("factionId").isJsonNull()) {
                    fp.setFactionId(UUID.fromString(obj.get("factionId").getAsString()));
                }
                if (obj.has("lastSeen")) fp.setLastSeen(obj.get("lastSeen").getAsLong());
                if (obj.has("factionJoinDate")) fp.setFactionJoinDate(obj.get("factionJoinDate").getAsLong());
                if (obj.has("customTitle") && !obj.get("customTitle").isJsonNull()) {
                    fp.setCustomTitle(obj.get("customTitle").getAsString());
                }
                if (obj.has("territoryMessages")) {
                    fp.setTerritoryMessages(obj.get("territoryMessages").getAsBoolean());
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

    public void saveAll() {
        for (Faction f : plugin.getFactionManager().getAllFactions()) {
            saveFaction(f);
        }
        savePlayers();
    }

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

        // Allies (array)
        JsonArray allies = new JsonArray();
        for (UUID id : faction.getAlliesInternal()) allies.add(new JsonPrimitive(id.toString()));
        obj.add("allies", allies);

        // Truces
        JsonArray truces = new JsonArray();
        for (UUID id : faction.getTrucesInternal()) truces.add(new JsonPrimitive(id.toString()));
        obj.add("truces", truces);

        // Enemies
        JsonArray enemies = new JsonArray();
        for (UUID id : faction.getEnemiesInternal()) enemies.add(new JsonPrimitive(id.toString()));
        obj.add("enemies", enemies);

        // Pending ally requests
        JsonArray pendingAllies = new JsonArray();
        for (UUID id : faction.getPendingAllyRequestsInternal()) pendingAllies.add(new JsonPrimitive(id.toString()));
        obj.add("pendingAllyRequests", pendingAllies);

        // Pending truce requests
        JsonArray pendingTruces = new JsonArray();
        for (UUID id : faction.getPendingTruceRequestsInternal()) pendingTruces.add(new JsonPrimitive(id.toString()));
        obj.add("pendingTruceRequests", pendingTruces);

        // Banned players
        JsonArray banned = new JsonArray();
        for (UUID id : faction.getBannedPlayersInternal()) banned.add(new JsonPrimitive(id.toString()));
        obj.add("bannedPlayers", banned);

        // Permission grid
        JsonObject permGrid = new JsonObject();
        for (Map.Entry<PermTarget, EnumSet<FactionPermission>> e : faction.getPermGridInternal().entrySet()) {
            permGrid.add(e.getKey().name(), writePermSet(e.getValue()));
        }
        obj.add("permGrid", permGrid);

        // Per-player permission overrides
        JsonObject playerPerms = new JsonObject();
        for (Map.Entry<UUID, EnumSet<FactionPermission>> e : faction.getPlayerPermsInternal().entrySet()) {
            playerPerms.add(e.getKey().toString(), writePermSet(e.getValue()));
        }
        obj.add("playerPerms", playerPerms);

        // Per-faction permission overrides
        JsonObject factionPerms = new JsonObject();
        for (Map.Entry<UUID, EnumSet<FactionPermission>> e : faction.getFactionPermsInternal().entrySet()) {
            factionPerms.add(e.getKey().toString(), writePermSet(e.getValue()));
        }
        obj.add("factionPerms", factionPerms);

        // Warps
        JsonArray warps = new JsonArray();
        for (FactionWarp w : faction.getWarpsInternal().values()) {
            JsonObject wo = new JsonObject();
            wo.addProperty("name",  w.getName());
            wo.addProperty("world", w.getWorld());
            wo.addProperty("x",     w.getX());
            wo.addProperty("y",     w.getY());
            wo.addProperty("z",     w.getZ());
            wo.addProperty("yaw",   w.getYaw());
            wo.addProperty("pitch", w.getPitch());
            warps.add(wo);
        }
        obj.add("warps", warps);

        // Claims
        JsonArray claims = new JsonArray();
        for (FLocation loc : faction.getClaimsInternal()) {
            JsonObject c = new JsonObject();
            c.addProperty("world",  loc.getWorld());
            c.addProperty("chunkX", loc.getChunkX());
            c.addProperty("chunkZ", loc.getChunkZ());
            claims.add(c);
        }
        obj.add("claims", claims);

        // Spawn
        if (faction.hasSpawn()) {
            JsonObject spawn = new JsonObject();
            spawn.addProperty("world", faction.getSpawnWorld());
            spawn.addProperty("x",     faction.getSpawnX());
            spawn.addProperty("y",     faction.getSpawnY());
            spawn.addProperty("z",     faction.getSpawnZ());
            spawn.addProperty("yaw",   faction.getSpawnYaw());
            spawn.addProperty("pitch", faction.getSpawnPitch());
            obj.add("spawn", spawn);
        } else {
            obj.add("spawn", JsonNull.INSTANCE);
        }

        obj.addProperty("lastAnnouncementTime", faction.getLastAnnouncementTime());
        obj.addProperty("chestEnabled", faction.isChestEnabled());
        obj.addProperty("lastAllOfflineEpoch", faction.getLastAllOfflineEpoch());
        obj.addProperty("tag", faction.getRawTag());
        obj.addProperty("open", faction.isOpen());
        obj.addProperty("foundedDate", faction.getFoundedDate());

        writeJson(file, obj);
    }

    public void deleteFactionFile(UUID factionId) {
        new File(factionsDir, factionId.toString() + ".json").delete();
    }

    public void savePlayers() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (FPlayer fp : plugin.getFPlayerManager().getAllFPlayers()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid",            fp.getUuid().toString());
            obj.addProperty("name",            fp.getName());
            obj.addProperty("power",           fp.getPower());
            obj.addProperty("powerRegenAnchor", fp.getPowerRegenAnchor());
            obj.addProperty("factionId",       fp.getFactionId() != null ? fp.getFactionId().toString() : null);
            obj.addProperty("lastSeen",        fp.getLastSeen());
            obj.addProperty("factionJoinDate", fp.getFactionJoinDate());
            obj.addProperty("customTitle",     fp.getCustomTitle());
            obj.addProperty("territoryMessages", fp.isTerritoryMessages());
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
