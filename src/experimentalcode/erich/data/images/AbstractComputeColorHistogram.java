package experimentalcode.erich.data.images;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

public abstract class AbstractComputeColorHistogram extends AbstractParameterizable implements ComputeColorHistogram {
  public BufferedImage loadImage(File file) throws IOException {
    ImageInputStream is = ImageIO.createImageInputStream(file);
    Iterator<ImageReader> iter = ImageIO.getImageReaders(is);

    if(!iter.hasNext()) {
      throw new IOException("Unsupported file format.");
    }
    ImageReader imageReader = iter.next();
    imageReader.setInput(is);
    return imageReader.read(0);
  }

  /* (non-Javadoc)
   * @see experimentalcode.erich.data.images.ComputeColorHistogram#computeColorHistogram(java.lang.String)
   */
  public double[] computeColorHistogram(File file) throws IOException {
    BufferedImage image = loadImage(file);
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
  
  protected abstract int getNumBins();

  protected abstract int getBinForColor(int rgb);
}
