package me.robifoxx.block.mysql;

import java.util.List;

public class FoundReward {

    private int blockCount;
    private List<String> commands;

    public FoundReward(int blockCount, List<String> commands) {
        this.blockCount = blockCount;
        this.commands = commands;
    }

    public List<String> getCommands() {
        return commands;
    }

    public int getBlockCount() {
        return blockCount;
    }
}
