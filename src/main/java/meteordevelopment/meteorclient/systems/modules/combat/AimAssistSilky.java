package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Set;

public class AimAssistSilky extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAim = settings.createGroup("瞄准手感");

    // ==================== 基础 ====================
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range").description("生效距离").defaultValue(5.0).min(0).max(8).build());

    private final Setting<Boolean> clickOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("click-only").description("仅按左键生效").defaultValue(true).build());
    
    private final Setting<Boolean> weaponOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("weapon-only").description("仅手持武器生效").defaultValue(true).build());

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities").description("目标").defaultValue(Set.of(EntityType.PLAYER)).build());

    // ==================== 防抖核心 ====================

    private final Setting<Double> speed = sgAim.add(new DoubleSetting.Builder()
        .name("aim-speed")
        .description("吸附速度 (建议 40-80，越高越快)")
        .defaultValue(60.0).min(1.0).max(150.0).build());

    private final Setting<Double> deadzone = sgAim.add(new DoubleSetting.Builder()
        .name("deadzone")
        .description("死区角度 (关键！防止抖动。建议 1.0 - 2.0，表示准星在目标边缘就不动了)")
        .defaultValue(1.5).min(0.1).max(5.0).build());

    private final Setting<Double> fov = sgGeneral.add(new DoubleSetting.Builder()
        .name("fov").description("FOV").defaultValue(90.0).min(10).max(180).build());

    private final Setting<Boolean> ignoreRecoil = sgAim.add(new BoolSetting.Builder()
        .name("ignore-recoil")
        .description("忽略垂直后坐力抖动").defaultValue(true).build());

    public AimAssistSilky() {
        super(Categories.Combat, "aim-assist-silky", "丝滑且不抖的自瞄");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        
        if (clickOnly.get() && !mc.options.attackKey.isPressed()) return;
        if (weaponOnly.get() && !isHoldingWeapon()) return;

        Entity target = getTarget();
        if (target == null) return;

        aim(target);
    }

    private void aim(Entity target) {
        // [修复] 使用 new Vec3d(x,y,z) 代替 getPos() 以兼容所有环境
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ()).add(0, target.getHeight() * 0.65, 0);
        
        double dx = targetPos.x - mc.player.getX();
        double dy = targetPos.y - mc.player.getEyeY();
        double dz = targetPos.z - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float desiredYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float desiredPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        float yawDelta = MathHelper.wrapDegrees(desiredYaw - mc.player.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(desiredPitch - mc.player.getPitch());

        // ==================== 防抖逻辑 ====================

        if (Math.abs(yawDelta) < deadzone.get()) yawDelta = 0;
        if (Math.abs(pitchDelta) < deadzone.get()) pitchDelta = 0;

        if (yawDelta == 0 && pitchDelta == 0) return;

        double smooth = Math.max(1.0, (150.0 - speed.get()) / 10.0);
        
        float moveYaw = (float) (yawDelta / smooth);
        float movePitch = (float) (pitchDelta / smooth);

        moveYaw = applyGCD(moveYaw);
        movePitch = applyGCD(movePitch);
        
        if (ignoreRecoil.get()) {
            movePitch *= 0.5f;
        }

        mc.player.changeLookDirection(moveYaw, movePitch);
    }

    private float applyGCD(float delta) {
        if (Math.abs(delta) < 0.001) return 0;

        double sensitivity = mc.options.getMouseSensitivity().getValue();
        double sensMult = sensitivity * 0.6 + 0.2;
        double gcd = sensMult * sensMult * sensMult * 1.2; 
        
        gcd *= 0.15 * 8.0; 

        double f = Math.round(delta / gcd) * gcd;
        return (float) f;
    }

    private Entity getTarget() {
        return java.util.stream.StreamSupport.stream(mc.world.getEntities().spliterator(), false)
            .filter(e -> e != mc.player)
            .filter(e -> e.isAlive())
            .filter(e -> entities.get().contains(e.getType()))
            .filter(e -> mc.player.distanceTo(e) <= range.get())
            .filter(e -> !Friends.get().shouldAttack((PlayerEntity) e) == false)
            .filter(e -> mc.player.canSee(e))
            .min(Comparator.comparingDouble(this::getAngleDiff))
            .filter(e -> getAngleDiff(e) <= fov.get() / 2.0)
            .orElse(null);
    }

    private double getAngleDiff(Entity e) {
        // [修复] 同样这里也替换为手动构建 Vec3d
        Vec3d targetPos = new Vec3d(e.getX(), e.getY(), e.getZ()).add(0, e.getHeight() * 0.5, 0);
        double dx = targetPos.x - mc.player.getX();
        double dy = targetPos.y - mc.player.getEyeY();
        double dz = targetPos.z - mc.player.getZ();
        
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        
        return Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw())) + 
               Math.abs(MathHelper.wrapDegrees(pitch - mc.player.getPitch()));
    }

    private boolean isHoldingWeapon() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandStack();
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.getItem() instanceof TridentItem;
    }
}