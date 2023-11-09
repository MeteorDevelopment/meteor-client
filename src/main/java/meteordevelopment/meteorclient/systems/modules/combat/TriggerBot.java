package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import nekiplay.meteorplus.MeteorPlus;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TriggerBot extends Module {
	public TriggerBot() {
		super(Categories.Combat, "Trigger-bot", "Attacks specified entities around you.");
	}

	@Override
	public void onDeactivate() {
		hitDelayTimer = 0;
		targets.clear();
	}

	private final SettingGroup sgGeneral = settings.getDefaultGroup();

	private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
		.name("entities")
		.description("Entities to attack.")
		.onlyAttackable()
		.build()
	);

	private final Setting<Boolean> babies = sgGeneral.add(new BoolSetting.Builder()
		.name("babies")
		.description("Whether or not to attack baby variants of the entity.")
		.defaultValue(true)
		.build()
	);

	private final Setting<Boolean> smartDelay = sgGeneral.add(new BoolSetting.Builder()
		.name("smart-delay")
		.description("Uses the vanilla cooldown to attack entities.")
		.defaultValue(true)
		.build()
	);

	private final Setting<Integer> hitDelay = sgGeneral.add(new IntSetting.Builder()
		.name("hit-delay")
		.description("How fast you hit the entity in ticks.")
		.defaultValue(0)
		.min(0)
		.sliderMax(60)
		.visible(() -> !smartDelay.get())
		.build()
	);

	private final Setting<Boolean> randomDelayEnabled = sgGeneral.add(new BoolSetting.Builder()
		.name("random-delay-enabled")
		.description("Adds a random delay between hits to attempt to bypass anti-cheats.")
		.defaultValue(false)
		.visible(() -> !smartDelay.get())
		.build()
	);

	private final Setting<Integer> randomDelayMax = sgGeneral.add(new IntSetting.Builder()
		.name("random-delay-max")
		.description("The maximum value for random delay.")
		.defaultValue(4)
		.min(0)
		.sliderMax(20)
		.visible(() -> randomDelayEnabled.get() && !smartDelay.get())
		.build()
	);

	private final List<Entity> targets = new ArrayList<>();

	private int hitDelayTimer;

	private boolean entityCheck(Entity entity) {
		if (entity.equals(mc.player) || entity.equals(mc.cameraEntity)) return false;
		if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
		if (!entities.get().contains(entity.getType())) return false;
		if (entity instanceof Tameable tameable
			&& tameable.getOwnerUuid() != null
			&& tameable.getOwnerUuid().equals(mc.player.getUuid())) return false;
		if (entity instanceof PlayerEntity) {
			if (((PlayerEntity) entity).isCreative()) return false;
			if (!Friends.get().shouldAttack((PlayerEntity) entity)) return false;
		}
		return !(entity instanceof AnimalEntity) || babies.get() || !((AnimalEntity) entity).isBaby();
	}

	private boolean delayCheck() {
		if (smartDelay.get()) return mc.player.getAttackCooldownProgress(0.5f) >= 1;


		if (hitDelayTimer > 0) {
			hitDelayTimer--;
			return false;
		} else {
			hitDelayTimer = hitDelay.get();
			if (randomDelayEnabled.get()) hitDelayTimer += Math.round(Math.random() * randomDelayMax.get());
			return true;
		}
	}

	@EventHandler
	private void onTick(TickEvent.Pre event) {
		if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameMode.SPECTATOR) return;
		if (mc.targetedEntity == null) return;
		MultiTasks multiTasks = Modules.get().get(MultiTasks.class);
		if (!multiTasks.isActive() && (mc.player.isUsingItem() || mc.interactionManager.isBreakingBlock())) return;

		if (delayCheck()) hitEntity(mc.targetedEntity);
	}

	private void hitEntity(Entity target) {
		mc.interactionManager.attackEntity(mc.player, target);
		mc.player.swingHand(Hand.MAIN_HAND);
	}
}
