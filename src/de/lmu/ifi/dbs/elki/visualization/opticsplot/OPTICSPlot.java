package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;

/**
 * Class to produce an OPTICS plot image.
 * 
 * @author Erich Schubert
 * @param <D> Distance type
 */
public class OPTICSPlot<D extends Distance<?>> {
  /**
   * Logger
   */
  protected Logging logger = Logging.getLogger(OPTICSPlot.class);

  /**
   * Prefix for filenames
   */
  private static final String IMGFILEPREFIX = "elki-optics-";

  /**
   * Scale to use
   */
  LinearScale scale;

  /**
   * Width of plot
   */
  int width;

  /**
   * Height of plot
   */
  int height;

  /**
   * The result to plot
   */
  final ClusterOrderResult<D> co;

  /**
   * Color adapter to use
   */
  final OPTICSColorAdapter colors;

  /**
   * The mapping from cluster order entry to value
   */
  final OPTICSDistanceAdapter<D> distanceAdapter;

  /**
   * The Optics plot.
   */
  protected RenderedImage plot;

  /**
   * The plot saved to a temp file.
   */
  protected File tempFile;

  /**
   * Constructor.
   * 
   * @param co Cluster order to plot.
   * @param colors Colorization strategy
   * @param distanceAdapter Distance adapter
   */
  public OPTICSPlot(ClusterOrderResult<D> co, OPTICSColorAdapter colors, OPTICSDistanceAdapter<D> distanceAdapter) {
    super();
    this.co = co;
    this.colors = colors;
    this.distanceAdapter = distanceAdapter;
  }

  /**
   * Trigger a redraw of the OPTICS plot
   */
  public void replot() {
    List<ClusterOrderEntry<D>> order = co.getClusterOrder();

    width = order.size();
    height = Math.min(200, (int) Math.ceil(width / 5));
    if(scale == null) {
      scale = computeScale(order);
    }

    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    int x = 0;
    for(ClusterOrderEntry<D> coe : order) {
      double reach = distanceAdapter.getDoubleForEntry(coe);
      final int y;
      if(!Double.isInfinite(reach) && !Double.isNaN(reach)) {
        y = (height - 1) - (int) Math.floor(scale.getScaled(reach) * (height - 1));
      }
      else {
        y = 0;
      }
      try {
        int col = colors.getColorForEntry(coe);
        for(int y2 = height - 1; y2 >= y; y2--) {
          img.setRGB(x, y2, col);
        }
      }
      catch(ArrayIndexOutOfBoundsException e) {
        logger.error("Plotting out of range: " + x + "," + y + " >= " + width + "x" + height);
      }
      x++;
    }

    plot = img;
  }

  /**
   * Compute the scale (value range)
   * 
   * @param order Cluster order to process
   * @return Scale for value range of cluster order
   */
  protected LinearScale computeScale(List<ClusterOrderEntry<D>> order) {
    MinMax<Double> range = new MinMax<Double>();
    // calculate range
    for(ClusterOrderEntry<D> coe : order) {
      double reach = distanceAdapter.getDoubleForEntry(coe);
      if(!Double.isInfinite(reach) && !Double.isNaN(reach)) {
        range.put(reach);
      }
    }
    // Avoid a null pointer exception when we don't have valid range values.
    if(range.getMin() == null) {
      range.put(0.0);
      range.put(1.0);
    }
    return new LinearScale(range.getMin(), range.getMax());
  }

  /**
   * @return the scale
   */
  public LinearScale getScale() {
    if (plot == null && tempFile == null) {
      replot();
    }
    return scale;
  }

  /**
   * @return the width
   */
  public int getWidth() {
    if (plot == null && tempFile == null) {
      replot();
    }
    return width;
  }

  /**
   * @return the height
   */
  public int getHeight() {
    if (plot == null && tempFile == null) {
      replot();
    }
    return height;
  }

  /**
   * Get width-to-height ratio of image.
   * 
   * @return {@code width / height}
   */
  public double getRatio() {
    if (plot == null && tempFile == null) {
      replot();
    }
    return ((double) width) / height;
  }

  /**
   * Get the OPTICS plot.
   * 
   * @return plot image
   */
  public RenderedImage getPlot() {
    if(plot == null) {
      replot();
    }
    return plot;
  }

  /**
   * Get a temporary file for the optics plot.
   * 
   * @return Temp file containing the plot
   * @throws IOException
   */
  public File getAsTempFile() throws IOException {
    if(tempFile == null) {
      tempFile = File.createTempFile(IMGFILEPREFIX, ".png");
      tempFile.deleteOnExit();
      ImageIO.write(getPlot(), "PNG", tempFile);
    }
    return tempFile;
  }
 
  /**
   * Free memory used by rendered image.
   */
  public void forgetRenderedImage() {
    plot = null;
  }
}