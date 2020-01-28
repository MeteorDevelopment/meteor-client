package minegame159.meteorclient.events;

import minegame159.meteorclient.events.packets.PlaySoundPacketEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.packet.PlaySoundS2CPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.Packet;

public class EventStore {
    private static ActiveModulesChangedEvent activeModulesChangedEvent = new ActiveModulesChangedEvent();
    private static Render2DEvent render2DEvent = new Render2DEvent();
    private static TickEvent tickEvent = new TickEvent();
    private static PlaySoundPacketEvent playSoundPacketEvent = new PlaySoundPacketEvent();
    private static RenderEvent renderEvent = new RenderEvent();
    private static KeyEvent keyEvent = new KeyEvent();
    private static SendPacketEvent sendPacketEvent = new SendPacketEvent();
    private static DeathEvent deathEvent = new DeathEvent();
    private static BlockShouldDrawSideEvent blockShouldDrawSideEvent = new BlockShouldDrawSideEvent();
    private static ChamsEvent chamsEvent = new ChamsEvent();
    private static RenderFogEvent renderFogEvent = new RenderFogEvent();
    private static HurtCamEvent hurtCamEvent = new HurtCamEvent();
    private static OpenScreenEvent openScreenEvent = new OpenScreenEvent();
    private static ModuleBindChangedEvent moduleBindChangedEvent = new ModuleBindChangedEvent();
    private static CharTypedEvent charTypedEvent = new CharTypedEvent();

    public static ActiveModulesChangedEvent activeModulesChangedEvent() {
        activeModulesChangedEvent.setCancelled(false);
        return activeModulesChangedEvent;
    }

    public static Render2DEvent render2DEvent(int screenWidth, int screenHeight, float tickDelta) {
        render2DEvent.setCancelled(false);
        render2DEvent.screenWidth = screenWidth;
        render2DEvent.screenHeight = screenHeight;
        render2DEvent.tickDelta = tickDelta;
        return render2DEvent;
    }

    public static TickEvent tickEvent() {
        tickEvent.setCancelled(false);
        return tickEvent;
    }

    public static PlaySoundPacketEvent playSoundPacketEvent(PlaySoundS2CPacket packet) {
        playSoundPacketEvent.setCancelled(false);
        playSoundPacketEvent.packet = packet;
        return playSoundPacketEvent;
    }

    public static RenderEvent renderEvent(float tickDelta) {
        renderEvent.setCancelled(false);
        renderEvent.tickDelta = tickDelta;
        return renderEvent;
    }

    public static KeyEvent keyEvent(int key, boolean push) {
        keyEvent.setCancelled(false);
        keyEvent.key = key;
        keyEvent.push = push;
        return keyEvent;
    }

    public static SendPacketEvent sendPacketEvent(Packet packet) {
        sendPacketEvent.setCancelled(false);
        sendPacketEvent.packet = packet;
        return sendPacketEvent;
    }

    public static DeathEvent deathEvent() {
        deathEvent.setCancelled(false);
        return deathEvent;
    }

    public static BlockShouldDrawSideEvent blockShouldDrawSideEvent(BlockState state) {
        blockShouldDrawSideEvent.setCancelled(false);
        blockShouldDrawSideEvent.state = state;
        blockShouldDrawSideEvent.shouldRenderSide = false;
        return blockShouldDrawSideEvent;
    }

    public static ChamsEvent chamsEvent(LivingEntity livingEntity) {
        chamsEvent.setCancelled(false);
        chamsEvent.livingEntity = livingEntity;
        chamsEvent.enabled = false;
        return chamsEvent;
    }

    public static RenderFogEvent renderFogEvent() {
        renderFogEvent.setCancelled(false);
        return renderFogEvent;
    }

    public static HurtCamEvent hurtCamEvent() {
        hurtCamEvent.setCancelled(false);
        return hurtCamEvent;
    }

    public static OpenScreenEvent openScreenEvent(Screen screen) {
        openScreenEvent.setCancelled(false);
        openScreenEvent.screen = screen;
        return openScreenEvent;
    }

    public static ModuleBindChangedEvent moduleBindChangedEvent(Module module) {
        moduleBindChangedEvent.setCancelled(false);
        moduleBindChangedEvent.module = module;
        return moduleBindChangedEvent;
    }

    public static CharTypedEvent charTypedEvent(char c) {
        charTypedEvent.setCancelled(false);
        charTypedEvent.c = c;
        return charTypedEvent;
    }
}
