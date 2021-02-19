package minegame159.meteorclient.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.entity.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;


public class AntiBot extends Module {

	private final SettingGroup sgGeneral = settings.getDefaultGroup();

	private final Setting<Boolean> removeInvisible = sgGeneral.add(new BoolSetting.Builder()
			.name("remove-invisible")
			.description("Removes bot only if they are invisible.")
			.defaultValue(true)
			.build()
	);

	public AntiBot()
	{
		super(Categories.Misc, "anti-bot", "Detects and removes bots.");
	}

	@EventHandler
	public void onTick(TickEvent.Post tickEvent)
	{
		for (Entity entity : mc.world.getEntities())
		{
			if (removeInvisible.get() && !entity.isInvisible()) continue;

			if (isBot(entity)) entity.remove();
		}
	}

	private boolean isBot(Entity entity)
	{
		if (entity == null) return false;
		if (!(entity instanceof PlayerEntity)) return false;

		// Gamemode check
		return EntityUtils.getGameMode(((PlayerEntity)entity)) == null; // Assume bot if invalid gamemode
	}
}
