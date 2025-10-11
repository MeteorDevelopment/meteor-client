/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.texture;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;

public class FrameMetadataFactory {
    /**
     * Factory pattern to get the FrameMetadata object to compose a GIF. This factory can be extended in the future in case
     * we want other animated formats like APNG or WEBP. (Not that Oracle's jdk supports them yet).
     * @param metadata the GIF's IIOMetadata object from the image reader.
     *                 It effectively contains all the metadata from a GIF frame.
     * @return FrameMetadata.
     */
    public static FrameMetadata fromGIF(IIOMetadata metadata) {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
        IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
        IIOMetadataNode desc = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
        int delay = Integer.parseInt(gce.getAttribute("delayTime"));
        String disposal = gce.getAttribute("disposalMethod");
        int topPos = Integer.parseInt(desc.getAttribute("imageTopPosition"));
        int leftPos = Integer.parseInt(desc.getAttribute("imageLeftPosition"));
        if (disposal.equals("restoreToBackground")) return new FrameMetadata(delay, disposal, new Offset(leftPos,topPos), getBgColor(root));
        return new FrameMetadata(delay, disposal, new Offset(leftPos,topPos), new Color(0,0,0));
    }

    /**
     * Gets the background color from a GIF's metadata. Needs to locate the index of the color in the LogicalScreenDescriptor
     * and search it inside the ColorTableEntry list in the GlobalColorTable.
     * @param root a IIOMetadataNode object (root) from the IIOMetadata.
     * @return Color.
     */
    private static java.awt.Color getBgColor(IIOMetadataNode root) {
        IIOMetadataNode lsd = (IIOMetadataNode) root.getElementsByTagName("LogicalScreenDescriptor").item(0); // LOL
        IIOMetadataNode gct =  (IIOMetadataNode) root.getElementsByTagName("GlobalColorTable").item(0);
        int bgcIndex = Integer.parseInt(lsd.getAttribute("backgroundColorIndex"));
        return getBgColor(gct, bgcIndex);
    }

    private static @NotNull java.awt.Color getBgColor(IIOMetadataNode gct, int bgcIndex) {
        java.awt.Color bgColor = new java.awt.Color(0,0,0);
        if (gct != null) {
            NodeList colorEntries = gct.getElementsByTagName("ColorTableEntry");
            for (int i = 0; i < colorEntries.getLength(); i++) {
                IIOMetadataNode colorEntry = (IIOMetadataNode) colorEntries.item(i);
                int colorIndex = Integer.parseInt(colorEntry.getAttribute("index"));
                if (colorIndex == bgcIndex) {
                    bgColor = new java.awt.Color(Integer.parseInt(colorEntry.getAttribute("red")),
                        Integer.parseInt(colorEntry.getAttribute("green")),
                        Integer.parseInt(colorEntry.getAttribute("blue")));
                }
            }
        }
        return bgColor;
    }

}
