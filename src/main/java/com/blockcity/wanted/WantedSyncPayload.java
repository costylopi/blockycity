package com.blockcity.wanted;

import com.blockcity.BlockCityMod;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record WantedSyncPayload(int stars) implements CustomPayload {
    public static final CustomPayload.Id<WantedSyncPayload> ID = new CustomPayload.Id<>(BlockCityMod.id("wanted"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, WantedSyncPayload> CODEC =
            PacketCodecs.VAR_INT.xmap(WantedSyncPayload::new, WantedSyncPayload::stars).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
