package minegame159.meteorclient.events;

import minegame159.meteorclient.events.packets.PlaySoundPacketEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.packet.PlaySoundS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.Packet;

public class EventStore {
    private static PlaySoundPacketEvent playSoundPacketEvent = new PlaySoundPacketEvent();
    private static SendPacketEvent sendPacketEvent = new SendPacketEvent();
    private static ActiveModulesChangedEvent activeModulesChangedEvent = new ActiveModulesChangedEvent();
    private static BlockShouldRenderSideEvent blockShouldRenderSideEvent = new BlockShouldRenderSideEvent();
    private static ChamsEvent chamsEvent = new ChamsEvent();
    private static CharTypedEvent charTypedEvent = new CharTypedEvent();
    private static EntityAddedEvent entityAddedEvent = new EntityAddedEvent();
    private static EntityRemovedEvent entityRemovedEvent = new EntityRemovedEvent();
    private static HurtCamEvent hurtCamEvent = new HurtCamEvent();
    private static KeyEvent keyEvent = new KeyEvent();
    private static ModuleBindChangedEvent moduleBindChangedEvent = new ModuleBindChangedEvent();
    private static ModuleVisibilityChangedEvent moduleVisibilityChangedEvent = new ModuleVisibilityChangedEvent();
    private static OpenScreenEvent openScreenEvent = new OpenScreenEvent();
    private static Render2DEvent render2DEvent = new Render2DEvent();
    private static RenderEvent renderEvent = new RenderEvent();
    private static RenderFogEvent renderFogEvent = new RenderFogEvent();
    private static TickEvent tickEvent = new TickEvent();
    private static TookDamageEvent tookDamageEvent = new TookDamageEvent();
    private static ChangeChatLengthEvent changeChatLengthEvent = new ChangeChatLengthEvent();
    private static GameJoinedEvent gameJoinedEvent = new GameJoinedEvent();

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

    public static BlockShouldRenderSideEvent blockShouldRenderSideEvent(BlockState state) {
        blockShouldRenderSideEvent.setCancelled(false);
        blockShouldRenderSideEvent.state = state;
        blockShouldRenderSideEvent.shouldRenderSide = true;
        return blockShouldRenderSideEvent;
    }

    public static ChamsEvent chamsEvent(LivingEntity entity, boolean enabled) {
        chamsEvent.setCancelled(false);
        chamsEvent.entity = entity;
        chamsEvent.enabled = enabled;
        return chamsEvent;
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

    public static HurtCamEvent hurtCamEvent() {
        hurtCamEvent.setCancelled(false);
        return hurtCamEvent;
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

    public static ModuleVisibilityChangedEvent moduleVisibilityChangedEvent(Module module) {
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

    public static RenderEvent renderEvent(float tickDelta) {
        renderEvent.tickDelta = tickDelta;
        return renderEvent;
    }

    public static RenderFogEvent renderFogEvent() {
        renderFogEvent.setCancelled(false);
        return renderFogEvent;
    }

    public static TickEvent tickEvent() {
        return tickEvent;
    }

    public static TookDamageEvent tookDamageEvent(LivingEntity entity) {
        tookDamageEvent.entity = entity;
        return tookDamageEvent;
    }

    public static ChangeChatLengthEvent changeChatLengthEvent(int length) {
        changeChatLengthEvent.length = length;
        return changeChatLengthEvent;
    }

    public static GameJoinedEvent gameJoinedEvent() {
        return gameJoinedEvent;
    }
}
