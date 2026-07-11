package com.genyo.core;

import com.genyo.managers.Managers;
import com.genyo.core.sound.SoundManager;
import com.genyo.systems.config.GenyoConfig;
import com.genyo.systems.enemies.Enemies;
import com.genyo.utils.math.timer.CacheTimer;
import com.genyo.utils.math.timer.Timer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Random;

import static com.genyo.core.sound.SoundManager.*;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class Core {

    private final Timer soundTimer = new CacheTimer();
    private final Random r = new Random();

    public Core() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (!mc.inGameHud.getChatHud().isChatFocused()) return;

        if (event.action == KeyAction.Press && GenyoConfig.get().typing.get()) {
            Managers.SOUND.playSound(SoundManager.KEYPRESS, (int) humanizeVolume(0.8f, 1f), humanizePitch(0.8f, 1.2f));
        }
    }

    // these make 'typing' sound more realistic because of random values

    private float humanizePitch(float min, float max) {
        return min + r.nextFloat() * (max - min);
    }

    private float humanizeVolume(float min, float max) {
        float globalTypingVolume = (float) GenyoConfig.get().typingVolume.get() / 100f;
        float randomed = (min + r.nextFloat() * (max - min));
        float returnValue = ((randomed * globalTypingVolume) * 100f);

        return returnValue;
    }

    @EventHandler
    public void onMessageReceive(ReceiveMessageEvent event) {
        String message = event.getMessage().getString().toLowerCase();
        GenyoConfig cfg = GenyoConfig.get();
        if (cfg == null) return;

        if (message.contains("genyo") && !message.startsWith("§") && cfg.vineSound.get()) {              // genyo
            Managers.SOUND.playSound(VINE, cfg.vineVolume.get());
        } else if (message.contains("verstappen") && cfg.verstappenSound.get()) {                        // verstappen
            Managers.SOUND.playSound(VERSTAPPEN, cfg.verstappenVolume.get());
        } else if ((message.contains("nigga") || message.contains("nigger")) && cfg.blackPerson.get()) { // n
            if (soundTimer.passed(6000)) {
                Managers.SOUND.playSound(BLACK, 10);
                soundTimer.reset();
            }
        } else if (message.contains("kiwi") && cfg.kiwiSound.get()) {                                    // kiwi
            Managers.SOUND.playSound(KIWI, cfg.kiwiVolume.get());
        }
    }

    @EventHandler
    public void onPlayerRender(EntityAddedEvent event) {
        if (!(event.entity instanceof PlayerEntity player)) return;
        if (!Enemies.get().isEnemy(player)) return;

        GenyoConfig cfg = GenyoConfig.get();
        if (cfg != null && cfg.hamburgerSound.get()) {
            Managers.SOUND.playSound(HAMBURGER, cfg.hamburgerVolume.get());
        }
    }

}
