package minegame159.meteorclient.utils.render.color;

import minegame159.meteorclient.friends.FriendManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class ColorUtil {
    public static Color getEntityColor(Entity entity, Color playersColor, Color animalsColor, Color waterAnimalsColor, Color monstersColor, Color ambientColor, Color miscColor) {
        if (entity instanceof PlayerEntity) return FriendManager.INSTANCE.getColor((PlayerEntity) entity, playersColor);

        switch (entity.getType().getSpawnGroup()) {
            case CREATURE:       return animalsColor;
            case WATER_CREATURE: return waterAnimalsColor;
            case MONSTER:        return monstersColor;
            case AMBIENT:        return ambientColor;
            case MISC:           return miscColor;
        }

        return new Color(255, 255, 255);
    }
}
