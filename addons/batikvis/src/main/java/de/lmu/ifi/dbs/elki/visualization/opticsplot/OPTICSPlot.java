/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.opticsplot;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.ThumbnailRegistryEntry;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;

/**
 * Class to produce an OPTICS plot image.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @composed - - - LinearScale
 * @navhas - renders - ClusterOrder
 */
public class OPTICSPlot implements Result {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(OPTICSPlot.class);

  /**
   * Minimum and maximum vertical resolution.
   */
  private static final int MIN_HEIGHT = 25, MAX_HEIGHT = 300;

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
   * Ratio of plot
   */
  double ratio;

  /**
   * The result to plot.
   */
  final ClusterOrder co;

  /**
   * Color adapter to use
   */
  final StylingPolicy colors;

  /**
   * The Optics plot.
   */
  protected RenderedImage plot;

  /**
   * The plot number for Batik
   */
  protected int plotnum = -1;

  /**
   * Constructor, with automatic distance adapter detection.
   *
   * @param co Cluster order to plot.
   * @param colors Coloring strategy
   */
  public OPTICSPlot(ClusterOrder co, StylingPolicy colors) {
    super();
    this.co = co;
    this.colors = colors;
  }

  /**
   * Trigger a redraw of the OPTICS plot
   */
  public void replot() {
    width = co.size();
    height = (int) Math.ceil(width * .2);
    ratio = width / (double) height;
    height = height < MIN_HEIGHT ? MIN_HEIGHT : height > MAX_HEIGHT ? MAX_HEIGHT : height;
    if(scale == null) {
      scale = computeScale(co);
    }

    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    int x = 0;
    for(DBIDIter it = co.iter(); it.valid(); it.advance()) {
      double reach = co.getReachability(it);
      final int y = scaleToPixel(reach);
      try {
        int col = colors.getColorForDBID(it);
        for(int y2 = height - 1; y2 >= y; y2--) {
          img.setRGB(x, y2, col);
        }
      }
      catch(ArrayIndexOutOfBoundsException e) {
        LOG.error("Plotting out of range: " + x + "," + y + " >= " + width + "x" + height);
      }
      x++;
    }

    plot = img;
  }

  /**
   * Scale a reachability distance to a pixel value.
   *
   * @param reach Reachability
   * @return Pixel value.
   */
  public int scaleToPixel(double reach) {
    return (Double.isInfinite(reach) || Double.isNaN(reach)) ? 0 : //
    (int) Math.round(scale.getScaled(reach, height - .5, .5));
  }

  /**
   * Scale a pixel value to a reachability
   *
   * @param y Pixel value
   * @return Reachability
   */
  public double scaleFromPixel(double y) {
    return scale.getUnscaled((y - .5) / (height - 1.));
  }

  /**
   * Compute the scale (value range)
   *
   * @param order Cluster order to process
   * @return Scale for value range of cluster order
   */
  protected LinearScale computeScale(ClusterOrder order) {
    DoubleMinMax range = new DoubleMinMax();
    // calculate range
    for(DBIDIter it = order.iter(); it.valid(); it.advance()) {
      final double reach = co.getReachability(it);
      if(reach < Double.POSITIVE_INFINITY) {
        range.put(reach);
      }
    }
    // Ensure we have a valid range
    if(!range.isValid()) {
      range.put(0.0);
      range.put(1.0);
    }
    return new LinearScale(range.getMin(), range.getMax());
  }

  /**
   * @return the scale
   */
  public LinearScale getScale() {
    if(plot == null) {
      replot();
    }
    return scale;
  }

  /**
   * @return the width
   */
  public int getWidth() {
    if(plot == null) {
      replot();
    }
    return width;
  }

  /**
   * @return the height
   */
  public int getHeight() {
    if(plot == null) {
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
    if(plot == null) {
      replot();
    }
    return ratio;
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
   * Free memory used by rendered image.
   */
  public void forgetRenderedImage() {
    plotnum = -1;
    plot = null;
  }

  /**
   * Get the SVG registered plot number
   *
   * @return Plot URI
   */
  public String getSVGPlotURI() {
    if(plotnum < 0) {
      plotnum = ThumbnailRegistryEntry.registerImage(plot);
    }
    return ThumbnailRegistryEntry.INTERNAL_PREFIX + plotnum;
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
   * Static method to find an optics plot for a result, or to create a new one
   * using the given context.
   *
   * @param co Cluster order
   * @param context Context (for colors and reference clustering)
   *
   * @return New or existing optics plot
   */
  public static OPTICSPlot plotForClusterOrder(ClusterOrder co, VisualizerContext context) {
    // Check for an existing plot
    // ArrayList<OPTICSPlot<D>> plots = ResultUtil.filterResults(co,
    // OPTICSPlot.class);
    // if (plots.size() > 0) {
    // return plots.get(0);
    // }
    final StylingPolicy policy = context.getStylingPolicy();
    OPTICSPlot opticsplot = new OPTICSPlot(co, policy);
    // co.addChildResult(opticsplot);
    return opticsplot;
  }

  /**
   * Get the cluster order we are attached to.
   *
   * @return Cluster order
   */
  public ClusterOrder getClusterOrder() {
    return co;
  }
}
