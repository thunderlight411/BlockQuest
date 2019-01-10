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

public class BlockQuestCommand implements CommandExecutor {
    
	private Main main;
    
    // get reference to main class
    public BlockQuestCommand(Main main) {
        this.main = main;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
    	// ================================ Permission Check (should be checked by spigot) =============================== //
    	if(!sender.hasPermission("blockquest.command")) {
            sender.sendMessage(main.getConfig().getString("no-permission").replace("&", "§"));
            return true;
        }
        // ========================================================== "blockquest" command (no args) ========================================================== //
    	if(args.length < 1) {
    		// add player to edit mode
            if (main.inEdit.remove(sender.getName())) {
                sender.sendMessage("§cYou disabled edit mode.");
            } else {
            	// SPIT OUT A BIG FUCKING MESSAGE
                sender.sendMessage("§7§m----------------------------------------");
                sender.sendMessage("§aYou entered edit mode!");
                sender.sendMessage("§aClick on blocks to add it to the config file!");
                sender.sendMessage("§aType §6/blockquest §ato exit edit mode.");
                sender.sendMessage("§7§m----------------------------------------");
                sender.sendMessage("§a§lType §6§l/blockquest reload §a§lto reload the config!");
                sender.sendMessage("§a§lType §6§l/blockquest stats [player] §a§lto check stats!");
                sender.sendMessage("§a§lType §6§l/blockquest save §a§lto save stats!");
                sender.sendMessage("§7§m----------------------------------------");
                // if skull hunt is disabled
                if(!main.enabled) {
                    sender.sendMessage("§c§lBlocks are disabled. Players cant find them until you enable it with §6§l/blockquest toggle");
                }
                // add player to edit mode
                main.inEdit.add(sender.getName());
            }
        // ========================================================== "blockquest" command with arguments ========================================================== //
        } else {
        	// ============================================= "reload" argument ============================================= //
            if(args[0].equalsIgnoreCase("reload")) {
                main.reloadConfig();
                Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("config-reloaded"));
            // ============================================= "toggle" argument ============================================= //
            } else if(args[0].equalsIgnoreCase("toggle")) {
                // toggle boolean
            	main.enabled = !main.enabled;
                // send msg to player
            	if(main.enabled) {
                    Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("enabled-blocks"));
                } else {
                    Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("disabled-blocks"));
                }
            	// save enabled/disable state to config file
                main.getConfig().set("enabled", main.enabled);
                main.saveConfig();
            // ============================================= "save" argument ============================================= //  
            } else if(args[0].equalsIgnoreCase("save")) {
                int amount = 0;
                // for each player online
                for(Player pl : Bukkit.getOnlinePlayers()) { 
                    if (main.saved_x.get(pl.getName()) != null) {
                        amount++;
                        Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("saving-data-for").replace("%target%", pl.getName()));
                        if (main.useMysql) {
                            SQLPlayer.setString(Utils.getIdentifier(pl), "X", main.saved_x.get(pl.getName()));
                            SQLPlayer.setString(Utils.getIdentifier(pl), "Y", main.saved_y.get(pl.getName()));
                            SQLPlayer.setString(Utils.getIdentifier(pl), "Z", main.saved_z.get(pl.getName()));
                            SQLPlayer.setString(Utils.getIdentifier(pl), "WORLD", main.saved_world.get(pl.getName()));
                        } else {
                            main.data.getConfig().set("data." + Utils.getIdentifier(pl) + ".x", main.saved_x.get(pl.getName()));
                            main.data.getConfig().set("data." + Utils.getIdentifier(pl) + ".y", main.saved_y.get(pl.getName()));
                            main.data.getConfig().set("data." + Utils.getIdentifier(pl) + ".z", main.saved_z.get(pl.getName()));
                            main.data.getConfig().set("data." + Utils.getIdentifier(pl) + ".world", main.saved_world.get(pl.getName()));
                            main.data.saveConfig();
                        }
                    }
                }
                // send msg to player
                Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("finished-saving").replace("%amount%", "" + amount));
             // ============================================= "stats" argument ============================================= //  
            } else if(args[0].equalsIgnoreCase("stats")) {
                int currentBlocks = main.getConfig().getStringList("blocks").size();
                // ============================================= [player string] second argument =============================================
                if(args.length >= 2) {
                	// send msg to player
                    Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("personal-stats").replace("%target%", args[1])
                            .replace("%currentBlocks%", "" + currentBlocks)
                            .replace("%percent%", "" + BlockQuestAPI.getInstance().getFoundPercent(args[1], 2))
                            .replace("%foundBlocks%", "" + BlockQuestAPI.getInstance().getFoundBlocks(args[1])));
                // ============================================= single argument code =============================================
                } else {
                    int foundAllBlocks = 0;
                    if (!main.useMysql) {
                        for (String s : main.data.getConfig().getConfigurationSection("data").getKeys(false)) {
                            if (!s.equalsIgnoreCase("1-1-1-1-1-1")) {
                                int foundBlocks = main.data.getConfig().getString("data." + s + ".x").split(";").length - 1;
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
                 // send msg to player
                    Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("global-stats")
                            .replace("%currentBlocks%", "" + currentBlocks)
                            .replace("%percent%", BlockQuestAPI.getInstance().getFoundPercent(2) + "")
                            .replace("%foundAllBlocks%", "" + foundAllBlocks));
                }
            // ============================================= UNUSED wipedata argument ============================================= //  
            } /*else if(args[0].equalsIgnoreCase("wipedata")) {
                    sender.sendMessage("§aWiping data...");
                    boolean success = false;
                    if(useMysql) {
                        mysql.update("DROP TABLE BlockQuest");
                        createMySQL();
                        success = true;
                    } else {

                        Config c = new Config("plugins/BlockQuest", "data.yml");
                        c.create();
                        if(c.toFile().delete()) {
                            c.setDefault("data.yml");
                            c.getConfig().options().copyDefaults(true);
                            c.saveConfig();

                            data = c;
                            success = true;
                        }
                    }
                    if(success) {
                        sender.sendMessage("§aData Wiped successfully!");
                    } else {
                        sender.sendMessage("§cData wipe failed! :(");
                    }
                }*/
        }
        return true;
    }
}
