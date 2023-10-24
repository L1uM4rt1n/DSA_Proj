

import java.io.*;
import java.util.*;

public class Utility implements Serializable {

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        Quadtree root = buildQuadtree(pixels, 0, 0, pixels.length, pixels[0].length);
        root.size = pixels.length;  // Ensure the root size is set

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) { 
            oos.writeObject(root); 
        }
    }

    private Quadtree buildQuadtree(int[][][] pixels, int x, int y, int width, int height) {
        if (isUniform(pixels, x, y, width, height) || width == 1 || height == 1) {
            QuadtreeLeaf leaf = new QuadtreeLeaf(getAverageColor(pixels, x, y, width, height));
            leaf.size = Math.max(width, height);  // Set the size for the leaf
            return leaf;
        }

        int halfWidth = width / 2;
        int halfHeight = height / 2;

        Quadtree topLeft = buildQuadtree(pixels, x, y, halfWidth, halfHeight);
        Quadtree topRight = buildQuadtree(pixels, x + halfWidth, y, halfWidth, halfHeight);
        Quadtree bottomLeft = buildQuadtree(pixels, x, y + halfHeight, halfWidth, halfHeight);
        Quadtree bottomRight = buildQuadtree(pixels, x + halfWidth, y + halfHeight, halfWidth, halfHeight);

        QuadtreeNode node = new QuadtreeNode(topLeft, topRight, bottomLeft, bottomRight);
        node.size = Math.max(width, height);  // Set the size for the internal node
        return node;
    }
    
    private boolean isUniform(int[][][] pixels, int x, int y, int width, int height) {
        int[] firstColor = pixels[x][y];
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                if (!Arrays.equals(pixels[i][j], firstColor)) {
                    return false;
                }
            }
        }
        return true;
    }

    private int[] getAverageColor(int[][][] pixels, int x, int y, int width, int height) {
        long totalRed = 0, totalGreen = 0, totalBlue = 0;
        int pixelCount = width * height;

        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                totalRed += pixels[i][j][0];
                totalGreen += pixels[i][j][1];
                totalBlue += pixels[i][j][2];
            }
        }

        return new int[] {(int) (totalRed / pixelCount), (int) (totalGreen / pixelCount), (int) (totalBlue / pixelCount)};
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            Object object = ois.readObject();

            if (object instanceof Quadtree) {
                Quadtree root = (Quadtree) object;
                int[][][] decompressedPixels = new int[root.size][root.size][3];
                decompressQuadtree(root, decompressedPixels, 0, 0, root.size, root.size);
                return decompressedPixels;
            } else {
                throw new IOException("Invalid object type in the input file");
            }
        }
    }

    private void decompressQuadtree(Quadtree node, int[][][] pixels, int x, int y, int width, int height) {
        if (node instanceof QuadtreeLeaf) {
            int[] color = ((QuadtreeLeaf) node).color;
            for (int i = x; i < x + width; i++) {
                for (int j = y; j < y + height; j++) {
                    pixels[i][j] = color;
                }
            }
        } else if (node instanceof QuadtreeNode) {
            QuadtreeNode internal = (QuadtreeNode) node;
            int halfWidth = width / 2;
            int halfHeight = height / 2;

            decompressQuadtree(internal.topLeft, pixels, x, y, halfWidth, halfHeight);
            decompressQuadtree(internal.topRight, pixels, x + halfWidth, y, halfWidth, halfHeight);
            decompressQuadtree(internal.bottomLeft, pixels, x, y + halfHeight, halfWidth, halfHeight);
            decompressQuadtree(internal.bottomRight, pixels, x + halfWidth, y + halfHeight, halfWidth, halfHeight);
        }
    }

    private abstract class Quadtree implements Serializable {
        int size;
    }

    private class QuadtreeLeaf extends Quadtree {
        int[] color;

        QuadtreeLeaf(int[] color) {
            this.color = color;
        }
    }

    private class QuadtreeNode extends Quadtree {
        Quadtree topLeft, topRight, bottomLeft, bottomRight;

        QuadtreeNode(Quadtree topLeft, Quadtree topRight, Quadtree bottomLeft, Quadtree bottomRight) {
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomLeft = bottomLeft;
            this.bottomRight = bottomRight;
        }
    }
}
