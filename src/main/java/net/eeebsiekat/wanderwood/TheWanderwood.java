package net.eeebsiekat.wanderwood;

import net.eeebsiekat.wanderwood.glow.block.GlowExtractorBlock;
import net.eeebsiekat.wanderwood.glow.block.entity.GlowExtractorBlockEntity;
import net.eeebsiekat.wanderwood.glow.data.GlowNetworkSavedData;
import net.eeebsiekat.wanderwood.glow.item.GlowGogglesItem;
import net.eeebsiekat.wanderwood.glow.network.ClientboundGlowSyncPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<GlowExtractorBlock> GLOW_EXTRACTOR_BLOCK = BLOCKS.register("glow_extractor",
            () -> new GlowExtractorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.5F)));

    public static final DeferredItem<BlockItem> GLOW_EXTRACTOR_ITEM = ITEMS.registerSimpleBlockItem("glow_extractor", GLOW_EXTRACTOR_BLOCK);

    public static final DeferredItem<GlowGogglesItem> GLOW_GOGGLES = ITEMS.register("glow_goggles",
            () -> new GlowGogglesItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GlowExtractorBlockEntity>> GLOW_EXTRACTOR_BE =
            BLOCK_ENTITIES.register("glow_extractor_be", () ->
                    BlockEntityType.Builder.of(GlowExtractorBlockEntity::new, GLOW_EXTRACTOR_BLOCK.get()).build(null)
            );

    public TheWanderwood(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(
                ClientboundGlowSyncPacket.TYPE,
                ClientboundGlowSyncPacket.STREAM_CODEC,
                ClientboundGlowSyncPacket::handleClient
        );
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            GlowNetworkSavedData data = GlowNetworkSavedData.get(serverLevel);
            PacketDistributor.sendToPlayer(
                    (ServerPlayer) event.getEntity(),
                    new ClientboundGlowSyncPacket(new ArrayList<>(data.getNodes()), data.getLines())
            );
        }
    }
}