/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.visualization.silhouette;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import elki.database.ids.DoubleDBIDList;
import elki.database.ids.DoubleDBIDListIter;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.math.scales.LinearScale;
import elki.visualization.VisualizerContext;
import elki.visualization.batikutil.ThumbnailRegistryEntry;
import elki.visualization.style.StylingPolicy;

/**
 * Class to produce an Silhouette plot image.
 *
 * @author Robert Gehde
 * @since 0.8.0
 *
 * @composed - - - LinearScale
 */
public class SilhouettePlot {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(SilhouettePlot.class);

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
   * The silhouette values.
   */
  final DoubleDBIDList[] silhouettes;

  /**
   * Color adapter to use
   */
  final StylingPolicy colors;

  /**
   * The Silhouette plot.
   */
  protected RenderedImage plot;

  /**
   * The plot number for Batik
   */
  protected int plotnum = -1;

  /**
   * Constructor, with automatic distance adapter detection.
   *
   * @param silhouettes silhouette values to plot.
   * @param colors Coloring strategy
   */
  public SilhouettePlot(DoubleDBIDList[] silhouettes, StylingPolicy colors) {
    super();
    this.silhouettes = silhouettes;
    this.colors = colors;
  }

  /**
   * Trigger a redraw of the Silhouette plot
   */
  public void replot() {
    // calc width
    width = 0;
    for(DoubleDBIDList doubleDBIDList : silhouettes) {
      width += doubleDBIDList.size();
    }
    // here i need to add some pixels between each cluster so + 3 *
    // (clus.size-1)
    width += 3 * (silhouettes.length - 1);
    height = (int) Math.ceil(width * .2);
    ratio = width / (double) height;
    height = height < MIN_HEIGHT ? MIN_HEIGHT : height > MAX_HEIGHT ? MAX_HEIGHT : height;
    scale = scale != null ? scale : computeScale(silhouettes);

    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    int x = 0;
    final int start = scaleToPixel(0);
    for(DoubleDBIDList list : silhouettes) {
      for(DoubleDBIDListIter it = list.iter(); it.valid(); it.advance()) {
        double silhouette = it.doubleValue();
        final int y = scaleToPixel(silhouette);
        try {
          int col = colors.getColorForDBID(it);
          int dir = silhouette < 0 ? 1 : -1;
          for(int y2 = start; y2 != y + dir; y2 += dir) {
            // y + dir means that we include y itself and then stop
            img.setRGB(x, y2, col);
          }
        }
        catch(ArrayIndexOutOfBoundsException e) {
          LOG.error("Plotting out of range: " + x + "," + y + " >= " + width + "x" + height);
        }
        x++;
      }
      // leave space between clusters
      x += 3;
    }
    plot = img;
  }

  /**
   * Scale a silhouette score to a pixel value.
   *
   * @param silhouette silhouette
   * @return Pixel value.
   */
  public int scaleToPixel(double silhouette) {
    return (Double.isInfinite(silhouette) || Double.isNaN(silhouette)) ? 0 : //
        (int) Math.round(scale.getScaled(silhouette, height - 1, 0));
  }

  /**
   * Scale a pixel value to a silhouette value
   *
   * @param y Pixel value
   * @return silhouette
   */
  public double scaleFromPixel(double y) {
    return scale.getUnscaled((y - .5) / (height - 1.));
  }

  /**
   * Compute the scale (value range)
   *
   * @param silhouettes Silhouette values to process
   * @return Scale for value range of silhouette values
   */
  protected LinearScale computeScale(DoubleDBIDList[] silhouettes) {
    DoubleMinMax range = new DoubleMinMax(0, 0); // Always include 0
    for(DoubleDBIDList list : silhouettes) {
      for(DoubleDBIDListIter it = list.iter(); it.valid(); it.advance()) {
        final double silhouette = it.doubleValue();
        if(Double.isFinite(silhouette)) {
          range.put(silhouette);
        }
      }
    }
    // Ensure we have a valid range
    if(!range.isValid()) {
      range.put(0.0);
      range.put(1.0);
    }
    return new LinearScale(Math.min(0, range.getMin()), range.getMax());
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
   * Get the Silhouette plot.
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

  // @Override
  public String getLongName() {
    return "Silhouette Plot";
  }

  // @Override
  public String getShortName() {
    return "silhouette plot";
  }

  /**
   * Static method to find a silhouette plot for a result, or to create a new
   * one using the given context.
   *
   * @param silhouettes silhouette values
   * @param context Context (for colors and reference clustering)
   * @return New or existing silhouette plot
   */
  public static SilhouettePlot plotForSilhouetteValues(DoubleDBIDList[] silhouettes, VisualizerContext context) {
    return new SilhouettePlot(silhouettes, context.getStylingPolicy());
  }
}
