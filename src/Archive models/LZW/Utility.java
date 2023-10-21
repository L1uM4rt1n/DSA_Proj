import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utility {

    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        // Initialize the dictionary with all possible pixel values
        Map<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put("" + (char) i, i);
        }

        // Initialize the output stream
        BitOutputStream bos = new BitOutputStream(new FileOutputStream(outputFileName));

        // Compress each pixel value using LZW coding
        String current = "";
        for (int[][] row : pixels) {
            for (int[] pixel : row) {
                String next = pixel[0] + "," + pixel[1] + "," + pixel[2];
                Integer code = dictionary.get(current + next);
                if (code != null) {
                    current += next;
                } else {
                    bos.write(dictionary.get(current), 12);
                    if (dictionary.size() < 4096) {
                        dictionary.put(current + next, dictionary.size());
                    }
                    current = next;
                }
            }
        }

        // Write the last code to the output stream
        bos.write(dictionary.get(current), 12);

        // Close the output stream
        bos.close();
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        // Initialize the dictionary with all possible pixel values
        Map<Integer, String> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put(i, "" + (char) i);
        }

        // Initialize the input stream
        BitInputStream bis = new BitInputStream(new FileInputStream(inputFileName));

        // Decompress each code using LZW coding
        List<Integer> codes = new ArrayList<>();
        int code = bis.read(12);
        while (code != -1) {
            codes.add(code);
            code = bis.read(12);
        }

        // Convert the codes back to pixel values
        List<int[]> pixels = new ArrayList<>();
        String current = dictionary.get(codes.get(0));
        for (int i = 1; i < codes.size(); i++) {
            int nextCode = codes.get(i);
            String next;
            if (dictionary.containsKey(nextCode)) {
                next = dictionary.get(nextCode);
            } else {
                next = current + current.charAt(0);
            }
            pixels.add(parsePixel(current));
            if (dictionary.size() < 4096) {
                dictionary.put(dictionary.size(), current + next.charAt(0));
            }
            current = next;
        }
        pixels.add(parsePixel(current));

        // Convert the list of pixels to a 3D array
        int[][][] result = new int[pixels.size() / 256][256][3];
        for (int i = 0; i < pixels.size(); i++) {
            int[][] row = result[i / 256];
            int[] pixel = row[i % 256];
            System.arraycopy(pixels.get(i), 0, pixel, 0, 3);
        }

        // Close the input stream
        bis.close();

        // Return the decompressed pixel array
        return result;
    }

    private int[] parsePixel(String s) {
        String[] parts = s.split(",");
        int[] result = new int[3];
        for (int i = 0; i < 3; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
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