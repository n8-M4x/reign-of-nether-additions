package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.registrars.PacketHandler;
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
    long bonusTicks;

    public static void enableAndSetDifficulty(WaveDifficulty diff) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SurvivalClientboundPacket(diff, 0, 0L));
    }

    public static void setWaveNumber(int waveNum) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SurvivalClientboundPacket(WaveDifficulty.EASY, waveNum, 0L));
    }

    public static void setBonusTicks(long ticks) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SurvivalClientboundPacket(WaveDifficulty.EASY, 0, ticks));
    }

    public SurvivalClientboundPacket(WaveDifficulty difficulty, int waveNumber, long bonusTicks) {
        this.difficulty = difficulty;
        this.waveNumber = waveNumber;
        this.bonusTicks = bonusTicks;
    }

    public SurvivalClientboundPacket(FriendlyByteBuf buffer) {
        this.difficulty = buffer.readEnum(WaveDifficulty.class);
        this.waveNumber = buffer.readInt();
        this.bonusTicks = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.difficulty);
        buffer.writeInt(this.waveNumber);
        buffer.writeLong(this.bonusTicks);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (waveNumber > 0)
                            SurvivalClientEvents.waveNumber = waveNumber;
                        else if (bonusTicks > 0)
                            SurvivalClientEvents.bonusTicks = bonusTicks;
                        else
                            SurvivalClientEvents.enable(difficulty);
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
