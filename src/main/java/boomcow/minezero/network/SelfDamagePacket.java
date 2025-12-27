package boomcow.minezero.network;

import boomcow.minezero.MineZeroMain;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SelfDamagePacket() implements CustomPayload {

    public static final Id<SelfDamagePacket> ID = new Id<>(Identifier.of(MineZeroMain.MOD_ID, "self_damage_trigger_v1"));
    public static final PacketCodec<RegistryByteBuf, SelfDamagePacket> CODEC = PacketCodec.unit(new SelfDamagePacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
