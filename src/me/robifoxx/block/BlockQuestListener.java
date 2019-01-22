package me.robifoxx.block;

import me.robifoxx.block.api.FindEffect;
import me.robifoxx.block.events.BlockFindEvent;
import me.robifoxx.block.mysql.SQLPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

public class BlockQuestListener implements Listener {
    private Main main;

    // get reference to main class
    public BlockQuestListener(Main main) {
        this.main = main;
    }

    // ======================================================================================= join ======================================================================================= //
    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (!main.useMysql) {
            if (main.data.getConfig().get("data." + Utils.getIdentifier(e.getPlayer()) + ".x") == null) {
                main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".x", "none");
                main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".y", "none");
                main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".z", "none");
                main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".world", "none");
                main.data.saveConfig();
            }
        } else {
            if (!SQLPlayer.playerExists(Utils.getIdentifier(e.getPlayer())))
                SQLPlayer.createPlayer(e.getPlayer(), "none", "none", "none", "none");
        }
        if (main.blocksss.get(e.getPlayer().getName()) == null) {
            String x;
            String y;
            String z;
            String world;
            if (main.saved_x.get(e.getPlayer().getName()) != null) {
                x = main.saved_x.get(e.getPlayer().getName());
                y = main.saved_z.get(e.getPlayer().getName());
                z = main.saved_y.get(e.getPlayer().getName());
                world = main.saved_world.get(e.getPlayer().getName());
            } else {
                if (main.useMysql) {
                    x = SQLPlayer.getString(Utils.getIdentifier(e.getPlayer()), "X");
                    y = SQLPlayer.getString(Utils.getIdentifier(e.getPlayer()), "Y");
                    z = SQLPlayer.getString(Utils.getIdentifier(e.getPlayer()), "Z");
                    world = SQLPlayer.getString(Utils.getIdentifier(e.getPlayer()), "WORLD");
                } else {
                    x = main.data.getConfig().getString("data." + Utils.getIdentifier(e.getPlayer()) + ".x");
                    y = main.data.getConfig().getString("data." + Utils.getIdentifier(e.getPlayer()) + ".y");
                    z = main.data.getConfig().getString("data." + Utils.getIdentifier(e.getPlayer()) + ".z");
                    world = main.data.getConfig().getString("data." + Utils.getIdentifier(e.getPlayer()) + ".world");
                }
            }
            main.saved_x.put(e.getPlayer().getName(), x);
            main.saved_y.put(e.getPlayer().getName(), y);
            main.saved_z.put(e.getPlayer().getName(), z);
            main.saved_world.put(e.getPlayer().getName(), world);

            String[] x_splt = x.split(";");
            String[] y_splt = y.split(";");
            String[] z_splt = z.split(";");
            String[] world_splt = world.split(";");

            int loc = 0;
            List<String> lst = new ArrayList<>();
            for (String s : x_splt) {
                if (!s.equalsIgnoreCase("none")) {
                    lst.add(x_splt[loc] + ";" + y_splt[loc] + ";" + z_splt[loc] + ";" + world_splt[loc]);
                }
                loc++;
            }
            main.blocksss.put(e.getPlayer().getName(), lst);
        }
        Utils.hideFoundBlocks(e.getPlayer());
    }

    // ======================================================================================= leave ======================================================================================= //
    @EventHandler
    public void leave(PlayerQuitEvent e) {
        if (main.useMysql) {
            SQLPlayer.setString(Utils.getIdentifier(e.getPlayer()), "X", main.saved_x.get(e.getPlayer().getName()));
            SQLPlayer.setString(Utils.getIdentifier(e.getPlayer()), "Y", main.saved_y.get(e.getPlayer().getName()));
            SQLPlayer.setString(Utils.getIdentifier(e.getPlayer()), "Z", main.saved_z.get(e.getPlayer().getName()));
            SQLPlayer.setString(Utils.getIdentifier(e.getPlayer()), "WORLD", main.saved_world.get(e.getPlayer().getName()));
        } else {
            main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".x", main.saved_x.get(e.getPlayer().getName()));
            main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".y", main.saved_y.get(e.getPlayer().getName()));
            main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".z", main.saved_z.get(e.getPlayer().getName()));
            main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".world", main.saved_world.get(e.getPlayer().getName()));
            main.data.saveConfig();
        }
        main.saved_x.remove(e.getPlayer().getName());
        main.saved_y.remove(e.getPlayer().getName());
        main.saved_z.remove(e.getPlayer().getName());
        main.saved_world.remove(e.getPlayer().getName());
        main.blocksss.remove(e.getPlayer().getName());
    }

    // ======================================================================================= click ======================================================================================= //
    @EventHandler
    public void click(PlayerInteractEvent e) {
        // check if previous event for target player has finished
        if (main.pendingEvents.contains(e.getPlayer().getUniqueId())) {
            return;
        }
        // add player to pending event list
        main.pendingEvents.add(e.getPlayer().getUniqueId());
        // schedule pending remove task next tick
        Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
            main.pendingEvents.remove(e.getPlayer().getUniqueId());
        }, 1);
        // =========================== if action is right click =========================== //
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // schedule task next tick
            Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                // ??????????????
                Utils.hideFoundBlocks(e.getPlayer());
            });
            // convert block location data to readable string format X;Y;Z;worldname
            final String clickedBlock = BlockQuestAPI.getInstance().convertLocToString(e.getClickedBlock().getLocation());
            // =============================== check if player is in edit mode =============================== //
            if (main.inEdit.contains(e.getPlayer().getUniqueId())) {
                // check if target block is a skull
                if(e.getClickedBlock().getType() == Material.PLAYER_HEAD || e.getClickedBlock().getType() == Material.PLAYER_WALL_HEAD) {
                    // check if location is already added to the hunt list if so remove it
                    if (BlockQuestAPI.getInstance().removeLocation(e.getClickedBlock().getLocation())) {
                        e.getPlayer().sendMessage("§cRemoved this block!");
                    } else {
                        // not added, add location to auxiliary storage
                        BlockQuestAPI.getInstance().addLocation(e.getClickedBlock().getLocation());
                        e.getPlayer().sendMessage("§aAdded this block!");
                    }
                // block is not a skull
                } else {
                    e.getPlayer().sendMessage("§cThis is not a player head!");
                }
                // =============================== player is not in edit mode =============================== //
            } else {
                // check clicked block for hunting block type
                if (main.getConfig().getStringList("blocks").contains(clickedBlock)) {
                    // ============ check if block wasn't previously found ============ //
                    if (main.blocksss.get(e.getPlayer().getName()) == null
                            || !main.blocksss.get(e.getPlayer().getName()).contains(clickedBlock)) {
                        // check if block quest hunting is disabled
                        if (!main.enabled) {
                            // end method with msg
                            e.getPlayer().sendMessage(main.disabledMsg.replace("&", "§"));
                            return;
                        }
                        // write found block to hash tables
                        main.saved_x.put(e.getPlayer().getName(), main.saved_x.get(e.getPlayer().getName()) + ";" + e.getClickedBlock().getLocation().getBlockX());
                        main.saved_y.put(e.getPlayer().getName(), main.saved_y.get(e.getPlayer().getName()) + ";" + e.getClickedBlock().getLocation().getBlockY());
                        main.saved_z.put(e.getPlayer().getName(), main.saved_z.get(e.getPlayer().getName()) + ";" + e.getClickedBlock().getLocation().getBlockZ());
                        main.saved_world.put(e.getPlayer().getName(), main.saved_world.get(e.getPlayer().getName()) + ";" + e.getClickedBlock().getLocation().getWorld().getName());
                        // create new arraylist for blocksss if none is available
                        if (main.blocksss.get(e.getPlayer().getName()) == null) {
                            List<String> lst = new ArrayList<>();
                            lst.add(clickedBlock);
                            // write block to blocksss
                            main.blocksss.put(e.getPlayer().getName(), lst);
                        } else {
                            // write block to blocksss
                            main.blocksss.get(e.getPlayer().getName()).add(clickedBlock);
                        }
                        // if using useMysql write block to database
                        if (main.useMysql) {
                            if (!main.unsafeSave) {
                                // write saved_x/y/z/world data to database
                                SQLPlayer.setString(Utils.getIdentifier(e.getPlayer()), "X", main.saved_x.get(e.getPlayer().getName()));
                                SQLPlayer.setString(Utils.getIdentifier(e.getPlayer()), "Y", main.saved_y.get(e.getPlayer().getName()));
                                SQLPlayer.setString(Utils.getIdentifier(e.getPlayer()), "Z", main.saved_z.get(e.getPlayer().getName()));
                                SQLPlayer.setString(Utils.getIdentifier(e.getPlayer()), "WORLD", main.saved_world.get(e.getPlayer().getName()));
                            }
                        // write block to flat file if not using useMysql
                        } else {
                            // write saved_x/y/z/world data to file
                            main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".x", main.saved_x.get(e.getPlayer().getName()));
                            main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".y", main.saved_y.get(e.getPlayer().getName()));
                            main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".z", main.saved_z.get(e.getPlayer().getName()));
                            main.data.getConfig().set("data." + Utils.getIdentifier(e.getPlayer()) + ".world", main.saved_world.get(e.getPlayer().getName()));
                            main.data.saveConfig();
                        }
                        // get the amount of blocks not found
                        int blocksLeft = main.getConfig().getStringList("blocks").size() - main.blocksss.get(e.getPlayer().getName()).size();
                        // stores true if all block are found, false otherwise
                        boolean foundAllBlocks = main.blocksss.get(e.getPlayer().getName()).size() >= main.getConfig().getStringList("blocks").size();
                        // if not enough free space is available and all blocks are found then (main.checkFullInventory default value = 0)
                        if (Utils.getEmptyInventorySpaces(e.getPlayer()) < main.checkFullInventory && foundAllBlocks) {
                            // remove block found info from blocksss hash map, ??? but not from saved_x/y/z/world that's also flushed to disk ???
                            main.blocksss.get(e.getPlayer().getName()).remove(clickedBlock);
                            // cancel block find with error msg
                            e.getPlayer().sendMessage(main.fullInventoryMsg.replace("&", "§"));
                            return;
                        }
                        // ???? effect crap ???? deprecated ????
                        BlockFindEvent evnt = new BlockFindEvent(e.getPlayer(), e.getClickedBlock(), main.findEffectC);
                        Bukkit.getPluginManager().callEvent(evnt);
                        if (evnt.isCancelled()) {
                            // cancel block find
                            main.blocksss.get(e.getPlayer().getName()).remove(clickedBlock);
                            return;
                        }
                        // Play the awesome Chantal flies through the air effect crap!
                        playFindEffect(e.getClickedBlock().getLocation().clone().add(0.5, 0, 0.5), evnt.getEffect());
                        // Execute player found block commands
                        for (String s : main.getConfig().getStringList("find-block-commands")) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replace("%player%", e.getPlayer().getName())
                                    .replace("%pLocX%", "" + e.getPlayer().getLocation().getX())
                                    .replace("%pLocY%", "" + e.getPlayer().getLocation().getY())
                                    .replace("%pLocZ%", "" + e.getPlayer().getLocation().getZ())
                                    .replace("%locX5%", "" + (e.getClickedBlock().getLocation().getX() + 0.5))
                                    .replace("%locY5%", "" + (e.getClickedBlock().getLocation().getY() + 0.5))
                                    .replace("%locZ5%", "" + (e.getClickedBlock().getLocation().getZ() + 0.5))
                                    .replace("%locX%", "" + e.getClickedBlock().getLocation().getX())
                                    .replace("%locY%", "" + e.getClickedBlock().getLocation().getY())
                                    .replace("%locZ%", "" + e.getClickedBlock().getLocation().getZ())
                                    .replace("%blockLeft%", "" + blocksLeft)
                                    .replace("%blocksLeft%", "" + blocksLeft));
                        }
                        // Execute player found all blocks commands if foundAllBlocks is true
                        if (foundAllBlocks) {
                            // Execute player already found all blocks commands
                            for (String s : main.getConfig().getStringList("all-blocks-found-commands")) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replace("%player%", e.getPlayer().getName())
                                        .replace("%pLocX%", "" + e.getPlayer().getLocation().getX())
                                        .replace("%pLocY%", "" + e.getPlayer().getLocation().getY())
                                        .replace("%pLocZ%", "" + e.getPlayer().getLocation().getZ())
                                        .replace("%locX5%", "" + (e.getClickedBlock().getLocation().getX() + 0.5))
                                        .replace("%locY5%", "" + (e.getClickedBlock().getLocation().getY() + 0.5))
                                        .replace("%locZ5%", "" + (e.getClickedBlock().getLocation().getZ() + 0.5))
                                        .replace("%locX%", "" + e.getClickedBlock().getLocation().getX())
                                        .replace("%locY%", "" + e.getClickedBlock().getLocation().getY())
                                        .replace("%locZ%", "" + e.getClickedBlock().getLocation().getZ())
                                        .replace("%blockLeft%", "" + blocksLeft)
                                        .replace("%blocksLeft%", "" + blocksLeft));
                            }
                        }
                        // ============ block is already found ============ //
                    } else {
                        if (main.blocksss.get(e.getPlayer().getName()).contains(clickedBlock)) {
                            int blocksLeft = main.getConfig().getStringList("blocks").size() - main.blocksss.get(e.getPlayer().getName()).size();
                            if (blocksLeft <= 0) {
                                // Execute player already found all blocks commands
                                for (String s : main.getConfig().getStringList("already-found-all-blocks")) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replace("%player%", e.getPlayer().getName())
                                            .replace("%pLocX%", "" + e.getPlayer().getLocation().getX())
                                            .replace("%pLocY%", "" + e.getPlayer().getLocation().getY())
                                            .replace("%pLocZ%", "" + e.getPlayer().getLocation().getZ())
                                            .replace("%locX5%", "" + (e.getClickedBlock().getLocation().getX() + 0.5))
                                            .replace("%locY5%", "" + (e.getClickedBlock().getLocation().getY() + 0.5))
                                            .replace("%locZ5%", "" + (e.getClickedBlock().getLocation().getZ() + 0.5))
                                            .replace("%locX%", "" + e.getClickedBlock().getLocation().getX())
                                            .replace("%locY%", "" + e.getClickedBlock().getLocation().getY())
                                            .replace("%locZ%", "" + e.getClickedBlock().getLocation().getZ())
                                            .replace("%blockLeft%", "" + blocksLeft)
                                            .replace("%blocksLeft%", "" + blocksLeft));
                                }
                            } else {
                                // Execute player already found block commands
                                for (String s : main.getConfig().getStringList("already-found-commands")) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replace("%player%", e.getPlayer().getName())
                                            .replace("%pLocX%", "" + e.getPlayer().getLocation().getX())
                                            .replace("%pLocY%", "" + e.getPlayer().getLocation().getY())
                                            .replace("%pLocZ%", "" + e.getPlayer().getLocation().getZ())
                                            .replace("%locX5%", "" + (e.getClickedBlock().getLocation().getX() + 0.5))
                                            .replace("%locY5%", "" + (e.getClickedBlock().getLocation().getY() + 0.5))
                                            .replace("%locZ5%", "" + (e.getClickedBlock().getLocation().getZ() + 0.5))
                                            .replace("%locX%", "" + e.getClickedBlock().getLocation().getX())
                                            .replace("%locY%", "" + e.getClickedBlock().getLocation().getY())
                                            .replace("%locZ%", "" + e.getClickedBlock().getLocation().getZ())
                                            .replace("%blockLeft%", "" + blocksLeft)
                                            .replace("%blocksLeft%", "" + blocksLeft));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ======================================================================================= dmg ======================================================================================= //
    @EventHandler
    public void dmg(EntityDamageEvent e) {
        if (e.getEntity().getCustomName() != null
                && e.getEntity().getCustomName().startsWith("§b§l§o§c§k")) {
            e.setCancelled(true);
        }
    }

    // ======================================================================================= rl ======================================================================================= //
    @EventHandler
    public void rl(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().equalsIgnoreCase("/rl")
                || e.getMessage().equalsIgnoreCase("/reload")) {
            if (e.getPlayer().isOp()) {
                e.getPlayer().sendMessage("§c[§2B§alockQuest§c] DO NOT reload!");
                e.getPlayer().sendMessage("§c[§2B§alockQuest§c] Use restart instead, as reload messes up player stats.");
            }
        }
    }

    // ================================================================================== helper methods ================================================================================== //

    public void playFindEffect(Location l, FindEffect e) {
        if (!main.findEffect) {
            return;
        }
        double offset = 0.25;
        if (main.getConfig().get("find-effect.y-start") != null) {
            offset = main.getConfig().getDouble("find-effect.y-start");
        }
        /*String head = main.getConfig().getString("find-effect.head").equalsIgnoreCase("NONE") ? null : main.getConfig().getString("find-effect.head");
        String chest = main.getConfig().getString("find-effect.chest").equalsIgnoreCase("NONE") ? null : main.getConfig().getString("find-effect.chest");
        String leg = main.getConfig().getString("find-effect.leg").equalsIgnoreCase("NONE") ? null : main.getConfig().getString("find-effect.leg");
        String boot = main.getConfig().getString("find-effect.boot").equalsIgnoreCase("NONE") ? null : main.getConfig().getString("find-effect.boot");*/
        ArmorStand a = e.getArmorStand(l.clone().add(0, offset, 0));
        //a.setInvulnerable(true); // 1.8 :(
        if (!main.getConfig().getString("find-effect.sound").equalsIgnoreCase("DISABLED")
                || !main.getConfig().getString("find-effect.sound").equalsIgnoreCase("NONE")) {
            a.getWorld().playSound(a.getLocation(), Sound.valueOf(main.getConfig().getString("find-effect.sound")), 1, main.getConfig().getInt("find-effect.sound-pitch"));
        }
        for (int i = 0; i < main.getConfig().getInt("find-effect.loop"); i++) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                Location newLoc = a.getLocation().clone();
                newLoc.add(0.0, main.getConfig().getDouble("find-effect.levitation-per-loop"), 0.0);
                newLoc.setYaw(a.getLocation().getYaw() + main.getConfig().getInt("find-effect.yaw-rotation"));
                a.teleport(newLoc);
                String particle = main.getConfig().getString("find-effect.particle");
                if (!particle.equalsIgnoreCase("DISABLED")) {
                    //ParticleEffect.valueOf(particle).display(0, 0, 0, 0, 1, a.getLocation(), 16);
                }
            }, i * main.getConfig().getInt("find-effect.scheduler"));
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
            if (main.getConfig().get("find-effect.disappear-commands.enabled") != null
                    && main.getConfig().getBoolean("find-effect.disappear-commands.enabled")) {
                for (String s : main.getConfig().getStringList("find-effect.disappear-commands.commands")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replace("%locX%", a.getLocation().getX() + "").replace("%locY%", a.getLocation().getY() + "").replace("%locZ%", a.getLocation().getZ() + ""));
                }
            }
            a.remove();
        }, main.getConfig().getInt("find-effect.loop") * main.getConfig().getInt("find-effect.scheduler"));
    }
}
