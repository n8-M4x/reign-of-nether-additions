package com.solegendary.reignofnether.sounds;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SoundClientboundPacket {

    SoundAction soundAction;
    BlockPos bp;
    String playerName;

    public static void playSoundForAllPlayers(SoundAction soundAction, BlockPos bp) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, bp, ""));
    }
    public static void playSoundForAllPlayers(SoundAction soundAction) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, new BlockPos(0,0,0), ""));
    }
    public static void playSoundForPlayer(SoundAction soundAction, String name) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, new BlockPos(0,0,0), name));
    }

    public SoundClientboundPacket(SoundAction soundAction, BlockPos bp, String playerName) {
        this.soundAction = soundAction;
        this.bp = bp;
        this.playerName = playerName;
    }

    public SoundClientboundPacket(FriendlyByteBuf buffer) {
        this.soundAction = buffer.readEnum(SoundAction.class);
        this.bp = buffer.readBlockPos();
        this.playerName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.soundAction);
        buffer.writeBlockPos(this.bp);
        buffer.writeUtf(this.playerName);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (bp.equals(new BlockPos(0,0,0))) {
                            if (playerName.isBlank())
                                SoundClientEvents.playSoundForAllPlayers(soundAction);
                            else
                                SoundClientEvents.playSoundForPlayer(soundAction, playerName);
                        }
                        else
                            SoundClientEvents.playSoundForAction(soundAction, bp);
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
