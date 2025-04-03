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
import boomcow.minezero.ModGameRules;

public class ArtifactFluteItem extends Item {
    public ArtifactFluteItem(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Check if the flute cooldown gamerule is enabled.
                boolean cooldownEnabled = level.getGameRules().getBoolean(ModGameRules.FLUTE_COOLDOWN_ENABLED);
                if (cooldownEnabled) {
                    // Get the cooldown duration from the gamerule (in seconds) and convert to ticks.
                    int cooldownSeconds = level.getGameRules().getInt(ModGameRules.FLUTE_COOLDOWN_DURATION);
                    int cooldownTicks = cooldownSeconds * 20;

                    // If the item is still on cooldown, notify the player and cancel use.
                    if (serverPlayer.getCooldowns().isOnCooldown(this)) {
                        serverPlayer.displayClientMessage(Component.literal("Artifact Flute is on cooldown!"), true);
                        return InteractionResultHolder.fail(player.getItemInHand(hand));
                    } else {
                        // Apply the cooldown.
                        serverPlayer.getCooldowns().addCooldown(this, cooldownTicks);
                    }
                }

                // Trigger the checkpoint functionality
                CheckpointManager.setCheckpoint(serverPlayer);

                // Play the custom sound.
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSoundEvents.FLUTE_CHIME.get(), // Ensure you have defined this sound.
                        SoundSource.PLAYERS,
                        1.0f, // Volume
                        1.0f  // Pitch
                );

                // Notify the player.
                serverPlayer.displayClientMessage(Component.literal("Checkpoint set using the Artifact Flute!"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

}
