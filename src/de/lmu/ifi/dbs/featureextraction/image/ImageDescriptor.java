package de.lmu.ifi.dbs.featureextraction.image;

import de.lmu.ifi.dbs.linearalgebra.Matrix;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Represents a description of a jpg image including color histogram, color moments and
 * 13 Haralick texture features.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class ImageDescriptor {
  /**
   * Contains the distances of the neighboring pixels for one pixel.
   */
  static final int[] DISTANCES = {1, 3, 5};

  /**
   * Contains the feature names.
   */
  static final String[] featureNames = {"colorhistogram", "colormoments",
  "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10", "f11", "f12", "f13"};

  /**
   * Contains the number of attributes for the first two features.
   */
  static final int[] numAttributes = new int[15];

  static {
    numAttributes[0] = 32;
    numAttributes[1] = 9;
    for (int i = 2; i < numAttributes.length; i++) {
      numAttributes[i] = DISTANCES.length;
    }
  }

  /**
   * The default weight for red samples in the conversion, 0.3f.
   */
  private static final double DEFAULT_RED_WEIGHT = 0.3f;

  /**
   * The default weight for green samples in the conversion, 0.59f.
   */
  private static final double DEFAULT_GREEN_WEIGHT = 0.59f;

  /**
   * The default weight for blue samples in the conversion, 0.11f.
   */
  private static final double DEFAULT_BLUE_WEIGHT = 0.11f;

  /**
   * The constant for shifting the rgb value for getting the red value.
   */
  private static final int RED_SHIFT = 16;

  /**
   * The constant for shifting the rgb value for getting the green value.
   */
  private static final int GREEN_SHIFT = 8;

  /**
   * The constant for shifting the rgb value for getting the blue value.
   */
  private static final int BLUE_SHIFT = 0;

  /**
   * The number of gray values:
   */
  private static final int NUM_GRAY_VALUES = 16;

  /**
   * The scale for the gray values for conversion hsv to gray values.
   */
  public static final double GRAY_SCALE = 256 / NUM_GRAY_VALUES;

  /**
   * The range of the h values.
   */
  private static final int H_RANGES = 8;

  /**
   * The range of the s values.
   */
  private static final int S_RANGES = 4;

  /**
   * Indicates whether the underlying image is empty.
   */
  private boolean notEmpty;

  /**
   * The name of the underlying image.
   */
  private String imageName;

  /**
   * The class id of the underlying image.
   */
  private Integer classID;

  /**
   * The width of the underlying image.
   */
  private int width;

  /**
   * Contains the hsv values of the image.
   */
  private double[][] hsvValues;

  /**
   * Contains the mean value of the hsv values of the underlying image.
   */
  private double meanHSV[] = new double[3];

  /**
   * Contains the standard deviation of the hsv values.
   */
  private double standardDeviationsHSV[] = new double[3];

  /**
   * Contains the skewness of the hsv values.
   */
  private double skewnessHSV[] = new double[3];

  /**
   * Contains the color histogram of the image.
   */
  private double colorHistogram[] = new double[H_RANGES * S_RANGES];

  /**
   * The value for one increment in the color histogram.
   */
  private double histogramIncrement;

  /**
   * Contains the gray values of each pixel of the image.
   */
  private byte[] grayValue;

  /**
   * Contains the mean value of the gray values of the underlying image.
   */
  private double meanGrayValue;

  /**
   * The cooccurrence matrices for each neighboring distance value and for the
   * different orientations and one summarized orientation.
   */
  private Matrix[] cooccurrenceMatrices = new Matrix[DISTANCES.length];

  /**
   * Contains the sum of the entries of each cooccurrence matrix.
   */
  private double[] sums = new double[DISTANCES.length];

  /**
   * Contains the row mean value of each cooccurrence matrix.
   */
  private double[] mu_x = new double[DISTANCES.length];

  /**
   * Contains the column mean value of each cooccurrence matrix.
   */
  private double[] mu_y = new double[DISTANCES.length];

  /**
   * Contains the row variance of each cooccurrence matrix.
   */
  private double[] var_x = new double[DISTANCES.length];

  /**
   * Contains the column variance of each cooccurrence matrix.
   */
  private double[] var_y = new double[DISTANCES.length];

  /**
   * Contains the p_x statistics of each cooccurrence matrix.
   */
  private double[][] p_x = new double[DISTANCES.length][NUM_GRAY_VALUES];

  /**
   * Contains the p_y statistics of each cooccurrence matrix.
   */
  private double[][] p_y = new double[DISTANCES.length][NUM_GRAY_VALUES];

  /**
   * Contains the p_(x+y) statistics of each cooccurrence matrix.
   */
  private double[][] p_x_plus_y = new double[DISTANCES.length][2 * NUM_GRAY_VALUES - 1];

  /**
   * Contains the p_(x-y) statistics of each cooccurrence matrix.
   */
  private double[][] p_x_minus_y = new double[DISTANCES.length][NUM_GRAY_VALUES];

  /**
   * Contains the HXY1 statistics of each cooccurrence matrix.
   */
  private double[] hx = new double[DISTANCES.length];

  /**
   * Contains the HXY2 statistics of each cooccurrence matrix.
   */
  private double[] hy = new double[DISTANCES.length];

  /**
   * Contains the HXY1 statistics of each cooccurrence matrix.
   */
  private double[] hxy1 = new double[DISTANCES.length];

  /**
   * Contains the HXY2 statistics of each cooccurrence matrix.
   */
  private double[] hxy2 = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f1 for each cooccurrence matrix.
   */
  private double f1[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f2 for each cooccurrence matrix.
   */
  private double f2[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f3 for each cooccurrence matrix.
   */
  private double f3[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f4 for each cooccurrence matrix.
   */
  private double f4[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f5 for each cooccurrence matrix.
   */
  private double f5[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f6 for each cooccurrence matrix.
   */
  private double f6[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f7 for each cooccurrence matrix.
   */
  private double f7[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f8 for each cooccurrence matrix.
   */
  private double f8[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f9 for each cooccurrence matrix.
   */
  private double f9[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f10 for each cooccurrence matrix.
   */
  private double f10[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f11 for each cooccurrence matrix.
   */
  private double f11[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f12 for each cooccurrence matrix.
   */
  private double f12[] = new double[DISTANCES.length];

  /**
   * The Haralick texture feature f13 for each cooccurrence matrix.
   */
  private double f13[] = new double[DISTANCES.length];

  /**
   * Creates a new image descriptor for the specified image.
   *
   * @param image the image for which the description should be created
   */
  public ImageDescriptor(BufferedImage image) {
    // init width, heigt and size
    this.width = image.getWidth(null);
    int height = image.getHeight(null);
    int size = height * width;
    // set the value for one increment in the color histogram
    this.histogramIncrement = 1.0f / size;
    // init the arrays for the gray values and the hsv values
    this.grayValue = new byte[size];
    this.hsvValues = new double[size][3];
    // image is not empty per default
    this.notEmpty = false;
    // init cooccurrence matrices
    for (int d = 0; d < DISTANCES.length; d++) {
      cooccurrenceMatrices[d] = new Matrix(NUM_GRAY_VALUES, NUM_GRAY_VALUES);
    }

    // calculate hsv and gray values, color histogram and cooccurrence matrix
    // for each pixel
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int pos = width * y + x;
        calculateValues(pos, image.getRGB(x, y), x, y);

        // update mean
        meanGrayValue += grayValue[pos];
        for (int i = 0; i < 3; i++) {
          meanHSV[i] += hsvValues[pos][i];
        }
      }
    }

    // update mean
    meanGrayValue /= size;
    for (int i = 0; i < 3; i++) {
      meanHSV[i] /= hsvValues.length;
    }

    // if image is not empty: calculate moments and statistics
    if (notEmpty) {
      calculateMoments();
      calculateFeatures();
    }
  }

  /**
   * Returns true if the image is empty, false otherwise.
   * @return true if the image is empty, false otherwise
   */
  public boolean isEmpty() {
    return !notEmpty;
  }

  /**
   * Calculates the second and third moment of the hsv values
   * (standard deviation and skewness).
   */
  private void calculateMoments() {
    double[] sum_2 = new double[3];
    double[] sum_3 = new double[3];

    // sum^2 and sum^3
    for (double[] hsvValue : hsvValues) {
      for (int j = 0; j < 3; j++) {
        double delta = hsvValue[j] - meanHSV[j];
        double square_delta = delta * delta;
        sum_2[j] += square_delta;
        sum_3[j] += square_delta * delta;
      }
    }

    // standard deviation and skewness
    for (int i = 0; i < standardDeviationsHSV.length; i++) {
      standardDeviationsHSV[i] = Math.sqrt(sum_2[i] / hsvValues.length);
      if (Double.isNaN(standardDeviationsHSV[i])) {
        standardDeviationsHSV[i] = 0;
      }

      double s_3 = standardDeviationsHSV[i] * standardDeviationsHSV[i] * standardDeviationsHSV[i];
      skewnessHSV[i] = sum_3[i] / ((hsvValues.length - 1) * s_3);
      if (Double.isNaN(skewnessHSV[i])) {
        skewnessHSV[i] = 0;
      }
    }
  }

  /**
   * Calculates the texture features.
   */
  private void calculateFeatures() {
    calculateStatistics();

    for (int d = 0; d < DISTANCES.length; d++) {
      for (int i = 0; i < NUM_GRAY_VALUES; i++) {
        double sum_j_p_x_minus_y = 0;
        for (int j = 0; j < NUM_GRAY_VALUES; j++) {
          double p_ij = cooccurrenceMatrices[d].get(i, j);

          sum_j_p_x_minus_y += j * p_x_minus_y[d][j];

          f1[d] += p_ij * p_ij;
          f3[d] += i * j * p_ij - mu_x[d] * mu_y[d];
          f4[d] += (i - meanGrayValue) * (i - meanGrayValue) * p_ij;
          f5[d] += p_ij / (1 + (i - j) * (i - j));
          f9[d] += p_ij * log(p_ij);
        }

        f2[d] += i * i * p_x_minus_y[d][i];
        f10[d] += (i - sum_j_p_x_minus_y) * (i - sum_j_p_x_minus_y) * p_x_minus_y[d][i];
        f11[d] += p_x_minus_y[d][i] * log(p_x_minus_y[d][i]);
      }

      f3[d] /= Math.sqrt(var_x[d] * var_y[d]);
      f9[d] *= -1;
      f11[d] *= -1;
      f12[d] = (f9[d] - hxy1[d]) / Math.max(hx[d], hy[d]);
      f13[d] = Math.sqrt(1 - Math.exp(-2 * (hxy2[d] - f9[d])));

      for (int i = 0; i < 2 * NUM_GRAY_VALUES - 1; i++) {
        f6[d] += i * p_x_plus_y[d][i];
        f8[d] += p_x_plus_y[d][i] * log(p_x_plus_y[d][i]);

        double sum_j_p_x_plus_y = 0;
        for (int j = 0; j < 2 * NUM_GRAY_VALUES - 1; j++) {
          sum_j_p_x_plus_y += j * p_x_plus_y[d][j];
        }
        f7[d] += (i - sum_j_p_x_plus_y) * (i - sum_j_p_x_plus_y) * p_x_plus_y[d][i];
      }

      f8[d] *= -1;
    }
  }

  /**
   * Calculates the statistical properties.
   */
  private void calculateStatistics() {
    for (int d = 0; d < DISTANCES.length; d++) {
      // normalize the cooccurrence matrix
      cooccurrenceMatrices[d].timesEquals(1 / sums[d]);

      // p_x, p_y, p_x+y, p_x-y
      for (int i = 0; i < NUM_GRAY_VALUES; i++) {
        for (int j = 0; j < NUM_GRAY_VALUES; j++) {
          double p_ij = cooccurrenceMatrices[d].get(i, j);

          p_x[d][i] += p_ij;
          p_y[d][j] += p_ij;

          p_x_plus_y[d][i + j] += p_ij;
          p_x_minus_y[d][Math.abs(i - j)] += p_ij;
        }
      }

      // mean values
      for (int i = 0; i < NUM_GRAY_VALUES; i++) {
        mu_x[d] += i * p_x[d][i];
        mu_y[d] += i * p_y[d][i];
      }

      for (int i = 0; i < NUM_GRAY_VALUES; i++) {
        // variances
        var_x[d] += (i - mu_x[d]) * (i - mu_x[d]) * p_x[d][i];
        var_y[d] += (i - mu_y[d]) * (i - mu_y[d]) * p_y[d][i];

        // hx and hy
        hx[d] += p_x[d][i] * log(p_x[d][i]);
        hy[d] += p_y[d][i] * log(p_y[d][i]);

        // hxy1 and hxy2
        for (int j = 0; j < NUM_GRAY_VALUES; j++) {
          double p_ij = cooccurrenceMatrices[d].get(i, j);
          hxy1[d] += p_ij * log(p_x[d][i] * p_y[d][j]);
          hxy2[d] += p_x[d][i] * p_y[d][j] * log(p_x[d][i] * p_y[d][j]);
        }
      }
      hx[d] *= -1;
      hy[d] *= -1;
      hxy1[d] *= -1;
      hxy2[d] *= -1;
    }
  }

  /**
   * Returns the logarithm of the specified value.
   *
   * @param value the value for which the logarithm should be returned
   * @return the logarithm of the specified value
   */
  private double log(double value) {
    if (value == 0) return 0;
    return Math.log(value);
  }

  /**
   * Calculates the hsv values, gray values, color histogram entry and
   * the entry in the coocurrence matrix for the specified pixel
   *
   * @param pos the flatened osition of the pixel
   * @param rgb the rgb value of the pixel
   * @param x   the coordinate of the pixel
   * @param y   the y coordinate of the pixel
   */
  @SuppressWarnings({"UnusedAssignment"})
  private void calculateValues(int pos, int rgb, int x, int y) {
    int r = ((rgb >> RED_SHIFT) & 0xff);
    int g = (rgb >> GREEN_SHIFT) & 0xff;
    //noinspection PointlessBitwiseExpression
    int b = (rgb >> BLUE_SHIFT) & 0xff;

    // check if image is empty
    if (!notEmpty && (r > 0 || g > 0 || b > 0)) {
      notEmpty = true;
    }

    // determine hsv value
    hsvValues[pos] = RGBtoHSV(r / 255.0, g / 255.0, b / 255.0);

    // determine gray value
    grayValue[pos] = (byte) ((r * DEFAULT_RED_WEIGHT +
                              g * DEFAULT_GREEN_WEIGHT +
                              b * DEFAULT_BLUE_WEIGHT) / GRAY_SCALE);
    //check gray value
    if (grayValue[pos] >= NUM_GRAY_VALUES) {
      throw new RuntimeException("Should never happen!");
    }

    // color histogram entry
    double h = hsvValues[pos][0];
    double s = hsvValues[pos][1];
    int index = ((int) (((H_RANGES - 1) * h / 360f)) + ((H_RANGES - 1) * (int) ((S_RANGES) * s)));
    if (index > (colorHistogram.length - 1)) {
      index = (colorHistogram.length - 1);
    }
    colorHistogram[index] += histogramIncrement;

    for (int k = 0; k < DISTANCES.length; k++) {
      int d = DISTANCES[k];
      // horizontal neighbor: 0 degrees
      int i = x - d;
      int j = y;
      if (!(i < 0)) {
        increment(grayValue[pos], grayValue[pos - d], k);
      }

      // vertical neighbor: 90 degree
      i = x;
      j = y - d;
      if (!(j < 0)) {
        increment(grayValue[pos], grayValue[pos - d * width], k);
      }

      // 45 degree diagonal neigbor
      i = x + d;
      j = y - d;
      if (i < width && !(j < 0)) {
        increment(grayValue[pos], grayValue[pos + d - d * width], k);
      }

      // 135 vertical neighbor
      i = x - d;
      j = y - d;
      if (!(i < 0) && !(j < 0)) {
        increment(grayValue[pos], grayValue[pos - d - d * width], k);
      }
    }
  }

  /**
   * Incremets the specified coocurrence matrix and the summarized coocurrence matrix
   * of the specified distance value d at the specified positions (g1,g2) and (g2,g1).
   *
   * @param g1 the gray value of the first pixel
   * @param g2 the gray value of the second pixel
   * @param d  the index of the distance value specifiying the coocurrence matrix
   */
  private void increment(int g1, int g2, int d) {
    cooccurrenceMatrices[d].increment(g1, g2, 1);
    cooccurrenceMatrices[d].increment(g2, g1, 1);
    sums[d] += 2;
  }

  /**
   * Transforms the specified rgb value to the corresponding hsv value.
   *
   * @param r the r value
   * @param g the g value
   * @param b the b value
   * @return the hsv values for the specified rgb value
   */
  private double[] RGBtoHSV(double r, double g, double b) {
    double[] hsv = new double[3];

    double min = min(r, g, b);
    double max = max(r, g, b);

    // h value
    if (max == min) {
      hsv[0] = 0;
    }
    else if (r == max) {
      hsv[0] = ((g - b) / (max - min)) * 60;
    }
    else if (g == max) {
      hsv[0] = (2 + (b - r) / (max - min)) * 60;
    }
    else if (b == max) {
      hsv[0] = (4 + (r - g) / (max - min)) * 60;
    }
    if (hsv[0] < 0) hsv[0] = hsv[0] + 360;

    // s value
    if (max == 0) {
      hsv[1] = 0;
    }
    else {
      hsv[1] = (max - min) / max;
    }

    // v value
    hsv[2] = max;

    return hsv;
  }

  /**
   * Returns the maximum of the three specified double values.
   * @param r first value
   * @param g secon value
   * @param b third value
   * @return the maximum of the three specified double values
   */
  private double max(double r, double g, double b) {
    return Math.max(r, Math.max(g, b));
  }

  /**
   * Returns the minimum of the three specified double values.
   * @param r first value
   * @param g secon value
   * @param b third value
   * @return the minimum of the three specified double values
   */
  private double min(double r, double g, double b) {

    return Math.min(r, Math.min(g, b));
  }

  /**
   * Writes the color histogram of the image with the specified writer.
   *
   * @param writer the writer to write the color histograms
   */
  public void writeColorHistogram(String separator, String classPrefix, BufferedWriter writer) throws IOException {
    writer.write(imageName);
    for (double ch : colorHistogram) {
      writer.write(separator);
      writer.write(String.valueOf(ch));
    }
    writer.write(separator + classPrefix + classID);
    writer.newLine();
  }

  /**
   * Writes the color moments of the image with the specified writer.
   *
   * @param writer the writer to write the color histograms
   */
  public void writeColorMoments(String separator, String classPrefix, BufferedWriter writer) throws IOException {
    writer.write(imageName);
    for (double mean : meanHSV) {
      writer.write(separator);
      writer.write(String.valueOf(mean));
    }
    for (double stdDevHSV : standardDeviationsHSV) {
      writer.write(separator);
      writer.write(String.valueOf(stdDevHSV));
    }
    for (double skewHSV : skewnessHSV) {
      writer.write(separator);
      writer.write(String.valueOf(skewHSV));
    }
    writer.write(separator + classPrefix + classID);
    writer.newLine();
  }

  /**
   * Writes the 13 texture features of each orientation with the specified writers.
   *
   * @param writers the 13 writers to write the 13 texture features
   */
  public void writeTextureFeatures(String separator, String classPrefix, BufferedWriter[] writers) throws IOException {
    if (writers.length != 13)
      throw new IllegalArgumentException("Wrong number of writers!");

    int i = 0;
    writeFeature(f1, separator, classPrefix, writers[i++]);
    writeFeature(f2, separator, classPrefix, writers[i++]);
    writeFeature(f3, separator, classPrefix, writers[i++]);
    writeFeature(f4, separator, classPrefix, writers[i++]);
    writeFeature(f5, separator, classPrefix, writers[i++]);
    writeFeature(f6, separator, classPrefix, writers[i++]);
    writeFeature(f7, separator, classPrefix, writers[i++]);
    writeFeature(f8, separator, classPrefix, writers[i++]);
    writeFeature(f9, separator, classPrefix, writers[i++]);
    writeFeature(f10, separator, classPrefix, writers[i++]);
    writeFeature(f11, separator, classPrefix, writers[i++]);
    writeFeature(f12, separator, classPrefix, writers[i++]);
    writeFeature(f13, separator, classPrefix, writers[i]);
  }

  /**
   * Writes the specified feature to output.
   * @param feature the feature to write
   * @param separator the separator between the single attributes (e.g. comma or whitespace)
   * @param classPrefix the prefix for the class label
   * @param writer the writer to write to
   * @throws IOException
   */
  private void writeFeature(double[] feature, String separator, String classPrefix, BufferedWriter writer) throws IOException {
    writer.write(imageName);
    for (double f : feature) {
      writer.write(separator);
      writer.write(String.valueOf(f));
    }
    writer.write(separator + classPrefix + classID);

    writer.newLine();
  }

  /**
   * Sets the name of the underlying image.
   *
   * @param imageName the name of the image to be set
   */
  public void setImageName(String imageName) {
    this.imageName = imageName;
  }

  /**
   * Sets the class id for the underlying image.
   *
   * @param classID the id of the class to be set
   */
  public void setClassID(Integer classID) {
    this.classID = classID;
  }
}
