package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.misc.ThreadUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.concurrent.ThreadLocalRandom;

public class AutoSteal extends Module {
	public AutoSteal()
	{
		super(Category.Misc, "auto-steal", "Automatically loot chests.");
	}

	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgDelays = settings.createGroup("Delays");

	public final Setting<Integer> minimumDelay = sgDelays.add(new IntSetting.Builder()
			.name("min-delay")
			.description("Minimum delay between stealing the next stack in milliseconds. Use 0 to steal the entire inventory instantly")
			.min(0)
			.sliderMax(1000)
			.defaultValue(180)
			.build()
	);

	public final Setting<Integer> randomDelay = sgDelays.add(new IntSetting.Builder()
			.name("random-delay")
			.description("Randomly adds a delay of up to the specified time in milliseconds. Helps avoid anti-cheats.") // Actually ms - 1, due to the RNG excluding upper bound
			.min(0)
			.sliderMax(1000)
			.defaultValue(50)
			.build()
	);


	/**
	 * Runs {@link #steal(GenericContainerScreenHandler)} in a separate thread
	 * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
	 */
	public void stealAsync(GenericContainerScreenHandler handler)
	{
		ThreadUtils.runInThread(() -> steal(handler));
	}

	/**
	 * Thread-blocking operation to steal from containers. You REALLY should use {@link #stealAsync(GenericContainerScreenHandler)}
	 * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
	 */
	public void steal(GenericContainerScreenHandler handler)
	{
		for (int i = 0; i < handler.getRows() * 9; i++) {
			if (!handler.getSlot(i).hasStack())
				continue;

			int sleep = minimumDelay.get() + (randomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, randomDelay.get()) : 0);
			if (sleep > 0)
				ThreadUtils.sleep(sleep);

			// Exit if user closes screen
			if (mc.currentScreen == null)
				break;

			InvUtils.clickSlot(i, 0, SlotActionType.QUICK_MOVE);
		}
	}

	public void dumpAsync(GenericContainerScreenHandler handler)
	{
		ThreadUtils.runInThread(() -> dump(handler));
	}

	/**
	 * Thread-blocking operation to dump to containers. You REALLY should use {@link #dumpAsync(GenericContainerScreenHandler)}
	 * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
	 */
	public void dump(GenericContainerScreenHandler handler)
	{
		for (int i = handler.getRows() * 9; i < handler.getRows() * 9 + 9 + 3 * 9; i++) { // wtf?
			if (!handler.getSlot(i).hasStack())
				continue;

			int sleep = minimumDelay.get() + (randomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, randomDelay.get()) : 0);
			if (sleep > 0)
				ThreadUtils.sleep(sleep);

			// Exit if user closes screen
			if (mc.currentScreen == null)
				break;

			InvUtils.clickSlot(i, 0, SlotActionType.QUICK_MOVE);
		}
	}

}
