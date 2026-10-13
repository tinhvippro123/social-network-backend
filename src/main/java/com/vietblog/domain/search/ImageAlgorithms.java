package com.vietblog.domain.search;

import java.awt.Color;
import java.awt.image.BufferedImage;

/** HSV histogram, Sobel orientation histogram and horizontal GLCM statistics. */
public final class ImageAlgorithms {
    public static final int DIMENSIONS = 51;
    private ImageAlgorithms() {}

    public static double[] features(BufferedImage image) {
        int size = 64;
        double[] vector = new double[DIMENSIONS];
        double[][] gray = new double[size][size];
        int[][] levels = new int[size][size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int pixel = image.getRGB(x * image.getWidth() / size, y * image.getHeight() / size);
            int r = (pixel >>> 16) & 255, g = (pixel >>> 8) & 255, b = pixel & 255;
            float[] hsv = Color.RGBtoHSB(r, g, b, null);
            int bin = Math.min(7, (int) (hsv[0] * 8)) * 4 + Math.min(1, (int) (hsv[1] * 2)) * 2 + Math.min(1, (int) (hsv[2] * 2));
            vector[bin]++;
            gray[y][x] = 0.299 * r + 0.587 * g + 0.114 * b;
            levels[y][x] = Math.min(7, (int) gray[y][x] / 32);
        }
        normalize(vector, 0, 32, 0.7);
        for (int y = 1; y < size - 1; y++) for (int x = 1; x < size - 1; x++) {
            double gx = -gray[y-1][x-1] + gray[y-1][x+1] - 2*gray[y][x-1] + 2*gray[y][x+1] - gray[y+1][x-1] + gray[y+1][x+1];
            double gy = -gray[y-1][x-1] - 2*gray[y-1][x] - gray[y-1][x+1] + gray[y+1][x-1] + 2*gray[y+1][x] + gray[y+1][x+1];
            double angle = (Math.atan2(gy, gx) + Math.PI) / (2 * Math.PI);
            vector[32 + Math.min(15, (int) (angle * 16))] += Math.hypot(gx, gy);
        }
        normalize(vector, 32, 48, 0.2);
        double[][] glcm = new double[8][8];
        double pairs = size * (size - 1) * 2.0;
        for (int y = 0; y < size; y++) for (int x = 0; x < size - 1; x++) { glcm[levels[y][x]][levels[y][x+1]]++; glcm[levels[y][x+1]][levels[y][x]]++; }
        for (int i = 0; i < 8; i++) for (int j = 0; j < 8; j++) {
            double p = glcm[i][j] / pairs;
            vector[48] += p * (i-j) * (i-j) / 49.0;
            vector[49] += p * p;
            vector[50] += p / (1 + Math.abs(i-j));
        }
        normalize(vector, 48, 51, 0.1);
        return vector;
    }

    private static void normalize(double[] values, int from, int to, double weight) {
        double sum = 0;
        for (int i = from; i < to; i++) sum += values[i] * values[i];
        if (sum == 0) return;
        for (int i = from; i < to; i++) values[i] = values[i] / Math.sqrt(sum) * weight;
    }

    public static double cosine(double[] left, double[] right) {
        if (left.length != right.length) return 0;
        double dot = 0, a = 0, b = 0;
        for (int i = 0; i < left.length; i++) { dot += left[i]*right[i]; a += left[i]*left[i]; b += right[i]*right[i]; }
        return a == 0 || b == 0 ? 0 : Math.min(1, dot / Math.sqrt(a*b));
    }
}
