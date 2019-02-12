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
package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.jafama.FastMath;

/**
 * Visualizer, displaying the key for a clustering.
 *
 * TODO: re-add automatic sizing depending on the number of clusters.
 *
 * TODO: also show in scatter plot detail view.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class KeyVisualization implements VisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Cluster Key";

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    // Ensure there is a clustering result:
    if(!VisualizationTree.findNewResults(context, start).filter(Clustering.class).valid()) {
      return;
    }
    for(It<VisualizationTask> i2 = VisualizationTree.findVis(context).filter(VisualizationTask.class); i2.valid(); i2.advance()) {
      if(i2.get().getFactory() instanceof KeyVisualization) {
        return; // At most one key per plot.
      }
    }
    context.addVis(context.getStylingPolicy(), new VisualizationTask(this, NAME, context.getStylingPolicy(), null) //
        .level(VisualizationTask.LEVEL_STATIC).with(UpdateFlag.ON_STYLEPOLICY));
  }

  /**
   * Compute the size of the clustering.
   *
   * @param c Clustering
   * @return Array storing the depth and the number of leaf nodes.
   */
  protected static <M extends Model> int[] findDepth(Clustering<M> c) {
    final Hierarchy<Cluster<M>> hier = c.getClusterHierarchy();
    int[] size = { 0, 0 };
    for(It<Cluster<M>> iter = c.iterToplevelClusters(); iter.valid(); iter.advance()) {
      findDepth(hier, iter.get(), size);
    }
    return size;
  }

  /**
   * Recursive depth computation.
   *
   * @param hier Hierarchy
   * @param cluster Current cluster
   * @param size Counting array.
   */
  private static <M extends Model> void findDepth(Hierarchy<Cluster<M>> hier, Cluster<M> cluster, int[] size) {
    if(hier.numChildren(cluster) > 0) {
      for(It<Cluster<M>> iter = hier.iterChildren(cluster); iter.valid(); iter.advance()) {
        findDepth(hier, iter.get(), size);
      }
      size[0] += 1; // Depth
    }
    else {
      size[1] += 1; // Leaves
    }
  }

  /**
   * Compute the preferred number of columns.
   *
   * @param width Target width
   * @param height Target height
   * @param numc Number of clusters
   * @param maxwidth Max width of entries
   * @return Preferred number of columns
   */
  protected static int getPreferredColumns(double width, double height, int numc, double maxwidth) {
    // Maximum width (compared to height) of labels - guess.
    // FIXME: do we really need to do this three-step computation?
    // Number of rows we'd use in a squared layout:
    final double rows = Math.ceil(FastMath.pow(numc * maxwidth, height / (width + height)));
    // Given this number of rows (plus one for header), use this many columns:
    return (int) Math.ceil(numc / (rows + 1));
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    return false;
  }

  /**
   * Instance
   *
   * @author Erich Schubert
   *
   * @navhas - visualizes - Clustering
   */
  public class Instance extends AbstractVisualization {
    /**
     * CSS class for key captions.
     */
    private static final String KEY_CAPTION = "key-caption";

    /**
     * CSS class for key entries.
     */
    private static final String KEY_ENTRY = "key-entry";

    /**
     * CSS class for hierarchy plot lines
     */
    private static final String KEY_HIERLINE = "key-hierarchy";

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height) {
      super(context, task, plot, width, height);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      StyleLibrary style = context.getStyleLibrary();
      setupCSS(svgp);
      layer = svgp.svgElement(SVGConstants.SVG_G_TAG);

      StylingPolicy pol = context.getStylingPolicy();
      if(!(pol instanceof ClusterStylingPolicy)) {
        Element label = svgp.svgText(0.1, 0.7, "No clustering selected.");
        SVGUtil.setCSSClass(label, KEY_CAPTION);
        layer.appendChild(label);
        final double margin = style.getSize(StyleLibrary.MARGIN);
        final String transform = SVGUtil.makeMarginTransform(getWidth(), getHeight(), 10., 1., margin / StyleLibrary.SCALE);
        SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
        return;
      }
      @SuppressWarnings("unchecked")
      Clustering<Model> clustering = (Clustering<Model>) ((ClusterStylingPolicy) pol).getClustering();

      MarkerLibrary ml = style.markers();
      final List<Cluster<Model>> allcs = clustering.getAllClusters();
      final List<Cluster<Model>> topcs = clustering.getToplevelClusters();

      // Add a label for the clustering.
      {
        Element label = svgp.svgText(0.1, 0.7, clustering.getLongName());
        SVGUtil.setCSSClass(label, KEY_CAPTION);
        layer.appendChild(label);
      }

      double kwi, khe;
      if(allcs.size() == topcs.size()) {
        // Maximum width (compared to height) of labels - guess.
        // FIXME: compute from labels?
        final double maxwidth = 10.;

        // Flat clustering. Use multiple columns.
        final int numc = allcs.size();
        final int cols = getPreferredColumns(getWidth(), getHeight(), numc, maxwidth);
        final int rows = (int) Math.ceil(numc / (double) cols);
        // We use a coordinate system based on rows, so columns are at
        // c*maxwidth

        int i = 0;
        for(Cluster<Model> c : allcs) {
          final int col = i / rows;
          final int row = i % rows;
          ml.useMarker(svgp, layer, 0.3 + maxwidth * col, row + 1.5, i, 0.5);
          Element label = svgp.svgText(0.7 + maxwidth * col, row + 1.7, c.getNameAutomatic());
          SVGUtil.setCSSClass(label, KEY_ENTRY);
          layer.appendChild(label);
          i++;
        }
        kwi = cols * maxwidth;
        khe = rows;
      }
      else {
        // For consistent keying:
        Object2IntOpenHashMap<Cluster<Model>> cnum = new Object2IntOpenHashMap<>(allcs.size());
        int i = 0;
        for(Cluster<Model> c : allcs) {
          cnum.put(c, i);
          i++;
        }
        // Hierarchical clustering. Draw recursively.
        DoubleDoublePair size = new DoubleDoublePair(0., 1.),
            pos = new DoubleDoublePair(0., 1.);
        Hierarchy<Cluster<Model>> hier = clustering.getClusterHierarchy();
        for(Cluster<Model> cluster : topcs) {
          drawHierarchy(svgp, ml, size, pos, 0, cluster, cnum, hier);
        }
        kwi = size.first;
        khe = size.second;
      }

      final double margin = style.getSize(StyleLibrary.MARGIN);
      final String transform = SVGUtil.makeMarginTransform(getWidth(), getHeight(), kwi, khe, margin / StyleLibrary.SCALE);
      SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    }

    private double drawHierarchy(SVGPlot svgp, MarkerLibrary ml, DoubleDoublePair size, DoubleDoublePair pos, int depth, Cluster<Model> cluster, Object2IntOpenHashMap<Cluster<Model>> cnum, Hierarchy<Cluster<Model>> hier) {
      final double maxwidth = 8.;
      DoubleDoublePair subpos = new DoubleDoublePair(pos.first + maxwidth, pos.second);
      int numc = hier.numChildren(cluster);
      double posy;
      if(numc > 0) {
        double[] mids = new double[numc];
        It<Cluster<Model>> iter = hier.iterChildren(cluster);
        for(int i = 0; iter.valid(); iter.advance(), i++) {
          mids[i] = drawHierarchy(svgp, ml, size, subpos, depth, iter.get(), cnum, hier);
        }
        // Center:
        posy = (pos.second + subpos.second) * .5;
        for(int i = 0; i < numc; i++) {
          Element line = svgp.svgLine(pos.first + maxwidth - 1., posy + .5, pos.first + maxwidth, mids[i] + .5);
          SVGUtil.setCSSClass(line, KEY_HIERLINE);
          layer.appendChild(line);
        }
        // Use vertical extends of children:
        pos.second = subpos.second;
      }
      else {
        posy = pos.second + .5;
        pos.second += 1.;
      }
      ml.useMarker(svgp, layer, 0.3 + pos.first, posy + 0.5, cnum.getInt(cluster), 0.3);
      Element label = svgp.svgText(0.7 + pos.first, posy + 0.7, cluster.getNameAutomatic());
      SVGUtil.setCSSClass(label, KEY_ENTRY);
      layer.appendChild(label);
      size.first = Math.max(size.first, pos.first + maxwidth);
      size.second = Math.max(size.second, pos.second);
      return posy;
    }

    /**
     * Registers the Tooltip-CSS-Class at a SVGPlot.
     *
     * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
     */
    protected void setupCSS(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleLibrary();
      final double fontsize = style.getTextSize(StyleLibrary.KEY);
      final String fontfamily = style.getFontFamily(StyleLibrary.KEY);
      final String color = style.getColor(StyleLibrary.KEY);

      CSSClass keycaption = new CSSClass(svgp, KEY_CAPTION);
      keycaption.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
      keycaption.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, fontfamily);
      keycaption.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
      keycaption.setStatement(SVGConstants.CSS_FONT_WEIGHT_PROPERTY, SVGConstants.CSS_BOLD_VALUE);
      svgp.addCSSClassOrLogError(keycaption);

      CSSClass keyentry = new CSSClass(svgp, KEY_ENTRY);
      keyentry.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
      keyentry.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, fontfamily);
      keyentry.setStatement(SVGConstants.CSS_FILL_PROPERTY, color);
      svgp.addCSSClassOrLogError(keyentry);

      CSSClass hierline = new CSSClass(svgp, KEY_HIERLINE);
      hierline.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
      hierline.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth("key.hierarchy") / StyleLibrary.SCALE);
      svgp.addCSSClassOrLogError(hierline);

      svgp.updateStyleElement();
    }
  }
}
