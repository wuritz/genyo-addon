package com.genyo.addon.modules;

import com.genyo.addon.GenyoAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.*;

public class GenyoAutoEZ extends Module {

    public GenyoAutoEZ() {
        super(GenyoAddon.GENYO, "genyo-auto-ez", "igen igen igen, dikta mamo tyibori.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    //--------------------General--------------------//
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Enemy Range")
        .description("Only send message if enemy died inside this range.")
        .defaultValue(25)
        .min(0)
        .sliderRange(0, 50)
        .build()
    );
    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .description("How many ticks to wait between sending messages.")
        .defaultValue(50)
        .min(0)
        .sliderRange(0, 100)
        .build()
    );

    private final Setting<Boolean> pop = sgGeneral.add(new BoolSetting.Builder()
        .name("Pop")
        .description("nyugi ez nem csinál semmit, csak dísznek van XD")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> trackPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("Track Players")
        .description("követi, hogy kit öltél meg ewkgnwekjghhewjkfhew")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> popMessages = sgGeneral.add(new StringListSetting.Builder()
        .name("Pop Messages")
        .description("Messages to send when popping an enemy")
        .defaultValue(List.of("ez pop <NAME> <COUNT>", "pop <NAME> <COUNT>", "i love kiwi pop <NAME> <COUNT>"))
        .build()
    );

    private final Random r = new Random();
    private int lastPop;
    private final List<Message> messageQueue = new LinkedList<>();
    private final HashMap<PlayerEntity, Integer> taggedPlayers = new HashMap<>();
    private int timer = 0;

    @Override
    public void onActivate() {
        super.onActivate();
        taggedPlayers.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        timer++;
        if (mc.player != null && mc.world != null) {
            if (timer >= tickDelay.get() && !messageQueue.isEmpty()) {
                Message msg = messageQueue.get(0);
                ChatUtils.sendPlayerMsg(msg.message);
                timer = 0;

                if (msg.kill) messageQueue.clear();
                else messageQueue.removeFirst();
            }
        }
    }

    @EventHandler
    private void onReceive(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            // Pop
            if (packet.getStatus() == 35) {
                Entity entity = packet.getEntity(mc.world);
                if (mc.player != null && mc.world != null && entity instanceof PlayerEntity playerEntity) {
                    if (entity != mc.player && mc.player.getPos().distanceTo(entity.getPos()) <= range.get()) {

                        if (trackPlayers.get() && taggedPlayers.containsKey(playerEntity)) {
                            int count =  taggedPlayers.get(playerEntity) + 1;

                            taggedPlayers.replace(playerEntity, count);
                            sendPopMessage(playerEntity.getName().getString(), count);
                        } else {
                            sendPopMessage(playerEntity.getName().getString(), 0);
                            taggedPlayers.put(playerEntity, 1);
                        }
                    }
                }
            }
        }
    }

    private void sendPopMessage(String name, int count) {
        if (!popMessages.get().isEmpty()) {
            int num = r.nextInt(0, popMessages.get().size());
            if (num == lastPop) {
                num = num < popMessages.get().size() - 1 ? num + 1 : 0;
            }
            lastPop = num;
            String messageString = popMessages.get().get(num).replace("<NAME>", name);
            String countString = String.valueOf(count);

            if (count > 0) {
                messageString = messageString.replace("<COUNT>", "+" + countString);
            } else {
                messageString = messageString.replace("<COUNT>", "+1");
            }

            Message message = new Message(messageString, false);
            messageQueue.add(message);
        }
    }

    private record Message(String message, boolean kill) {
    }
}
