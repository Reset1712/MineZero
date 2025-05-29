package boomcow.minezero;

import boomcow.minezero.checkpoint.CheckpointData;
import boomcow.minezero.checkpoint.CheckpointManager;
import boomcow.minezero.checkpoint.PlayerData;
import boomcow.minezero.command.SetCheckPointCommand;
import boomcow.minezero.command.SetSubaruPlayer;
import boomcow.minezero.command.TriggerRBD;
import boomcow.minezero.event.*;
import boomcow.minezero.input.KeyBindings;
import boomcow.minezero.items.ArtifactFluteItem;
import boomcow.minezero.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.Locale;


@Mod(MineZero.MODID)
public class MineZero {

    public static final String MODID = "minezero";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // For other types like SoundEvents, you'd use:
    // public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);


    // Block and BlockItem example
    // DeferredRegister.register now typically returns DeferredHolder<T>
    public static final DeferredHolder<Block, Block> EXAMPLE_BLOCK = BLOCKS.register("example_block",
            () -> new Block(BlockBehaviour.Properties.of().strength(1.5F, 6.0F))); // Example properties for stone

    public static final DeferredHolder<Item, BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block",
            () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    // Artifact Flute Item
    public static final DeferredHolder<Item, ArtifactFluteItem> ARTIFACT_FLUTE = ITEMS.register("artifact_flute",
            () -> new ArtifactFluteItem(new Item.Properties().stacksTo(1)));

    public MineZero(IEventBus modEventBus, ModContainer modContainer) { // NeoForge MDKs often pass IEventBus to constructor

        // Register event listeners on the MOD event bus
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup); // Client setup on mod bus
        modEventBus.addListener(this::registerKeyMappings); // Key mappings on mod bus
        modEventBus.addListener(this::addCreative);
        // Config events on mod bus
        modEventBus.addListener(ConfigHandler::onLoad); // Assuming static methods in ConfigHandler
        modEventBus.addListener(ConfigHandler::onReload);

        // Register DeferredRegisters to the mod event bus
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModSoundEvents.SOUND_EVENTS.register(modEventBus); // Register sounds
        PacketHandler.register(modEventBus);

        // Register general event handlers to the NeoForge event bus
        NeoForge.EVENT_BUS.register(this); // For @SubscribeEvent methods in this class
        NeoForge.EVENT_BUS.register(new DeathEventHandler());

        NeoForge.EVENT_BUS.register(BlockChangeListener.class); // <--- ADD THIS LINE
        NeoForge.EVENT_BUS.register(CheckpointTicker.class); // <--- ADD THIS LINE
        NeoForge.EVENT_BUS.register(ExplosionEventHandler.class); // <--- ADD THIS LINE
        NeoForge.EVENT_BUS.register(GlobalTickHandler.class); // <--- ADD THIS LINE
        NeoForge.EVENT_BUS.register(LightningStrikeListener.class); // Register DeathEventHandler for player death events
        NeoForge.EVENT_BUS.register(NonPlayerChangeHandler.class); // Register DeathEventHandler for player death events





