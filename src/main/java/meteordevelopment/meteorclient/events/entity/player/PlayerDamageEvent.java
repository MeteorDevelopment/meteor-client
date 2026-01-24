package meteordevelopment.meteorclient.events.entity.player;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;

public class PlayerDamageEvent extends Cancellable {
    public float damage;
    public DamageSource source;
    public float knockback;
    public Entity attacker; // 新增：直接传递攻击者

    public PlayerDamageEvent(float damage, DamageSource source, float knockback, Entity attacker) {
        this.damage = damage;
        this.source = source;
        this.knockback = knockback;
        this.attacker = attacker;
    }
    
    // 兼容旧构造函数（如果其他地方用了）
    public PlayerDamageEvent(float damage, DamageSource source, float knockback) {
        this(damage, source, knockback, null);
    }
}