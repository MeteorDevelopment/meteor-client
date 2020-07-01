package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.entity.EntityType;

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

    public EntityLogger() {
        super(Category.Misc, "entity-logger", "Sends chat message when selected entities appear.");
    }

    @EventHandler
    private final Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (entities.get().contains(event.entity.getType())) {
            Utils.sendMessage(String.format("#blue[Meteor]: #white%s #grayspawned at #white%.0f#gray, #white%.0f#gray, #white%.0f#gray.", event.entity.getType().getName().getString(), event.entity.getX(), event.entity.getY(), event.entity.getZ()));
        }
    });
}
