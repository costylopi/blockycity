package com.blockcity.item;

import com.blockcity.ModEntities;
import com.blockcity.entity.CarEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Click dreapta pe un bloc = apare o masina acolo. */
public class CarKeyItem extends Item {
    public CarKeyItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;
        BlockPos pos = ctx.getBlockPos().offset(ctx.getSide());
        CarEntity car = ModEntities.CAR.create(world);
        if (car == null) return ActionResult.FAIL;
        car.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ctx.getPlayerYaw(), 0f);
        world.spawnEntity(car);
        PlayerEntity player = ctx.getPlayer();
        if (player == null || !player.isCreative()) ctx.getStack().decrement(1);
        return ActionResult.CONSUME;
    }
}
