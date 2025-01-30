package boomcow.minezero.items;

import boomcow.minezero.ModSoundEvents;
import boomcow.minezero.checkpoint.CheckpointManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ArtifactFluteItem extends Item {
    public ArtifactFluteItem(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Trigger the checkpoint functionality
                CheckpointManager.setCheckpoint(serverPlayer);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSoundEvents.FLUTE_CHIME.get(), // Use your custom sound
                        SoundSource.PLAYERS,
                        1.0f, // Volume
                        1.0f // Pitch
                );

                // Notify the player
                serverPlayer.displayClientMessage(Component.literal("Checkpoint set using the Artifact Flute!"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

}
