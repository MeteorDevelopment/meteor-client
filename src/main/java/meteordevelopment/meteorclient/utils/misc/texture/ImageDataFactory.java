/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.texture;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;


import static meteordevelopment.meteorclient.systems.hud.elements.ImageHud.MAX_TEX_SIZE;
import static meteordevelopment.meteorclient.utils.misc.texture.Offset.getFrameOffset;

public class ImageDataFactory {
    /**
     * Creates an ImageData object from a GIF. If support for other animated formats is to be added, another similar method
     * should be used (fromWEBP, fromAPNG, ...)
     */
    public static ImageData fromGIF(String name, ImageReader reader) throws IOException {
        if (reader == null || !reader.getFormatName().equalsIgnoreCase("gif")) {
            throw new IOException("Invalid image format");
        }
        ImageData imageData = new ImageData(name);
        imageData.width = reader.getWidth(0);
        imageData.height = reader.getHeight(0);
        imageData.framesPerColumn = MAX_TEX_SIZE / imageData.height;
        imageData.totalFrames = reader.getNumImages(true);
        imageData.canvasWidth = imageData.width * imageData.getColumns();
        imageData.canvasHeight = Math.min(imageData.height * imageData.framesPerColumn, MAX_TEX_SIZE );
        imageData.delays = new ArrayList<>();
        BufferedImage image = composeGIF(reader,imageData);
        imageData.texture = TextureUtils.bufferedToNative(image);
        return imageData;
    }

    /**
     * Creates an ImageData object from a regular static image. Probably overkill, but helps with consistency.
     */
    public static ImageData fromStatic(String name, ImageReader reader) throws IOException {
        ImageData imageData = new ImageData(name);
        imageData.width = reader.getWidth(0);
        imageData.height = reader.getHeight(0);
        imageData.framesPerColumn = 1;
        imageData.totalFrames = 1;
        imageData.canvasWidth = imageData.width;
        imageData.canvasHeight = imageData.height;
        imageData.delays = new ArrayList<>(0);
        imageData.texture = TextureUtils.bufferedToNative(reader.read(0));
        return imageData;
    }

    /**
     * Reads each frame of the GIF and composes it depending on the disposal method used in that frame.
     * @return a BufferedImage consisting of all frames stacked by rows and then by columns.
     */
    private static BufferedImage composeGIF(ImageReader reader, ImageData imageData) throws IOException {
        BufferedImage canvas = new BufferedImage(imageData.canvasWidth, imageData.canvasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvasGraphics = canvas.createGraphics();
        int lastUndisposed = 0;
        for (int i = 0; i < imageData.totalFrames; i++) {
            IIOMetadata metadata = reader.getImageMetadata(i);
            FrameMetadata frameMetadata = FrameMetadataFactory.fromGIF(metadata);
            imageData.delays.add(frameMetadata.delay());
            Offset offset = getFrameOffset(i, imageData.framesPerColumn, imageData.width, imageData.height);
            switch(frameMetadata.disposal()) {
                case "doNotDispose" -> { // Set the background to the current undisposed frame and draw the next frame over.
                    Offset prevOffset = (i==0) ? new Offset(0,0) : getFrameOffset(lastUndisposed, imageData.framesPerColumn, imageData.width, imageData.height);
                    drawWithDisposal(reader.read(i),canvas,canvasGraphics,frameMetadata,offset,prevOffset,imageData.width,imageData.height);
                    lastUndisposed = i;
                }
                case "restoreToPrevious" -> { // Set the background to the previous undisposed frame and draw the next frame over.
                    Offset prevOffset = getFrameOffset(lastUndisposed, imageData.framesPerColumn,imageData.width, imageData.height);
                    drawWithDisposal(reader.read(i),canvas,canvasGraphics,frameMetadata,offset,prevOffset,imageData.width,imageData.height);
                }
                case "restoreToBackground" -> { // Set the background color and draw the next frame over.
                    canvasGraphics.setColor(frameMetadata.backgroundColor());
                    canvasGraphics.fillRect(offset.x(), offset.y(), imageData.width, imageData.height);
                    canvasGraphics.drawImage(reader.read(i),offset.x()+frameMetadata.offset().x(), offset.y()+frameMetadata.offset().y(), null);
                }
                default -> canvasGraphics.drawImage(reader.read(i),offset.x(), offset.y(), null); // Just draw the next frame as fallback.
            }
        }
        canvasGraphics.dispose();
        return canvas;
    }

    /**
     * Taking into account the disposal type and the offsets of the frame, takes one base frame and draws another over it.
     * Instead of making a BufferedImage for each frame, the base canvas is used and the frames are cut from there.
     */
    private static void drawWithDisposal(BufferedImage next, BufferedImage canvas, Graphics2D canvasGraphics,
                                         FrameMetadata frameMetadata, Offset offset, Offset prevOffset, int width, int height) {
        canvasGraphics.drawImage(canvas,
            offset.x(), offset.y(),
            offset.x()+width, offset.y()+height,
            prevOffset.x(),prevOffset.y(),
            prevOffset.x()+width,prevOffset.y()+height
            ,null);
        canvasGraphics.drawImage(next,
            offset.x() + frameMetadata.offset().x(), offset.y() + frameMetadata.offset().y(),
            null);
    }
}
