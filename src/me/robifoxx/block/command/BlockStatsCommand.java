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

public class BlockStatsCommand implements CommandExecutor {
    private Main main;
    public BlockStatsCommand(Main main) {
        this.main = main;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final String firstArgument = (args.length >= 1) ? args[0] : null;
        BlockQuestCommand.statsCommand(main, sender, firstArgument);
        return true;
    }

}
