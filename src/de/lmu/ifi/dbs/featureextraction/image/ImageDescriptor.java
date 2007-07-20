package de.lmu.ifi.dbs.featureextraction.image;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a description of a jpg image including color histogram, color moments and
 * 13 Haralick texture features, Roughness and Facet-Orientation.
 *
 * @author Elke Achtert 
 */
class ImageDescriptor extends AbstractLoggable {

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
   * The number of gray levels in an image
   */
  private static final int GRAY_RANGES = 256;

  /**
   * The number of gray values for the textures
   */
  private static final int NUM_GRAY_VALUES = 32;

  /**
   * The scale for the gray values for conversion rgb to gray values.
   */
  public static final double GRAY_SCALE = (double) GRAY_RANGES / (double) NUM_GRAY_VALUES;

  /**
   * The range of the h values.
   */
  private static final int HS_H_RANGES = 8;

  /**
   * The range of the s values.
   */
  private static final int HS_S_RANGES = 4;

  /**
   * The range of the y values.
   */
  private static final int YU_Y_RANGES = 6;

  /**
   * The range of the y values.
   */
  private static final int YU_U_RANGES = 6;

  /**
   * Contains the distances of the neighboring pixels for one pixel.
   */
  private static final int[] DISTANCES = {1, 3, 5, 7, 11};

  /**
   * Indicates whether the underlying image is empty.
   */
  private boolean notEmpty;

  /**
   * Contains the hsv values of the image.
   */
  private float[][] hsvValues;

  /**
   * Contains the hsl values of the image.
   */
  private float[][] hslValues;

  /**
   * Contains the yuv values of the image.
   */
  private float[][] yuvValues;

  /**
   * Contains the gray histogram of the image.
   */
  private double[] grayHistogram = new double[GRAY_RANGES];

  /**
   * Contains the scaled gray histogram of the image.
   */
  private double[] grayscaleHistogram = new double[NUM_GRAY_VALUES];

  /**
   * Contains the color histogram of the image.
   */
  private double[] colorhist_HSV_SecShell_HS = new double[HS_H_RANGES * HS_S_RANGES];
  private double[] colorhist_HSL_SecShell_HS = new double[HS_H_RANGES * HS_S_RANGES];
  private double[] colorhist_YUV_SecShell_YU = new double[YU_Y_RANGES * YU_U_RANGES];

  /**
   * Contains the correlation histogram of the image.
   */
  @SuppressWarnings({"unchecked"})
  private ArrayList<Point>[] correloHistogram = new ArrayList[HS_H_RANGES * HS_S_RANGES];

  /**
   * The value for one increment in the gray/color histograms.
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
   * Contains the mean value of the hsv values of the underlying image.
   */
  private double[] meanHSV = new double[3];

  /**
   * Contains the standard deviation of the hsv values.
   */
  private double[] stddevHSV = new double[3];

  /**
   * Contains the skewness of the hsv values.
   */
  private double[] skewnessHSV = new double[3];

  /**
   * Contains the kurtosis of the hsv values.
   */
  private double[] kurtosisHSV = new double[3];

  /**
   * Contains the mean value of the hsl values of the underlying image.
   */
  private double[] meanHSL = new double[3];

  /**
   * Contains the standard deviation of the hsl values.
   */
  private double[] stddevHSL = new double[3];

  /**
   * Contains the skewness of the hsl values.
   */
  private double[] skewnessHSL = new double[3];

  /**
   * Contains the kurtosis of the hsl values.
   */
  private double[] kurtosisHSL = new double[3];

  /**
   * Contains the mean value of the yuv values of the underlying image.
   */
  private double[] meanYUV = new double[3];

  /**
   * Contains the standard deviation of the yuv values.
   */
  private double[] stddevYUV = new double[3];

  /**
   * Contains the skewness of the yuv values.
   */
  private double[] skewnessYUV = new double[3];

  /**
   * Contains the kurtosis of the yuv values.
   */
  private double[] kurtosisYUV = new double[3];

  /**
   * The cooccurrence matrices for each neighboring distance value and for the
   * different orientations and one summarized orientation.
   */
  private double[][][] cooccurrenceMatrices = new double[DISTANCES.length][][];

