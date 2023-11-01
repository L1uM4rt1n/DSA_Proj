import java.io.*;

public class Utility {

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        // Initialize the output stream
        BitOutputStream bos = new BitOutputStream(new FileOutputStream(outputFileName));

        // write the dimensions of the image of dimension 1024x1024
        bos.write(pixels.length, 10);
        bos.write(pixels[0].length, 10);

        // compress the pixel values using quadtree compression
        QuadtreeNode root = buildQuadtree(pixels);
        writeQuadtree(root, bos);

        bos.close();
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        // Initialize the input stream
        BitInputStream bis = new BitInputStream(new FileInputStream(inputFileName));

        // read the dimensions of the image of dimension 1024x1024
        int width = bis.read(10);
        int height = bis.read(10);

        // decompress the pixel values using quadtree compression
        QuadtreeNode root = readQuadtree(bis);

        // create and fill the pixels array in chunks
        int chunkSize = 32; //adjusted to prevent going out of memory
        int[][][] pixels = new int[width][height][3];
        for (int i = 0; i < width; i += chunkSize) {
            for (int j = 0; j < height; j += chunkSize) {
                fillPixels(root, pixels, i, j, chunkSize);
            }
        }

        bis.close();

        return pixels;
    }

    private QuadtreeNode buildQuadtree(int[][][] pixels) {
        return buildQuadtree(pixels, 0, 0, 256);
    }

    private QuadtreeNode buildQuadtree(int[][][] pixels, int x, int y, int size) {
        QuadtreeNode node = new QuadtreeNode();
        if (size <= 1000) {
            node.color = averageColour(pixels, x, y, size);
        } else {
            int halfSize = size / 2;
            QuadtreeNode[] children = new QuadtreeNode[4];
            children[0] = buildQuadtree(pixels, x, y, halfSize);
            children[1] = buildQuadtree(pixels, x + halfSize, y, halfSize);
            children[2] = buildQuadtree(pixels, x, y + halfSize, halfSize);
            children[3] = buildQuadtree(pixels, x + halfSize, y + halfSize, halfSize);
            node.children = children;
        }
        return node;
    }

    private int[] averageColour(int[][][] pixels, int x, int y, int size) {
        int[] avgColour = new int[3];
        int pixelCounter = 0;
        for (int i = x; i < x + size; i++) {
            for (int j = y; j < y + size; j++) {
                avgColour[0] += pixels[i][j][0];
                avgColour[1] += pixels[i][j][1];
                avgColour[2] += pixels[i][j][2];
                pixelCounter++;
            }
        }

        avgColour[0] /= pixelCounter;
        avgColour[1] /= pixelCounter;
        avgColour[2] /= pixelCounter;
        return avgColour;
    }

    private void writeQuadtree(QuadtreeNode node, BitOutputStream bos) throws IOException {
        if (node.children == null) {
            bos.write(1, 1);
            bos.write(node.color[0], 8);
            bos.write(node.color[1], 8);
            bos.write(node.color[2], 8);
        } else {
            bos.write(0, 1);
            for (QuadtreeNode child : node.children) {
                writeQuadtree(child, bos);
            }
        }
    }

    private QuadtreeNode readQuadtree(BitInputStream bis) throws IOException {
        QuadtreeNode node = new QuadtreeNode();
        int bit = bis.read(1);
        if (bit == 1) {
            node.color = new int[] { bis.read(8), bis.read(8), bis.read(8) };
        } else {
            QuadtreeNode[] children = new QuadtreeNode[4];
            for (int i = 0; i < 4; i++) {
                children[i] = readQuadtree(bis);
            }
            node.children = children;
        }
        return node;
    }

    private void fillPixels(QuadtreeNode node, int[][][] pixels) {
        if (node.children == null) {
            int[] color = node.color;
            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j++) {
                    pixels[i][j] = color;
                }
            }
        } else {
            int halfSize = 256 / 2;
            fillPixels(node.children[0], pixels, 0, 0, halfSize);
            fillPixels(node.children[1], pixels, halfSize, 0, halfSize);
            fillPixels(node.children[2], pixels, 0, halfSize, halfSize);
            fillPixels(node.children[3], pixels, halfSize, halfSize, halfSize);
        }
    }

    private void fillPixels(QuadtreeNode node, int[][][] pixels, int x, int y, int size) {
        if (node.children == null) {
            int[] color = node.color;
            for (int i = x; i < Math.min(x + size, pixels.length); i++) {
                for (int j = y; j < Math.min(y + size, pixels[i].length); j++) {
                    pixels[i][j] = color;
                }
            }
        } else {
            int halfSize = size / 2;
            fillPixels(node.children[0], pixels, x, y, halfSize);
            fillPixels(node.children[1], pixels, x + halfSize, y, halfSize);
            fillPixels(node.children[2], pixels, x, y + halfSize, halfSize);
            fillPixels(node.children[3], pixels, x + halfSize, y + halfSize, halfSize);
        }
    }

    private static class QuadtreeNode {
        public int[] color;
        public QuadtreeNode[] children;
    }

    private static class BitOutputStream {
        private OutputStream out;
        private int buffer;
        private int bits;

        public BitOutputStream(OutputStream out) {
            this.out = out;
            this.buffer = 0;
            this.bits = 0;
        }

        public void write(int value, int numBits) throws IOException {
            for (int i = numBits - 1; i >= 0; i--) {
                buffer |= ((value >> i) & 1) << bits;
                bits++;
                if (bits == 8) {
                    out.write(buffer);
                    buffer = 0;
                    bits = 0;
                }
            }
        }

        public void close() throws IOException {
            if (bits > 0) {
                out.write(buffer);
            }
            out.close();
        }
    }

    private static class BitInputStream {
        private InputStream in;
        private int buffer;
        private int bits;

        public BitInputStream(InputStream in) {
            this.in = in;
            this.buffer = 0;
            this.bits = 0;
        }

        public int read(int numBits) throws IOException {
            int result = 0;
            for (int i = numBits - 1; i >= 0; i--) {
                if (bits == 0) {
                    buffer = in.read();
                    if (buffer == -1) {
                        return -1;
                    }
                    bits = 8;
                }
                result |= ((buffer >> (bits - 1)) & 1) << i;
                bits--;
            }
            return result;
        }

        public void close() throws IOException {
            in.close();
        }
    }
}