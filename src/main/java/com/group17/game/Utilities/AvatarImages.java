package com.group17.game.Utilities;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

// Checks an uploaded profile picture and turns it into a 256 x 256 PNG
public final class AvatarImages {
    public static final long MAX_BYTES = 2L * 1024 * 1024;
    public static final int MIN_SIZE = 128;
    public static final int OUTPUT_SIZE = 256;
    // stops tiny files that unpack into huge images from using up memory
    private static final int MAX_SIDE = 8000;

    public static final String NOT_ALLOWED = "Only JPG, PNG or WebP images are allowed.";
    public static final String NO_GIFS = "GIFs aren't allowed. Please upload a JPG, PNG or WebP image.";
    public static final String NO_ANIMATION = "Animated images aren't allowed. Please upload a still JPG, PNG or WebP image.";
    public static final String TOO_BIG = "Your image must be 2 MB or smaller.";
    public static final String TOO_SMALL = "Your image must be at least " + MIN_SIZE + " × " + MIN_SIZE + " px.";
    public static final String TOO_LARGE = "Your image can't be bigger than " + MAX_SIDE + " × " + MAX_SIDE + " px.";
    public static final String UNREADABLE = "We couldn't read that image. Please try a different file.";
    public static final String EMPTY = "Please choose an image to upload.";

    static {
        // picks up the WebP reader from the TwelveMonkeys library
        ImageIO.scanForPlugins();
    }

    private AvatarImages() {}

    public static class InvalidAvatarException extends Exception {
        public InvalidAvatarException(String message) {
            super(message);
        }
    }

    private enum Format { JPEG, PNG, WEBP, GIF, OTHER }

    public static byte[] process(byte[] data) throws InvalidAvatarException {
        if (data == null || data.length == 0) {
            throw new InvalidAvatarException(EMPTY);
        }
        if (data.length > MAX_BYTES) {
            throw new InvalidAvatarException(TOO_BIG);
        }

        // check the file's actual bytes, not its name or the browser's content type
        Format format = detect(data);
        if (format == Format.GIF) {
            throw new InvalidAvatarException(NO_GIFS);
        }
        if (format == Format.OTHER) {
            throw new InvalidAvatarException(NOT_ALLOWED);
        }
        if ((format == Format.PNG && isAnimatedPng(data)) || (format == Format.WEBP && isAnimatedWebp(data))) {
            throw new InvalidAvatarException(NO_ANIMATION);
        }

        BufferedImage image = read(data);
        BufferedImage square = cropToSquare(image);
        BufferedImage resized = resize(square, OUTPUT_SIZE);
        return toPng(resized);
    }

    private static Format detect(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return Format.JPEG;
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return Format.PNG;
        }
        if (d.length >= 12 && ascii(d, 0, 4).equals("RIFF") && ascii(d, 8, 4).equals("WEBP")) {
            return Format.WEBP;
        }
        if (d.length >= 4 && ascii(d, 0, 4).equals("GIF8")) {
            return Format.GIF;
        }
        return Format.OTHER;
    }

    // animated PNGs have an acTL chunk before the image data
    private static boolean isAnimatedPng(byte[] d) {
        int pos = 8;
        while (pos + 8 <= d.length) {
            long length = ((d[pos] & 0xFFL) << 24) | ((d[pos + 1] & 0xFF) << 16)
                    | ((d[pos + 2] & 0xFF) << 8) | (d[pos + 3] & 0xFF);
            String type = ascii(d, pos + 4, 4);
            if (type.equals("acTL")) {
                return true;
            }
            if (type.equals("IDAT")) {
                return false;
            }
            pos += 12 + length; // length + type + data + crc
        }
        return false;
    }

    // animated WebPs start with a VP8X chunk that has the animation flag set
    private static boolean isAnimatedWebp(byte[] d) {
        return d.length > 20 && ascii(d, 12, 4).equals("VP8X") && (d[20] & 0x02) != 0;
    }

    private static BufferedImage read(byte[] data) throws InvalidAvatarException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (in == null) {
                throw new InvalidAvatarException(UNREADABLE);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new InvalidAvatarException(UNREADABLE);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < MIN_SIZE || height < MIN_SIZE) {
                    throw new InvalidAvatarException(TOO_SMALL);
                }
                if (width > MAX_SIDE || height > MAX_SIDE) {
                    throw new InvalidAvatarException(TOO_LARGE);
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw new InvalidAvatarException(UNREADABLE);
        }
    }

    // keeps the middle of the image
    private static BufferedImage cropToSquare(BufferedImage image) {
        int side = Math.min(image.getWidth(), image.getHeight());
        int x = (image.getWidth() - side) / 2;
        int y = (image.getHeight() - side) / 2;
        return image.getSubimage(x, y, side, side);
    }

    // halves the size step by step first, which looks much smoother than one big jump
    private static BufferedImage resize(BufferedImage image, int size) {
        BufferedImage current = image;
        int side = image.getWidth();
        while (side / 2 >= size) {
            side /= 2;
            current = draw(current, side);
        }
        return draw(current, size);
    }

    private static BufferedImage draw(BufferedImage source, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, size, size, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static byte[] toPng(BufferedImage image) throws InvalidAvatarException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new InvalidAvatarException(UNREADABLE);
        }
    }

    private static String ascii(byte[] d, int offset, int length) {
        if (offset + length > d.length) {
            return "";
        }
        return new String(d, offset, length, StandardCharsets.US_ASCII);
    }
}
