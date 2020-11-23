/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class MFont {
    private static final int IMG_SIZE = 512;
    private static final minegame159.meteorclient.utils.Color SHADOW_COLOR = new minegame159.meteorclient.utils.Color(60, 60, 60, 180);

    private final MeshBuilder mb = new MeshBuilder(16384);
    private final AbstractTexture texture;
    private final CharData[] charData = new CharData[256];
    private int fontHeight = -1;

    public double scale = 1;

    public MFont(Font font, boolean antiAlias, boolean fractionalMetrics) {
        texture = setupTexture(font, antiAlias, fractionalMetrics, this.charData);
    }

    private AbstractTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        BufferedImage img = generateFontImage(font, antiAlias, fractionalMetrics, chars);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            byte[] bytes = baos.toByteArray();

            ByteBuffer data = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            ((Buffer) data).flip();

            return new NativeImageBackedTexture(NativeImage.read(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private BufferedImage generateFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        BufferedImage bufferedImage = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setFont(font);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, IMG_SIZE, IMG_SIZE);
        g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        FontMetrics fontMetrics = g.getFontMetrics();

        int charHeight = 0;
        int positionX = 0;
        int positionY = 1;

        for (int i = 0; i < chars.length; i++) {
            char ch = (char) i;

            CharData charData = new CharData();
            Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g);
            charData.srcWidth = (dimensions.getBounds().width + 8);
            charData.srcHeight = dimensions.getBounds().height;

            if (positionX + charData.srcWidth >= IMG_SIZE) {
                positionX = 0;
                positionY += charHeight;
                charHeight = 0;
            }

            if (charData.srcHeight > charHeight) {
                charHeight = charData.srcHeight;
            }

            charData.srcX = positionX;
            charData.srcY = positionY;

            if (charData.srcHeight > this.fontHeight) {
                this.fontHeight = charData.srcHeight;
            }

            chars[i] = charData;

            g.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());

            positionX += charData.srcWidth;
        }
        return bufferedImage;
    }

    public void begin() {
        mb.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);
    }

    public boolean isBuilding() {
        return mb.isBuilding();
    }

    public void end() {
        texture.bindTexture();
        mb.end(true);
    }

    public double renderString(String string, double x, double y, minegame159.meteorclient.utils.Color color) {
        boolean wasBuilding = isBuilding();
        if (!isBuilding()) begin();

        x -= 1;
        y -= 1;

        x *= 2;
        y *= 2;

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            if (c < charData.length) {
                charData[c].render(mb, x, y, color);
                x += (charData[c].srcWidth - 8) * scale;
            }
        }

        if (!wasBuilding) end();
        return x / 2;
    }

    public double renderStringWithShadow(String string, double x, double y, minegame159.meteorclient.utils.Color color) {
        boolean wasBuilding = isBuilding();
        if (!isBuilding()) begin();

        double shadowWidth = renderString(string, x + 1 * scale, y + 1 * scale, SHADOW_COLOR);
        double width = Math.max(shadowWidth, renderString(string, x, y, color));

        if (!wasBuilding) end();
        return width;
    }

    public double getHeight() {
        return Math.round((this.fontHeight - 8.0) / 2 + 2) * scale;
    }

    public double getStringWidth(String text) {
        int width = 0;

        for (char c : text.toCharArray()) {
            if (c < this.charData.length) width += this.charData[c].srcWidth - 8;
        }

        return Math.round(width / 2.0) * scale;
    }

    public class CharData {
        public int srcX;
        public int srcY;
        public int srcWidth;
        public int srcHeight;

        public void render(MeshBuilder mb, double x, double y, minegame159.meteorclient.utils.Color color) {
            double texX = (double) srcX / IMG_SIZE;
            double texY = (double) srcY / IMG_SIZE;
            double texWidth = (double) srcWidth / IMG_SIZE;
            double texHeight = (double) srcHeight / IMG_SIZE;

            MeshBuilder preMb = ShapeBuilder.triangles;
            ShapeBuilder.triangles = mb;

            ShapeBuilder.texQuad(x / 2, y / 2, srcWidth / 2.0 * scale, srcHeight / 2.0 * scale, texX, texY, texWidth, texHeight, color, color, color, color);

            ShapeBuilder.triangles = preMb;
        }
    }
}