  /**
   * Contains the sum of the entries of each cooccurrence matrix.
   */
  private double[] cooccurrenceSums = new double[DISTANCES.length];

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
   * Contains the feature informations.
   */
  public final DescriptorInfo[] featureInfos = {
      // histograms
      new DescriptorInfo("grayhistogram", grayscaleHistogram),
      new DescriptorInfo("colorhistogram_hsv", colorhist_HSV_SecShell_HS),
      new DescriptorInfo("colorhistogram_hsl", colorhist_HSL_SecShell_HS),
      new DescriptorInfo("colorhistogram_yuv", colorhist_YUV_SecShell_YU),
      // moments
      new DescriptorInfo("colormoment_hsv_mean", meanHSV),
      new DescriptorInfo("colormoment_hsv_stdd", stddevHSV),
      new DescriptorInfo("colormoment_hsv_skew", skewnessHSV),
      new DescriptorInfo("colormoment_hsv_kurt", kurtosisHSV),
      new DescriptorInfo("colormoment_hsl_mean", meanHSL),
      new DescriptorInfo("colormoment_hsl_stdd", stddevHSL),
      new DescriptorInfo("colormoment_hsl_skew", skewnessHSL),
      new DescriptorInfo("colormoment_hsl_kurt", kurtosisHSL),
      new DescriptorInfo("colormoment_yuv_mean", meanYUV),
      new DescriptorInfo("colormoment_yuv_stdd", stddevYUV),
      new DescriptorInfo("colormoment_yuv_skew", skewnessYUV),
      new DescriptorInfo("colormoment_yuv_kurt", kurtosisYUV),
      // texture
      new DescriptorInfo("haralick_01", haralick_01),
      new DescriptorInfo("haralick_02", haralick_02),
      new DescriptorInfo("haralick_03", haralick_03),
      new DescriptorInfo("haralick_04", haralick_04),
      new DescriptorInfo("haralick_05", haralick_05),
      new DescriptorInfo("haralick_06", haralick_06),
      new DescriptorInfo("haralick_07", haralick_07),
      new DescriptorInfo("haralick_08", haralick_08),
      new DescriptorInfo("haralick_09", haralick_09),
      new DescriptorInfo("haralick_10", haralick_10),
      new DescriptorInfo("haralick_11", haralick_11),
      new DescriptorInfo("haralick_12", haralick_12),
      new DescriptorInfo("haralick_13", haralick_13),
      // roughness
      new DescriptorInfo("roughness_rv", roughness_rv),
      new DescriptorInfo("roughness_rp", roughness_rp),
      new DescriptorInfo("roughness_rt", roughness_rt),
      new DescriptorInfo("roughness_rm", roughness_rm),
      new DescriptorInfo("roughness_ra", roughness_ra),
      new DescriptorInfo("roughness_rq", roughness_rq),
      new DescriptorInfo("roughness_rsk", roughness_rsk),
      new DescriptorInfo("roughness_rku", roughness_rku),
      // facet
      new DescriptorInfo("facet_min", facet_min),
      new DescriptorInfo("facet_max", facet_max),
      new DescriptorInfo("facet_med", facet_med),
      new DescriptorInfo("facet_mean", facet_mean),
      new DescriptorInfo("facet_stdd", facet_stdd),
      new DescriptorInfo("facet_skew", facet_skew),
      new DescriptorInfo("facet_kurt", facet_kurt),
      new DescriptorInfo("facet_area", facet_area),
      new DescriptorInfo("facet_white", facet_white),
//    new DescriptorInfo("facet_polar", facet_polar),
  };

