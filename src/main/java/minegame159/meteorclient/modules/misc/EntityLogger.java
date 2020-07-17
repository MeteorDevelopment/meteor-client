package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class EntityLogger extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entites")
            .description("Select specific entities.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> playerNames = sgGeneral.add(new BoolSetting.Builder()
            .name("player-names")
            .description("Show player names.")
            .defaultValue(true)
            .build()
    );

    public EntityLogger() {
        super(Category.Misc, "entity-logger", "Sends chat message when selected entities appear.");
    }

    @EventHandler
    private final Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (event.entity.getUuid().equals(mc.player.getUuid())) return;

        if (entities.get().contains(event.entity.getType())) {
            String name;
            if (playerNames.get() && event.entity instanceof PlayerEntity) name = ((PlayerEntity) event.entity).getGameProfile().getName() + " (Player)";
            else name = event.entity.getType().getName().asString();

            Chat.info(this, "(highlight)%s (default)spawned at (highlight)%.0f(default), (highlight)%.0f(default), (highlight)%.0f(default).", name, event.entity.x, event.entity.y, event.entity.z);
        }
    });
}
