package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WCheckbox;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.misc.Names;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ParticleEffectListSettingScreen extends WindowScreen {

    private final Setting<List<ParticleEffect>> setting;
    private final WTextBox filter;
    private String filterText = "";

    public ParticleEffectListSettingScreen(Setting<List<ParticleEffect>> setting) {
        super("Particle Effects", true);
        this.setting = setting;

        filter = new WTextBox("", 400);
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.getText().trim();

            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {

        add(filter).fillX().expandX().getWidget();
        row();

        for (ParticleType<?> particleType : Registry.PARTICLE_TYPE) {
            if (!(particleType instanceof ParticleEffect)) continue;

            ParticleEffect effect = (ParticleEffect) particleType;
            String name = Names.get(effect);

            if (!filterText.isEmpty()) if (!StringUtils.containsIgnoreCase(name, filterText)) continue;

            add(new WLabel(name));
            WCheckbox checkbox = add(new WCheckbox(setting.get().contains(effect))).fillX().right().getWidget();
            checkbox.action = () -> {
                if (checkbox.checked && !setting.get().contains(effect)) {
                    setting.get().add(effect);
                    setting.changed();
                } else if (!checkbox.checked && setting.get().remove(effect)) {
                    setting.changed();
                }
            };

            row();
        }
    }
}