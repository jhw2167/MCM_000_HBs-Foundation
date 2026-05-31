package com.holybuckets.foundation.event;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastructure.ConcurrentSet;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.util.*;
import java.util.function.Supplier;

//Project imports

public class CommandRegistry {

    private static final String CLASS_ID = "011";
    private static final Deque<Supplier<LiteralArgumentBuilder<CommandSourceStack>>> COMMANDS = new ArrayDeque<>();

    //Package private
    static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LoggerBase.logDebug(null, "011001", "Registering Commands " + COMMANDS.size());
        Iterator<Supplier<LiteralArgumentBuilder<CommandSourceStack>>> it = COMMANDS.iterator();
        while(it.hasNext()) {
            Supplier<LiteralArgumentBuilder<CommandSourceStack>> s = it.next();
            dispatcher.register(s.get());
        }
        LoggerBase.logDebug(null, "011002", "Finished Registering Commands");
    }

    public static void register( Supplier<LiteralArgumentBuilder<CommandSourceStack>> command)
    {
        //LoggerBase.logDebug(null, "011000", "Adding command" + COMMANDS.size());
        COMMANDS.add(command);
    }


    /*
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal(PREFIX)
            .then(Commands.argument("command", StringArgumentType.string())
                .executes(context -> execute(context, ""))
            .then(Commands.argument("arg1", StringArgumentType.string())
                .executes(context -> execute(context, ""))
            .then(Commands.argument("arg2", StringArgumentType.string())
                .executes(context -> execute(context, ""))
            .then(Commands.argument("arg3", StringArgumentType.string())
                .executes(context -> execute(context, ""))
            .then(Commands.argument("arg4", StringArgumentType.string())
                .executes(context -> execute(context, "")))))))
            .executes(context -> execute(context, "")));

    }
    */



}
