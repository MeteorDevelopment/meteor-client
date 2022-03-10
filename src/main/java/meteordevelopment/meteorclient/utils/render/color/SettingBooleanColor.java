package meteordevelopment.meteorclient.utils.render.color;

import net.minecraft.nbt.NbtCompound;

public class SettingBooleanColor {
	SettingColor color = null;
	boolean active = false;

	public SettingBooleanColor() {}

	public SettingBooleanColor(SettingColor color, boolean active) {
		this.color = color;
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public SettingColor getColor() {
		return color;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setColor(SettingColor color) {
		this.color = color;
	}

	public NbtCompound toTag() {
		NbtCompound tag = (color != null) ? color.toTag() : new NbtCompound();
		if (active) tag.putBoolean("active", true);
		return tag;
	}

	public SettingBooleanColor fromTag(NbtCompound tag) {
		if (tag.contains("active"))
			this.active = tag.getBoolean("active");
		if (tag.contains("rainbow"))
			this.color = new SettingColor().fromTag(tag);
		return this;
	}
}
