package me.robifoxx.block;

import com.darkblade12.particleeffect.ParticleEffect;
import me.robifoxx.block.api.Config;
import me.robifoxx.block.api.FindEffect;
import me.robifoxx.block.api.Skulls;
import me.robifoxx.block.command.BlockQuestCommand;
import me.robifoxx.block.command.BlockStatsCommand;
import me.robifoxx.block.command.BlockQuestTab;
import me.robifoxx.block.mysql.MySQL;
import me.robifoxx.block.mysql.SQLPlayer;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by RobiFoxx.
 * All rights reserved.
 */
public class Main extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();

// ======================================================================== global data ======================================================================== //
	// MySQL object reference
    public MySQL mysql;
    // stores players in editing mode
    public ArrayList<UUID> inEdit = new ArrayList<>();
    // block data
    public HashMap<String, List<String>> blocksss = new HashMap<>();
    public HashMap<String, String> saved_x = new HashMap<>();
    public HashMap<String, String> saved_y = new HashMap<>();
    public HashMap<String, String> saved_z = new HashMap<>();
    public HashMap<String, String> saved_world = new HashMap<>();
    // config references
    public Config data;
    public Config msgs;
    // stores if database is used
    public boolean useMysql = false;
    // stores if Mysql unsafe saves is used
    public boolean unsafeSave = true;
    // stores for which players the click event is running
    public ArrayList<UUID> pendingEvents = new ArrayList<>();
    // stores if Effects are used
    public boolean findEffect = false;
    // blockquest enabled state (toggled by toggle command)
    public boolean enabled = false;
    // stores the amount of free inv slots required for a reward
    public int checkFullInventory = 0;
    // config msgs
    public String disabledMsg = "&cBlocks aren't enabled yet!";
    public String fullInventoryMsg = "&c&lYour inventory is full!";
    // block used to replace found blocks
    public Material hideFoundBlocks = Material.AIR;
    // stores effect data
    public FindEffect findEffectC;
    // stores reward info
    public FoundReward[] blockFoundRewards = null;
    // reference to Main singleton class instance
    private static Main plugin;

    // ================================================================== LoadRewardConfigInfo ====================================================================== //

    // the new reward-names: & rewards: yml config loader
    public void LoadRewardConfigInfo() {
        List<String> rewardNames = getConfig().getStringList("reward-names");
        blockFoundRewards = new FoundReward[rewardNames.size()];
        for(int i = 0; i < rewardNames.size(); i++) {
            String rewardName = rewardNames.get(i);
            int blockGoal = getConfig().getInt("rewards." + rewardName + ".block-goal");
            List<String> commands = getConfig().getStringList("rewards." + rewardName + ".commands");
            blockFoundRewards[i] = new FoundReward(blockGoal, commands);
        }
    }

