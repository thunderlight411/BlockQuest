package me.robifoxx.block.command;

import me.robifoxx.block.BlockQuestAPI;
import me.robifoxx.block.Main;
import me.robifoxx.block.Utils;
import me.robifoxx.block.mysql.SQLPlayer;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BlockQuestCommand implements CommandExecutor {
    
	private Main main;
    
    public BlockQuestCommand(Main main) {
        this.main = main;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	if(args.length < 1) {
    		noArgumentsCommand(sender);
        } else {
        	if(args[0].equalsIgnoreCase("edit")) {
        		editModeToggleCommand(sender);
        	} else if(args[0].equalsIgnoreCase("reload")) {
            	reloadCommand(sender);
            } else if(args[0].equalsIgnoreCase("toggle")) {
            	toggleCommand(sender); 
            } else if(args[0].equalsIgnoreCase("save")) {
            	saveCommand(sender);  
            } else if(args[0].equalsIgnoreCase("stats")) {
            	final String secondArgument = (args.length >= 2) ? args[1] : null;
            	BlockQuestCommand.statsCommand(main, sender, secondArgument);
            }
        }
        return true;
    }
    
    private void noArgumentsCommand(CommandSender sender) {
        //sender.sendMessage("§7§m----------------------------------------");
        //sender.sendMessage("§aYou entered edit mode!");
        //sender.sendMessage("§aClick on blocks to add it to the config file!");
        //sender.sendMessage("§aType §6/blockquest §ato exit edit mode.");
        sender.sendMessage("§7----------BlockQuest Help----------------");
        sender.sendMessage("§a§lType §6§l/blockquest edit §a§lto enter/leave edit mode!");
        sender.sendMessage("§a§lType §6§l/blockquest reload §a§lto reload the config!");
        sender.sendMessage("§a§lType §6§l/blockquest toggle §a§lto enable/disable block hunting!");
        sender.sendMessage("§a§lType §6§l/blockquest stats [player] §a§lto check stats!");
        sender.sendMessage("§a§lType §6§l/blockquest save §a§lto save stats!");
        sender.sendMessage("§7----------------------------------------");
        if(!main.enabled) {
            sender.sendMessage("§c§lBlocks are disabled. Players cant find them until you enable it with §6§l/blockquest toggle");
        }
    }
    
    private void editModeToggleCommand(CommandSender sender) {
    	if(sender instanceof Player) {
    		final UUID playerId = ((Player)sender).getUniqueId();
    		if (main.inEdit.contains(playerId)) {
    			main.inEdit.remove(playerId);
    			sender.sendMessage("§cYou disabled edit mode.");
                sender.sendMessage("§7----------Edit mode disabled----------------");
                sender.sendMessage("§a§lIt's no longer possible to add blocks");
                sender.sendMessage("§a§lType §6§l/blockquest edit §a§lto enable edit mode again");
                sender.sendMessage("§7----------------------------------------");
    		} else {
    			main.inEdit.add(playerId);
    			sender.sendMessage("§aYou enabled edit mode.");
                sender.sendMessage("§7----------Edit mode Enabled----------------");
                sender.sendMessage("§a§lClick on skulls you want to add to the hunt!");
                sender.sendMessage("§a§lIf its already in the config it will be removed!");
                sender.sendMessage("§a§lType §6§l/blockquest edit §a§lto exit Edit mode");
                sender.sendMessage("§7----------------------------------------");
    		}
    	} else {
    		sender.sendMessage("§c§mCan't execute this command as console!");
    	}
    }
    
    private void reloadCommand(CommandSender sender) {
        main.reloadConfig();
        Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("config-reloaded"));
    }
    
    private void toggleCommand(CommandSender sender) {
    	main.enabled = !main.enabled;
    	if(main.enabled) {
            Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("enabled-blocks"));
        } else {
            Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("disabled-blocks"));
        }
    	// save enabled/disable state to config file
        main.getConfig().set("enabled", main.enabled);
        main.saveConfig();
    }
    
    private void saveCommand(CommandSender sender) {
        int amount = 0;
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
        Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("finished-saving").replace("%amount%", "" + amount));
    }
    
    public static void statsCommand(Main main, CommandSender sender, String secondArgStr) {
    	int currentBlocks = main.getConfig().getStringList("blocks").size();
    	if(secondArgStr != null) {
            Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("personal-stats").replace("%target%", secondArgStr)
                    .replace("%currentBlocks%", "" + currentBlocks)
                    .replace("%percent%", "" + BlockQuestAPI.getInstance().getFoundPercent(secondArgStr, 2))
                    .replace("%foundBlocks%", "" + BlockQuestAPI.getInstance().getFoundBlocks(secondArgStr)));
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
            Utils.sendMessageFromMSGS(sender, main.msgs.getConfig().getString("global-stats")
                    .replace("%currentBlocks%", "" + currentBlocks)
                    .replace("%percent%", BlockQuestAPI.getInstance().getFoundPercent(2) + "")
                    .replace("%foundAllBlocks%", "" + foundAllBlocks));
        }
    }
}
    // ============================================= UNUSED wipedata argument ============================================= //  
    /*else if(args[0].equalsIgnoreCase("wipedata")) {
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
