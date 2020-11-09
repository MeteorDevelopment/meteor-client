package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.BoundingBox;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.utils.ISerializable;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.nbt.CompoundTag;

public abstract class HudModule implements ISerializable<HudModule> {
    public final String name, title;
    public final String description;

    public boolean active = true;

    protected final HUD hud;

    public final BoundingBox box = new BoundingBox();

    public HudModule(HUD hud, String name, String description) {
        this.hud = hud;
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
    }

    public abstract void update(HudRenderer renderer);

    public abstract void render(HudRenderer renderer);

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("name", name);
        tag.putBoolean("active", active);
        tag.put("box", box.toTag());

        return tag;
    }

    @Override
    public HudModule fromTag(CompoundTag tag) {
        active = tag.getBoolean("active");
        box.fromTag(tag.getCompound("box"));

        return this;
    }
}
