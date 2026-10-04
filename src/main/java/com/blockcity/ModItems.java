package com.blockcity;

import com.blockcity.item.CarKeyItem;
import com.blockcity.item.PistolItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    public static final Item PISTOL = new PistolItem(new Item.Settings().maxCount(1));
    public static final Item CAR_KEY = new CarKeyItem(new Item.Settings().maxCount(16));

    public static void register() {
        Registry.register(Registries.ITEM, BlockCityMod.id("pistol"), PISTOL);
        Registry.register(Registries.ITEM, BlockCityMod.id("car_key"), CAR_KEY);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(PISTOL);
            entries.add(CAR_KEY);
        });
    }

    private ModItems() {}
}
