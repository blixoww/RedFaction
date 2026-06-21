package fr.redfaction.main;

import fr.redfaction.api.RedFactionAPI;
import fr.redfaction.commands.AChatCommand;
import fr.redfaction.commands.FChatCommand;
import fr.redfaction.commands.FCommand;
import fr.redfaction.data.DataManager;
import fr.redfaction.hooks.WorldGuardHook;
import fr.redfaction.listeners.*;
import fr.redfaction.managers.ChestManager;
import fr.redfaction.managers.ClaimManager;
import fr.redfaction.managers.CombatTagManager;
import fr.redfaction.managers.FactionManager;
import fr.redfaction.managers.FPlayerManager;
import fr.redfaction.managers.NametagManager;
import fr.redfaction.tasks.AutoDisbandTask;
import fr.redfaction.tasks.AutoSaveTask;
import fr.redfaction.tasks.PowerRegenTask;
import fr.redfaction.utils.ConfigUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public final class RedFaction extends JavaPlugin {

    private static RedFaction instance;

    /** Config re-read as UTF-8 so '§'-prefixed values don't show a stray "Â". */
    private FileConfiguration utf8Config;

    private FPlayerManager   fPlayerManager;
    private FactionManager   factionManager;
    private ClaimManager     claimManager;
    private ChestManager     chestManager;
    private CombatTagManager combatTagManager;
    private NametagManager   nametagManager;
    private DataManager      dataManager;
    private ConfigUtil       configUtil;
    private ChatListener     chatListener;
    private WorldGuardHook   worldGuardHook;
    private fr.redfaction.hooks.VaultHook vaultHook;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig(); // load as UTF-8 (see override below)
        configUtil = new ConfigUtil(this);

        fPlayerManager   = new FPlayerManager();
        claimManager     = new ClaimManager();
        factionManager   = new FactionManager();
        chestManager     = new ChestManager(this);
        combatTagManager = new CombatTagManager();
        nametagManager   = new NametagManager(this);

        dataManager = new DataManager(this);
        dataManager.loadAll();

        RedFactionAPI.init(this);

        // Optional WorldGuard hook
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook();
            getLogger().info("[RedFaction] WorldGuard détecté — intégration activée.");
        }

        // Optional Vault hook (economy balance shown in /f show)
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                vaultHook = new fr.redfaction.hooks.VaultHook();
                getLogger().info("[RedFaction] Vault détecté — solde affiché au survol dans /f show.");
            } catch (Throwable t) {
                getLogger().warning("[RedFaction] Vault présent mais économie indisponible: " + t.getMessage());
            }
        }

        // Optional PlaceholderAPI expansion (%redfaction_*%)
        if (configUtil.isPlaceholderApiEnabled()
                && getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new fr.redfaction.hooks.RedFactionExpansion(this).register();
                getLogger().info("[RedFaction] PlaceholderAPI détecté — placeholders %redfaction_*% enregistrés.");
            } catch (Throwable t) {
                getLogger().warning("[RedFaction] Échec de l'enregistrement PlaceholderAPI: " + t.getMessage());
            }
        }

        // Register listeners
        chatListener = new ChatListener(this);
        FCommand fCmd = new FCommand(this);
        getServer().getPluginManager().registerEvents(new ClaimProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PowerListener(this), this);
        getServer().getPluginManager().registerEvents(new PvPListener(this), this);
        getServer().getPluginManager().registerEvents(chatListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new AutoClaimListener(this), this);
        getServer().getPluginManager().registerEvents(new AutomapListener(this, fCmd.getMapCommand()), this);
        getServer().getPluginManager().registerEvents(new CombatTagListener(this), this);
        getServer().getPluginManager().registerEvents(new fr.redfaction.listeners.PermGuiListener(this), this);

        // Register commands
        getCommand("f").setExecutor(fCmd);
        getCommand("f").setTabCompleter(fCmd);
        getCommand("fc").setExecutor(new FChatCommand(this));
        getCommand("ac").setExecutor(new AChatCommand(this));

        // Schedule tasks
        long regenInterval   = 20L * 60;
        long saveInterval    = 20L * 60 * configUtil.getAutoSaveIntervalMinutes();
        long disbandInterval = 20L * 60 * 60; // every hour
        new PowerRegenTask(this).runTaskTimer(this, regenInterval, regenInterval);
        new AutoSaveTask(this).runTaskTimer(this, saveInterval, saveInterval);
        new AutoDisbandTask(this).runTaskTimer(this, disbandInterval, disbandInterval);

        // Relation-coloured nametags (optional; takes over scoreboards)
        if (configUtil.isNametagsEnabled()) {
            getServer().getScheduler().runTaskTimer(this, () -> nametagManager.refreshAll(), 60L, 60L);
            getLogger().info("[RedFaction] Nametags par relation activés.");
        }

        getLogger().info("§c§lRED §f§lCONFLICT §av" + getDescription().getVersion() + " activé !");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        if (chestManager != null) {
            chestManager.saveAll();
        }
        getLogger().info("[RedFaction] Données sauvegardées. Plugin désactivé.");
        instance = null;
    }

    /**
     * Re-reads config.yml using an explicit UTF-8 reader. Bukkit's default loader
     * uses the platform charset on some setups (typically Windows servers), which
     * turns the UTF-8 lead byte of '§' into a literal "Â" in rank names, chat
     * prefixes, etc. Reading as UTF-8 here fixes every config string at once.
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) { utf8Config = null; return; }
        try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            YamlConfiguration loaded = YamlConfiguration.loadConfiguration(reader);
            InputStream defStream = getResource("config.yml");
            if (defStream != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8)));
            }
            utf8Config = loaded;
        } catch (IOException e) {
            getLogger().warning("[RedFaction] Lecture UTF-8 de config.yml impossible: " + e.getMessage());
            utf8Config = null;
        }
    }

    @Override
    public FileConfiguration getConfig() {
        if (utf8Config == null) reloadConfig();
        return utf8Config != null ? utf8Config : super.getConfig();
    }

    public static RedFaction getInstance() { return instance; }

    public FPlayerManager  getFPlayerManager()   { return fPlayerManager; }
    public FactionManager  getFactionManager()   { return factionManager; }
    public ClaimManager    getClaimManager()     { return claimManager; }
    public ChestManager    getChestManager()     { return chestManager; }
    public DataManager     getDataManager()      { return dataManager; }
    public ConfigUtil      getConfigUtil()       { return configUtil; }
    public ChatListener    getChatListener()     { return chatListener; }
    public WorldGuardHook  getWorldGuardHook()   { return worldGuardHook; }

    public CombatTagManager getCombatTagManager() {
        return combatTagManager;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public fr.redfaction.hooks.VaultHook getVaultHook() {
        return vaultHook;
    }
}
