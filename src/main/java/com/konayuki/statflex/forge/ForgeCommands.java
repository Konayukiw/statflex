package com.konayuki.statflex.forge;

import com.konayuki.statflex.utils.Commands;
import com.konayuki.statflex.utils.Messages;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.Collections;
import java.util.List;

public final class ForgeCommands implements ICommand {
    private final Commands command = new Commands();

    ForgeCommands() {
    }

    @Override
    public String getCommandName() {
        return Commands.commandName();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Messages.USAGE;
    }

    @Override
    public List<String> getCommandAliases() {
        return command.aliases();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        command.execute(args);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return Collections.emptyList();
    }

    @Override
    public int compareTo(ICommand other) {
        return getCommandName().compareTo(other.getCommandName());
    }
}
