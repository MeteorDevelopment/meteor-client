package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class EntityTypeListSettingScreen extends WindowScreen {
    private final Setting<List<EntityType<?>>> setting;
    private final WTextBox filter;

    public EntityTypeListSettingScreen(Setting<List<EntityType<?>>> setting) {
        super("Select entities", true);
        this.setting = setting;

        // Filter
        filter = new WTextBox("", 0);
        filter.setFocused(true);
        filter.action = textBox -> {
            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        add(filter).fillX().expandX();
        row();

        WTable table = add(new WTable()).fillX().expandX().getWidget();
        Registry.ENTITY_TYPE.forEach(entityType -> {
            if (StringUtils.containsIgnoreCase(entityType.getName().asString(), filter.text)) addEntityType(table, entityType);
        });
    }

    private void addEntityType(WTable table, EntityType<?> entityType) {
        table.add(new WLabel(entityType.getName().asString()));
        table.add(new WCheckbox(setting.get().contains(entityType))).getWidget().action = checkbox -> {
            if (checkbox.checked) {
                if (!setting.get().contains(entityType)) setting.get().add(entityType);
            } else {
                setting.get().remove(entityType);
            }

            setting.changed();
        };

        table.row();
    }
}
