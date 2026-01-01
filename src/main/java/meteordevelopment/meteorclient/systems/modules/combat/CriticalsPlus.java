package meteordevelopment.meteorclient.systems.modules.combat;
 
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Criticals;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public class CriticalsPlus extends Module {
	public CriticalsPlus() {
		super(Categories.Combat, "Criticals+", "Better criticals module with enhanced functionality");
	}

	private static MinecraftClient mc = MinecraftClient.getInstance();

	/**
	 * 检查玩家是否已经处于可以暴击的状态
	 */
	public static boolean canCrit() {
		return !mc.player.isOnGround() && mc.player.fallDistance > 0 && !mc.player.isClimbing() && !mc.player.isSubmergedInWater() && !mc.player.isInLava();
	}

	/**
	 * 检查是否应该跳过暴击尝试
	 */
	public static boolean skipCrit() {
		return !mc.player.isOnGround() || mc.player.isSubmergedInWater() || mc.player.isInLava() || mc.player.isClimbing() || mc.player.isTouchingWater();
	}

	/**
	 * 检查是否允许进行暴击攻击
	 */
	public static boolean allowCrit() {
		if (canCrit()) {
			return true;
		}
		else if (Modules.get().get(Criticals.class).isActive()) {
            return !skipCrit();
		}
		return false;
	}

    /**
     * 判断目标是否需要暴击攻击（即目标血量大于等于普通攻击伤害）
     */
    public static boolean needCrit(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getHealth() >= DamageUtils.getAttackDamage(mc.player, livingEntity);
        }
        return false;
    }

    /**
     * 执行暴击操作
     */
    public static void doCrit() {
        if (mc.player == null) return;
        if (canCrit()) return;
        
        var criticals = Modules.get().get(Criticals.class);
        if (criticals != null && criticals.isActive()) {
            if (!skipCrit() && mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
    }

    /**
     * 更高级的暴击判断，考虑目标血量和当前是否适合暴击
     */
    public static boolean shouldAttemptCrit(LivingEntity target) {
        if (target == null || mc.player == null) return false;
        
        // 如果已经在半空可以暴击，直接返回true
        if (canCrit()) return true;
        
        // 检查是否需要暴击来确保击杀
        if (needCrit(target)) {
            // 检查是否可以安全地进行暴击（在地面上，不在水中等）
            if (!skipCrit() && mc.player.isOnGround()) {
                return true;
            }
        }
        
        return false;
    }
}