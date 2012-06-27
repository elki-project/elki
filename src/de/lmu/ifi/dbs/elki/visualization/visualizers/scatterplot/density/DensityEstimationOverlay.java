package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.density;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.KMLOutputHandler;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.batikutil.ThumbnailRegistryEntry;
import de.lmu.ifi.dbs.elki.visualization.projections.CanvasSize;
import de.lmu.ifi.dbs.elki.visualization.projector.ScatterPlotProjector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot.AbstractScatterplotVisualization;

/**
 * A simple density estimation visualization, based on a simple kernel-density
 * <em>in the projection, not the actual data!</em>
 * 
 * @author Erich Schubert
 */
// TODO: make parameterizable, in particular color map, kernel bandwidth and
// kernel function
public class DensityEstimationOverlay extends AbstractScatterplotVisualization {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Density estimation overlay";

  /**
   * Density map resolution
   */
  private int resolution = 500;

  /**
   * The actual image
   */
  private BufferedImage img = null;

  /**
   * Constructor.
   * 
   * @param task Task
   */
  public DensityEstimationOverlay(VisualizationTask task) {
    super(task);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    if(img == null) {
      renderImage();
    }

    CanvasSize canvas = proj.estimateViewport();
    String imguri = ThumbnailRegistryEntry.INTERNAL_PREFIX + ThumbnailRegistryEntry.registerImage(img);
    Element itag = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(itag, SVGConstants.SVG_IMAGE_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_SPEED_VALUE);
    SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, canvas.minx);
    SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, canvas.miny);
    SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, canvas.maxx - canvas.minx);
    SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, canvas.maxy - canvas.miny);
    SVGUtil.setAtt(itag, SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_OPACITY_PROPERTY + ": .5");
    itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, imguri);

    layer.appendChild(itag);
  }

  @Reference(authors = "D. W. Scott", title = "Multivariate density estimation", booktitle = "Multivariate Density Estimation: Theory, Practice, and Visualization", url = "http://dx.doi.org/10.1002/9780470316849.fmatter")
  private double[] initializeBandwidth(double[][] data) {
    MeanVariance mv0 = new MeanVariance();
    MeanVariance mv1 = new MeanVariance();
    // For Kernel bandwidth.
    for(double[] projected : data) {
      mv0.put(projected[0]);
      mv1.put(projected[1]);
    }
    // Set bandwidths according to Scott's rule:
    // Note: in projected space, d=2.
    double[] bandwidth = new double[2];
    bandwidth[0] = MathUtil.SQRT5 * mv0.getSampleStddev() * Math.pow(rel.size(), -1 / 6.);
    bandwidth[1] = MathUtil.SQRT5 * mv1.getSampleStddev() * Math.pow(rel.size(), -1 / 6.);
    return bandwidth;
  }

  private void renderImage() {
    // TODO: SAMPLE? Do region queries?
    // Project the data just once, keep a copy.
    double[][] data = new double[rel.size()][];
    {
      int i = 0;
      for(DBIDIter iditer = rel.iterDBIDs(); iditer.valid(); iditer.advance()) {
        DBID id  = iditer.getDBID();
        data[i] = proj.fastProjectDataToRenderSpace(rel.get(id));
        i++;
      }
    }
    double[] bandwidth = initializeBandwidth(data);
    // Compare by first component
    Comparator<double[]> comp0 = new Comparator<double[]>() {
      @Override
      public int compare(double[] o1, double[] o2) {
        return Double.compare(o1[0], o2[0]);
      }
    };
    // Compare by second component
    Comparator<double[]> comp1 = new Comparator<double[]>() {
      @Override
      public int compare(double[] o1, double[] o2) {
        return Double.compare(o1[1], o2[1]);
      }
    };
    // TODO: choose comparator order based on smaller bandwidth?
    Arrays.sort(data, comp0);

    CanvasSize canvas = proj.estimateViewport();
    double min0 = canvas.minx, max0 = canvas.maxx, ste0 = (max0 - min0) / resolution;
    double min1 = canvas.miny, max1 = canvas.maxy, ste1 = (max1 - min1) / resolution;

    double kernf = 9. / (16 * bandwidth[0] * bandwidth[1]);
    double maxdens = 0.0;
    double[][] dens = new double[resolution][resolution];
    {
      // TODO: incrementally update the loff/roff values?
      for(int x = 0; x < resolution; x++) {
        double xlow = min0 + ste0 * x, xhig = xlow + ste0;
        int loff = unflip(Arrays.binarySearch(data, new double[] { xlow - bandwidth[0] }, comp0));
        int roff = unflip(Arrays.binarySearch(data, new double[] { xhig + bandwidth[0] }, comp0));
        // Resort by second component
        Arrays.sort(data, loff, roff, comp1);
        for(int y = 0; y < resolution; y++) {
          double ylow = min1 + ste1 * y, yhig = ylow + ste1;
          int boff = unflip(Arrays.binarySearch(data, loff, roff, new double[] { 0, ylow - bandwidth[1] }, comp1));
          int toff = unflip(Arrays.binarySearch(data, loff, roff, new double[] { 0, yhig + bandwidth[1] }, comp1));
          for(int pos = boff; pos < toff; pos++) {
            double[] val = data[pos];
            double d0 = (val[0] < xlow) ? (xlow - val[0]) : (val[0] > xhig) ? (val[0] - xhig) : 0;
            double d1 = (val[1] < ylow) ? (ylow - val[1]) : (val[1] > yhig) ? (val[1] - yhig) : 0;
            d0 = d0 / bandwidth[0];
            d1 = d1 / bandwidth[1];
            dens[x][y] += kernf * (1 - d0 * d0) * (1 - d1 * d1);
          }
          maxdens = Math.max(maxdens, dens[x][y]);
        }
        // Restore original sorting, as the intervals overlap
        Arrays.sort(data, loff, roff, comp0);
      }
    }
    img = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_ARGB);
    {
      for(int x = 0; x < resolution; x++) {
        for(int y = 0; y < resolution; y++) {
          int rgb = KMLOutputHandler.getColorForValue(dens[x][y] / maxdens).getRGB();
          img.setRGB(x, y, rgb);
        }
      }
    }
  }

  private int unflip(int binarySearch) {
    if(binarySearch < 0) {
      return (-binarySearch) - 1;
    }
    else {
      return binarySearch;
    }
  }

  /**
   * The visualization factory
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses DensityEstimationOverlay oneway - - «create»
   */
  public static class Factory extends AbstractVisFactory {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new DensityEstimationOverlay(task);
    }

    @Override
    public void processNewResult(HierarchicalResult baseResult, Result result) {
      Collection<ScatterPlotProjector<?>> ps = ResultUtil.filterResults(result, ScatterPlotProjector.class);
      for(ScatterPlotProjector<?> p : ps) {
        final VisualizationTask task = new VisualizationTask(NAME, p.getRelation(), p.getRelation(), this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA + 1);
        task.put(VisualizationTask.META_VISIBLE_DEFAULT, false);
        baseResult.getHierarchy().add(p, task);
      }
    }
  }
}