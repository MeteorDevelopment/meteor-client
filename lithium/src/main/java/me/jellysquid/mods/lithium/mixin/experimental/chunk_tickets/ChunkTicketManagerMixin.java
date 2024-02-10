package me.jellysquid.mods.lithium.mixin.experimental.chunk_tickets;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.jellysquid.mods.lithium.common.util.collections.ChunkTicketSortedArraySet;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.collection.SortedArraySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.Iterator;

@Mixin(ChunkTicketManager.class)
public abstract class ChunkTicketManagerMixin {
    @Shadow
    private long age;

    @Shadow
    @Final
    Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> ticketsByPosition;

    private final Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> positionWithExpiringTicket = new Long2ObjectOpenHashMap<>();

    private static boolean canNoneExpire(SortedArraySet<ChunkTicket<?>> tickets) {
        if (!tickets.isEmpty()) {
            for (ChunkTicket<?> ticket : tickets) {
                if (canExpire(ticket)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Redirect(method = "method_14041", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/SortedArraySet;create(I)Lnet/minecraft/util/collection/SortedArraySet;"))
    private static SortedArraySet<ChunkTicket<?>> useLithiumSortedArraySet(int initialCapacity) {
        return new ChunkTicketSortedArraySet<>(initialCapacity);
    }

    private static boolean canExpire(ChunkTicket<?> ticket) {
        return ticket.getType().getExpiryTicks() != 0;
    }

    /**
     * Mark all locations that have tickets that can expire as such. Allows iterating only over locations with
     * tickets that can expire when purging expired tickets.
     */
    @Inject(
            method = "addTicket(JLnet/minecraft/server/world/ChunkTicket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicket;setTickCreated(J)V"),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void registerExpiringTicket(long position, ChunkTicket<?> ticket, CallbackInfo ci, SortedArraySet<ChunkTicket<?>> ticketsAtPos, int i, ChunkTicket<?> chunkTicket) {
        if (canExpire(ticket)) {
            this.positionWithExpiringTicket.put(position, ticketsAtPos);
        }
    }

    @Inject(
            method = "removeTicket(JLnet/minecraft/server/world/ChunkTicket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$TicketDistanceLevelPropagator;updateLevel(JIZ)V")
    )
    private void unregisterExpiringTicket(long pos, ChunkTicket<?> ticket, CallbackInfo ci) {
        if (canExpire(ticket)) {
            SortedArraySet<ChunkTicket<?>> ticketsAtPos = this.positionWithExpiringTicket.get(pos);
            if (canNoneExpire(ticketsAtPos)) {
                this.positionWithExpiringTicket.remove(pos);
            }
        }
    }

    @Inject(
            method = "addTicket(JLnet/minecraft/server/world/ChunkTicket;)V",
            at = @At(
                    value = "INVOKE", shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/util/collection/SortedArraySet;addAndGet(Ljava/lang/Object;)Ljava/lang/Object;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void updateSetMinExpiryTime(long position, ChunkTicket<?> ticket, CallbackInfo ci, SortedArraySet<ChunkTicket<?>> sortedArraySet, int i) {
        if (canExpire(ticket) && sortedArraySet instanceof ChunkTicketSortedArraySet<?> chunkTickets) {
            chunkTickets.addExpireTime(this.age + ticket.getType().getExpiryTicks());
        }
    }


    @Redirect(method = "purge",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/server/world/ChunkTicketManager;ticketsByPosition:Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;",
                    ordinal = 0
            )
    )
    private Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> getExpiringTicketsByPosition(ChunkTicketManager chunkTicketManager) {
        return this.positionWithExpiringTicket;
    }

    @Redirect(method = "purge",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/util/collection/SortedArraySet;isEmpty()Z"
            )
    )
    private boolean retCanNoneExpire(SortedArraySet<ChunkTicket<?>> tickets) {
        return canNoneExpire(tickets);
    }

    @Inject(method = "purge", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE", shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/util/collection/SortedArraySet;isEmpty()Z"
            )
    )
    private void removeIfEmpty(CallbackInfo ci, ObjectIterator<?> objectIterator, Long2ObjectMap.Entry<SortedArraySet<ChunkTicket<?>>> entry) {
        SortedArraySet<ChunkTicket<?>> ticketsAtPos = entry.getValue();
        if (ticketsAtPos.isEmpty()) {
            this.ticketsByPosition.remove(entry.getLongKey(), ticketsAtPos);
        }
    }

    @Redirect(method = "purge",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/collection/SortedArraySet;iterator()Ljava/util/Iterator;"
            )
    )
    private Iterator<ChunkTicket<?>> skipIfNotExpiringNow(SortedArraySet<ChunkTicket<?>> ticketsAtPos) {
        if (ticketsAtPos instanceof ChunkTicketSortedArraySet<?> optimizedSet && optimizedSet.getMinExpireTime() > this.age) {
            return Collections.emptyIterator();
        } else {
            return ticketsAtPos.iterator();
        }
    }
}
