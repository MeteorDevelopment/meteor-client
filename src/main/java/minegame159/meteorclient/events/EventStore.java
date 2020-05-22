package minegame159.meteorclient.events;

import minegame159.meteorclient.events.packets.PlaySoundPacketEvent;
import minegame159.meteorclient.events.packets.ReceivePacketEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.Pool;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

public class EventStore {
    private static PlaySoundPacketEvent playSoundPacketEvent = new PlaySoundPacketEvent();
    private static SendPacketEvent sendPacketEvent = new SendPacketEvent();
    private static ActiveModulesChangedEvent activeModulesChangedEvent = new ActiveModulesChangedEvent();
    private static CharTypedEvent charTypedEvent = new CharTypedEvent();
    private static EntityAddedEvent entityAddedEvent = new EntityAddedEvent();
    private static EntityRemovedEvent entityRemovedEvent = new EntityRemovedEvent();
    private static KeyEvent keyEvent = new KeyEvent();
    private static ModuleBindChangedEvent moduleBindChangedEvent = new ModuleBindChangedEvent();
    private static ModuleVisibilityChangedEvent moduleVisibilityChangedEvent = new ModuleVisibilityChangedEvent();
    private static OpenScreenEvent openScreenEvent = new OpenScreenEvent();
    private static Render2DEvent render2DEvent = new Render2DEvent();
    private static RenderEvent renderEvent = new RenderEvent();
    private static TickEvent tickEvent = new TickEvent();
    private static TookDamageEvent tookDamageEvent = new TookDamageEvent();
    private static GameJoinedEvent gameJoinedEvent = new GameJoinedEvent();
    private static GameDisconnectedEvent gameDisconnectedEvent = new GameDisconnectedEvent();
    private static MiddleMouseButtonEvent middleMouseButtonEvent = new MiddleMouseButtonEvent();
    private static FriendListChangedEvent friendListChangedEvent = new FriendListChangedEvent();
    private static MacroListChangedEvent macroListChangedEvent = new MacroListChangedEvent();
    private static ReceivePacketEvent receivePacketEvent = new ReceivePacketEvent();
    private static PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent();
    private static AccountListChangedEvent accountListChangedEvent = new AccountListChangedEvent();
    private static Pool<ChunkDataEvent> chunkDataEventPool = new Pool<>(ChunkDataEvent::new);
    private static AttackEntityEvent attackEntityEvent = new AttackEntityEvent();
    private static StartBreakingBlockEvent startBreakingBlockEvent = new StartBreakingBlockEvent();
    private static EntityDestroyEvent entityDestroyEvent = new EntityDestroyEvent();
    private static DamageEvent damageEvent = new DamageEvent();
    private static RightClickEvent rightClickEvent = new RightClickEvent();

    public static PlaySoundPacketEvent playSoundPacketEvent(PlaySoundS2CPacket packet) {
        playSoundPacketEvent.packet = packet;
        return playSoundPacketEvent;
    }

    public static SendPacketEvent sendPacketEvent(Packet<?> packet) {
        sendPacketEvent.setCancelled(false);
        sendPacketEvent.packet = packet;
        return sendPacketEvent;
    }

    public static ActiveModulesChangedEvent activeModulesChangedEvent() {
        return activeModulesChangedEvent;
    }

    public static CharTypedEvent charTypedEvent(char c) {
        charTypedEvent.setCancelled(false);
        charTypedEvent.c = c;
        return charTypedEvent;
    }

    public static EntityAddedEvent entityAddedEvent(Entity entity) {
        entityAddedEvent.entity = entity;
        return entityAddedEvent;
    }

    public static EntityRemovedEvent entityRemovedEvent(Entity entity) {
        entityRemovedEvent.entity = entity;
        return entityRemovedEvent;
    }

    public static KeyEvent keyEvent(int key, boolean push) {
        keyEvent.setCancelled(false);
        keyEvent.key = key;
        keyEvent.push = push;
        return keyEvent;
    }

