package de.lmu.ifi.dbs.featureextraction.image;

import de.lmu.ifi.dbs.linearalgebra.Matrix;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

// -in "P:\home\wiss\achtert\Stock4b\Classes" -out  "C:\results" -class "P:\home\wiss\achtert\Stock4b\Classes\Classes.txt" -verbose -time
// -in "C:\workspace\TextureCluster\bilder" -out  "C:\results" -class "C:\workspace\TextureCluster\bilder\classes.txt" -verbose -time

/**
 * Represents a description of a jpg image including color histogram, color moments and
 * 13 Haralick texture features, Roughness and Facet-Orientation.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class ImageDescriptor {
  /**
   * Contains the distances of the neighboring pixels for one pixel.
   */
  static final int[] DISTANCES = {1, 3, 5, 7};

  /**
   * Contains the feature names.
   */
  static final String[] featureNames = {"colorhistogram_hs", "colormoment_mean", "colormoment_stdd", "colormoment_skew",
  "haralick_01", "haralick_02", "haralick_03", "haralick_04", "haralick_05", "haralick_06", "haralick_07", "haralick_08", "haralick_09", "haralick_10", "haralick_11", "haralick_12", "haralick_13",
  "roughness_rv", "roughness_rp", "roughness_rt", "roughness_rm", "roughness_ra", "roughness_rq", "roughness_rsk", "roughness_rku",
  "facet_min", "facet_max", "facet_med", "facet_mean", "facet_stdd", "facet_skew", "facet_kurt", "facet_area", "facet_white", "facet_polar"};

  /**
   * Contains the number of attributes for the first two features.
   */
  static final int[] numAttributes = new int[35];

  static {
    numAttributes[0] = 32;
    numAttributes[1] = 3;
    numAttributes[2] = 3;
    numAttributes[3] = 3;
    for (int i = 4; i < 4 + 13; i++) {
      numAttributes[i] = DISTANCES.length;  // haralick
    }
    for (int i = 17; i < 17 + 8; i++) {
      numAttributes[i] = 6;  // roughness
    }
    for (int i = 25; i < 25 + 9; i++) {
      numAttributes[i] = 6;  // facet-orientation
    }
//    numAttributes[34] = 36;
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
  private static final int NUM_GRAY_VALUES = 32;

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
   * Contains the quantized gray values of each pixel of the image.
   */
  private byte[] grayValue;

  /**
   * Contains the gray values of each pixel of the image (in the range [0..1]).
   */
  private float[] grayPixel;

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
   * The Haralick texture feature f1 - f13 for each cooccurrence matrix.
   */
  private double[] haralick_01 = new double[DISTANCES.length];
  private double[] haralick_02 = new double[DISTANCES.length];
  private double[] haralick_03 = new double[DISTANCES.length];
  private double[] haralick_04 = new double[DISTANCES.length];
  private double[] haralick_05 = new double[DISTANCES.length];
  private double[] haralick_06 = new double[DISTANCES.length];
  private double[] haralick_07 = new double[DISTANCES.length];
  private double[] haralick_08 = new double[DISTANCES.length];
  private double[] haralick_09 = new double[DISTANCES.length];
  private double[] haralick_10 = new double[DISTANCES.length];
  private double[] haralick_11 = new double[DISTANCES.length];
  private double[] haralick_12 = new double[DISTANCES.length];
  private double[] haralick_13 = new double[DISTANCES.length];

  /**
   * The Roughness statistics of the picture.
   */
  private double[] roughness_rv = new double[6];
  private double[] roughness_rp = new double[6];
  private double[] roughness_rt = new double[6];
  private double[] roughness_rm = new double[6];
  private double[] roughness_ra = new double[6];
  private double[] roughness_rq = new double[6];
  private double[] roughness_rsk = new double[6];
  private double[] roughness_rku = new double[6];

  /**
   * The Facet-Orientation statistics of the picture.
   */
  private double[] facet_min = new double[6];
  private double[] facet_max = new double[6];
  private double[] facet_med = new double[6];
  private double[] facet_mean = new double[6];
  private double[] facet_stdd = new double[6];
  private double[] facet_skew = new double[6];
  private double[] facet_kurt = new double[6];
  private double[] facet_area = new double[6];
  private double[] facet_white = new double[6];
//  private double[] facet_polar = new double[36];


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
    // init the arrays for the gray values, gray pixels and the hsv values
    this.grayValue = new byte[size];
    this.grayPixel = new float[size];
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

    // co-occurence debugging
/*    if (false) {
      try {
        for (int d = 0; d < DISTANCES.length; d++) {
          BufferedWriter cooccurrenceWriter = new BufferedWriter(new FileWriter("cooccurrence_" + DISTANCES[d] + ".txt"));
          for (int y = 0; y < NUM_GRAY_VALUES; y++) {
            for (int x = 0; x < NUM_GRAY_VALUES; x++) {
              double value = cooccurrenceMatrices[d].get(x, y);

//              if (value > 0)
//            	  value = Math.log(value);
//              else
//            	  value = 0;

              cooccurrenceWriter.write(Double.toString(value));
              if ((x + 1) < NUM_GRAY_VALUES) {
                cooccurrenceWriter.write(", ");
              }
            }
            cooccurrenceWriter.write("\n");
          }
          cooccurrenceWriter.flush();
          cooccurrenceWriter.close();
        }
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
*/

    // update mean
    meanGrayValue /= size;
    for (int i = 0; i < 3; i++) {
      meanHSV[i] /= hsvValues.length;
    }

    // if image is not empty: calculate moments and statistics
    if (notEmpty) {
      calculateMoments();
      calculateFeatures();

      calculateRoughness(grayPixel, width, height, false, false, 0, 5.0, 0.12, 0);
      calculateRoughness(grayPixel, width, height, false, true, 0, 5.0, 0.12, 1);
      calculateRoughness(grayPixel, width, height, false, true, 1, 5.0, 0.12, 2);
      calculateRoughness(grayPixel, width, height, true, false, 0, 5.0, 0.12, 3);
      calculateRoughness(grayPixel, width, height, true, true, 0, 5.0, 0.12, 4);
      calculateRoughness(grayPixel, width, height, true, true, 1, 5.0, 0.12, 5);
    }
  }

  /**
   * Returns true if the image is empty, false otherwise.
   *
   * @return true if the image is empty, false otherwise
   */
  public boolean isEmpty() {
    return !notEmpty;
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
    double gray = (r * DEFAULT_RED_WEIGHT + g * DEFAULT_GREEN_WEIGHT + b * DEFAULT_BLUE_WEIGHT) / (DEFAULT_RED_WEIGHT + DEFAULT_GREEN_WEIGHT + DEFAULT_BLUE_WEIGHT);
//    double gray = (r + g + b) / 3.0;	// unweighted conversion
    grayValue[pos] = (byte) (gray / GRAY_SCALE);  // quantized for texture analysis
    grayPixel[pos] = (float) (gray / 255.0);  // full resolution for gradient analysis

    // check gray value
    if (grayValue[pos] >= NUM_GRAY_VALUES) {
      throw new RuntimeException("Should never happen!");
    }

    // color histogram entry
    double h = hsvValues[pos][0];  // hue
    double s = hsvValues[pos][1];  // saturation
/*
    // buggy, mixes H_RANGES and S_RANGES
    int index = ((int) ((H_RANGES - 1) * h) + ((H_RANGES - 1) * (int) ((S_RANGES) * s)));
    if (index > (colorHistogram.length - 1)) {
      index = (colorHistogram.length - 1);
    }
    colorHistogram[index] += histogramIncrement;
*/
    int hindex = (int) (H_RANGES * h);
    if (hindex >= H_RANGES) {
      hindex = H_RANGES - 1;
    }
    int sindex = (int) (S_RANGES * s);
    if (sindex >= S_RANGES) {
      sindex = S_RANGES - 1;
    }
    colorHistogram[hindex + sindex * H_RANGES] += histogramIncrement;  // sector/shell model for the colors

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
   * Transforms the specified rgb value to the corresponding hsv (Hue Saturation Value) value.
   *
   * @param r the r value
   * @param g the g value
   * @param b the b value
   * @return the hsv values for the specified rgb value (normalized to the range [0..1])
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
    hsv[0] /= 360;	// normalize

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
   *
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
   *
   * @param r first value
   * @param g secon value
   * @param b third value
   * @return the minimum of the three specified double values
   */
  private double min(double r, double g, double b) {

    return Math.min(r, Math.min(g, b));
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
   * Calculates the Haralick texture features.
   */
  private void calculateFeatures() {
    calculateStatistics();

    for (int d = 0; d < DISTANCES.length; d++) {
      for (int i = 0; i < NUM_GRAY_VALUES; i++) {
        double sum_j_p_x_minus_y = 0;
        for (int j = 0; j < NUM_GRAY_VALUES; j++) {
          double p_ij = cooccurrenceMatrices[d].get(i, j);

          sum_j_p_x_minus_y += j * p_x_minus_y[d][j];

          haralick_01[d] += p_ij * p_ij;
          haralick_03[d] += i * j * p_ij - mu_x[d] * mu_y[d];
          haralick_04[d] += (i - meanGrayValue) * (i - meanGrayValue) * p_ij;
          haralick_05[d] += p_ij / (1 + (i - j) * (i - j));
          haralick_09[d] += p_ij * log(p_ij);
        }

        haralick_02[d] += i * i * p_x_minus_y[d][i];
        haralick_10[d] += (i - sum_j_p_x_minus_y) * (i - sum_j_p_x_minus_y) * p_x_minus_y[d][i];
        haralick_11[d] += p_x_minus_y[d][i] * log(p_x_minus_y[d][i]);
      }

      haralick_03[d] /= Math.sqrt(var_x[d] * var_y[d]);
      haralick_09[d] *= -1;
      haralick_11[d] *= -1;
      haralick_12[d] = (haralick_09[d] - hxy1[d]) / Math.max(hx[d], hy[d]);
      haralick_13[d] = Math.sqrt(1 - Math.exp(-2 * (hxy2[d] - haralick_09[d])));

      for (int i = 0; i < 2 * NUM_GRAY_VALUES - 1; i++) {
        haralick_06[d] += i * p_x_plus_y[d][i];
        haralick_08[d] += p_x_plus_y[d][i] * log(p_x_plus_y[d][i]);

        double sum_j_p_x_plus_y = 0;
        for (int j = 0; j < 2 * NUM_GRAY_VALUES - 1; j++) {
          sum_j_p_x_plus_y += j * p_x_plus_y[d][j];
        }
        haralick_07[d] += (i - sum_j_p_x_plus_y) * (i - sum_j_p_x_plus_y) * p_x_plus_y[d][i];
      }

      haralick_08[d] *= -1;
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
  

  /*
    * SURFACE LEVELING
    * Check Level surface to perform tilt correction. This is accomplished by scanning
    * the surface profile and calculating a regression plane according to Bhattacharyya
    * and Johnson (1997). The images are then horizontally aligned by subtracting the
    * regression plane from the surface profile according to the following equation:
    * z_i = z_i - (a + b_1 * i_1 + b_2 * i_2)
    */
  private float[] levelSurface(float[] pixels, int ww, int hh, int iter) {

    float[] tempArray = new float[ww * hh];
    System.arraycopy(pixels, 0, tempArray, 0, ww * hh);
    
    int n = ww * hh;
    
    // iterate several times (default 4)
    for (int it = 0; it < iter; it++) {
      float Ex1, Ex1x1, Ex2x2, Ex1x2, Ey, Ex2, Ex1y, Ex2y;
      float x1Mean, x2Mean, yMean;
      float Sx1y, Sx2y, Sx1x2, Sx1x1, Sx2x2, b1, b2, b0;

      Ex1 = 0;
      Ex1x1 = 0;
      Ex2x2 = 0;
      Ex1x2 = 0;
      Ey = 0;
      Ex2 = 0;
      Ex1y = 0;
      Ex2y = 0;
      
      // go through the pixel values in the image
      // x1 og x2 are the pixel coordinates, y are the topography values
      for (int x2 = 0; x2 < hh; x2++) {
        for (int x1 = 0; x1 < ww; x1++) {
          int index = x1 + ww * x2;
          // int index2 = x2 + hh*x1;
          float y = tempArray[index];

          Ex1 = Ex1 + x1;      // sums x, y...
          Ex2 = Ex2 + x2;
          Ey = Ey + y;
          Ex1x1 = Ex1x1 + x1 * x1;  // sums of squares and
          Ex2x2 = Ex2x2 + x2 * x2;
          Ex1x2 = Ex1x2 + x1 * x2;  // cross products of variables
          Ex1y = Ex1y + x1 * y;
          Ex2y = Ex2y + x2 * y;
        }
      }

      yMean = Ey / n;
      x1Mean = Ex1 / n;
      x2Mean = Ex2 / n;
      Sx1x2 = Ex1x2 - (n * (x1Mean) * (x2Mean));
      Sx1x1 = Ex1x1 - (n * (x1Mean) * (x1Mean));
      Sx2x2 = Ex2x2 - (n * (x2Mean) * (x2Mean));
      Sx1y = Ex1y - (n * (x1Mean) * (yMean));
      Sx2y = Ex2y - (n * (x2Mean) * (yMean));

      // calculate the values of the least square estimates
      b1 = ((Sx2x2 * Sx1y) + (Sx1x2 * Sx2y)) / ((Sx1x1 * Sx2x2) + (Sx1x2 * Sx1x2));
      b2 = (Sx2y - (b1 * Sx1x2)) / (Sx2x2);
      b0 = yMean - b1 * x1Mean - b2 * x2Mean;

      for (int x2 = 0; x2 < hh; x2++) {
        for (int x1 = 0; x1 < ww; x1++) {
          int index = x1 + ww * x2;
          float y = tempArray[index];
          
          // regression model applied to each pixel in the image
          tempArray[index] = y - (b0 + b1 * x1 + b2 * x2);
        }
      }
    }
    return tempArray;
  }

  private float[] gaussianFiltering(float[] pixels, int ww, int hh, double r, boolean roughness) {
    float[] pixels2 = gaussianBlur(pixels, ww, hh, r);
    if (roughness) {
      int counter = ww * hh;
      for (int j = 0; j < counter; j++) {
        pixels2[j] = pixels[j] - pixels2[j];
      }
    }
    return pixels2;
  }

  /*
    * ROUGHNESS STATISTICS
    * Measure R-values on the whole image, gives roughness values according
    * to the ISO 4287/2000 standard:
    *  Rv: Lowest valley
    *  Rp: Highest peak
    *  Rt: The total height of the profile
    *  Ra: Arithmetical mean deviation
    *  Rq: Root mean square deviation
    *  Rsk: Skewness of the assessed profile
    *  Rku: Kurtosis of the assessed profile
    *
    * levelSurf (true/false):
    *  Level surface to perform tilt correction.
    *
    * filterSurf (true/false):
    *  Filter the original image with a Gaussian filter having a radius corresponding
    *  to the structure size limit (default 5). The image is then separated into a roughness
    *  and a waviness component.
    *  type 0: the roughness image is given and the R-values calculated.
    *  type 1: the waviness image is given and the R-values calculated.
    *
    * See also:
    *  http://www.gcsca.net/IJ/SurfCharJ.html
    * and
    *  Chinga, G., Gregersen, O., Dougherty, B.,
    *  "Paper surface characterisation by laser profilometry and image analysis",
    *  Journal of Microscopy and Analysis, July 2003.
    *
    */
  private void calculateRoughness(float[] pixels, int ww, int hh, boolean levelSurf, boolean filterSurf, int filterType, double lStruct, double polarThreshold, int s) {

    double pSize = 1;  // pixel size

    float[] pixels2;
    if (levelSurf) {
      pixels2 = levelSurface(pixels, ww, hh, 4);
    }
    else {
      pixels2 = pixels;
    }

    if (filterSurf) {
      if (filterType == 0) {
        pixels2 = gaussianFiltering(pixels2, ww, hh, (lStruct / 2), true);    // Gaussian radius is equal to half the lStruct size
      }
      else if (filterType == 1) {
        pixels2 = gaussianFiltering(pixels2, ww, hh, (lStruct / 2), false);
      }
      else {
        throw new RuntimeException("Illegal argument!");
      }
    }

    int N = ww * hh;
    float zMin = Float.MAX_VALUE;
    float zMax = -Float.MAX_VALUE;
    float ra = 0, sk = 0, ku = 0;
    float zMed = (float) calculateMedian(pixels2, ww, hh);
    float zMean = (float) calculateMean(pixels2, ww, hh);
    float zStd = (float) calculateStdd(pixels2, ww, hh, zMean);

    for (int j = 0; j < N; j++) {
      float temp = pixels2[j];
      float zTemp = temp - zMean;
      sk += sqr3(zTemp / zStd);
      ku += sqr4(zTemp / zStd);
      ra += Math.abs(zTemp);
      zMin = Math.min(zMin, temp);
      zMax = Math.max(zMax, temp);
    }

    // roughness statistics
    roughness_rv[s] = zMin;  // Rv
    roughness_rp[s] = zMax;  // Rp
    roughness_rt[s] = zMax + Math.abs(zMin);  // Rt
    roughness_rm[s] = zMed;
    roughness_ra[s] = ra / N;  // Ra
    roughness_rq[s] = zStd;  // Rq
    roughness_rsk[s] = sk / N;  // Rsk
    roughness_rku[s] = (ku / N) - 3;  // Rku

    float[] ipPolar = new float[ww * hh];
    float[] ipAzimuthal = new float[ww * hh];
    float sArea = (float) getFacetDetails(pixels2, ww, hh, pSize, true, ipPolar, ipAzimuthal);

    float sMin = Float.MAX_VALUE;
    float sMax = -Float.MAX_VALUE;
    float sSkew = 0, sKurt = 0;
    float sMed = (float) calculateMedian(ipPolar, ww, hh);
    float sMean = (float) calculateMean(ipPolar, ww, hh);
    float sStd = (float) calculateStdd(ipPolar, ww, hh, sMean);

    for (int j = 0; j < N; j++) {
      float temp = ipPolar[j];
      float sTemp = temp - sMean;
      sSkew += sqr3(sTemp / sStd);
      sKurt += sqr4(sTemp / sStd);
      sMin = Math.min(sMin, temp);
      sMax = Math.max(sMax, temp);
    }

    // facet-orientation statistics
    facet_min[s] = sMin;  // FO-Lowest
    facet_max[s] = sMax;  // FO-Highest
    facet_med[s] = sMed;
    facet_mean[s] = sMean;  // FO-Mean
    facet_stdd[s] = sStd;  // FO-Variation
    facet_skew[s] = sSkew / N;
    facet_kurt[s] = (sKurt / N) - 3;  // excess kurtosis (the kurtosis for a standard normal distribution is three)
    facet_area[s] = sArea; // Surface area
    facet_white[s] = (float) calculateWhiteArea(ipPolar, ww, hh, polarThreshold);  // White area (0 <= polarThreshold <= 90 degrees, default 30)

//    if (false) {
//      int pw = 400, ph = 400;
//      int[] aAngles = registerPolarAngles(ipAzimuthal, ww, hh);  // register the frequencies of angles 0-360
//      int[][] xyCoords = calculatePolarPlot(aAngles, pw, ph);  // returns the x,y coordinates in a polar plot
//			facet_polar[s] = ;	// TODO: either reduce the size of the polar histogram or generate a smaller histogram (360 bins are too much)
//    }
  }

  // based on the Facetorientation plugin by Bob Dougherty
  private double getFacetDetails(float[] pixels, int ww, int hh, double ps, boolean polarSlice, float[] theta, float[] phi) {

    final float[] kernelX = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
    final float[] kernelY = {1, 2, 1, 0, 0, 0, -1, -2, -1};

    // evaluate the gradients
    float[] gradX = convolve3x3(pixels, ww, hh, kernelX, true);
    float[] gradY = convolve3x3(pixels, ww, hh, kernelY, true);

    double sArea = 0;
    for (int y = 0; y < hh; y++) {
      for (int x = 0; x < ww; x++) {
        int index = x + ww * y;
        // the derivative requires the factors 1/(2*dx) for the centered
        // finite difference and 1/4 for the coefficients of the kernel.
        double gx = gradX[index] / (8 * ps);
        double gy = gradY[index] / (8 * ps);
        theta[index] = (float) (Math.atan(Math.sqrt(gx * gx + gy * gy)) * (180 / Math.PI));
        phi[index] = (float) ((Math.atan2(gy, gx)) * (180 / Math.PI));
        // computes the surface element by da = sqrt(1 + (tan(theta))^2)
        // (suggestion given by Bob Dougherty, Optinav.com)
        sArea += sqr2(ps) * Math.sqrt(1 + sqr2(Math.tan(theta[index] / (180 / Math.PI))));
      }
    }
    return sArea;
  }

  private int[] registerPolarAngles(float[] azimuth, int ww, int hh) {
    float[] azi = new float[ww * hh];
    System.arraycopy(azimuth, 0, azi, 0, ww * hh);

    int[] aziAngle = new int[361];
    for (int y = 1; y < hh - 2; y++) {
      for (int x = 1; x < ww - 2; x++) {
        int index = x + ww * y;
        if (azi[index] < 0) azi[index] = 360 + azi[index];
        aziAngle[(int) azi[index]]++;
      }
    }

    // filter the values in order to remove deviating peaks at 0, 45, 90, 135, etc.
    float[] tempAngles = new float[3];
    for (int ii = 0; ii < 358; ii++) {
      for (int iii = 0; iii < 3; iii++) {
        tempAngles[iii] = aziAngle[ii + iii];
      }
      aziAngle[ii] = (int) calculateMedian(tempAngles, 3, 1);
    }
    tempAngles[0] = aziAngle[358];
    tempAngles[1] = aziAngle[359];
    tempAngles[2] = aziAngle[0];
    aziAngle[358] = (int) calculateMedian(tempAngles, 3, 1);

    tempAngles[0] = aziAngle[359];
    tempAngles[1] = aziAngle[0];
    tempAngles[2] = aziAngle[1];
    aziAngle[359] = (int) calculateMedian(tempAngles, 3, 1);

    return aziAngle;
  }

  private int[][] calculatePolarPlot(int[] aAngles, int ww, int hh) {
    int [][] Coords = new int[2][360];
    for (int ii = 0; ii < 360; ii++) {
      double angle = ii * (Math.PI / 180);  // convert the angles to radians
      Coords[0][ii] = -(int) (aAngles[ii] * Math.cos(angle));  // inverse the x-axis
      Coords[1][ii] = (int) (aAngles[ii] * Math.sin(angle));
    }

    // move the plot to positive values
    int yMin = 999999, xMin = 999999, yMax = -999999, xMax = -999999;
    for (int ii = 0; ii < 360; ii++) {
      if (Coords[0][ii] < xMin)
        xMin = Coords[0][ii];
      else if (Coords[0][ii] > xMax)
        xMax = Coords[0][ii];

      if (Coords[1][ii] < yMin)
        yMin = Coords[1][ii];
      else if (Coords[1][ii] > yMax)
        yMax = Coords[1][ii];
    }

    // rescale to fit
    double maxDist;
    if ((xMax - xMin) > (yMax - yMin))
      maxDist = (xMax - xMin);
    else
      maxDist = (yMax - yMin);

    double scaleFactor = maxDist / (ww - 100);

    int xP = (int) ((ww - ((xMax - xMin) / scaleFactor)) / 2);   // finds the starting point
    int yP = (int) ((ww - ((yMax - yMin) / scaleFactor)) / 2);  // the plot is placed on the center of the image

    // moves the plot to the center of the image
    for (int ii = 0; ii < 360; ii++) {
      Coords[0][ii] = (int) (xP + (Coords[0][ii] + Math.abs(xMin)) / scaleFactor);
      Coords[1][ii] = (int) (yP + (Coords[1][ii] + Math.abs(yMin)) / scaleFactor);
    }
    return Coords;
  }


  // 3x3 convolution by Glynne Casteel
  @SuppressWarnings({"UnusedAssignment"})
  private float[] convolve3x3(float[] pixels, int ww, int hh, float[] kernel, boolean normalize) {
    float p1, p2, p3, p4, p5, p6, p7, p8, p9;
    float k1 = kernel[0], k2 = kernel[1], k3 = kernel[2],
    k4 = kernel[3], k5 = kernel[4], k6 = kernel[5],
    k7 = kernel[6], k8 = kernel[7], k9 = kernel[8];

    // normalize convolution kernel
    float scale = 0f;
    if (normalize) {
      for (float k : kernel)
        scale += k;
      if (scale == 0)
        scale = 1f;
      else
        scale = 1f / scale;
    }
    else {
      scale = 1f;
    }

    float[] pixels2 = new float[ww * hh];
    // setup limits for 3x3 filters
    int xMin = 1;
    int xMax = ww - 2;
    int yMin = 1;
    int yMax = hh - 2;
    int rowOffset = ww;
    for (int y = yMin; y <= yMax; y++) {
      int offset;
      offset = xMin + y * ww;
      p1 = 0f;
      p2 = pixels[offset - rowOffset - 1];
      p3 = pixels[offset - rowOffset];
      p4 = 0f;
      p5 = pixels[offset - 1];
      p6 = pixels[offset];
      p7 = 0f;
      p8 = pixels[offset + rowOffset - 1];
      p9 = pixels[offset + rowOffset];

      for (int x = xMin; x <= xMax; x++) {
        float sum;
        p1 = p2;
        p2 = p3;
        p3 = pixels[offset - rowOffset + 1];
        p4 = p5;
        p5 = p6;
        p6 = pixels[offset + 1];
        p7 = p8;
        p8 = p9;
        p9 = pixels[offset + rowOffset + 1];
        sum = k1 * p1 + k2 * p2 + k3 * p3
              + k4 * p4 + k5 * p5 + k6 * p6
              + k7 * p7 + k8 * p8 + k9 * p9;
        pixels2[offset++] = sum * scale;
      }
    }
    return pixels2;
  }

  private double calculateMean(float[] pixels, int ww, int hh) {
    double mValue = 0;
    int counter = ww * hh;
    for (int j = 0; j < counter; j++) {
      mValue += pixels[j];
    }
    return mValue / (double) counter;
  }

  private double calculateStdd(float[] pixels, int ww, int hh, double mean) {
    double sValue = 0;
    int counter = ww * hh;
    if (counter > 1) {
      for (int j = 0; j < counter; j++) {
        sValue += sqr2(mean - pixels[j]);
      }
      sValue = Math.sqrt(sValue / (counter - 1));
    }
    return sValue;
  }

  private double calculateMedian(float[] pixels, int ww, int hh) {
    float[] pixels2 = new float[ww * hh];
    System.arraycopy(pixels, 0, pixels2, 0, ww * hh);
    int lengthArray = pixels.length;
    Arrays.sort(pixels2);
    int mid;
    double med;
    if ((lengthArray % 2) != 0) {
      mid = lengthArray / 2;
      med = pixels2[mid];
    }
    else {
      mid = (int) ((lengthArray / 2) + 0.5);
      med = (pixels2[mid - 1] + pixels2[mid]) / 2;
    }
    return med;
  }

  private double calculateWhiteArea(float[] pixels, int ww, int hh, double thr) {
    int wa = 0;
    int counter = ww * hh;
    for (int j = 0; j < counter; j++) {
      if (pixels[j] < thr) wa++;
    }
    return (double) wa / (double) counter;
  }

  private double sqr2(double x) {
    return x * x;
  }

  private double sqr3(double x) {
    return x * x * x;
  }

  private double sqr4(double x) {
    return x * x * x * x;
  }

  // performs a separate smoothing in each direction using a 1-D filter (the Gaussian filter is separable)
  private float[] gaussianBlur(float[] pixels, int ww, int hh, double radius) {
    float[] kernel = makeKernel(radius);
    pixels = convolve(pixels, ww, hh, kernel, kernel.length, 1, true);  // horizontal
    pixels = convolve(pixels, ww, hh, kernel, 1, kernel.length, true);  // vertical
    return pixels;
  }

  // compute a Gaussian Blur kernel
  private float[] makeKernel(double radius) {
    radius += 1;
    int size = (int) radius * 2 + 1;
    float[] kernel = new float[size];
    for (int i = 0; i < size; i++)
      kernel[i] = (float) Math.exp(-0.5 * (sqr2((i - radius) / (radius * 2))) / sqr2(0.2));
    float[] kernel2 = new float[size - 2];
    System.arraycopy(kernel, 1, kernel2, 0, size - 2);
    if (kernel2.length == 1)
      kernel2[0] = 1f;
    return kernel2;
  }

  // convolves the image with a kernel of width kw and height kh
  private float[] convolve(float[] pixels, int ww, int hh, float[] kernel, int kw, int kh, boolean normalize) {
    int x1 = 0;
    int y1 = 0;
    int x2 = x1 + ww;
    int y2 = y1 + hh;
    int uc = kw / 2;
    int vc = kh / 2;

    // normalize convolution kernel
    float scale = 0f;
    if (normalize) {
      for (float k : kernel)
        scale += k;
      if (scale == 0)
        scale = 1f;
      else
        scale = 1f / scale;
    }
    else {
      scale = 1f;
    }

    float[] pixels2 = new float[ww * hh];
    int xedge = ww - uc;
    int yedge = hh - vc;
    for (int y = y1; y < y2; y++) {
      for (int x = x1; x < x2; x++) {
        float sum;
        int i;
        boolean edgePixel;
        sum = 0.0f;
        i = 0;
        edgePixel = y < vc || y >= yedge || x < uc || x >= xedge;
        for (int v = -vc; v <= vc; v++) {
          int offset;
          offset = x + (y + v) * ww;
          for (int u = -uc; u <= uc; u++) {
            if (edgePixel)
              sum += getPixel(x + u, y + v, pixels, ww, hh) * kernel[i++];
            else
              sum += pixels[offset + u] * kernel[i++];
          }
        }
        pixels2[x + y * ww] = sum * scale;
      }
    }
    return pixels2;
  }

  private float getPixel(int x, int y, float[] pixels, int ww, int hh) {
    if (x <= 0) x = 0;
    if (x >= ww) x = ww - 1;
    if (y <= 0) y = 0;
    if (y >= hh) y = hh - 1;
    return pixels[x + y * ww];
  }


  /**
   * Writes the color histogram of the image with the specified writer.
   *
   * @param writer the writer to write the color histograms
   */
  public void writeColorHistogram(String separator, String classPrefix, BufferedWriter writer) throws IOException {
    writeFeature(colorHistogram, separator, classPrefix, writer);
  }

  /**
   * Writes the color moments of the image with the specified writer.
   *
   * @param writers the writer to write the color histograms
   */
  public void writeColorMoments(String separator, String classPrefix, BufferedWriter[] writers) throws IOException {
    if (writers.length != 3)
      throw new IllegalArgumentException("Wrong number of writers!");

    int i = 0;
    writeFeature(meanHSV, separator, classPrefix, writers[i++]);
    writeFeature(standardDeviationsHSV, separator, classPrefix, writers[i++]);
    writeFeature(skewnessHSV, separator, classPrefix, writers[i]);
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
    writeFeature(haralick_01, separator, classPrefix, writers[i++]);
    writeFeature(haralick_02, separator, classPrefix, writers[i++]);
    writeFeature(haralick_03, separator, classPrefix, writers[i++]);
    writeFeature(haralick_04, separator, classPrefix, writers[i++]);
    writeFeature(haralick_05, separator, classPrefix, writers[i++]);
    writeFeature(haralick_06, separator, classPrefix, writers[i++]);
    writeFeature(haralick_07, separator, classPrefix, writers[i++]);
    writeFeature(haralick_08, separator, classPrefix, writers[i++]);
    writeFeature(haralick_09, separator, classPrefix, writers[i++]);
    writeFeature(haralick_10, separator, classPrefix, writers[i++]);
    writeFeature(haralick_11, separator, classPrefix, writers[i++]);
    writeFeature(haralick_12, separator, classPrefix, writers[i++]);
    writeFeature(haralick_13, separator, classPrefix, writers[i]);
  }

  /**
   * Writes the roughness statictics with the specified writers.
   *
   * @param writers the writers to write the roughness statictics
   */
  public void writeRoughnessStats(String separator, String classPrefix, BufferedWriter[] writers) throws IOException {
    if (writers.length != 8)
      throw new IllegalArgumentException("Wrong number of writers!");

    int i = 0;
    writeFeature(roughness_rv, separator, classPrefix, writers[i++]);
    writeFeature(roughness_rp, separator, classPrefix, writers[i++]);
    writeFeature(roughness_rt, separator, classPrefix, writers[i++]);
    writeFeature(roughness_rm, separator, classPrefix, writers[i++]);
    writeFeature(roughness_ra, separator, classPrefix, writers[i++]);
    writeFeature(roughness_rq, separator, classPrefix, writers[i++]);
    writeFeature(roughness_rsk, separator, classPrefix, writers[i++]);
    writeFeature(roughness_rku, separator, classPrefix, writers[i]);
  }

  /**
   * Writes the facet-orientation statictics with the specified writers.
   *
   * @param writers the writers to write the facet-orientation statictics
   */
  public void writeFacetStats(String separator, String classPrefix, BufferedWriter[] writers) throws IOException {
    if (writers.length != 9)
      throw new IllegalArgumentException("Wrong number of writers!");

    int i = 0;
    writeFeature(facet_min, separator, classPrefix, writers[i++]);
    writeFeature(facet_max, separator, classPrefix, writers[i++]);
    writeFeature(facet_med, separator, classPrefix, writers[i++]);
    writeFeature(facet_mean, separator, classPrefix, writers[i++]);
    writeFeature(facet_stdd, separator, classPrefix, writers[i++]);
    writeFeature(facet_skew, separator, classPrefix, writers[i++]);
    writeFeature(facet_kurt, separator, classPrefix, writers[i++]);
    writeFeature(facet_area, separator, classPrefix, writers[i++]);
    writeFeature(facet_white, separator, classPrefix, writers[i]);
  }

  /**
   * Writes the specified feature to output.
   *
   * @param feature     the feature to write
   * @param separator   the separator between the single attributes (e.g. comma or whitespace)
   * @param classPrefix the prefix for the class label
   * @param writer      the writer to write to
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
