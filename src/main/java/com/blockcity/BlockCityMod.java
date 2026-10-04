package com.blockcity;

import com.blockcity.wanted.WantedManager;
import com.blockcity.wanted.WantedSyncPayload;
import com.blockcity.world.ModCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockCityMod implements ModInitializer {
    public static final String MOD_ID = "blockcity";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        BlockCityConfig.load();
        ModItems.register();
        ModEntities.register();
        PayloadTypeRegistry.playS2C().register(WantedSyncPayload.ID, WantedSyncPayload.CODEC);
        WantedManager.init();
        CommandRegistrationCallback.EVENT.register(ModCommands::register);
        LOGGER.info("Block City incarcat.");
    }
}
