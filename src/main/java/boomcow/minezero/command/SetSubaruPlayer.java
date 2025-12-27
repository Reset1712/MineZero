package boomcow.minezero.command;

import boomcow.minezero.checkpoint.CheckpointData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class SetSubaruPlayer {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
                CommandManager.literal("setSubaruPlayer")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                    ServerWorld world = target.getServerWorld();
                                    CheckpointData data = CheckpointData.get(world);
                                    data.setAnchorPlayerUUID(target.getUuid());
                                    context.getSource().sendFeedback(() -> Text.literal("Anchor player set to " + target.getName().getString()), true);
                                    return 1;
                                }))
        );
    }
}
