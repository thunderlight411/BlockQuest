package me.robifoxx.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Utils {
    public static boolean useUUID = true;
    
    public static String getIdentifier(Player p) {
        if(useUUID) {
            return p.getUniqueId().toString();
        } else {
            return p.getName();
        }
    }
    
    public static String getIdentifierFromUsername(String p) {
        if(!useUUID) {
            return p;
        } else {
            return Bukkit.getOfflinePlayer(p).getUniqueId().toString();
        }
    }
    
    public static String getUsername(String uuid) {
        return Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
    }
    
    
    public static void hideFoundBlocks(Player p) {
        // add task for the next tick
    	Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getProvidingPlugin(Main.class), () -> {
    		// check if there's a entity in the hashmap based 
    		if(Main.getPlugin(Main.class).blocksss.get(p.getName()) != null
    				// check if there's a block type to replace the skulls with
                    && Main.getPlugin(Main.class).hideFoundBlocks != null) {
                // load blocks: [string list] from config.yml ?????? but there's no blocks: entry in config file ??? no-op ???
                for (String s : Main.getProvidingPlugin(Main.class).getConfig().getStringList("blocks")) {
                    if(Main.getPlugin(Main.class).blocksss.get(p.getName()).contains(s)) {
                        String[] splt = s.split(";");
                        Location loc = new Location(Bukkit.getWorld(splt[3]), Integer.valueOf(splt[0]), Integer.valueOf(splt[1]), Integer.valueOf(splt[2]));
                        p.sendBlockChange(loc, Main.getPlugin(Main.class).hideFoundBlocks, (byte) 0);
                    }
                }
            }
        }, 1);
    }
    
    public static int getEmptyInventorySpaces(Player p) {
        int empty = 0;
        for(int i = 0; i < p.getInventory().getSize(); i++) {
            if(p.getInventory().getItem(i) == null
                    || p.getInventory().getItem(i).getType() == Material.AIR) {
                empty++;
            }
        }
        return empty;
    }
    
    // send a message stored in the config
    public static void sendMessageFromMSGS(CommandSender sender, String message) {
        sender.sendMessage(message.replace("&", "§").replace("%player%", sender.getName()).split("\\\\n"));
    }
}
