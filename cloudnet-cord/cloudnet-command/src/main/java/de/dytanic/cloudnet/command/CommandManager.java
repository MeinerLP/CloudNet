/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnet.command;

import de.dytanic.cloudnet.lib.NetworkUtils;
import jline.console.completer.Completer;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that manages commands for the interfaces of CloudNet.
 */
@Getter
public final class CommandManager
        implements Completer {

    private final Map<String, Command> commands = NetworkUtils.newConcurrentHashMap();
    private ConsoleCommandSender consoleSender = new ConsoleCommandSender();

    /**
     * Constructs a new command manager with a {@link ConsoleCommandSender} and
     * no commands.
     */
    public CommandManager()
    {
    }

    /**
     * Clears all the commands that are currently registered.
     *
     * @return the command manager this was called on, allows for chaining
     */
    public CommandManager clearCommands()
    {
        commands.clear();
        return this;
    }

    /**
     * Register a new command and all of its aliases to this command manager.
     *
     * @param command the command to register
     * @return the command manager this was called on, allows for chaining
     */
    public CommandManager registerCommand(Command command)
    {
        if (command == null) return this;

        this.commands.put(command.getName().toLowerCase(), command);

        if (command.getAliases().length != 0)
        {
            for (String aliases : command.getAliases())
            {
                commands.put(aliases.toLowerCase(), command);
            }
        }

        return this;
    }

    /**
     * Get the registered commands.
     *
     * @return a set containing all the registered command names and aliases
     */
    public Set<String> getCommands()
    {
        return commands.keySet();
    }

    /**
     * Get the command for a given name.
     *
     * @param name the name to get the command for
     * @return the command, if there is one with the given {@code name} or alias
     * or {@code null}, if no command matches the {@code name}
     */
    public Command getCommand(String name)
    {
        return commands.get(name.toLowerCase());
    }

    /**
     * Parses the given {@code command} and dispatches it using the
     * given {@code sender}.
     *
     * <ol>
     * <li>First all arguments get processed by the {@link CommandArgument} handlers.</li>
     * <li>Then the {@link Command} is executed with the processed commands</li>
     * <li>Last all arguments are processed again</li>
     * </ol>
     *
     * @param sender  the sender to execute the command as
     * @param command the command line to parse and dispatch
     * @return whether the command executed successfully
     */
    public boolean dispatchCommand(CommandSender sender, String command)
    {
        String[] a = command.split(" ");
        if (this.commands.containsKey(a[0].toLowerCase()))
        {
            String b = command.replace((command.contains(" ") ? command.split(" ")[0] + " " : command), NetworkUtils.EMPTY_STRING);
            try
            {
                for (String argument : a)
                {
                    for (CommandArgument commandArgument : this.commands.get(a[0].toLowerCase()).getCommandArguments())
                    {
                        if (commandArgument.getName().equalsIgnoreCase(argument))
                            commandArgument.preExecute(this.commands.get(a[0]), command);
                    }
                }

                if (b.equals(NetworkUtils.EMPTY_STRING))
                {
                    this.commands.get(a[0].toLowerCase()).onExecuteCommand(sender, new String[0]);
                } else
                {
                    String[] c = b.split(" ");
                    this.commands.get(a[0].toLowerCase()).onExecuteCommand(sender, c);
                }

                for (String argument : a)
                {
                    for (CommandArgument commandArgument : this.commands.get(a[0].toLowerCase()).getCommandArguments())
                    {
                        if (commandArgument.getName().equalsIgnoreCase(argument))
                            commandArgument.postExecute(this.commands.get(a[0]), command);
                    }
                }

            } catch
                    (Exception ex)
            {
                ex.printStackTrace();
            }
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * Parses the given {@code command} from the console and dispatches it using
     * a {@link ConsoleCommandSender}.
     *
     * <ol>
     * <li>First all arguments get processed by the {@link CommandArgument} handlers.</li>
     * <li>Then the {@link Command} is executed with the processed commands</li>
     * <li>Last all arguments are processed again</li>
     * </ol>
     *
     * @param command the command line to parse and dispatch
     * @return whether the command executed successfully
     * @see CommandManager#dispatchCommand(CommandSender, String)
     */
    public boolean dispatchCommand(String command)
    {
        return dispatchCommand(consoleSender, command);
    }

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates)
    {
        String[] input = buffer.split(" ");

        if (input.length == 0) return cursor;

        Command command = getCommand(input[0]);
        if (command instanceof TabCompletable)
        {
            List<String> tabCompletions = ((TabCompletable) command).onTab(input.length - 1, input[input.length - 1]);

            candidates.addAll(tabCompletions);

            final int lastSpace = buffer.lastIndexOf(' ');
            if (lastSpace == -1)
            {
                return cursor - buffer.length();
            } else
            {
                return cursor - (buffer.length() - lastSpace - 1);
            }
        }
        return cursor;
    }
}