    public static ModuleBindChangedEvent moduleBindChangedEvent(Module module) {
        moduleBindChangedEvent.module = module;
        return moduleBindChangedEvent;
    }

    public static ModuleVisibilityChangedEvent moduleVisibilityChangedEvent(ToggleModule module) {
        moduleVisibilityChangedEvent.module = module;
        return moduleVisibilityChangedEvent;
    }

    public static OpenScreenEvent openScreenEvent(Screen screen) {
        openScreenEvent.setCancelled(false);
        openScreenEvent.screen = screen;
        return openScreenEvent;
    }

    public static Render2DEvent render2DEvent(int screenWidth, int screenHeight, float tickDelta) {
        render2DEvent.screenWidth = screenWidth;
        render2DEvent.screenHeight = screenHeight;
        render2DEvent.tickDelta = tickDelta;
        return render2DEvent;
    }

    public static RenderEvent renderEvent(MatrixStack matrixStack, float tickDelta, double offsetX, double offsetY, double offsetZ) {
        renderEvent.matrixStack = matrixStack;
        renderEvent.tickDelta = tickDelta;
        renderEvent.offsetX = offsetX;
        renderEvent.offsetY = offsetY;
        renderEvent.offsetZ = offsetZ;
        return renderEvent;
    }

    public static TickEvent tickEvent() {
        return tickEvent;
    }

    public static TookDamageEvent tookDamageEvent(LivingEntity entity, DamageSource source) {
        tookDamageEvent.entity = entity;
        tookDamageEvent.source = source;
        return tookDamageEvent;
    }

    public static GameJoinedEvent gameJoinedEvent() {
        return gameJoinedEvent;
    }

    public static GameDisconnectedEvent gameDisconnectedEvent(Text disconnectReason) {
        gameDisconnectedEvent.disconnectReason = disconnectReason;
        return gameDisconnectedEvent;
    }

    public static MiddleMouseButtonEvent middleMouseButtonEvent() {
        return middleMouseButtonEvent;
    }

    public static FriendListChangedEvent friendListChangedEvent() {
        return friendListChangedEvent;
    }

    public static MacroListChangedEvent macroListChangedEvent() {
        return macroListChangedEvent;
    }

    public static ReceivePacketEvent receivePacketEvent(Packet<?> packet) {
        receivePacketEvent.setCancelled(false);
        receivePacketEvent.packet = packet;
        return receivePacketEvent;
    }

    public static PlayerMoveEvent playerMoveEvent(MovementType type, Vec3d movement) {
        playerMoveEvent.type = type;
        playerMoveEvent.movement = movement;
        return playerMoveEvent;
    }

    public static AccountListChangedEvent accountListChangedEvent() {
        return accountListChangedEvent;
    }

    public static ChunkDataEvent chunkDataEvent(WorldChunk chunk) {
        ChunkDataEvent event = chunkDataEventPool.get();
        event.chunk = chunk;
        return event;
    }

    public static void returnChunkDataEvent(ChunkDataEvent event) {
        chunkDataEventPool.free(event);
    }

    public static AttackEntityEvent attackEntityEvent(Entity entity) {
        attackEntityEvent.setCancelled(false);
        attackEntityEvent.entity = entity;
        return attackEntityEvent;
    }

    public static StartBreakingBlockEvent startBreakingBlockEvent(BlockPos blockPos, Direction direction) {
        startBreakingBlockEvent.setCancelled(false);
        startBreakingBlockEvent.blockPos = blockPos;
        startBreakingBlockEvent.direction = direction;
        return startBreakingBlockEvent;
    }

    public static EntityDestroyEvent entityDestroyEvent(Entity entity) {
        entityDestroyEvent.entity = entity;
        return entityDestroyEvent;
    }

    public static DamageEvent damageEvent(LivingEntity entity, DamageSource source) {
        damageEvent.entity = entity;
        damageEvent.source = source;
        return damageEvent;
    }

    public static RightClickEvent rightClickEvent(){return rightClickEvent;}
}
