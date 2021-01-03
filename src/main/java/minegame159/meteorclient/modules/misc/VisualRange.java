package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.entity.EntityAddedEvent;
import minegame159.meteorclient.events.entity.EntityRemovedEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.player.Chat;
import net.minecraft.entity.player.PlayerEntity;

public class VisualRange extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignores friends.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreFakes = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-fakeplayers")
            .description("Ignores fake players.")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> enterMessage = sgGeneral.add(new StringSetting.Builder()
            .name("enter-message")
            .description("The message for when a player enters your visual range.")
            .defaultValue("{player} has entered your visual range.")
            .build()
    );

    private final Setting<String> leaveMessage = sgGeneral.add(new StringSetting.Builder()
            .name("leave-message")
            .description("The message for when a player leaves your visual range.")
            .defaultValue("{player} has left your visual range.")
            .build()
    );


    public VisualRange() {
        super(Category.Misc, "visual-range", "Notifies you when a player enters/leaves your visual range.");
    }

    @EventHandler
    private final Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (event.entity.equals(mc.player) || !(event.entity instanceof PlayerEntity) || !FriendManager.INSTANCE.attack((PlayerEntity) event.entity) && ignoreFriends.get() || (event.entity instanceof FakePlayerEntity && ignoreFakes.get())) return;

        String enter = enterMessage.get().replace("{player}", ((PlayerEntity) event.entity).getGameProfile().getName());
        Chat.info(this, enter);
    });

    @EventHandler
    private final Listener<EntityRemovedEvent> onEntityRemoved = new Listener<>(event -> {
        if (event.entity.equals(mc.player) || !(event.entity instanceof PlayerEntity) || !FriendManager.INSTANCE.attack((PlayerEntity) event.entity) && ignoreFriends.get() || (event.entity instanceof FakePlayerEntity && ignoreFakes.get())) return;

        String leave = leaveMessage.get().replace("{player}", ((PlayerEntity) event.entity).getGameProfile().getName());
        Chat.info(this, leave);
    });
}