// ======================================================================== on plugin enable ======================================================================== //
    public void onEnable() {
        // get plugin folder name
        String fileName = this.getDescription().getName();
        // copy default config if it doesn't exist. getConfig() = reference to config.yml
/*        if(!(new File("plugins/" + fileName + "/config.yml").exists())) {
            getConfig().options().copyDefaults(true);
            saveConfig();
        }
*/
        config.options().copyDefaults(true);
        saveConfig();
        getServer().getPluginManager().registerEvents(this, this);


        // load rewards system
        LoadRewardConfigInfo();

        // reload config
        reloadConfig();

        {
            // open data.yml
            Config c = new Config("plugins/" + fileName, "data.yml", this);
            c.create();
            // load default data.yml if it doesn't exist.
            c.setDefault("data.yml");
            c.getConfig().options().copyDefaults(true);
            c.saveConfig();
            // pass current config reference to global variable data
            data = c;
        }
        {
            // open messages.yml
            Config c = new Config("plugins/" + fileName, "messages.yml", this);
            c.create();
            // load default messages.yml if it doesn't exist.
            c.setDefault("messages.yml");
            c.getConfig().options().copyDefaults(true);
            c.saveConfig();
            // pass current config reference to global variable msgs
            msgs = c;
        }

        // if use-mysql: equals true in config.yml
        if(config.getString("use-mysql").equalsIgnoreCase("true")) {
            // create mysql object
            mysql = new MySQL(config.getString("mysql-host"), config.getString("mysql-database"), config.getString("mysql-username"), config.getString("mysql-password"));
            // call to helper function
            createMySQL();
            useMysql = true;
        }

        // create and bind BlockQuestListener instance to bukkit
        Bukkit.getPluginManager().registerEvents(new BlockQuestListener(this), this);

        // load mysql-unsafe-save from config.yml
        if(config.getString("mysql-unsafe-save") != null) {
            if(config.getString("mysql-unsafe-save").equalsIgnoreCase("false")) {
                unsafeSave = false;
            }
        }

        // if already-found-all-blocks: in config.yml is null set default value.
        if(config.getStringList("already-found-all-blocks") == null) {
            config.set("already-found-all-blocks", new ArrayList<String>().add("msg %player% You already found all blocks!"));
        }

        // load plugin enabled/disabled state + disabled msg from config.yml
        if(config.get("enabled") != null) {
            enabled = config.getBoolean("enabled");
            if(config.get("disabled-msg") != null) {
                disabledMsg = config.getString("disabled-msg");
            }
        }

        // load useUUID value from config.yml
        if(config.get("use-uuid") != null) {
            Utils.useUUID = config.getBoolean("use-uuid");
        }

        // load the amount of free spaces required for a reward from config.yml
        if (config.get("check-full-inventory") != null) {
            checkFullInventory = config.getInt("check-full-inventory");
            if (config.get("full-inventory-msg") != null) {
                fullInventoryMsg = config.getString("full-inventory-msg");
            }
        }

        {
            // load hide block type based on string defined at hide-found-blocks: in config.yml
            String s = config.getString("hide-found-blocks");
            if (s != null) {
                if (s.equalsIgnoreCase("NONE")) {
                    hideFoundBlocks = null;
                } else {
                    hideFoundBlocks = Material.valueOf(s);
                }
            } else {
                hideFoundBlocks = null;
            }
        }

        // if using placeholderapi: true defined in config.yml
//        if(config.getString("placeholderapi") != null
//                && config.getString("placeholderapi").equalsIgnoreCase("true")) {
            if (config.getBoolean("placeholderapi")){
            if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new Placeholders(this).hook();
            } else {
                getLogger().warning("PlaceholderAPI not found, placeholders will not work.");
                getLogger().warning("Please install the following plugin:");
                getLogger().warning("https://www.spigotmc.org/resources/p.6245/");
            }
        }

        // load if effects are used
        findEffect = config.getBoolean("find-effect.enabled");
        boolean enabledParticle = config.getBoolean("particles.enabled");
        // load particle effect data
        if(enabledParticle) {
            int loop = config.getInt("particles.loop");
            String f_type;
            float f_dx;
            float f_dy;
            float f_dz;
            float f_speed;
            int f_quan;
            String nf_type;
            float nf_dx;
            float nf_dy;
            float nf_dz;
            float nf_speed;
            int nf_quan;
            {
                String f = "found";
                f_type =config.getString("particles." + f + ".type");
                f_dx = Float.valueOf(config.getDouble("particles." + f + ".dx") + "");
                f_dy = Float.valueOf(config.getDouble("particles." + f + ".dy") + "");
                f_dz = Float.valueOf(config.getDouble("particles." + f + ".dz") + "");
                f_speed = Float.valueOf(config.getDouble("particles." + f + ".speed") + "");
                f_quan = config.getInt("particles." + f + ".quantity");
            }
            {
                String f = "notfound";
                nf_type =config.getString("particles." + f + ".type");
                nf_dx = Float.valueOf(config.getDouble("particles." + f + ".dx") + "");
                nf_dy = Float.valueOf(config.getDouble("particles." + f + ".dy") + "");
                nf_dz = Float.valueOf(config.getDouble("particles." + f + ".dz") + "");
                nf_speed = Float.valueOf(config.getDouble("particles." + f + ".speed") + "");
                nf_quan = config.getInt("particles." + f + ".quantity");
            }
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                for(String s : config.getStringList("blocks")) {
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        for(Player pl : Bukkit.getOnlinePlayers()) {
                            //If it was a reload, then dont bother to proceed
                            if(blocksss.get(pl.getName()) != null) {
                                boolean found = blocksss.get(pl.getName()).contains(s);
                                String[] splt = s.split(";");
                                //x;y;z;w
                                Location loc = new Location(Bukkit.getWorld(splt[3]), Integer.valueOf(splt[0]) + 0.5, Integer.valueOf(splt[1]) + 0.25, Integer.valueOf(splt[2]) + 0.5);
                                if(found) {
                                    if(!f_type.equalsIgnoreCase("DISABLED")) {
                                        ParticleEffect.valueOf(f_type).display(
                                                f_dx,
                                                f_dy,
                                                f_dz,
                                                f_speed,
                                                f_quan,
                                                loc, pl);
                                    }
                                } else {
                                    if(!nf_type.equalsIgnoreCase("DISABLED")) {
                                        ParticleEffect.valueOf(nf_type).display(
                                                nf_dx,
                                                nf_dy,
                                                nf_dz,
                                                nf_speed,
                                                nf_quan,
                                                loc, pl);
                                    }
                                }
                            }
                        }
                    });
                }
            }, loop, loop);
        }


        // store this instance reference to static plugin variable
        plugin = this;

        // load effect data stored at find-effect: in config.yml
        {
            boolean visible = !config.getBoolean("find-effect.invisible");
            boolean small = config.getBoolean("find-effect.small");
            String head = config.getString("find-effect.head").equalsIgnoreCase("NONE") ? null : config.getString("find-effect.head");
            String chest = config.getString("find-effect.chest").equalsIgnoreCase("NONE") ? null : config.getString("find-effect.chest");
            String leg = config.getString("find-effect.leg").equalsIgnoreCase("NONE") ? null : config.getString("find-effect.leg");
            String boot = config.getString("find-effect.boot").equalsIgnoreCase("NONE") ? null : config.getString("find-effect.boot");
            ItemStack h = null;
            ItemStack c = null;
            ItemStack l = null;
            ItemStack b = null;
            if(head != null) {
                if(head.length() > 45) {
                    h = Skulls.createSkull(head);
                } else {
                    h = new ItemStack(Material.valueOf(head));
                }
            }
            if(chest != null) {
                c = new ItemStack(Material.valueOf(chest));
            }
            if(leg != null) {
                l = new ItemStack(Material.valueOf(leg));
            }
            if(boot != null) {
                b = new ItemStack(Material.valueOf(boot));
            }
            findEffectC = new FindEffect(h, c, l, b, visible, small, config.getString("find-effect.custom-name"));
        }

        // set command Executors and tab completers
        getCommand("blockquest").setExecutor(new BlockQuestCommand(this));
        getCommand("blockstats").setExecutor(new BlockStatsCommand(this));
        getCommand("blockquest").setTabCompleter(new BlockQuestTab());
    }

    // Helper method, called in onEnable
    private void createMySQL() {
        mysql.update("CREATE TABLE IF NOT EXISTS " + this.getDescription().getName() + " (UUID varchar(128), X varchar(2048) default \"none\", Y varchar(2048) default \"none\", Z varchar(2048) default \"none\", WORLD varchar(2048) default \"none\")");
    }

 // ======================================================================== on plugin disable ======================================================================== //
    
    public void onDisable() {
        // for each player save blocks
        for(Player pl : Bukkit.getOnlinePlayers()) {
            if(saved_x.get(pl.getName()) != null) {
                if (useMysql) {
                    SQLPlayer.setString(Utils.getIdentifier(pl), "X", saved_x.get(pl.getName()));
                    SQLPlayer.setString(Utils.getIdentifier(pl), "Y", saved_y.get(pl.getName()));
                    SQLPlayer.setString(Utils.getIdentifier(pl), "Z", saved_z.get(pl.getName()));
                    SQLPlayer.setString(Utils.getIdentifier(pl), "WORLD", saved_world.get(pl.getName()));
                } else {
                    data.getConfig().set("data." + Utils.getIdentifier(pl) + ".x", saved_x.get(pl.getName()));
                    data.getConfig().set("data." + Utils.getIdentifier(pl) + ".y", saved_y.get(pl.getName()));
                    data.getConfig().set("data." + Utils.getIdentifier(pl) + ".z", saved_z.get(pl.getName()));
                    data.getConfig().set("data." + Utils.getIdentifier(pl) + ".world", saved_world.get(pl.getName()));
                    data.saveConfig();
                }
            }
        }
    }
    // ======================================================================== get methods ======================================================================== //
    
    // get reference to Main singleton class t
    public static Main getPlugin() {
        return plugin;
    }

}
