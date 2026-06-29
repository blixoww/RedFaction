package fr.redfaction.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Optional Vault economy bridge. Only instantiated when the Vault plugin is
 * present, so the {@link Economy} class is never referenced without Vault loaded.
 */
public class VaultHook {

    private Economy economy;

    public VaultHook() {
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
    }

    public boolean hasEconomy() { return economy != null; }

    public double getBalance(OfflinePlayer player) {
        return economy != null ? economy.getBalance(player) : 0.0;
    }

    public String format(double amount) {
        return economy != null ? economy.format(amount) : String.format("%.2f", amount);
    }

    /** True if the player can afford {@code amount}. False if no economy is available. */
    public boolean has(OfflinePlayer player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    /** Withdraws {@code amount} from the player. Returns true only on a successful transaction. */
    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }
}
