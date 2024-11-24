package com.solegendary.reignofnether.sounds;

import com.solegendary.reignofnether.registrars.SoundRegistrar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundClientEvents {

    // mute the next sound played at each pos in ClientLevelMixin, then remove it from the list
    public static ArrayList<BlockPos> mutedBps = new ArrayList<>();

    public static void playSoundForAction(SoundAction soundAction, BlockPos bp) {
        SoundEvent soundEvent = SOUND_MAP.get(soundAction);
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null)
            level.playSound(null, bp.getX(), bp.getY(), bp.getZ(), soundEvent, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    public static void playSoundForAllPlayers(SoundAction soundAction) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            MC.player.playSound(SOUND_MAP.get(soundAction), 1.0f, 1.0f);
        }
    }

    public static void playSoundForPlayer(SoundAction soundAction, String playerName) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null && MC.player.getName().getString().equals(playerName)) {
            MC.player.playSound(SOUND_MAP.get(soundAction), 1.0f, 1.0f);
        }
    }

    // sounds which shouldn't follow the ClientLevelMixin rules of being changed to the location of what is selected
    // and is always audible while in orthoview mode
    public static List<SoundEvent> STATIC_SOUNDS = List.of(
            SoundEvents.AMBIENT_CAVE,
            SoundRegistrar.ALLY.get(),
            SoundRegistrar.CHAT.get(),
            SoundRegistrar.ENEMY.get()
    );

    private static final Map<SoundAction, SoundEvent> SOUND_MAP = new HashMap<>();

    static {
        SOUND_MAP.put(SoundAction.USE_PORTAL, SoundEvents.ENDERMAN_TELEPORT);
        SOUND_MAP.put(SoundAction.RANDOM_CAVE_AMBIENCE, SoundEvents.AMBIENT_CAVE);
        SOUND_MAP.put(SoundAction.ALLY, SoundRegistrar.ALLY.get());
        SOUND_MAP.put(SoundAction.CHAT, SoundRegistrar.CHAT.get());
        SOUND_MAP.put(SoundAction.ENEMY, SoundRegistrar.ENEMY.get());
    }


}
