package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.entity.Role;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * /f chest [toggle|log [page]|put|take <slot>]
 * Virtual faction chest accessible by rank.
 */
public class ChestCommand implements SubCommand {

    private final RedFaction plugin;

    public ChestCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { MessageUtil.sendError(sender, "Commande réservée aux joueurs."); return; }
        Player player = (Player) sender;
        FPlayer fp = plugin.getFPlayerManager().getFPlayer(player.getUniqueId());

        if (fp == null || !fp.hasFaction()) { MessageUtil.sendError(sender, "Vous n'avez pas de faction."); return; }

        Faction faction = fp.getFaction();

        if (args.length == 0) {
            showContents(player, fp, faction);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "toggle":
                handleToggle(sender, fp, faction);
                break;
            case "log":
                handleLog(player, faction, args);
                break;
            case "put":
                handlePut(player, fp, faction);
                break;
            case "take":
                if (args.length < 2) { MessageUtil.sendError(sender, "/f chest take <slot>"); return; }
                handleTake(player, fp, faction, args[1]);
                break;
            default:
                MessageUtil.sendError(sender, getUsage());
        }
    }

    private void showContents(Player player, FPlayer fp, Faction faction) {
        if (!faction.isChestEnabled()) {
            MessageUtil.sendError(player, "Le coffre de faction est désactivé.");
            return;
        }
        ItemStack[] contents = plugin.getChestManager().getContents(faction.getId());
        player.sendMessage(MessageUtil.header("Coffre - " + faction.getName()));
        int slots = plugin.getConfigUtil().getDefaultChestSlots();
        boolean empty = true;
        for (int i = 0; i < Math.min(slots, contents.length); i++) {
            if (contents[i] != null && contents[i].getType() != org.bukkit.Material.AIR) {
                player.sendMessage("§8[§e" + i + "§8] §f" + contents[i].getType().name()
                        + " §7x" + contents[i].getAmount());
                empty = false;
            }
        }
        if (empty) player.sendMessage("§7Le coffre est vide.");
        player.sendMessage("§7Utilisez §e/f chest put §7pour déposer / §e/f chest take <slot> §7pour retirer.");
        player.sendMessage("§8§m-----------------------------------");
    }

    private void handleToggle(CommandSender sender, FPlayer fp, Faction faction) {
        if (fp.getRole() != Role.LEADER) {
            MessageUtil.sendError(sender, "Seul le §6Chef §cpeut activer/désactiver le coffre.");
            return;
        }
        faction.setChestEnabled(!faction.isChestEnabled());
        plugin.getDataManager().saveFaction(faction);
        MessageUtil.sendSuccess(sender, "Coffre §e" + (faction.isChestEnabled() ? "activé" : "désactivé") + "§a.");
    }

    private void handleLog(Player player, Faction faction, String[] args) {
        List<String> log = plugin.getChestManager().getLog(faction.getId());
        int page = 1;
        if (args.length >= 2) {
            try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        int perPage = 8;
        int total = log.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / perPage));
        page = Math.max(1, Math.min(page, totalPages));
        player.sendMessage(MessageUtil.header("Coffre Log - " + faction.getName() + " (" + page + "/" + totalPages + ")"));
        int start = (page - 1) * perPage;
        int end   = Math.min(start + perPage, total);
        if (total == 0) {
            player.sendMessage("§7Aucun accès enregistré.");
        } else {
            for (int i = end - 1; i >= start; i--) {
                player.sendMessage("§7" + log.get(i));
            }
        }
        player.sendMessage("§8§m-----------------------------------");
    }

    private void handlePut(Player player, FPlayer fp, Faction faction) {
        if (!faction.isChestEnabled()) { MessageUtil.sendError(player, "Le coffre est désactivé."); return; }
        ItemStack held = player.getItemInHand();
        if (held == null || held.getType() == org.bukkit.Material.AIR) {
            MessageUtil.sendError(player, "Tenez un item en main.");
            return;
        }
        int slot = plugin.getChestManager().addItem(faction.getId(), held.clone());
        if (slot < 0) {
            MessageUtil.sendError(player, "Le coffre de faction est plein.");
            return;
        }
        player.setItemInHand(null);
        plugin.getChestManager().addLog(faction.getId(),
                player.getName() + " a déposé " + held.getType().name() + " x" + held.getAmount() + " (slot " + slot + ")");
        plugin.getChestManager().save(faction.getId());
        MessageUtil.sendSuccess(player, "§e" + held.getType().name() + " x" + held.getAmount()
                + " §adéposé dans le coffre (slot §e" + slot + "§a).");
    }

    private void handleTake(Player player, FPlayer fp, Faction faction, String slotStr) {
        if (!faction.isChestEnabled()) { MessageUtil.sendError(player, "Le coffre est désactivé."); return; }
        if (!fr.redfaction.utils.PermissionUtil.canManage(fp, fr.redfaction.entity.FactionPermission.FCHEST)) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission de retirer du coffre (§e/f perm§c).");
            return;
        }
        int slot;
        try { slot = Integer.parseInt(slotStr); } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "Slot invalide."); return;
        }
        ItemStack item = plugin.getChestManager().takeItem(faction.getId(), slot);
        if (item == null) {
            MessageUtil.sendError(player, "Slot §e" + slot + " §cvide ou invalide."); return;
        }
        player.getInventory().addItem(item);
        plugin.getChestManager().addLog(faction.getId(),
                player.getName() + " a retiré " + item.getType().name() + " x" + item.getAmount() + " (slot " + slot + ")");
        plugin.getChestManager().save(faction.getId());
        MessageUtil.sendSuccess(player, "§e" + item.getType().name() + " x" + item.getAmount() + " §aretiré du coffre.");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f chest [toggle|log|put|take <slot>]"; }
    @Override public String getDescription()  { return "Coffre de faction partagé."; }
}
