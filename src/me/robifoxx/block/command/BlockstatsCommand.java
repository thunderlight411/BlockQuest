package me.robifoxx.block.command;

import me.robifoxx.block.BlockQuestAPI;
import me.robifoxx.block.Main;
import me.robifoxx.block.Utils;
import me.robifoxx.block.mysql.SQLPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BlockstatsCommand implements CommandExecutor {
    private Main plugin;
    public BlockstatsCommand(Main plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.hasPermission("blockquest.stats")) {
            sender.sendMessage(plugin.getConfig().getString("no-permission").replace("&", "§"));
            return true;
        }
                int currentBlocks = plugin.getConfig().getStringList("blocks").size();
                if(args.length >= 2) {
                   /* String argReq = Utils.getIdentifierFromUsername(args[1]);
                    if((!plugin.useMysql && plugin.data.getConfig().getString("data." + argReq + ".x") != null)
                            || ( plugin.useMysql && SQLPlayer.playerExists(argReq))) {
                        foundBlocks = plugin.data.getConfig().getString("data." + argReq + ".x").split(";").length - 1;
                    } else {
                        Utils.sendMessageFromMSGS(sender, plugin.msgs.getConfig().getString("stats-unknown-player").replace("%target%", args[1]));
                        return true;
                    }*/
                    Utils.sendMessageFromMSGS(sender, plugin.msgs.getConfig().getString("personal-stats").replace("%target%", args[1])
                            .replace("%currentBlocks%", "" + currentBlocks)
                            .replace("%percent%", "" + BlockQuestAPI.getInstance().getFoundPercent(args[1], 2))
                            .replace("%foundBlocks%", "" + BlockQuestAPI.getInstance().getFoundBlocks(args[1])));
                } else {
                    int foundAllBlocks = 0;
                    if (!plugin.useMysql) {
                        for (String s : plugin.data.getConfig().getConfigurationSection("data").getKeys(false)) {
                            if (!s.equalsIgnoreCase("1-1-1-1-1-1")) {
                                int foundBlocks = plugin.data.getConfig().getString("data." + s + ".x").split(";").length - 1;
                                if (foundBlocks >= currentBlocks) {
                                    foundAllBlocks++;
                                }
                            }
                        }
                    } else {
                        for (String s : SQLPlayer.getAll()) {
                            int foundBlocks = SQLPlayer.getString(s, "X").split(";").length - 1;
                            if (foundBlocks >= currentBlocks) {
                                foundAllBlocks++;
                            }
                        }
                    }
                    Utils.sendMessageFromMSGS(sender, plugin.msgs.getConfig().getString("global-stats")
                            .replace("%currentBlocks%", "" + currentBlocks)
                            .replace("%percent%", BlockQuestAPI.getInstance().getFoundPercent(2) + "")
                            .replace("%foundAllBlocks%", "" + foundAllBlocks));
                }

        return true;
    }

}
