package minegame159.meteorclient.json;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingSerializer implements JsonSerializer<Setting<?>> {
    private enum SettingType {
        Bool,
        Int,
        Double,
        Enum,
        Color,
        String,
        BlockList
    }

    @Override
    public JsonElement serialize(Setting<?> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject o = new JsonObject();

        SettingType type = null;
        if (src instanceof BoolSetting) type = SettingType.Bool;
        else if (src instanceof IntSetting) type = SettingType.Int;
        else if (src instanceof DoubleSetting) type = SettingType.Double;
        else if (src instanceof EnumSetting) type = SettingType.Enum;
        else if (src instanceof ColorSetting) type = SettingType.Color;
        else if (src instanceof StringSetting) type = SettingType.String;
        else if (src instanceof BlockListSetting) type = SettingType.BlockList;

        o.addProperty("name", src.name);
        o.add("type", context.serialize(type));

        if (type == SettingType.BlockList) {
            JsonArray a = new JsonArray();
            for (Block block : ((BlockListSetting) src).get()) {
                a.add(context.serialize(block, Block.class));
            }
            o.add("value", a);
        }
        else o.add("value", context.serialize(src.get()));

        return o;
    }

    public static void deserialize(Setting setting, JsonObject json, JsonDeserializationContext context) {
        JsonElement e = json.get("value");

        switch (SettingType.valueOf(json.get("type").getAsString())) {
            case Bool:      setting.set(context.deserialize(e, Boolean.TYPE)); break;
            case Int:       setting.set(context.deserialize(e, Integer.TYPE)); break;
            case Double:    setting.set(context.deserialize(e, Double.TYPE)); break;
            case Enum:      setting.parse(e.getAsString()); break;
            case Color:     setting.set(context.deserialize(e, Color.class)); break;
            case String:    setting.set(context.deserialize(e, String.class)); break;
            case BlockList: setting.set(context.deserialize(e, new TypeToken<List<Block>>() {}.getType()));
        }
    }
}
