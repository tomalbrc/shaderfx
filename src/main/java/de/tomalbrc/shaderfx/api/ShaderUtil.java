package de.tomalbrc.shaderfx.api;

import net.minecraft.util.ARGB;

import java.awt.image.BufferedImage;

public class ShaderUtil {
    public static BufferedImage tintEdges(BufferedImage bufferedImage, int color) {
        bufferedImage.setRGB(0, 0, color);
        bufferedImage.setRGB(bufferedImage.getWidth()-1, 0, color);
        bufferedImage.setRGB(0, bufferedImage.getHeight()-1, color);
        bufferedImage.setRGB(bufferedImage.getWidth()-1, bufferedImage.getHeight()-1, color);
        return bufferedImage;
    }

    public static BufferedImage addAnimatedEmojiMarker(BufferedImage bufferedImage, int fps) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        int frames = Math.max(1, height / Math.max(1, width));
        int frameheight = Math.max(1, height / frames);

        BufferedImage res = new BufferedImage(width, height + frameheight + 1, bufferedImage.getType());

        int[] srcPixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
        res.setRGB(0, frameheight, width, height, srcPixels, 0, width);

        int metaColor = ARGB.color(252, frames, fps, frameheight);
        return tintEdges(res, metaColor);
    }
}
