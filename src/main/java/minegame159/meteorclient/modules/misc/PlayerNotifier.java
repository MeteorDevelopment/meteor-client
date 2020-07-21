package minegame159.meteorclient.modules.misc;

//Created by squidoodly 21/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerNotifier extends ToggleModule {
        public PlayerNotifier() {
            super(Category.Misc, "player-notifier", "Notifies you in chat when a player enters your render distance.");
        }

        private final SettingGroup sgGeneral = settings.getDefaultGroup();

        private final Setting<Boolean> notifyFriends = sgGeneral.add(new BoolSetting.Builder()
                .name("friend-notify")
                .description("Also notifies you when friends enter.")
                .defaultValue(false)
                .build()
        );

        @EventHandler
        private final Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
            if (!(event.entity instanceof PlayerEntity) || event.entity.getUuid().equals(mc.player.getUuid())) return;
            if (notifyFriends.get() && FriendManager.INSTANCE.get(event.entity.getName().toString()) != null) Chat.info(this, "Your friend (highlight)%s(default) entered your render distance", event.entity.getName());
            else Chat.info(this, "(highlight)%s(default) entered your render distance", event.entity.getName());
        });
}
