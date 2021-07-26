package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.text.Text;

public class EntityMountWarning extends Module {
	public EntityMountWarning() {
		super(Categories.Misc, "mount-warn", "Warns you if you mount entity illegally.");
	}

	public void warn() {
		info("WARNING!!! Mounting entity in midair can get you kicked!!!");
		Text text = Text.of("Warning");
		Text subtext = Text.of("Mounted illegally. Dismount to not get kicked.");
		mc.inGameHud.setTitle(text);
		mc.inGameHud.setSubtitle(subtext);
	}
}
