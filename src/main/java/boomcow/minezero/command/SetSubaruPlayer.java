package boomcow.minezero.command;

import boomcow.minezero.checkpoint.CheckpointManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import boomcow.minezero.checkpoint.CheckpointManager;
import boomcow.minezero.checkpoint.CheckpointData;
import net.minecraft.server.level.ServerLevel;

public class SetSubaruPlayer {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setSubaruPlayer")
                        .requires(cs -> cs.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    ServerLevel level = target.serverLevel();
                                    CheckpointData data = CheckpointData.get(level);
                                    data.setAnchorPlayerUUID(target.getUUID());
                                    context.getSource().sendSuccess(
                                            () -> Component
                                                    .literal("Anchor player set to " + target.getName().getString()),
                                            true);
                                    return 1;
                                })));

    }
}
