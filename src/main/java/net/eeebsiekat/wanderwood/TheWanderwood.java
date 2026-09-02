package net.eeebsiekat.wanderwood;

import net.eeebsiekat.wanderwood.glow.block.GlowExtractorBlock;
import net.eeebsiekat.wanderwood.glow.block.ShroomeBlock;
import net.eeebsiekat.wanderwood.glow.block.StumpBlock;
import net.eeebsiekat.wanderwood.glow.block.entity.GlowExtractorBlockEntity;
import net.eeebsiekat.wanderwood.glow.block.entity.ShroomeBlockEntity;
import net.eeebsiekat.wanderwood.glow.block.entity.StumpBlockEntity;
import net.eeebsiekat.wanderwood.glow.data.GlowNetworkSavedData;
import net.eeebsiekat.wanderwood.glow.data.GlowWaypointSavedData;
import net.eeebsiekat.wanderwood.glow.item.GlowGogglesItem;
import net.eeebsiekat.wanderwood.glow.network.ClientboundGlowSyncPacket;
import net.eeebsiekat.wanderwood.glow.network.ServerboundGlowTravelPacket;
import net.eeebsiekat.wanderwood.glow.network.ServerboundRequestWaypointsPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;

@Mod(TheWanderwood.MODID)
public class TheWanderwood {
    public static final String MODID = "wanderwood";

    // Deferred Registers
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    // Blocks
    public static final DeferredBlock<GlowExtractorBlock> GLOW_EXTRACTOR_BLOCK = BLOCKS.register("glow_extractor",
            () -> new GlowExtractorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.5F)));

    public static final DeferredBlock<ShroomeBlock> SHROOME_BLOCK = BLOCKS.register("shroome",
            () -> new ShroomeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0F)));

    public static final DeferredBlock<StumpBlock> STUMP_BLOCK = BLOCKS.register("stump",
            () -> new StumpBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F)));

    // Items
    public static final DeferredItem<BlockItem> GLOW_EXTRACTOR_ITEM = ITEMS.registerSimpleBlockItem("glow_extractor", GLOW_EXTRACTOR_BLOCK);
    public static final DeferredItem<BlockItem> SHROOME_ITEM = ITEMS.registerSimpleBlockItem("shroome", SHROOME_BLOCK);
    public static final DeferredItem<BlockItem> STUMP_ITEM = ITEMS.registerSimpleBlockItem("stump", STUMP_BLOCK);

    public static final DeferredItem<GlowGogglesItem> GLOW_GOGGLES = ITEMS.register("glow_goggles",
            () -> new GlowGogglesItem(new Item.Properties().stacksTo(1)));

    // Block Entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GlowExtractorBlockEntity>> GLOW_EXTRACTOR_BE =
            BLOCK_ENTITIES.register("glow_extractor_be", () ->
                    BlockEntityType.Builder.of(GlowExtractorBlockEntity::new, GLOW_EXTRACTOR_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShroomeBlockEntity>> SHROOME_BE =
            BLOCK_ENTITIES.register("shroome_be", () ->
                    BlockEntityType.Builder.of(ShroomeBlockEntity::new, SHROOME_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StumpBlockEntity>> STUMP_BE =
            BLOCK_ENTITIES.register("stump_be", () ->
                    BlockEntityType.Builder.of(StumpBlockEntity::new, STUMP_BLOCK.get()).build(null));

    public TheWanderwood(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            GlowNetworkSavedData networkData = GlowNetworkSavedData.get(serverLevel);
            GlowWaypointSavedData waypointData = GlowWaypointSavedData.get(serverLevel);

            PacketDistributor.sendToPlayer(
                    (ServerPlayer) event.getEntity(),
                    new ClientboundGlowSyncPacket(
                            new ArrayList<>(networkData.getNodes()),
                            networkData.getLines(),
                            new ArrayList<>(waypointData.getAllWaypoints())
                    )
            );
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1.0.0");

        registrar.playToClient(
                ClientboundGlowSyncPacket.TYPE,
                ClientboundGlowSyncPacket.STREAM_CODEC,
                ClientboundGlowSyncPacket::handleClient
        );

        registrar.playToServer(
                ServerboundGlowTravelPacket.TYPE,
                ServerboundGlowTravelPacket.STREAM_CODEC,
                ServerboundGlowTravelPacket::handleServer
        );

        registrar.playToServer(
                ServerboundRequestWaypointsPacket.TYPE,
                ServerboundRequestWaypointsPacket.STREAM_CODEC,
                ServerboundRequestWaypointsPacket::handleServer
        );
    }
}