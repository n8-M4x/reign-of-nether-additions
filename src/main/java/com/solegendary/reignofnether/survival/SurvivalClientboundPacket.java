package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.player.PlayerAction;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SurvivalClientboundPacket {

    WaveDifficulty difficulty;
    int waveNumber;

    public static void enableAndSetDifficulty(WaveDifficulty diff) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SurvivalClientboundPacket(diff, 0));
    }

    public static void enableAndSetDifficulty(WaveDifficulty diff, int waveNum) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SurvivalClientboundPacket(WaveDifficulty.EASY, waveNum));
    }

    public SurvivalClientboundPacket(WaveDifficulty difficulty, int waveNumber) {
        this.difficulty = difficulty;
        this.waveNumber = waveNumber;
    }

    public SurvivalClientboundPacket(FriendlyByteBuf buffer) {
        this.difficulty = buffer.readEnum(WaveDifficulty.class);
        this.waveNumber = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.difficulty);
        buffer.writeInt(this.waveNumber);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        SurvivalClientEvents.difficulty = difficulty;
                        SurvivalClientEvents.isEnabled = true;
                        if (waveNumber > 0)
                            SurvivalClientEvents.waveNumber = waveNumber;
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
