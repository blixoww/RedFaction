package fr.redfaction.commands.sub;

import fr.redfaction.commands.SubCommand;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import fr.redfaction.utils.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** /f list — Lists all normal factions with online/total member counts. */
public class ListCommand implements SubCommand {

    private final RedFaction plugin;

    public ListCommand(RedFaction plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<Faction> factions = new ArrayList<>(plugin.getFactionManager().getNormalFactions());
        factions.sort(Comparator.comparingDouble(Faction::getPower).reversed());

        sender.sendMessage(MessageUtil.header("Factions (" + factions.size() + ")"));
        if (factions.isEmpty()) {
            sender.sendMessage("§7Aucune faction créée.");
            return;
        }
        for (Faction f : factions) {
            String raidable = f.isRaidable() ? " §c[RAID]" : "";
            sender.sendMessage("§e" + f.getName()
                    + " §7(§a" + f.getOnlineCount() + "§7/§f" + f.getMembers().size() + "§7)"
                    + " §7Power: §e" + String.format("%.1f", f.getPower())
                    + " §7Claims: §e" + f.getClaimCount()
                    + raidable);
        }
        sender.sendMessage("§8§m-----------------------------------");
    }

    @Override public String getPermission()   { return "redfaction.use"; }
    @Override public String getUsage()        { return "/f list"; }
    @Override public String getDescription()  { return "Liste toutes les factions."; }
}

