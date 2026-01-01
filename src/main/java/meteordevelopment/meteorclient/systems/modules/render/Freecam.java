/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;


import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPathing = settings.createGroup("Pathing");

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your speed while in freecam.")
        .onChanged(aDouble -> speedValue = aDouble)
        .defaultValue(1.0)
        .min(0.0)
        .build()
    );

    private final Setting<Double> speedScrollSensitivity = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed-scroll-sensitivity")
        .description("Allows you to change speed value using scroll wheel. 0 to disable.")
        .defaultValue(0)
        .min(0)
        .sliderMax(2)
        .build()
    );

    private final Setting<Boolean> staySneaking = sgGeneral.add(new BoolSetting.Builder()
        .name("stay-sneaking")
        .description("If you are sneaking when you enter freecam, whether your player should remain sneaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleOnDamage = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-damage")
        .description("Disables freecam when you take damage.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-death")
        .description("Disables freecam when you die.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOnLog = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-on-log")
        .description("Disables freecam when you disconnect from a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> reloadChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("reload-chunks")
        .description("Disables cave culling.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderHands = sgGeneral.add(new BoolSetting.Builder()
        .name("show-hands")
        .description("Whether or not to render your hands in freecam.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates to the block or entity you are looking at.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> staticView = sgGeneral.add(new BoolSetting.Builder()
        .name("static")
        .description("Disables settings that move the view.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> baritoneClick = sgPathing.add(new BoolSetting.Builder()
        .name("click-to-path")
        .description("Sets a pathfinding goal to any block/entity you click at.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> requireDoubleClick = sgPathing.add(new BoolSetting.Builder()
        .name("double-click")
        .description("Require two clicks to start pathing.")
        .defaultValue(false)
        .build()
    );

    public final Vector3d pos = new Vector3d();
    public final Vector3d prevPos = new Vector3d();

    private Perspective perspective;
    private double speedValue;

    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private double fovScale;
    private boolean bobView;

    private boolean forward, backward, right, left, up, down, isSneaking;
    
    // 保存玩家状态
    private double playerX, playerY, playerZ;
    private double playerVelX, playerVelY, playerVelZ;
    private boolean playerOnGround;
    private float playerFallDistance;

    private long clickTs = 0;

    public Freecam() {
        super(Categories.Render, "freecam", "Allows the camera to move away from the player.");
    }

    @Override
    public void onActivate() {
        fovScale = mc.options.getFovEffectScale().getValue();
        bobView = mc.options.getBobView().getValue();
        if (staticView.get()) {
            mc.options.getFovEffectScale().setValue((double)0);
            mc.options.getBobView().setValue(false);
        }
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();

        perspective = mc.options.getPerspective();
        speedValue = speed.get();

        // 保存玩家状态
        playerX = mc.player.getX();
        playerY = mc.player.getY();
        playerZ = mc.player.getZ();
        
        Vec3d velocity = mc.player.getVelocity();
        playerVelX = velocity.x;
        playerVelY = velocity.y;
        playerVelZ = velocity.z;
        
        playerOnGround = mc.player.isOnGround();
        playerFallDistance = (float) mc.player.fallDistance;

        Utils.set(pos, mc.gameRenderer.getCamera().getPos());
        Utils.set(prevPos, mc.gameRenderer.getCamera().getPos());

        if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            yaw += 180;
            pitch *= -1;
        }

        lastYaw = yaw;
        lastPitch = pitch;

        isSneaking = mc.options.sneakKey.isPressed();

        forward = Input.isPressed(mc.options.forwardKey);
        backward = Input.isPressed(mc.options.backKey);
        right = Input.isPressed(mc.options.rightKey);
        left = Input.isPressed(mc.options.leftKey);
        up = Input.isPressed(mc.options.jumpKey);
        down = Input.isPressed(mc.options.sneakKey);

        unpress();
        if (reloadChunks.get()) mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        if (reloadChunks.get()) {
            mc.execute(mc.worldRenderer::reload);
        }

        mc.options.setPerspective(perspective);

        if (staticView.get()) {
            mc.options.getFovEffectScale().setValue(fovScale);
            mc.options.getBobView().setValue(bobView);
        }

        // 恢复玩家状态
        if (mc.player != null) {
            mc.player.setPos(playerX, playerY, playerZ);
            
            Vec3d velocity = mc.player.getVelocity();
            ((IVec3d) velocity).meteor$set(playerVelX, playerVelY, playerVelZ);
            
            mc.player.setOnGround(playerOnGround);
            mc.player.fallDistance = playerFallDistance;
        }

        isSneaking = false;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        unpress();

        prevPos.set(pos);
        lastYaw = yaw;
        lastPitch = pitch;
    }

    private void unpress() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // 优化穿墙处理
        if (mc.getCameraEntity().isInsideWall()) {
            mc.getCameraEntity().noClip = true;
        }
        
        // 确保第一人称视角
        if (!perspective.isFirstPerson()) {
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }

        // 优化视角计算
        Vec3d forward = Vec3d.fromPolar(0, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);
        double velX = 0;
        double velY = 0;
        double velZ = 0;

        // 优化自动旋转逻辑
        if (rotate.get() && mc.crosshairTarget != null) {
            if (mc.crosshairTarget instanceof EntityHitResult) {
                Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
                Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), 0, null);
            } else if (mc.crosshairTarget instanceof BlockHitResult) {
                BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
                Vec3d hitPos = blockHit.getPos();
                BlockPos blockPos = blockHit.getBlockPos();

                if (!mc.world.getBlockState(blockPos).isAir()) {
                    Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), 0, null);
                }
            }
        }

        // 优化速度计算
        double speedMultiplier = Input.isPressed(mc.options.sprintKey) ? 1.0 : 0.5;
        double effectiveSpeed = speedMultiplier * speedValue;

        // 优化移动方向处理
        boolean movingForward = false;
        if (this.forward) {
            velX += forward.x * effectiveSpeed;
            velZ += forward.z * effectiveSpeed;
            movingForward = true;
        }
        if (this.backward) {
            velX -= forward.x * effectiveSpeed;
            velZ -= forward.z * effectiveSpeed;
            movingForward = true;
        }

        boolean movingSideways = false;
        if (this.right) {
            velX += right.x * effectiveSpeed;
            velZ += right.z * effectiveSpeed;
            movingSideways = true;
        }
        if (this.left) {
            velX -= right.x * effectiveSpeed;
            velZ -= right.z * effectiveSpeed;
            movingSideways = true;
        }

        // 优化对角线移动速度
        if (movingForward && movingSideways) {
            double diagonalFactor = 1 / Math.sqrt(2);
            velX *= diagonalFactor;
            velZ *= diagonalFactor;
        }

        // 优化垂直移动
        if (this.up) {
            velY += effectiveSpeed;
        }
        if (this.down) {
            velY -= effectiveSpeed;
        }

        // 更新位置
        prevPos.set(pos);
        pos.set(pos.x + velX, pos.y + velY, pos.z + velZ);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKey(KeyEvent event) {
        // 优先检查F3键，避免调试时的干扰
        if (event.key() == GLFW.GLFW_KEY_F3) return;
        
        // 检查GUI移动设置
        if (checkGuiMove()) return;

        // 处理输入并取消事件
        if (onInput(event.key(), event.action)) {
            event.cancel();
        }
    }

    @Nullable
    private BlockPos rayCastEntity(Vec3d posVec, Vec3d max, short maxDist) {
        EntityHitResult res = ProjectileUtil.raycast(
            mc.player,
            posVec,
            max,
            Box.enclosing(BlockPos.ofFloored(posVec.x, posVec.y, posVec.z), BlockPos.ofFloored(max.x, max.y, max.z)),
            (entity) -> true,
            maxDist
        );

        if (res == null) return null;

        Vec3d vec = res.getPos();

        return BlockPos.ofFloored(vec.x, vec.y, vec.z);
    }

    @Nullable
    private BlockPos rayCastBlock(Vec3d posVec, Vec3d max) {
        RaycastContext ctx = new RaycastContext(
            posVec,
            max,
            RaycastContext.ShapeType.VISUAL,
            RaycastContext.FluidHandling.SOURCE_ONLY,
            ShapeContext.absent()
        );

        BlockHitResult res = mc.world.raycast(ctx);
        if (res.getType() == HitResult.Type.MISS) return null;

        // Don't move inside block
        return res.getBlockPos().add(res.getSide().getVector());
    }

    private void setGoal() {
        long prevClick = clickTs;
        clickTs = System.currentTimeMillis();

        if (requireDoubleClick.get() && clickTs - prevClick > 500) return;

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d posVec = cam.getPos();
        Vec3d lookVec = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
        short maxDist = 256;
        Vec3d max = posVec.add(lookVec.multiply(maxDist));

        BlockPos pos = rayCastEntity(posVec, max, maxDist);
        if (pos == null) {
            pos = rayCastBlock(posVec, max);
        }

        if (pos == null) return;

        PathManagers.get().moveTo(pos);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseClick(MouseClickEvent event) {
        // 检查GUI移动设置
        if (checkGuiMove()) return;

        // 优化Baritone路径设置逻辑
        if (baritoneClick.get() && event.action == KeyAction.Press && mc.options.attackKey.matchesMouse(event.click)) {
            setGoal();
        }

        // 处理鼠标输入并取消事件
        if (onInput(event.button(), event.action)) {
            event.cancel();
        }
    }

    private boolean onInput(int key, KeyAction action) {
        boolean isPressed = action != KeyAction.Release;
        
        // 优化输入处理逻辑，使用更清晰的条件结构
        if (Input.getKey(mc.options.forwardKey) == key) {
            forward = isPressed;
            mc.options.forwardKey.setPressed(false);
        } else if (Input.getKey(mc.options.backKey) == key) {
            backward = isPressed;
            mc.options.backKey.setPressed(false);
        } else if (Input.getKey(mc.options.rightKey) == key) {
            right = isPressed;
            mc.options.rightKey.setPressed(false);
        } else if (Input.getKey(mc.options.leftKey) == key) {
            left = isPressed;
            mc.options.leftKey.setPressed(false);
        } else if (Input.getKey(mc.options.jumpKey) == key) {
            up = isPressed;
            mc.options.jumpKey.setPressed(false);
        } else if (Input.getKey(mc.options.sneakKey) == key) {
            down = isPressed;
            mc.options.sneakKey.setPressed(false);
        } else {
            // 不是相关按键，不处理
            return false;
        }

        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onMouseScroll(MouseScrollEvent event) {
        // 优化速度滚动调整逻辑，提供更平滑的体验
        if (speedScrollSensitivity.get() > 0 && mc.currentScreen == null) {
            speedValue += event.value * 0.25 * (speedScrollSensitivity.get() * speedValue);
            speedValue = Math.max(speedValue, 0.1); // 确保速度不会低于0.1
            event.cancel();
        }
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }



    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (!toggleOnLog.get()) return;

        toggle();
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        // 防止玩家在Freecam模式下移动，保持位置稳定
        if (mc.player != null) {
            // 使用正确的方式修改移动
            ((IVec3d) event.movement).meteor$set(0, 0, 0);
            
            // 保持玩家在原来的位置，防止重力影响
            mc.player.setPos(playerX, playerY, playerZ);
            
            // 保持玩家原来的地面状态和下落距离
            mc.player.setOnGround(playerOnGround);
            mc.player.fallDistance = playerFallDistance;
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event)  {
        if (event.packet instanceof DeathMessageS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.playerId());
            if (entity == mc.player && toggleOnDeath.get()) {
                toggle();
                info("Toggled off because you died.");
            }
        }
        else if (event.packet instanceof HealthUpdateS2CPacket packet) {
            if (mc.player.getHealth() - packet.getHealth() > 0 && toggleOnDamage.get()) {
                toggle();
                info("Toggled off because you took damage.");
            }
        }
        else if (event.packet instanceof PlayerRespawnS2CPacket) {
            if (isActive()) {
                toggle();
                info("Toggled off because you changed dimensions.");
            }
        }
    }

    private boolean checkGuiMove() {
        // 优化GUI移动检查逻辑
        if (mc.currentScreen == null) return false;
        
        GUIMove guiMove = Modules.get().get(GUIMove.class);
        return !guiMove.isActive() || guiMove.skip();
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;

        yaw += (float) deltaX;
        pitch += (float) deltaY;

        pitch = MathHelper.clamp(pitch, -90, 90);
    }

    public boolean renderHands() {
        // 控制是否在Freecam模式下渲染玩家手部
        return !isActive() || renderHands.get();
    }

    public boolean staySneaking() {
        // 优化潜行状态保持逻辑
        return isActive() && !mc.player.getAbilities().flying && staySneaking.get() && isSneaking;
    }

    public double getX(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPos.x, pos.x);
    }
    public double getY(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPos.y, pos.y);
    }
    public double getZ(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPos.z, pos.z);
    }

    public double getYaw(float tickDelta) {
        return MathHelper.lerp(tickDelta, lastYaw, yaw);
    }
    public double getPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, lastPitch, pitch);
    }
}
