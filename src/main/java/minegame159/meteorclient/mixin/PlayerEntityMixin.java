package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements IPlayerEntity {
    @Shadow public PlayerInventory inventory;

    @Override
    public void setInventory(PlayerInventory inventory) {
        this.inventory = inventory;
    }
}