  /**
   * Creates a new image descriptor for the specified image.
   * FIXME: it would be better to have a separate extractFeatures() routine.
   *
   * @param image the image for which the description should be created
   */
  public ImageDescriptor(BufferedImage image) {
    super(LoggingConfiguration.DEBUG);

    // init width, heigt and size
    int width = image.getWidth(null);
    int height = image.getHeight(null);
    int size = height * width;

    // set the value for one increment in the gray/color histograms
    this.histogramIncrement = 1.0f / size;
    // init the arrays for the gray values, gray pixels
    this.grayValue = new byte[size];
    this.grayPixel = new float[size];

    // init the arrays for the hsv, hsl and yuv values
    this.hsvValues = new float[size][3];
    this.hslValues = new float[size][3];
    this.yuvValues = new float[size][3];

    // init cooccurrence matrices
    for (int d = 0; d < DISTANCES.length; d++) {
      cooccurrenceMatrices[d] = new double[NUM_GRAY_VALUES][NUM_GRAY_VALUES];
    }

    // init correlogram
    for (int i = 0; i < (HS_H_RANGES * HS_S_RANGES); i++) {
      correloHistogram[i] = new ArrayList<Point>();
    }

    // calculate histograms and cooccurrence matrices
    notEmpty = calculateValues(image, width, height);
/*
    if (true) {
      // image debugging
      try {
      // convert back to BufferedImage
      BufferedImage bufferimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pos = width * y + x;
          int rgb = image.getRGB(x, y);
          int r = ((rgb >> RED_SHIFT) & 0xff);
          int g = (rgb >> GREEN_SHIFT) & 0xff;
          int b = (rgb >> BLUE_SHIFT) & 0xff;
          float v = (float)((( 2097152*r - 1756152*g -  341000*b) / 255) + 2097152) / (float)4194304;	// normalize
          
          if (v <= 0.2) {
         	 rgb = ((0 << RED_SHIFT) | (0 << GREEN_SHIFT) | (0 << BLUE_SHIFT));
          }
          else if (hsvValues[pos][1] < 0.1 && v > 0.2) {
         	 rgb = (((int)(hsvValues[pos][2]*255) << RED_SHIFT) | ((int)(hsvValues[pos][2]*255) << GREEN_SHIFT) | ((int)(hsvValues[pos][2]*255) << BLUE_SHIFT));
          }
          else {
             rgb = hsv2rgb(hsvValues[pos][0], 1.0, 1.0);         	 
          }
//          rgb = (((int)(hsvValues[pos][0]*255) << RED_SHIFT) | ((int)(hsvValues[pos][0]*255) << GREEN_SHIFT) | ((int)(hsvValues[pos][0]*255) << BLUE_SHIFT));
//          rgb = (((int)(hsvValues[pos][1]*255) << RED_SHIFT) | ((int)(hsvValues[pos][1]*255) << GREEN_SHIFT) | ((int)(hsvValues[pos][1]*255) << BLUE_SHIFT));
//          rgb = (((int)(hsvValues[pos][2]*255) << RED_SHIFT) | ((int)(hsvValues[pos][2]*255) << GREEN_SHIFT) | ((int)(hsvValues[pos][2]*255) << BLUE_SHIFT));
//          rgb = hsv2rgb(hsvValues[pos][0], hsvValues[pos][1], 1.0);
          bufferimage.setRGB(x, y, rgb);
        }
      }
        ImageIO.write(bufferimage, "bmp", new File("c:\\debug_pic.bmp"));
		} catch (IOException e) {
        e.printStackTrace();
		}
    }
*/

/*    if (false) {
      // co-occurence debugging
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
    // if image is not empty: calculate moments and statistics
    if (notEmpty) {
      calculateHistograms(width, height);
      calculateMoments(width, height);
      calculateTexture(width, height);

      calculateRoughness(grayPixel, width, height, false, false, 0, 5.0, 0.12, 0);
      calculateRoughness(grayPixel, width, height, false, true, 0, 5.0, 0.12, 1);
      calculateRoughness(grayPixel, width, height, false, true, 1, 5.0, 0.12, 2);
      calculateRoughness(grayPixel, width, height, true, false, 0, 5.0, 0.12, 3);
      calculateRoughness(grayPixel, width, height, true, true, 0, 5.0, 0.12, 4);
      calculateRoughness(grayPixel, width, height, true, true, 1, 5.0, 0.12, 5);
    }
  }

  private int hsv2rgb(double H, double S, double V) {
    int i;
    double f, p, q, t;
    H = H * 6;
    if (H >= 6)
      H -= 6;
    i = (int) H;
    f = H - (double) i;
    p = V * (1.0 - S);
    q = V * (1.0 - (S * f));
    t = V * (1.0 - (S * (1.0 - f)));

    int col_r = 0, col_g = 0, col_b = 0;
    switch (i) {
      case 0:
        col_r = (int) (V * 255);
        col_g = (int) (t * 255);
        col_b = (int) (p * 255);
        break;
      case 1:
        col_r = (int) (q * 255);
        col_g = (int) (V * 255);
        col_b = (int) (p * 255);
        break;
      case 2:
        col_r = (int) (p * 255);
        col_g = (int) (V * 255);
        col_b = (int) (t * 255);
        break;
      case 3:
        col_r = (int) (p * 255);
        col_g = (int) (q * 255);
        col_b = (int) (V * 255);
        break;
      case 4:
        col_r = (int) (t * 255);
        col_g = (int) (p * 255);
        col_b = (int) (V * 255);
        break;
      case 5:
        col_r = (int) (V * 255);
        col_g = (int) (p * 255);
        col_b = (int) (q * 255);
        break;
    }

    return ((col_r << RED_SHIFT) | (col_g << GREEN_SHIFT) | (col_b << BLUE_SHIFT));
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
   * the entry in the coocurrence matrix for each pixel of the image
   *
   * @param image  the image to be analyzed
   * @param width  the width of the image
   * @param height the height of the image
   * @return a boolean flag indicating whether the picture is empty or not (ie. contains only black pixels)
   */
  @SuppressWarnings({"UnusedAssignment"})
  private boolean calculateValues(BufferedImage image, int width, int height) {
    // image is not empty per default
    boolean notBlack = false;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int pos = width * y + x;

        // for each pixel
        int rgb = image.getRGB(x, y);
        int r = (rgb >> RED_SHIFT) & 0xff;
        int g = (rgb >> GREEN_SHIFT) & 0xff;
        int b = (rgb >> BLUE_SHIFT) & 0xff;

        // check if image is empty
        if (!notBlack && (r > 0 || g > 0 || b > 0)) {
          notBlack = true;
        }

        // HSV color space
        RGBtoHSV(r, g, b, hsvValues[pos]);

        int hindex = (int) (HS_H_RANGES * hsvValues[pos][0]);  // hue
        if (hindex >= HS_H_RANGES) {
          hindex = HS_H_RANGES - 1;
        }
        int sindex = (int) (HS_S_RANGES * hsvValues[pos][1]);  // saturation
        if (sindex >= HS_S_RANGES) {
          sindex = HS_S_RANGES - 1;
        }
        colorhist_HSV_SecShell_HS[hindex + sindex * HS_H_RANGES] += histogramIncrement;  // sector/shell model for the colors

//        correloHistogram[hindex + sindex*H_RANGES].add(new Point(x,y));

        for (int i = 0; i < 3; i++) {
          meanHSV[i] += hsvValues[pos][i];
        }

        // HSL color space
        RGBtoHSL(r, g, b, hslValues[pos]);

        hindex = (int) (HS_H_RANGES * hslValues[pos][0]);  // hue
        if (hindex >= HS_H_RANGES) {
          hindex = HS_H_RANGES - 1;
        }
        sindex = (int) (HS_S_RANGES * hslValues[pos][1]);  // saturation
        if (sindex >= HS_S_RANGES) {
          sindex = HS_S_RANGES - 1;
        }
        colorhist_HSL_SecShell_HS[hindex + sindex * HS_H_RANGES] += histogramIncrement;  // sector/shell model for the colors

        for (int i = 0; i < 3; i++) {
          meanHSL[i] += hslValues[pos][i];
        }

        // YUV color space
        RGBtoYUV(r, g, b, yuvValues[pos]);

        int yindex = (int) (YU_Y_RANGES * yuvValues[pos][0]);  // chroma y
        if (yindex >= YU_Y_RANGES) {
          yindex = YU_Y_RANGES - 1;
        }
        int uindex = (int) (YU_U_RANGES * yuvValues[pos][1]);  // chroma u
        if (uindex >= YU_U_RANGES) {
          uindex = YU_U_RANGES - 1;
        }
        colorhist_YUV_SecShell_YU[yindex + uindex * YU_Y_RANGES] += histogramIncrement;  // square model for the colors

        for (int i = 0; i < 3; i++) {
          meanYUV[i] += yuvValues[pos][i];
        }

        // determine gray value [0..255]
        double gray = (r * DEFAULT_RED_WEIGHT + g * DEFAULT_GREEN_WEIGHT + b * DEFAULT_BLUE_WEIGHT) / (DEFAULT_RED_WEIGHT + DEFAULT_GREEN_WEIGHT + DEFAULT_BLUE_WEIGHT);
//        double gray = (r + g + b) / 3.0;	// unweighted conversion
        grayValue[pos] = (byte) (gray / GRAY_SCALE);  // quantized for texture analysis
        grayPixel[pos] = (float) (gray / 255.0);  // full resolution for gradient analysis

        // check gray value
        if (grayValue[pos] >= NUM_GRAY_VALUES) {
          throw new RuntimeException("Should never happen!");
        }

        // gray histogram entry
        int gindex = (int) gray;
        if (gindex >= GRAY_RANGES) {
          gindex = GRAY_RANGES - 1;  // should never happen
        }
        grayHistogram[gindex] += histogramIncrement;

        // update mean
        meanGrayValue += grayValue[pos];

        for (int k = 0; k < DISTANCES.length; k++) {
          int d = DISTANCES[k];

          // horizontal neighbor: 0 degrees
          int i = x - d;
          int j = y;
          if (!(i < 0)) {
            increment(grayValue[pos], grayValue[pos - d], k, cooccurrenceMatrices, cooccurrenceSums);
          }

          // vertical neighbor: 90 degree
          i = x;
          j = y - d;
          if (!(j < 0)) {
            increment(grayValue[pos], grayValue[pos - d * width], k, cooccurrenceMatrices, cooccurrenceSums);
          }

          // 45 degree diagonal neigbor
          i = x + d;
          j = y - d;
          if (i < width && !(j < 0)) {
            increment(grayValue[pos], grayValue[pos + d - d * width], k, cooccurrenceMatrices, cooccurrenceSums);
          }

          // 135 vertical neighbor
          i = x - d;
          j = y - d;
          if (!(i < 0) && !(j < 0)) {
            increment(grayValue[pos], grayValue[pos - d - d * width], k, cooccurrenceMatrices, cooccurrenceSums);
          }
        }
      }
    }
    return notBlack;
  }

  /**
   * Incremets the specified coocurrence matrix and the summarized coocurrence matrix
   * of the specified distance value d at the specified positions (g1,g2) and (g2,g1).
   *
   * @param g1   the gray value of the first pixel
   * @param g2   the gray value of the second pixel
   * @param d    the index of the distance value specifiying the coocurrence matrix
   * @param cooc the coocurrence matrix array
   * @param sums the coocurrence sum of entries matrix array
   */
  private void increment(int g1, int g2, int d, double[][][] cooc, double[] sums) {
    cooc[d][g1][g2] += 1;
    cooc[d][g2][g1] += 1;
    sums[d] += 2;
  }

  /**
   * Transforms the specified rgb value to the corresponding hsv (Hue Saturation Value) value.
   *
   * @param r   the r value
   * @param g   the g value
   * @param b   the b value
   * @param hsv the values for the specified rgb value (normalized to the range [0..1])
   */
  private void RGBtoHSV(int r, int g, int b, float[] hsv) {
    float min = min(r, g, b);
    float max = max(r, g, b);

    // h value
    if (max == min) {
      hsv[0] = 0;
    }
    else if (r == max) {
      hsv[0] = ((g - b) / (max - min)) * 60;  // between yellow & magenta
    }
    else if (g == max) {
      hsv[0] = (2 + (b - r) / (max - min)) * 60;  // between cyan & yellow
    }
    else if (b == max) {
      hsv[0] = (4 + (r - g) / (max - min)) * 60;  // between magenta & cyan
    }
    if (hsv[0] < 0) {
      hsv[0] += 360;
    }
    hsv[0] /= 360;  // normalize

    // s value
    if (max == 0) {
      hsv[1] = 0;
    }
    else {
      hsv[1] = (max - min) / max;
    }

    // v value
    hsv[2] = max / 255;  // normalize
  }

  /**
   * Transforms the specified rgb value to the corresponding hsv (Hue Saturation Value) value.
   *
   * @param r   the r value
   * @param g   the g value
   * @param b   the b value
   * @param hsl the values for the specified rgb value (normalized to the range [0..1])
   */
  private void RGBtoHSL(int r, int g, int b, float[] hsl) {
    float min = min(r, g, b);
    float max = max(r, g, b);

    if (max == min) {
      // h is undefined
      hsl[0] = 0;
    }
    else if (r == max) {
      hsl[0] = ((g - b) / (max - min)) * 60;  // between yellow & magenta
    }
    else if (g == max) {
      hsl[0] = (2 + (b - r) / (max - min)) * 60;  // between cyan & yellow
    }
    else if (b == max) {
      hsl[0] = (4 + (r - g) / (max - min)) * 60;  // between magenta & cyan
    }
    if (hsl[0] < 0) {
      hsl[0] += 360;
    }
    hsl[0] /= 360;  // normalize

    if (max == min) {
      // s is undefined
      hsl[1] = 0;
    }
    else {
      if ((max + min) <= 255)
        hsl[1] = (max - min) / (max + min);  // s
      else
        hsl[1] = (max - min) / (2 * 255 - (max + min));  // s
    }

    hsl[2] = (max + min) / (2 * 255); // l
  }

  /**
   * Transforms the specified rgb value to the corresponding yuv (Chrominance/Luminance) value.
   *
   * @param r   the r value
   * @param g   the g value
   * @param b   the b value
   * @param yuv the values for the specified rgb value (normalized to the range [0..1])
   */
  private void RGBtoYUV(int r, int g, int b, float[] yuv) {
//    yuv[0] = (( 0.299f*r + 0.587f*g + 0.114f*b) / 255);
//    yuv[1] = ((-0.147f*r - 0.289f*g + 0.436f*b) / 255)*(0.5f/0.436f) + 0.5f;	// normalize
//    yuv[2] = (( 0.615f*r - 0.515f*g - 0.100f*b) / 255)*(0.5f/0.615f) + 0.5f;	// normalize

    // integer version of the lines above
    yuv[0] = (float) (((1254097 * r + 2462056 * g + 478151 * b) / 255)) / (float) 4194304;
    yuv[1] = (float) (((-707067 * r - 1390085 * g + 2097152 * b) / 255) + 2097152) / (float) 4194304;  // normalize
    yuv[2] = (float) (((2097152 * r - 1756152 * g - 341000 * b) / 255) + 2097152) / (float) 4194304;  // normalize
  }

  /**
   * Transforms the specified rgb value to the corresponding panda value.
   *
   * @param r     the r value
   * @param g     the g value
   * @param b     the b value
   * @param panda the values for the specified rgb value (normalized to the range [0..1])
   */
  private void RGBtoPANDA(int r, int g, int b, float t, float[] panda) {
    float[] hsl = new float[3];

    RGBtoHSL(r, g, b, hsl);

    if (hsl[1] < t) {
      panda[0] = (float) (((1254097 * r + 2462056 * g + 478151 * b) / 255)) / (float) 4194304;  // gray
    }
    else {
      panda[0] = hsl[0] - 1.0f;  // color
    }
  }

  /**
   * Returns the maximum of the three specified double values.
   *
   * @param r first value
   * @param g secon value
   * @param b third value
   * @return the maximum of the three specified double values
   */
  private int max(int r, int g, int b) {
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
  private int min(int r, int g, int b) {
    return Math.min(r, Math.min(g, b));
  }

  /**
   * Calculates some staticsical values
   */
  private void calculateHistograms(int width, int height) {
    createGrayHistogram();
//	  getEntropie();
//	  getGlobalContrast();
//	  getLocalContrast(width, height);
//	  createCorrelogram(width, height);
  }

  private void createGrayHistogram() {
    for (int i = 0; i < grayHistogram.length; i++) {
      grayscaleHistogram[(i * NUM_GRAY_VALUES) / GRAY_RANGES] += grayHistogram[i];
    }
  }

  private double getEntropie() {
    StringBuffer msg = new StringBuffer();
    double entropie = 0.0;
    for (int i = 0; i < grayHistogram.length; i++) {
      double grayEntry = grayHistogram[i];
//    -sum Hp(i) log2Hp(i) (0<=i<K)
      if (debug) {
        msg.append("\ngrayEntry: [" + i + "]: " + grayEntry);
      }
      if (grayEntry != 0.0) {
        entropie += (grayEntry * (Math.log10(grayEntry) / Math.log10(2)));
      }
    }
    if (debug) {
      msg.append("Entropie: " + (-entropie));
      debugFine(msg.toString());

    }
    return -entropie;
  }

  private double getGlobalContrast() {
    int mingrau = 0;
    for (int i = 0; i < grayHistogram.length; i++) {
      if (grayHistogram[i] != 0.0) {
        mingrau = i;
        break;
      }
    }
    int maxgrau = grayHistogram.length - 1;
    for (int i = grayHistogram.length - 1; i > 0; i--) {
      if (grayHistogram[i] != 0.0) {
        maxgrau = i;
        break;
      }
    }

    if (debug) {
      debugFine("mingrau: " + mingrau + ", maxgrau: " + maxgrau);
    }
    return (double) (maxgrau - mingrau) / (double) (grayHistogram.length - 1);
  }

  private double getLocalContrast(int width, int height) {
    double contrast = 0.0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int pos = width * y + x;

        double viererNachbarschaft = 0.0;
        if (x > 0) {
          viererNachbarschaft += grayPixel[pos - 1];
        }
        if (x < width - 1) {
          viererNachbarschaft += grayPixel[pos + 1];
        }
        if (y > 0) {
          viererNachbarschaft += grayPixel[pos - width];
        }
        if (y < height - 1) {
          viererNachbarschaft += grayPixel[pos + width];
        }

        contrast += Math.abs(grayPixel[pos] - (viererNachbarschaft / 4.0));  // abs()???
      }
    }

    if (debug) {
      debugFine("contrast: " + contrast);
    }
    return contrast / (double) (width * height);
  }

  private void createCorrelogram(int width, int height) {
    Correlogram correlogram = new Correlogram();
    int distance = (int) Point.distance(0, 0, width, height);
    correlogram.setCorrelogram(new int[HS_H_RANGES * HS_S_RANGES][distance]);
    int maxcount = 0;
    for (int k = 0; k < correloHistogram.length; k++) {
      for (int i = 0; i < correloHistogram[k].size(); i++) {
        Point temp = correloHistogram[k].get(i);
        for (int j = i + 1; j < correloHistogram[k].size(); j++) {
          Point dummytemp = correloHistogram[k].get(j);
          distance = (int) temp.distance(dummytemp);

          if (correlogram.getCorrelogram()[i] == null) {
            correlogram.getCorrelogram()[i][distance] = 1;
          }
          else {
            correlogram.getCorrelogram()[i][distance]++;
          }

          if (correlogram.getCorrelogram()[i][distance] > maxcount) {
            maxcount = correlogram.getCorrelogram()[i][distance];
          }
        }
      }
    }
    correlogram.setMaxvalue(maxcount);
  }


  /**
   * Calculates the second and third moment of the hsv values
   * (standard deviation and skewness).
   */
  private void calculateMoments(int width, int height) {
    double normsize = 1.0 / (double) (width * height);

    // HSV color space
    for (int i = 0; i < 3; i++) {
      meanHSV[i] *= normsize;
    }

    double[] hsv_sum_2 = new double[3];
    double[] hsv_sum_3 = new double[3];
    double[] hsv_sum_4 = new double[3];

    // sum^2 and sum^3
    for (float[] hsvValue : hsvValues) {
      for (int j = 0; j < 3; j++) {
        double delta = hsvValue[j] - meanHSV[j];
        double square_delta = delta * delta;
        hsv_sum_2[j] += square_delta;
        hsv_sum_3[j] += square_delta * delta;
        hsv_sum_4[j] += square_delta * square_delta;
      }
    }

    // standard deviation, skewness and kurtosis
    for (int i = 0; i < stddevHSV.length; i++) {
      stddevHSV[i] = Math.sqrt(hsv_sum_2[i] / hsvValues.length);
      if (Double.isNaN(stddevHSV[i])) {
        stddevHSV[i] = 0;
      }

      double s_3 = stddevHSV[i] * stddevHSV[i] * stddevHSV[i];
      skewnessHSV[i] = hsv_sum_3[i] / (hsvValues.length * s_3);
      if (Double.isNaN(skewnessHSV[i])) {
        skewnessHSV[i] = 0;
      }

      double s_4 = s_3 * stddevHSV[i];
      kurtosisHSV[i] = (hsv_sum_4[i] / (hsvValues.length * s_4)) - 3;
      if (Double.isNaN(kurtosisHSV[i])) {
        kurtosisHSV[i] = 0;
      }
    }

    // HSL color space
    for (int i = 0; i < 3; i++) {
      meanHSL[i] *= normsize;
    }

    double[] hsl_sum_2 = new double[3];
    double[] hsl_sum_3 = new double[3];
    double[] hsl_sum_4 = new double[3];

    // sum^2 and sum^3
    for (float[] hslValue : hslValues) {
      for (int j = 0; j < 3; j++) {
        double delta = hslValue[j] - meanHSL[j];
        double square_delta = delta * delta;
        hsl_sum_2[j] += square_delta;
        hsl_sum_3[j] += square_delta * delta;
        hsl_sum_4[j] += square_delta * square_delta;
      }
    }

    // standard deviation, skewness and kurtosis
    for (int i = 0; i < stddevHSL.length; i++) {
      stddevHSL[i] = Math.sqrt(hsl_sum_2[i] / hslValues.length);
      if (Double.isNaN(stddevHSL[i])) {
        stddevHSL[i] = 0;
      }

      double s_3 = stddevHSL[i] * stddevHSL[i] * stddevHSL[i];
      skewnessHSL[i] = hsl_sum_3[i] / (hslValues.length * s_3);
      if (Double.isNaN(skewnessHSL[i])) {
        skewnessHSL[i] = 0;
      }

      double s_4 = s_3 * stddevHSL[i];
      kurtosisHSL[i] = (hsl_sum_4[i] / (hslValues.length * s_4)) - 3;
      if (Double.isNaN(kurtosisHSL[i])) {
        kurtosisHSL[i] = 0;
      }
    }

    // YUV color space
    for (int i = 0; i < 3; i++) {
      meanYUV[i] *= normsize;
    }

    double[] yuv_sum_2 = new double[3];
    double[] yuv_sum_3 = new double[3];
    double[] yuv_sum_4 = new double[3];

    // sum^2 and sum^3
    for (float[] yuvValue : yuvValues) {
      for (int j = 0; j < 3; j++) {
        double delta = yuvValue[j] - meanYUV[j];
        double square_delta = delta * delta;
        yuv_sum_2[j] += square_delta;
        yuv_sum_3[j] += square_delta * delta;
        yuv_sum_4[j] += square_delta * square_delta;
      }
    }

    // standard deviation, skewness and kurtosis
    for (int i = 0; i < stddevYUV.length; i++) {
      stddevYUV[i] = Math.sqrt(yuv_sum_2[i] / yuvValues.length);
      if (Double.isNaN(stddevYUV[i])) {
        stddevYUV[i] = 0;
      }

      double s_3 = stddevYUV[i] * stddevYUV[i] * stddevYUV[i];
      skewnessYUV[i] = yuv_sum_3[i] / (yuvValues.length * s_3);
      if (Double.isNaN(skewnessYUV[i])) {
        skewnessYUV[i] = 0;
      }

      double s_4 = s_3 * stddevYUV[i];
      kurtosisYUV[i] = (yuv_sum_4[i] / (yuvValues.length * s_4)) - 3;
      if (Double.isNaN(kurtosisYUV[i])) {
        kurtosisYUV[i] = 0;
      }
    }

  }

  /**
   * Calculates the Haralick texture features.
   */
  private void calculateTexture(int width, int height) {
    calculateStatistics();

    for (int d = 0; d < DISTANCES.length; d++) {
      for (int i = 0; i < NUM_GRAY_VALUES; i++) {
        double sum_j_p_x_minus_y = 0;
        for (int j = 0; j < NUM_GRAY_VALUES; j++) {
          double p_ij = cooccurrenceMatrices[d][i][j];

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
      timesEquals(cooccurrenceMatrices[d], NUM_GRAY_VALUES, NUM_GRAY_VALUES, 1 / cooccurrenceSums[d]);

      // p_x, p_y, p_x+y, p_x-y
      for (int i = 0; i < NUM_GRAY_VALUES; i++) {
        for (int j = 0; j < NUM_GRAY_VALUES; j++) {
          double p_ij = cooccurrenceMatrices[d][i][j];

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
          double p_ij = cooccurrenceMatrices[d][i][j];
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
   * Multiply a matrix by a scalar in place, A = s*A
   *
   * @param A matrix
   * @param s scalar
   */
  private void timesEquals(double[][] A, int m, int n, double s) {
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        A[i][j] *= s;
      }
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
  private float[] levelSurface(float[] pixels, int ww, int hh, ImageInfo oldInfo, int iter) {

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
    if (oldInfo != null) {
      ImageInfo newInfo = analyze(tempArray, ww, hh);
      normalize(tempArray, ww, hh, oldInfo, newInfo);
    }
    return tempArray;
  }

  private float[] gaussianFiltering(float[] pixels, int ww, int hh, ImageInfo oldInfo, double r, boolean roughness) {
    float[] pixels2 = gaussianBlur(pixels, ww, hh, oldInfo, r);
    if (roughness) {
      int counter = ww * hh;
      for (int j = 0; j < counter; j++) {
        pixels2[j] = pixels[j] - pixels2[j];
      }
      ImageInfo newInfo = analyze(pixels2, ww, hh);
      normalize(pixels2, ww, hh, oldInfo, newInfo);
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

    ImageInfo oldInfo = analyze(pixels, ww, hh);
    double pSize = 1;  // pixel size

    float[] pixels2;
    if (levelSurf) {
      pixels2 = levelSurface(pixels, ww, hh, oldInfo, 4);
    }
    else {
      pixels2 = pixels;
    }
/*
    if (true) {
       // image debugging
       try {
       // convert back to BufferedImage
       BufferedImage bufferimage = new BufferedImage(ww, hh, BufferedImage.TYPE_INT_RGB);
       for (int y = 0; y < hh; y++) {
         for (int x = 0; x < ww; x++) {
           int pos = ww * y + x;
           int gray = (int)(pixels2[pos] * 255.0);
           if (gray < 0) gray = 0;
           else if (gray > 255) gray = 255;
           int rgb = ((gray << RED_SHIFT) | (gray << GREEN_SHIFT) | (gray << BLUE_SHIFT));
           bufferimage.setRGB(x, y, rgb);
         }
       }
         ImageIO.write(bufferimage, "bmp", new File("c:\\debug_pic-level-"+levelSurf+".bmp"));
 		} catch (IOException e) {
         e.printStackTrace();
 		}
     }
*/

    if (filterSurf) {
      if (filterType == 0) {
        pixels2 = gaussianFiltering(pixels2, ww, hh, oldInfo, (lStruct / 2), true);    // Gaussian radius is equal to half the lStruct size
      }
      else if (filterType == 1) {
        pixels2 = gaussianFiltering(pixels2, ww, hh, oldInfo, (lStruct / 2), false);
      }
      else {
        throw new RuntimeException("Illegal argument!");
      }
    }
/*
    if (true) {
       // image debugging
       try {
       // convert back to BufferedImage
       BufferedImage bufferimage = new BufferedImage(ww, hh, BufferedImage.TYPE_INT_RGB);
       for (int y = 0; y < hh; y++) {
         for (int x = 0; x < ww; x++) {
           int pos = ww * y + x;
           int gray = (int)(pixels2[pos] * 255.0);
           if (gray < 0) gray = 0;
           else if (gray > 255) gray = 255;
           int rgb = ((gray << RED_SHIFT) | (gray << GREEN_SHIFT) | (gray << BLUE_SHIFT));
           bufferimage.setRGB(x, y, rgb);
         }
       }
         ImageIO.write(bufferimage, "bmp", new File("c:\\debug_pic-gauss-"+filterSurf+"-"+filterType+".bmp"));
 		} catch (IOException e) {
         e.printStackTrace();
 		}
     }
*/
    int N = ww * hh;
    double zMin = Double.MAX_VALUE;
    double zMax = -Double.MAX_VALUE;
    double ra = 0, sk = 0, ku = 0;
    double zMed = calculateMedian(pixels2, ww, hh);
    double zMean = calculateMean(pixels2, ww, hh);
    double zStd = calculateStdd(pixels2, ww, hh, zMean);

    for (int j = 0; j < N; j++) {
      double temp = pixels2[j];
      double zTemp = temp - zMean;
      sk += sqr3(zTemp / zStd);
      ku += sqr4(zTemp / zStd);
      ra += Math.abs(zTemp);
      zMin = Math.min(zMin, temp);
      zMax = Math.max(zMax, temp);
    }

    // roughness statistics
    roughness_rv[s] = zMin;  // lowest valley
    roughness_rp[s] = zMax;  // highest peak
    roughness_rt[s] = zMax + Math.abs(zMin);  // total height
    roughness_rm[s] = zMed;
    roughness_rq[s] = zStd;  // root mean square deviation
    roughness_ra[s] = ra / N;  // arithmetical mean deviation
    roughness_rsk[s] = sk / N;  // skewness
    roughness_rku[s] = (ku / N) - 3;  // kurtosis

    float[] ipPolar = new float[ww * hh];
    float[] ipAzimuthal = new float[ww * hh];
    double sArea = getFacetDetails(pixels2, ww, hh, oldInfo, pSize, true, ipPolar, ipAzimuthal);

    double sMin = Float.MAX_VALUE;
    double sMax = -Float.MAX_VALUE;
    double sSkew = 0, sKurt = 0;
    double sMed = calculateMedian(ipPolar, ww, hh);
    double sMean = calculateMean(ipPolar, ww, hh);
    double sStd = calculateStdd(ipPolar, ww, hh, sMean);

    for (int j = 0; j < N; j++) {
      double temp = ipPolar[j];
      double sTemp = temp - sMean;
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
    facet_area[s] = sArea / N; // Surface area
    facet_white[s] = calculateWhiteArea(ipPolar, ww, hh, polarThreshold);  // White area (0 <= polarThreshold <= 1.0 degrees, default 30)
//    facet_polar[s] = calculatePolarAngles(ipAzimuthal, ww, hh, 36);	// polar histogram
  }

  // Find the orientation of facets in a topographical image
  // (based on the Facetorientation plugin by Bob Dougherty)
  private double getFacetDetails(float[] pixels, int ww, int hh, ImageInfo oldInfo, double dx, boolean polarSlice, float[] theta, float[] phi) {

    final float[] kernelX = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
    final float[] kernelY = {1, 2, 1, 0, 0, 0, -1, -2, -1};

    // evaluate the gradients
    float[] gradX = convolve3x3(pixels, ww, hh, null, kernelX, true);
    float[] gradY = convolve3x3(pixels, ww, hh, null, kernelY, true);

    // Extract the orientation data from the gradients of the distance image
    // Normal vector components: nx = cos_phi sin_theta, ny = sin_phi sin_theta
    // Range image gradients: gx = -nx/nz, gy = -ny/nz
    // Solving: phi = atan(gy/gx) = atan2(gy,gx)
    //          theta = atan(sqrt(gx^2 + gy^2)
    double sArea = 0;
    for (int y = 0; y < hh; y++) {
      for (int x = 0; x < ww; x++) {
        int index = x + ww * y;
        // the derivative requires the factors 1/(2*dx) for the centered
        // finite difference and 1/4 for the coefficients of the kernel.
        double gx = (gradX[index] * 64) / (8 * dx);  // is this correct???
        double gy = (gradY[index] * 64) / (8 * dx);
        theta[index] = (float) (Math.atan(Math.sqrt(gx * gx + gy * gy)) / (Math.PI / 2.0));  // Math.hypot() is slow...
        phi[index] = (float) ((Math.atan2(gy, gx)) / (Math.PI / 2.0));
        // computes the surface element by da = sqrt(1 + (tan(theta))^2)
        // (suggestion given by Bob Dougherty, Optinav.com)
        sArea += sqr2(dx) * Math.sqrt(1 + sqr2(Math.tan(theta[index] * (Math.PI / 2.0))));
      }
    }
    return sArea;
  }

  private double calculateWhiteArea(float[] pixels, int ww, int hh, double thr) {
    int wa = 0;
    int counter = ww * hh;
    for (int j = 0; j < counter; j++) {
      if (pixels[j] < thr) wa++;
    }
    return (double) wa / (double) counter;
  }

  private double[] calculatePolarAngles(float[] azimuth, int ww, int hh, int size) {
    double[] aziAngle = new double[size];
    int aziCount = 0;
    for (int y = 1; y < hh - 2; y++) {
      for (int x = 1; x < ww - 2; x++) {
        int index = x + ww * y;
        int azi = (int) (azimuth[index] * size);
        if (azi < 0) azi += size;
        if (azi >= size) azi -= size;
        aziAngle[azi]++;
        aziCount++;
      }
    }
    // filter the values in order to remove deviating peaks (at 0, 45, 90, 135, etc.)
    float[] tempAngles = new float[3];
    for (int ii = 0; ii < size; ii++) {
      for (int iii = 0; iii < 3; iii++) {
        tempAngles[iii] = (float) aziAngle[(ii + iii) % size];
      }
      aziAngle[ii] = calculateMedian(tempAngles, 3, 1) / (double) aziCount;  // normalize
    }
    return aziAngle;
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

  private double sqr2(double x) {
    return x * x;
  }

  private double sqr3(double x) {
    return x * x * x;
  }

  private double sqr4(double x) {
    return x * x * x * x;
  }

  // analyze a picture of width ww and height hh
  private ImageInfo analyze(float[] pixels, int ww, int hh) {
    float minVal = Float.MAX_VALUE;
    float maxVal = -Float.MIN_VALUE;
    int counter = ww * hh;
    for (int j = 0; j < counter; j++) {
      float value = pixels[j];
      if (!Float.isInfinite(value)) {
        minVal = Math.min(minVal, value);
        maxVal = Math.max(maxVal, value);
      }
    }
    return new ImageInfo(minVal, maxVal);
  }

  // normalize a picture of width ww and height hh according to the provided information
  private void normalize(float[] pixels, int ww, int hh, ImageInfo oldInfo, ImageInfo newInfo) {
    if ((oldInfo.minVal != newInfo.minVal) || (oldInfo.maxVal != newInfo.maxVal)) {
      float bscale = (oldInfo.maxVal - oldInfo.minVal);
      if (bscale != 0) {
        float ascale = (newInfo.maxVal - newInfo.minVal);
        if (ascale != 0) {
          float abbias = (oldInfo.minVal * newInfo.maxVal) - (oldInfo.maxVal * newInfo.minVal);
          int counter = ww * hh;
          for (int j = 0; j < counter; j++) {
            pixels[j] = (pixels[j] * bscale + abbias) / ascale;
          }
        }
      }
    }
  }

  // 3x3 convolution by Glynne Casteel
  //@SuppressWarnings({"UnusedAssignment"})
  private float[] convolve3x3(float[] pixels, int ww, int hh, ImageInfo oldInfo, float[] kernel, boolean normalize) {
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
      float p1, p2, p3, p4, p5, p6, p7, p8, p9;
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
    if (oldInfo != null) {
      ImageInfo newInfo = analyze(pixels2, ww, hh);
      normalize(pixels2, ww, hh, oldInfo, newInfo);
    }
    return pixels2;
  }

  // performs a separate smoothing in each direction using a 1-D filter (the Gaussian filter is separable)
  private float[] gaussianBlur(float[] pixels, int ww, int hh, ImageInfo oldInfo, double radius) {
    float[] kernel = makeKernel(radius);
    pixels = convolve(pixels, ww, hh, oldInfo, kernel, kernel.length, 1, true);  // horizontal
    pixels = convolve(pixels, ww, hh, oldInfo, kernel, 1, kernel.length, true);  // vertical
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
  private float[] convolve(float[] pixels, int ww, int hh, ImageInfo oldInfo, float[] kernel, int kw, int kh, boolean normalize) {
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
    if (oldInfo != null) {
      ImageInfo newInfo = analyze(pixels2, ww, hh);
      normalize(pixels2, ww, hh, oldInfo, newInfo);
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

}
