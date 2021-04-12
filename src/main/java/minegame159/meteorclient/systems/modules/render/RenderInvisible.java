package minegame159.meteorclient.systems.modules.render;

import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

public class RenderInvisible extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> entities = sgGeneral.add(new BoolSetting.Builder()
			.name("entities")
			.description("Render invisible entities.")
			.defaultValue(true)
			.build()
	);

    private final Setting<Boolean> barrier = sgGeneral.add(new BoolSetting.Builder()
			.name("barrier")
			.description("Render barrier blocks.")
			.defaultValue(true)
            .onChanged(onChanged -> {
                if(this.isActive()) {
                    mc.worldRenderer.reload();
                }
            })
			.build()
	);

    private final Setting<Boolean> structureVoid = sgGeneral.add(new BoolSetting.Builder()
			.name("structure-void")
			.description("Render structure void blocks.")
			.defaultValue(true)
            .onChanged(onChanged -> {
                if(this.isActive()) {
                    mc.worldRenderer.reload();
                }
            })
			.build()
	);

    public RenderInvisible() {
        super(Categories.Render, "render-invisible", "Renders invisible entities and blocks.");
    }
    
    public boolean renderEntities() {
        return this.isActive() && entities.get();
    }

    public boolean renderBarriers() {
        return this.isActive() && barrier.get();
    }

    public boolean renderStructureVoid() {
        return this.isActive() && structureVoid.get();
    }
}
