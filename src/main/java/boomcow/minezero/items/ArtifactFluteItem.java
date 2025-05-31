package boomcow.minezero.items; // Your package

import boomcow.minezero.ModGameRules;    // Your Fabric ModGameRules class
import boomcow.minezero.ModSoundEvents;  // Your Fabric ModSoundEvents class
import boomcow.minezero.checkpoint.CheckpointManager; // Ensure this uses Yarn mappings

import net.minecraft.entity.player.PlayerEntity; // Yarn mapping
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity; // Yarn mapping
import net.minecraft.sound.SoundCategory; // Yarn mapping
import net.minecraft.text.Text; // Yarn mapping
import net.minecraft.util.ActionResult; // Correct return type for your environment
import net.minecraft.util.Hand; // Yarn mapping
import net.minecraft.world.World; // Yarn mapping

public class ArtifactFluteItem extends Item {
    public ArtifactFluteItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) { // Corrected signature
        ItemStack itemStackInHand = player.getStackInHand(hand);

        if (!world.getGameRules().getBoolean(ModGameRules.ARTIFACT_FLUTE_ENABLED)) {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(Text.literal("The Artifact Flute is currently disabled by a game rule."), false);
            }
            return ActionResult.FAIL; // Return simple ActionResult
        }

        if (!world.isClient()) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                boolean cooldownEnabled = world.getGameRules().getBoolean(ModGameRules.FLUTE_COOLDOWN_ENABLED);
                if (cooldownEnabled) {
                    int cooldownSeconds = world.getGameRules().getInt(ModGamerules.FLUTE_COOLDOWN_DURATION);
                    int cooldownTicks = cooldownSeconds * 20;

                    if (serverPlayer.getItemCooldownManager().isCoolingDown(this)) {
                        serverPlayer.sendMessage(Text.literal("Artifact Flute is on cooldown!"), true);
                        return ActionResult.FAIL;
                    } else {
                        serverPlayer.getItemCooldownManager().set(this, cooldownTicks);
                    }
                }

                CheckpointManager.setCheckpoint(serverPlayer);

                world.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        ModSoundEvents.FLUTE_CHIME,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f
                );

                serverPlayer.sendMessage(Text.literal("Checkpoint set using the Artifact Flute!"), true);
            }
        }

        // If an action was taken, return SUCCESS.
        // The hand swing is usually handled by vanilla if SUCCESS is returned.
        // If the item should be "consumed" (even if not literally shrinking stack count here),
        // ActionResult.CONSUME might be more appropriate.
        // For a non-consumable tool that performs an action, SUCCESS is typical.
        return ActionResult.SUCCESS;
    }
}