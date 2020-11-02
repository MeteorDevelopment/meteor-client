package minegame159.meteorclient.events;

import minegame159.meteorclient.events.packets.ContainerSlotUpdateEvent;
import minegame159.meteorclient.events.packets.PlaySoundPacketEvent;
import minegame159.meteorclient.events.packets.ReceivePacketEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.KeyAction;
import minegame159.meteorclient.utils.Pool;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

public class EventStore {
    private static final PlaySoundPacketEvent playSoundPacketEvent = new PlaySoundPacketEvent();
    private static final SendPacketEvent sendPacketEvent = new SendPacketEvent();
    private static final ActiveModulesChangedEvent activeModulesChangedEvent = new ActiveModulesChangedEvent();
    private static final CharTypedEvent charTypedEvent = new CharTypedEvent();
    private static final EntityAddedEvent entityAddedEvent = new EntityAddedEvent();
    private static final EntityRemovedEvent entityRemovedEvent = new EntityRemovedEvent();
    private static final KeyEvent keyEvent = new KeyEvent();
    private static final ModuleBindChangedEvent moduleBindChangedEvent = new ModuleBindChangedEvent();
    private static final ModuleVisibilityChangedEvent moduleVisibilityChangedEvent = new ModuleVisibilityChangedEvent();
    private static final OpenScreenEvent openScreenEvent = new OpenScreenEvent();
    private static final Render2DEvent render2DEvent = new Render2DEvent();
    private static final RenderEvent renderEvent = new RenderEvent();
    private static final PreTickEvent preTickEvent = new PreTickEvent();
    private static final PostTickEvent postTickEvent = new PostTickEvent();
    private static final TookDamageEvent tookDamageEvent = new TookDamageEvent();
    private static final GameJoinedEvent gameJoinedEvent = new GameJoinedEvent();
    private static final GameDisconnectedEvent gameDisconnectedEvent = new GameDisconnectedEvent();
    private static final MiddleMouseButtonEvent middleMouseButtonEvent = new MiddleMouseButtonEvent();
    private static final FriendListChangedEvent friendListChangedEvent = new FriendListChangedEvent();
    private static final MacroListChangedEvent macroListChangedEvent = new MacroListChangedEvent();
    private static final ReceivePacketEvent receivePacketEvent = new ReceivePacketEvent();
    private static final PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent();
    private static final AccountListChangedEvent accountListChangedEvent = new AccountListChangedEvent();
    private static final Pool<ChunkDataEvent> chunkDataEventPool = new Pool<>(ChunkDataEvent::new);
    private static final AttackEntityEvent attackEntityEvent = new AttackEntityEvent();
    private static final StartBreakingBlockEvent startBreakingBlockEvent = new StartBreakingBlockEvent();
    private static final EntityDestroyEvent entityDestroyEvent = new EntityDestroyEvent();
    private static final DamageEvent damageEvent = new DamageEvent();
    private static final RightClickEvent rightClickEvent = new RightClickEvent();
    private static final ContainerSlotUpdateEvent containerSlotUpdateEvent = new ContainerSlotUpdateEvent();
    private static final BlockActivateEvent blockActivateEvent = new BlockActivateEvent();
    private static final SendMessageEvent sendMessageEvent = new SendMessageEvent();
    private static final PlaySoundEvent playSoundEvent = new PlaySoundEvent();
    private static final WaypointListChangedEvent waypointListChangedEvent = new WaypointListChangedEvent();
    private static final BreakBlockEvent breakBlockEvent = new BreakBlockEvent();
    private static final PlaceBlockEvent placeBlockEvent = new PlaceBlockEvent();
    private static final DropItemsEvent dropItemsEvent = new DropItemsEvent();
    private static final PickItemsEvent pickItemsEvent = new PickItemsEvent();

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

    public static KeyEvent keyEvent(int key, KeyAction action) {
        keyEvent.setCancelled(false);
        keyEvent.key = key;
        keyEvent.action = action;
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

    public static RenderEvent renderEvent(float tickDelta, double offsetX, double offsetY, double offsetZ) {
        renderEvent.tickDelta = tickDelta;
        renderEvent.offsetX = offsetX;
        renderEvent.offsetY = offsetY;
        renderEvent.offsetZ = offsetZ;
        return renderEvent;
    }

    public static PreTickEvent preTickEvent() {
        return preTickEvent;
    }

    public static PostTickEvent postTickEvent() {
        return postTickEvent;
    }

    public static TookDamageEvent tookDamageEvent(LivingEntity entity, DamageSource source) {
        tookDamageEvent.entity = entity;
        tookDamageEvent.source = source;
        return tookDamageEvent;
    }

    public static GameJoinedEvent gameJoinedEvent() {
        return gameJoinedEvent;
    }

    public static GameDisconnectedEvent gameDisconnectedEvent() {
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

    public static ContainerSlotUpdateEvent containerSlotUpdateEvent(ScreenHandlerSlotUpdateS2CPacket packet) {
        containerSlotUpdateEvent.packet = packet;
        return containerSlotUpdateEvent;
    }

    public static BlockActivateEvent blockActivateEvent(BlockState blockState) {
        blockActivateEvent.blockState = blockState;
        return blockActivateEvent;
    }

    public static SendMessageEvent sendMessageEvent(String msg) {
        sendMessageEvent.msg = msg;
        return sendMessageEvent;
    }

    public static PlaySoundEvent playSoundEvent(SoundInstance sound) {
        playSoundEvent.setCancelled(false);
        playSoundEvent.sound = sound;
        return playSoundEvent;
    }

    public static WaypointListChangedEvent waypointListChangedEvent() {
        return waypointListChangedEvent;
    }

    public static BreakBlockEvent breakBlockEvent(BlockPos blockPos) {
        breakBlockEvent.blockPos = blockPos;
        return breakBlockEvent;
    }

    public static PlaceBlockEvent placeBlockEvent(BlockPos blockPos, Block block) {
        placeBlockEvent.blockPos = blockPos;
        placeBlockEvent.block = block;
        return placeBlockEvent;
    }

    public static DropItemsEvent dropItemEvent(ItemStack itemStack) {
        dropItemsEvent.itemStack = itemStack;
        return dropItemsEvent;
    }

    public static PickItemsEvent pickItemsEvent(ItemStack itemStack, int count) {
        pickItemsEvent.itemStack = itemStack;
        pickItemsEvent.count = count;
        return pickItemsEvent;
    }
}
