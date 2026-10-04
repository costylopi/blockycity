package com.blockcity.world;

import com.blockcity.BlockCityConfig;
import com.blockcity.ModEntities;
import com.blockcity.entity.CarEntity;
import com.blockcity.wanted.WantedManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** /blockcity city [marime] | /blockcity wanted <0-5> | /blockcity car   (necesita OP nivel 2) */
public final class ModCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access,
                                CommandManager.RegistrationEnvironment env) {
        dispatcher.register(CommandManager.literal("blockcity")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("city")
                        .executes(c -> city(c.getSource(), BlockCityConfig.get().citySize))
                        .then(CommandManager.argument("size", IntegerArgumentType.integer(1, 6))
                                .executes(c -> city(c.getSource(), IntegerArgumentType.getInteger(c, "size")))))
                .then(CommandManager.literal("wanted")
                        .then(CommandManager.argument("stars", IntegerArgumentType.integer(0, 5))
                                .executes(c -> {
                                    ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                                    int s = IntegerArgumentType.getInteger(c, "stars");
                                    WantedManager.setStars(p, s);
                                    c.getSource().sendFeedback(() -> Text.literal("Wanted = " + s + " stele"), false);
                                    return 1;
                                })))
                .then(CommandManager.literal("car")
                        .executes(c -> {
                            ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                            CarEntity car = ModEntities.CAR.create(p.getServerWorld());
                            if (car == null) return 0;
                            car.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), p.getYaw(), 0f);
                            p.getServerWorld().spawnEntity(car);
                            return 1;
                        })));
    }

    private static int city(ServerCommandSource src, int size) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity p = src.getPlayerOrThrow();
        src.sendFeedback(() -> Text.literal("Generez orasul " + size + "x" + size + "... (poate dura cateva secunde)"), false);
        int cars = CityGenerator.generate(p.getServerWorld(), p.getBlockPos(), size);
        src.sendFeedback(() -> Text.literal("Gata! Masini plasate: " + cars + ". Pietonii apar singuri pe trotuare."), false);
        return 1;
    }

    private ModCommands() {}
}