        if (modContainer != null) { // Ensure modContainer is available
            modContainer.registerConfig(ModConfig.Type.COMMON, ConfigHandler.COMMON_CONFIG_SPEC);
        } else {
            LOGGER.warn("ModContainer was not injected, attempting to get active container for config registration. This might be unreliable.");
            ModContainer fallbackContainer = ModLoadingContext.get().getActiveContainer();
            LOGGER.error("CRITICAL: ModContainer not available for config registration. Configs will not be loaded for {}.", MODID);
        }
    }

    // RegisterCommandsEvent is on the NeoForge event bus
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SetCheckPointCommand.register(event.getDispatcher());
        SetSubaruPlayer.register(event.getDispatcher());
        TriggerRBD.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info("Loading custom game rules: autoCheckpointEnabled = {}", ModGameRules.AUTO_CHECKPOINT_ENABLED);

        // --- CORRECTED: Use event.enqueueWork directly ---
        // If you have tasks for common setup that need to be on the main thread:
        event.enqueueWork(() -> {
            LOGGER.info("This is a task running on the main thread after common setup dispatch (from commonSetup).");
            // Example: Some registration that must happen on main thread and isn't an event listener setup.
            // MyOtherClass.initializeCommonThreadSafeStuff();
        });
        // --- END CORRECTION ---
    }

    // Listener for BuildCreativeModeTabContentsEvent (on NeoForge.EVENT_BUS)
    public void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Accessing CreativeModeTabs constants directly
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM.get()); // .get() on DeferredHolder
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ARTIFACT_FLUTE.get()); // .get() on DeferredHolder
        }
    }

    // setupNetworking merged into commonSetup via enqueueWork

    // Config event listeners are now registered in the constructor to the modEventBus
    // The static methods in ConfigHandler will be called.


    // PlayerEvent.PlayerLoggedInEvent is on the NeoForge event bus
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        ServerLevel level = player.serverLevel();
        CheckpointData data = CheckpointData.get(level);
        HolderLookup.Provider lookupProvider = level.registryAccess();

        LOGGER.info("[MineZero][LOGIN] Player {} (UUID: {}) logged in.", player.getName().getString(), player.getUUID());

        if (data.getAnchorPlayerUUID() == null && data.getPlayerData(player.getUUID(), lookupProvider) == null) {
            LOGGER.info("[MineZero][LOGIN] No anchor player set and player {} is new. Setting initial checkpoint.", player.getName().getString());
            CheckpointManager.setCheckpoint(player);
            LOGGER.info("[MineZero][LOGIN] Initial anchor and checkpoint set for {}", player.getName().getString());
        } else if (data.getPlayerData(player.getUUID(), lookupProvider) == null) {
            LOGGER.info("[MineZero][LOGIN] Player {} (UUID: {}) not found in current checkpoint data. Adding them now.", player.getName().getString(), player.getUUID());
            PlayerData pDataForNewPlayer = new PlayerData();
            pDataForNewPlayer.posX = player.getX();
            pDataForNewPlayer.posY = player.getY();
            pDataForNewPlayer.posZ = player.getZ();
            pDataForNewPlayer.yaw = player.getYRot();
            pDataForNewPlayer.pitch = player.getXRot();
            pDataForNewPlayer.dimension = player.level().dimension();
            pDataForNewPlayer.gameMode = player.gameMode.getGameModeForPlayer().getName().toLowerCase(Locale.ROOT);
            pDataForNewPlayer.motionX = player.getDeltaMovement().x;
            pDataForNewPlayer.motionY = player.getDeltaMovement().y;
            pDataForNewPlayer.motionZ = player.getDeltaMovement().z;
            pDataForNewPlayer.fallDistance = player.fallDistance;
            pDataForNewPlayer.health = player.getHealth();
            pDataForNewPlayer.hunger = player.getFoodData().getFoodLevel();
            pDataForNewPlayer.experienceLevel = player.experienceLevel;
            pDataForNewPlayer.experienceProgress = player.experienceProgress;
            pDataForNewPlayer.fireTicks = player.getRemainingFireTicks();

            BlockPos spawn = player.getRespawnPosition();
            ResourceKey<Level> spawnDim = player.getRespawnDimension();
            if (spawn != null && spawnDim != null) {
                pDataForNewPlayer.spawnX = spawn.getX() + 0.5;
                pDataForNewPlayer.spawnY = spawn.getY();
                pDataForNewPlayer.spawnZ = spawn.getZ() + 0.5;
                pDataForNewPlayer.spawnDimension = spawnDim;
                pDataForNewPlayer.spawnForced = player.isRespawnForced();
            }

            pDataForNewPlayer.potionEffects.clear();
            for (MobEffectInstance effectInstance : player.getActiveEffects()) { // Renamed variable for clarity
                pDataForNewPlayer.potionEffects.add(new MobEffectInstance(effectInstance)); // Copy constructor
            }

            pDataForNewPlayer.inventory.clear();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                pDataForNewPlayer.inventory.add(player.getInventory().getItem(i).copy());
            }

            CompoundTag advTag = new CompoundTag();
            if (player.server != null) {
                for (AdvancementHolder advancementHolder : player.server.getAdvancements().getAllAdvancements()) {
                    // Advancement advancement = advancementHolder.value(); // Not needed if getOrStartProgress takes Holder
                    AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancementHolder);
                    CompoundTag progressTag = new CompoundTag();
                    for (String criterion : progress.getCompletedCriteria()) {
                        progressTag.putBoolean(criterion, true);
                    }
                    advTag.put(advancementHolder.id().toString(), progressTag); // Use holder.id()
                }
            }
            pDataForNewPlayer.advancements = advTag;

            CompoundTag playerDataNbt = pDataForNewPlayer.toNBT(lookupProvider); // Use the lookupProvider obtained earlier
            data.savePlayerData(player.getUUID(), playerDataNbt);
            LOGGER.info("[MineZero][LOGIN] Player {} added to checkpoint.", player.getName().getString());
        } else {
            LOGGER.info("[MineZero][LOGIN] Player {} (UUID: {}) already has data in checkpoint.", player.getName().getString(), player.getUUID());
            LOGGER.info("[MineZero][LOGIN] Player UUID = {}, Stored Anchor = {}",
                    player.getUUID(), data.getAnchorPlayerUUID());
        }
    }

    // ServerStartingEvent is on the NeoForge event bus
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    // Client specific setup, on MOD event bus
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("HELLO FROM CLIENT SETUP (Mod Event Bus)");

        // Register client-specific game event handlers to NeoForge.EVENT_BUS
        NeoForge.EVENT_BUS.register(ClientForgeEvents.class);

        // --- CORRECTED: Use event.enqueueWork directly ---
        // For client-side tasks that need to run on the main game thread after client setup
        event.enqueueWork(() -> {
            LOGGER.info("Performing client-side enqueued work (from clientSetup).");
            // Example: KeyBindings.initModels(), screen registrations, renderer registrations
            // SomeModelRegistry.registerModels();
            // SomeScreenRegistry.registerScreens();
        });
        // --- END CORRECTION ---
    }

    // Key Mappings registration, on MOD event bus
    private void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        LOGGER.info("Registering MineZero Key Mappings (Mod Event Bus)");

        // Get the KeyMapping instance from the Lazy object using .get()
        if (KeyBindings.EXAMPLE_ACTION_KEY != null) { // The Lazy object itself can be checked for null if not final, though usually it's final
            event.register(KeyBindings.EXAMPLE_ACTION_KEY.get()); // <--- Use .get()
        }
        if (KeyBindings.SELF_DAMAGE_KEY != null) {
            event.register(KeyBindings.SELF_DAMAGE_KEY.get());    // <--- Use .get()
        }
    }


    // The static inner class for client events is no longer strictly necessary
    // if its methods (clientSetup, registerKeyMappings) are moved to the main class
    // and registered correctly on the MOD event bus with Dist.CLIENT filtering if needed,
    // or if they are Dist-specific events like FMLClientSetupEvent.
    // @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    // public static class ClientModEvents { ... }
}