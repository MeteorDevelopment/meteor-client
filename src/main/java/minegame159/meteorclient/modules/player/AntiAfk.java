package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AntiAfk extends ToggleModule {

    public AntiAfk() {
        super(Category.Player, "Anti-Afk", "Performs different actions to prevent getting kicked for AFK.");
    }

    private final SettingGroup sgActions = settings.createGroup("Actions");
    private final SettingGroup sgMessages = settings.createGroup("Messages");

    // Actions
    private final Setting<Boolean> spin = sgActions.add(new BoolSetting.Builder()
            .name("spin")
            .description("Spins.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> spinSpeed = sgActions.add(new IntSetting.Builder()
            .name("spin-speed")
            .description("The speed to spin you.")
            .defaultValue(7)
            .min(1)
            .max(10)
            .build()
    );

    private final Setting<Boolean> jump = sgActions.add(new BoolSetting.Builder()
            .name("jump")
            .description("Jumps.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> click = sgActions.add(new BoolSetting.Builder()
            .name("click")
            .description("Clicks.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> disco = sgActions.add(new BoolSetting.Builder()
            .name("disco")
            .description("Sneaks.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> strafe = sgActions.add(new BoolSetting.Builder()
            .name("strafe")
            .description("Strafe right and left")
            .defaultValue(false)
            .onChanged(aBoolean -> {
                strafeTimer = 0;
                direction = false;
            })
            .build());

    // Messages
    private final Setting<Boolean> sendMessages = sgMessages.add(new BoolSetting.Builder()
            .name("send-messages")
            .description("Sends messages to prevent getting kicked for AFK.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> delay = sgMessages.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between specified messages in ticks.")
            .defaultValue(20)
            .min(0)
            .sliderMax(500)
            .build()
    );

    private final Setting<Boolean> randomMessage = sgMessages.add(new BoolSetting.Builder()
            .name("random")
            .description("Selects a random message from your message list.")
            .defaultValue(false)
            .build()
    );

    private final List<String> messages = new ArrayList<>();
    private int timer;
    private int messageI;
    private int strafeTimer = 0;
    private boolean direction = false;

    private final Random random = new Random();

    @SuppressWarnings("unused")
    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (mc.player != null && mc.world != null) {
            if (spin.get())
                mc.player.yaw = (mc.player.yaw >= 360) ? 0 : mc.player.yaw + random.nextInt(spinSpeed.get()) + 1;
            if (jump.get() && mc.options.keyJump.isPressed())
                ((IKeyBinding) mc.options.keyJump).setPressed(false);
            else if (jump.get() && random.nextInt(99) + 1 == 50)
                ((IKeyBinding) mc.options.keyJump).setPressed(true);
            if (click.get() && random.nextInt(99) + 1 == 45) {
                mc.options.keyAttack.setPressed(true);
                Utils.leftClick();
                mc.options.keyAttack.setPressed(false);
            }
            if (jump.get() && mc.options.keySneak.isPressed())
                ((IKeyBinding) mc.options.keySneak).setPressed(false);
            else if (disco.get() && random.nextInt(24) + 1 == 15) {
                ((IKeyBinding) mc.options.keySneak).setPressed(true);
            }
            if (messages.isEmpty()) return;

            if (sendMessages.get())
                if (timer <= 0) {
                    int i;
                    if (randomMessage.get()) {
                        i = Utils.random(0, messages.size());
                    } else {
                        if (messageI >= messages.size()) messageI = 0;
                        i = messageI++;
                    }

                    mc.player.sendChatMessage(messages.get(i));

                    timer = delay.get();
                } else {
                    timer--;
                }

            if (strafe.get() && strafeTimer == 20) {
                ((IKeyBinding) mc.options.keyLeft).setPressed(!direction);
                ((IKeyBinding) mc.options.keyRight).setPressed(direction);
                direction = !direction;
                strafeTimer = 0;
            } else
                strafeTimer++;

        }
    });

    @Override
    public WWidget getWidget() {
        messages.removeIf(String::isEmpty);

        WTable table = new WTable();
        fillTable(table);
        return table;
    }

    private void fillTable(WTable table) {
        table.add(new WHorizontalSeparator("Message List"));

        // Messages
        for (int i = 0; i < messages.size(); i++) {
            int msgI = i;
            String message = messages.get(i);

            WTextBox textBox = table.add(new WTextBox(message, 100)).fillX().expandX().getWidget();
            textBox.action = () -> messages.set(msgI, textBox.getText());

            WMinus minus = table.add(new WMinus()).getWidget();
            minus.action = () -> {
                messages.remove(msgI);

                table.clear();
                fillTable(table);
            };

            table.row();
        }

        // New Message
        WPlus plus = table.add(new WPlus()).fillX().right().getWidget();
        plus.action = () -> {
            messages.add("");

            table.clear();
            fillTable(table);
        };
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();

        messages.removeIf(String::isEmpty);
        ListTag messagesTag = new ListTag();

        for (String message : messages) messagesTag.add(StringTag.of(message));
        tag.put("messages", messagesTag);

        return tag;
    }

    @Override
    public ToggleModule fromTag(CompoundTag tag) {
        messages.clear();

        if (tag.contains("messages")) {
            ListTag messagesTag = tag.getList("messages", 8);
            for (Tag messageTag : messagesTag) messages.add(messageTag.asString());
        } else {
            messages.add("This is an AntiAFK message. Meteor on Crack!");
        }

        return super.fromTag(tag);
    }
}