import java.io.*;
import java.awt.Color;

public class Utility implements Serializable {

    static int threshold = 12; // Default threshold value
    static int MIN_SZ = 6; // Minimum size for quadtree division

    public void Compress(int[][][] pixels, String outputFileName, int threshold) throws IOException {
        Node root = buildQuadtree(pixels, 0, 0, pixels.length, pixels[0].length, threshold);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
            oos.writeObject(root); // Serialize the quadtree root to the output file
        }
    }
    
    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        Compress(pixels, outputFileName, threshold); // Overloaded method using the default threshold
    }
    
    private Node buildQuadtree(int[][][] pixels, int x, int y, int width, int height, int threshold) {
        if (entropy(pixels, x, y, width, height) < threshold || width <= MIN_SZ || height <= MIN_SZ) {
            int[] color = getAverageColor(pixels, x, y, width, height);
            return new Node(color, null, x, y, width, height); // Create a leaf node with average color
        }
        // Recursive division into quadrants
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        return new Node(null, new Node[]{
                buildQuadtree(pixels, x, y, halfWidth, halfHeight, threshold),
                buildQuadtree(pixels, x + halfWidth, y, width - halfWidth, halfHeight, threshold),
                buildQuadtree(pixels, x, y + halfHeight, halfWidth, height - halfHeight, threshold),
                buildQuadtree(pixels, x + halfWidth, y + halfHeight, width - halfWidth, height - halfHeight, threshold)
        }, x, y, width, height); // Create an internal node with children
    }
    
    private double entropy(int[][][] pixels, int x, int y, int width, int height) {
        double s = 0;
        int[] avg = getAverageColor(pixels, x, y, width, height);
        Color avgColor = new Color(avg[0], avg[1], avg[2]);
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                Color c = new Color(pixels[i][j][0], pixels[i][j][1], pixels[i][j][2]);
                s += Math.abs(avgColor.getRed() - c.getRed());
                s += Math.abs(avgColor.getGreen() - c.getGreen());
                s += Math.abs(avgColor.getBlue() - c.getBlue());
            }
        }
        return (double) s / (width * height); // Return the average color variation
    }
    

    private int[] getAverageColor(int[][][] pixels, int x, int y, int width, int height) {
        int sumRed = 0, sumGreen = 0, sumBlue = 0, count = 0;
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                sumRed += pixels[i][j][0];
                sumGreen += pixels[i][j][1];
                sumBlue += pixels[i][j][2];
                count++;
            }
        }
        return new int[]{sumRed / count, sumGreen / count, sumBlue / count}; // Return the average RGB color
    }
    

   

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            Object object = ois.readObject();
            if (object instanceof Node) {
                Node root = (Node) object;
                int[][][] decompressedPixels = new int[root.width][root.height][3];
                decompressQuadtree(root, decompressedPixels, 0, 0, root.width, root.height);
                return decompressedPixels; // Return the decompressed pixel array
            } else {
                throw new IOException("Invalid object type in the input file");
            }
        }
    }
    
    private void decompressQuadtree(Node node, int[][][] pixels, int x, int y, int width, int height) {
        if (node.children == null) {
            int[] color = node.color;
            for (int i = x; i < x + width; i++) {
                for (int j = y; j < y + height; j++) {
                    pixels[i][j] = color; // Fill the quadrant with the average color
                }
            }
        } else {
            // Recursive division into quadrants
            int halfWidth = width / 2;
            int halfHeight = height / 2;
            decompressQuadtree(node.children[0], pixels, x, y, halfWidth, halfHeight);
            decompressQuadtree(node.children[1], pixels, x + halfWidth, y, width - halfWidth, halfHeight);
            decompressQuadtree(node.children[2], pixels, x, y + halfHeight, halfWidth, height - halfHeight);
            decompressQuadtree(node.children[3], pixels, x + halfWidth, y + halfHeight, width - halfWidth, height - halfHeight);
        }
    }
    

    // Node class
    private static class Node implements Serializable {
        int[] color; // Average color for leaf nodes
        Node[] children; // Child quadrants for internal nodes
        int x, y, width, height; // Position and dimensions of the quadrant
    
        public Node(int[] color, Node[] children, int x, int y, int width, int height) {
            this.color = color;
            this.children = children;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}

