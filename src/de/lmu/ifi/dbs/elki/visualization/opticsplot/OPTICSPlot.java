package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.distance.distancevalue.CorrelationDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Class to produce an OPTICS plot image.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf LinearScale
 * @apiviz.composedOf OPTICSColorAdapter
 * @apiviz.composedOf OPTICSDistanceAdapter
 * @apiviz.has ClusterOrderResult oneway - - renders
 * 
 * @param <D> Distance type
 */
public class OPTICSPlot<D extends Distance<D>> implements Result {
  /**
   * Logger
   */
  protected static final Logging logger = Logging.getLogger(OPTICSPlot.class);

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
   * @param colors Coloring strategy
   * @param distanceAdapter Distance adapter
   */
  public OPTICSPlot(ClusterOrderResult<D> co, OPTICSColorAdapter colors, OPTICSDistanceAdapter<D> distanceAdapter) {
    super();
    this.co = co;
    this.colors = colors;
    this.distanceAdapter = distanceAdapter;
  }

  /**
   * Constructor, with automatic distance adapter detection.
   * 
   * @param co Cluster order to plot.
   * @param colors Coloring strategy
   */
  public OPTICSPlot(ClusterOrderResult<D> co, OPTICSColorAdapter colors) {
    super();
    this.co = co;
    this.colors = colors;
    this.distanceAdapter = getAdapterForDistance(co);
  }

  /**
   * Try to find a distance adapter.
   * 
   * @param <D> distance type
   * @param co ClusterOrderResult
   * @return distance adapter
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <D extends Distance<D>> OPTICSDistanceAdapter<D> getAdapterForDistance(ClusterOrderResult<D> co) {
    Class<?> dcls = co.getDistanceClass();
    if(dcls != null && NumberDistance.class.isAssignableFrom(dcls)) {
      return new OPTICSNumberDistance();
    }
    else if(dcls != null && CorrelationDistance.class.isAssignableFrom(dcls)) {
      return new OPTICSCorrelationDimensionalityDistance();
    }
    else if(dcls == null) {
      throw new UnsupportedOperationException("No distance in cluster order?!?");
    }
    else {
      throw new UnsupportedOperationException("No distance adapter found for distance class: " + dcls);
    }
  }

  /**
   * Test whether this class can produce an OPTICS plot for the given cluster
   * order.
   * 
   * @param <D> Distance type
   * @param co Cluster order result
   * @return test result
   */
  public static <D extends Distance<D>> boolean canPlot(ClusterOrderResult<D> co) {
    try {
      if(getAdapterForDistance(co) != null) {
        return true;
      }
      return false;
    }
    catch(UnsupportedOperationException e) {
      return false;
    }
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
    if(plot == null && tempFile == null) {
      replot();
    }
    return scale;
  }

  /**
   * @return the width
   */
  public int getWidth() {
    if(plot == null && tempFile == null) {
      replot();
    }
    return width;
  }

  /**
   * @return the height
   */
  public int getHeight() {
    if(plot == null && tempFile == null) {
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
    if(plot == null && tempFile == null) {
      replot();
    }
    return ((double) width) / height;
  }

  /**
   * Get the OPTICS plot.
   * 
   * @return plot image
   */
  public synchronized RenderedImage getPlot() {
    if(plot == null) {
      replot();
    }
    return plot;
  }

  /**
   * Get the distance adapter-
   * 
   * @return the distanceAdapter
   */
  public OPTICSDistanceAdapter<D> getDistanceAdapter() {
    return distanceAdapter;
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

  @Override
  public String getLongName() {
    return "OPTICS Plot";
  }

  @Override
  public String getShortName() {
    return "optics plot";
  }
  
  /**
   * Static method to find an optics plot for a result,
   * or to create a new one using the given context.
   * 
   * @param <D> Distance type
   * @param co Cluster order
   * @param context Context (for colors and reference clustering)
   * 
   * @return New or existing optics plot
   */
  public static <D extends Distance<D>> OPTICSPlot<D> plotForClusterOrder(ClusterOrderResult<D> co, VisualizerContext context) {
    // Check for an existing plot
    ArrayList<OPTICSPlot<D>> plots = ResultUtil.filterResults(co, OPTICSPlot.class);
    if (plots.size() > 0) {
      return plots.get(0);
    }
    // Supported by this class?
    if (!OPTICSPlot.canPlot(co)) {
      return null;
    }
    final ColorLibrary colors = context.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
    final Clustering<?> refc = context.getOrCreateDefaultClustering();
    final OPTICSColorAdapter opcolor = new OPTICSColorFromClustering(colors, refc);
    
    OPTICSPlot<D> opticsplot = new OPTICSPlot<D>(co, opcolor);
    co.addChildResult(opticsplot);
    return opticsplot;
  }
}