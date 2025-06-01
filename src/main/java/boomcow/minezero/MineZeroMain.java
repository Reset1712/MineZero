package boomcow.minezero; // Your package

// Corrected Fabric API and YARN Minecraft imports for 1.21.5

import boomcow.minezero.command.SetCheckPointCommand;
import boomcow.minezero.command.SetSubaruPlayer;
import boomcow.minezero.command.TriggerRBD;
import boomcow.minezero.ConfigHandler;
import boomcow.minezero.event.*;
import boomcow.minezero.items.ArtifactFluteItem;
import boomcow.minezero.util.LightningScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MineZeroMain implements ModInitializer {

    public static final String MOD_ID = "minezero";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Block Example using AbstractBlock.Settings
    public static final Block EXAMPLE_BLOCK = new Block(AbstractBlock.Settings.create().strength(1.5F, 6.0F));
    // Item.Settings for BlockItem
    public static final BlockItem EXAMPLE_BLOCK_ITEM = new BlockItem(EXAMPLE_BLOCK, new Item.Settings());

    // Artifact Flute Item - its constructor will need to take Item.Settings
    public static final ArtifactFluteItem ARTIFACT_FLUTE = new ArtifactFluteItem(new Item.Settings().maxCount(1));

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MineZero for Fabric with Yarn Mappings!");
        ConfigHandler.register();
        ModGameRules.initialize();

        registerBlocksAndItems();
        ModSoundEvents.registerSoundEvents(); // Ensure ModSoundEvents uses Yarn
        // PacketHandler.registerC2SModPackets();
        registerServerTickEvents();
        BlockChangeListener.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Pass registryAccess (DynamicRegistryManager) to commands if they need it
            SetCheckPointCommand.register(dispatcher, registryAccess);
            SetSubaruPlayer.register(dispatcher, registryAccess);
            TriggerRBD.register(dispatcher, registryAccess);
        });

        registerServerEventHandlers();
        // ConfigHandler.loadConfig();

        // Use ItemGroups for creative tabs
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(this::addBuildingBlocksToCreativeTab);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS_AND_UTILITIES).register(this::addToolsToCreativeTab);

        LOGGER.info("DIRT BLOCK KEY >> {}", Registries.BLOCK.getId(Blocks.DIRT)); // Use getId for ResourceLocation equivalent
        LOGGER.info("Common setup tasks from onInitialize() completed.");
    }

    private static void registerBlocksAndItems() {
        // Use Identifier for ResourceLocation
        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "example_block"), EXAMPLE_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "example_block"), EXAMPLE_BLOCK_ITEM);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "artifact_flute"), ARTIFACT_FLUTE);

        LOGGER.info("MineZero blocks and items registered with Yarn mappings.");
    }

    private void addBuildingBlocksToCreativeTab(FabricItemGroupEntries entries) {
        entries.add(EXAMPLE_BLOCK_ITEM);
    }

    private void addToolsToCreativeTab(FabricItemGroupEntries entries) {
        entries.add(ARTIFACT_FLUTE);
    }

    private void registerServerEventHandlers() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer(); // Use ServerPlayerEntity
            onPlayerLogin(player);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("MineZero: Server starting...");
        });

        // CRITICAL: These event handler classes must be fully ported to Fabric API
        // and use Yarn names for all Minecraft classes they interact with.
        BlockChangeListener.register();
        CheckpointTicker.register();
        ExplosionEventHandler.register();
        // GlobalTickHandler.register();
        LightningStrikeListener.register();
        NonPlayerChangeHandler.register();
        DeathEventHandler.register();
    }

    // Parameter type changed to ServerPlayerEntity
    public void onPlayerLogin(ServerPlayerEntity player) {
        // player.getWorld() returns World. Cast to ServerWorld if server-specific methods are needed.
        if (player.getWorld().isClient()) return; // Correct way to check side for a World

        ServerWorld world = (ServerWorld) player.getWorld(); // Cast to ServerWorld
        // DynamicRegistryManager registryManager = world.getRegistryManager(); // How to get registry access

        LOGGER.info("Player {} logged in. MineZero login logic to be fully ported.", player.getName().getString());
        // ... (Rest of your onPlayerLogin logic)
        // Ensure CheckpointData, PlayerData, etc., all use Yarn names (NbtCompound, Identifier, StatusEffectInstance, etc.)
        // and that any interaction with 'world' or 'player' uses their Yarn methods.
    }

    private void registerServerTickEvents() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // This event fires for the overall MinecraftServer
            // You need to iterate through all loaded server worlds
            for (ServerWorld world : server.getWorlds()) {
                LightningScheduler.tick(world);
            }
        });

        // Alternatively, if you only care about a specific world or want per-world ticking:
        // ServerTickEvents.END_WORLD_TICK.register(world -> {
        //     // This event fires for each ServerWorld individually
        //     if (world instanceof ServerWorld) { // Should always be true here
        //         LightningScheduler.tick((ServerWorld) world);
        //     }
        // });
        // Using END_SERVER_TICK and iterating server.getWorlds() is common for global systems.
    }
}