package meteordevelopment.meteorclient.systems.seeds;

import kaptainwutax.mcutils.version.MCVersion;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public class Seed {
    public final Long seed;
    public final MCVersion version;

    public Seed(Long seed, MCVersion version) {
        this.seed = seed;
        if (version == null)
            version = MCVersion.latest();
        this.version = version;
    }

    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("seed", NbtLong.of(seed));
        tag.put("version", NbtString.of(version.name));
        return tag;
    }

    public static Seed fromTag(NbtCompound tag) {
        return new Seed(
            tag.getLong("seed"),
            MCVersion.fromString(tag.getString("version"))
        );
    }

    public Text toText() {
        BaseText text = new LiteralText(String.format("[%s%s%s] (%s)",
            Formatting.GREEN,
            seed.toString(),
            Formatting.WHITE,
            version.toString()
        ));
        text.setStyle(text.getStyle()
            .withClickEvent(new ClickEvent(
                ClickEvent.Action.COPY_TO_CLIPBOARD,
                seed.toString()
            ))
            .withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new LiteralText("Copy to clipboard")
            ))
        );
        return text;
    }
}
