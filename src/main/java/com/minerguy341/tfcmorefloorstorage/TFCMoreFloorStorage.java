package com.minerguy341.tfcmorefloorstorage;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(TFCMoreFloorStorage.MOD_ID)
public class TFCMoreFloorStorage
{
    public static final String MOD_ID = "tfcmorefloorstorage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TFCMoreFloorStorage()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, IngotMeshClientConfig.SPEC);

        modBus.addListener(this::onCommonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            modBus.addListener(this::onClientSetup);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(ClayPiling::register);
        LOGGER.info("Registered clay pile interaction for tag {} (capacity {}).", ModTags.PILEABLE_CLAYS.location(), ClayPileBlock.MAX_COUNT);
    }

    private void onClientSetup(FMLClientSetupEvent event)
    {
        IngotMeshClientConfig.logActiveSettings();
    }
}
