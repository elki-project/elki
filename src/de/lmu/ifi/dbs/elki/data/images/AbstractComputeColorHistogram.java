package de.lmu.ifi.dbs.elki.data.images;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;

/**
 * Abstract class for color histogram computation.
 *  
 * @author Erich Schubert
 */
public abstract class AbstractComputeColorHistogram extends AbstractLoggable implements ComputeColorHistogram {
  @Override
  public double[] computeColorHistogram(File file) throws IOException {
    BufferedImage image = ImageUtil.loadImage(file);
    int height = image.getHeight();
    int width = image.getWidth();
    double[] bins = new double[getNumBins()];

    for(int x = 0; x < width; x++) {
      for(int y = 0; y < height; y++) {
        int bin = getBinForColor(image.getRGB(x, y));
        assert(bin < bins.length);
        bins[bin] += 1;
      }
    }
    for (int i = 0; i < bins.length; i++) {
      bins[i] /= height * width;
    }
    return bins;
  }

  /**
   * Get the number of bins.
   * 
   * @return Number of bins
   */
  protected abstract int getNumBins();

  /**
   * Compute the bin number from a pixel color value.
   * 
   * @param rgb Pixel color value
   * @return Bin number
   */
  protected abstract int getBinForColor(int rgb);
}