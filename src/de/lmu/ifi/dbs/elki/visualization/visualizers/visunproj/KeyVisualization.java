package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Collection;
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGButton;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualizer, displaying the key for a clustering.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class KeyVisualization extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Cluster Key";

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // Find clusterings we can visualize:
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(newResult, Clustering.class);
    for (Clustering<?> c : clusterings) {
      final int numc = c.getAllClusters().size();
      final int topc = c.getToplevelClusters().size();
      if (numc > 0) {
        final VisualizationTask task = new VisualizationTask(NAME, c, null, this);
        if (numc == topc) {
          // FIXME: compute from labels?
          final double maxwidth = 10.;
          // Flat clustering.
          final int cols = getPreferredColumns(1.0, 1.0, numc, maxwidth);
          final int rows = (int) Math.ceil(numc / (double) cols);
          final double ratio = cols * maxwidth / (2. + rows);
          task.width = (ratio >= 1.) ? 1 : 1. / ratio;
          task.height = (ratio >= 1.) ? 1. / ratio : 1;
          if (numc > 100) {
            task.width *= 2;
            task.height *= 2;
          }
        } else {
          // Hierarchical clustering.
          final int[] shape = findDepth(c);
          final double maxwidth = 8.;
          final double ratio = shape[0] * maxwidth / (2. + shape[1]);
          task.width = (ratio >= 1.) ? 1 : 1. / ratio;
          task.height = (ratio >= 1.) ? 1. / ratio : 1;
          if (shape[0] * maxwidth > 20 || shape[1] > 18) {
            task.width *= 2;
            task.height *= 2;
          }
        }
        task.level = VisualizationTask.LEVEL_STATIC;
        if (numc < 20) {
          task.nodetail = true;
        }
        baseResult.getHierarchy().add(c, task);
      }
    }
  }

  private static <M extends Model> int[] findDepth(Clustering<M> c) {
    final Hierarchy<Cluster<M>> hier = c.getClusterHierarchy();
    int[] size = { 0, 0 };
    for (Iter<Cluster<M>> iter = c.iterToplevelClusters(); iter.valid(); iter.advance()) {
      findDepth(hier, iter.get(), size);
    }
    return size;
  }

  private static <M extends Model> void findDepth(Hierarchy<Cluster<M>> hier, Cluster<M> cluster, int[] size) {
    if (hier.numChildren(cluster) > 0) {
      for (Iter<Cluster<M>> iter = hier.iterChildren(cluster); iter.valid(); iter.advance()) {
        findDepth(hier, iter.get(), size);
      }
      size[0] += 1; // Depth
    } else {
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
  public static int getPreferredColumns(double width, double height, int numc, double maxwidth) {
    // Maximum width (compared to height) of labels - guess.
    // FIXME: do we really need to do this three-step computation?
    // Number of rows we'd use in a squared layout:
    final double rows = Math.ceil(Math.pow(numc * maxwidth, height / (width + height)));
    // Given this number of rows (plus two for header), use this many columns:
    return (int) Math.ceil(numc / (rows + 2));
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    return new Instance(task);
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
   * @apiviz.has Clustering oneway - - visualizes
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
     * Clustering to display
     */
    private Clustering<Model> clustering;

    /**
     * Constructor.
     * 
     * @param task Visualization task
     */
    public Instance(VisualizationTask task) {
      super(task);
      this.clustering = task.getResult();
      context.addResultListener(this);
    }

    @Override
    public void destroy() {
      context.removeResultListener(this);
      super.destroy();
    }

    @Override
    public void resultChanged(Result current) {
      super.resultChanged(current);
      if (current == context.getStyleResult()) {
        incrementalRedraw();
      }
    }

    @Override
    protected void redraw() {
      StyleLibrary style = context.getStyleResult().getStyleLibrary();
      MarkerLibrary ml = style.markers();

      final List<Cluster<Model>> allcs = clustering.getAllClusters();
      final List<Cluster<Model>> topcs = clustering.getToplevelClusters();

      setupCSS(svgp);
      layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
      // Add a label for the clustering.
      {
        Element label = svgp.svgText(0.1, 0.7, clustering.getLongName());
        SVGUtil.setCSSClass(label, KEY_CAPTION);
        layer.appendChild(label);
      }

      final int extrarows = 2;
      double kwi, khe;
      if (allcs.size() == topcs.size()) {
        // Maximum width (compared to height) of labels - guess.
        // FIXME: compute from labels?
        final double maxwidth = 10.;

        // Flat clustering. Use multiple columns.
        final int numc = allcs.size();
        final int cols = getPreferredColumns(task.getWidth(), task.getHeight(), numc, maxwidth);
        final int rows = (int) Math.ceil(numc / (double) cols);
        // We use a coordinate system based on rows, so columns are at
        // c*maxwidth

        int i = 0;
        for (Cluster<Model> c : allcs) {
          final int col = i / rows;
          final int row = i % rows;
          ml.useMarker(svgp, layer, 0.3 + maxwidth * col, row + 1.5, i, 0.3);
          Element label = svgp.svgText(0.7 + maxwidth * col, row + 1.7, c.getNameAutomatic());
          SVGUtil.setCSSClass(label, KEY_ENTRY);
          layer.appendChild(label);
          i++;
        }
        kwi = cols * maxwidth;
        khe = rows;
      } else {
        // For consistent keying:
        TObjectIntMap<Cluster<Model>> cnum = new TObjectIntHashMap<>(allcs.size());
        int i = 0;
        for (Cluster<Model> c : allcs) {
          cnum.put(c, i);
          i++;
        }
        // Hierarchical clustering. Draw recursively.
        DoubleDoublePair size = new DoubleDoublePair(0., 1.), pos = new DoubleDoublePair(0., 1.);
        Hierarchy<Cluster<Model>> hier = clustering.getClusterHierarchy();
        for (Cluster<Model> cluster : topcs) {
          drawHierarchy(svgp, ml, size, pos, 0, cluster, cnum, hier);
        }
        kwi = size.first;
        khe = size.second;
      }

      // Add a button to set style policy
      {
        StylingPolicy sp = context.getStyleResult().getStylingPolicy();
        if (sp instanceof ClusterStylingPolicy && ((ClusterStylingPolicy) sp).getClustering() == clustering) {
          // Don't show the button when active. May confuse people more than the
          // disappearing button?

          // SVGButton button = new SVGButton(.1, rows + 1.1, 3.8, .7, .2);
          // button.setTitle("Active style", "darkgray");
          // layer.appendChild(button.render(svgp));
        } else {
          SVGButton button = new SVGButton(.1, khe + 1.1, 3.8, .7, .2);
          button.setTitle("Set style", "black");
          Element elem = button.render(svgp);
          // Attach listener
          EventTarget etr = (EventTarget) elem;
          etr.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, new EventListener() {
            @Override
            public void handleEvent(Event evt) {
              setStylePolicy();
            }
          }, false);
          layer.appendChild(elem);
        }
      }

      final double margin = style.getSize(StyleLibrary.MARGIN);
      final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), kwi, khe + extrarows, margin / StyleLibrary.SCALE);
      SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    }

    private double drawHierarchy(SVGPlot svgp, MarkerLibrary ml, DoubleDoublePair size, DoubleDoublePair pos, int depth, Cluster<Model> cluster, TObjectIntMap<Cluster<Model>> cnum, Hierarchy<Cluster<Model>> hier) {
      final double maxwidth = 8.;
      DoubleDoublePair subpos = new DoubleDoublePair(pos.first + maxwidth, pos.second);
      int numc = hier.numChildren(cluster);
      double posy;
      if (numc > 0) {
        double[] mids = new double[numc];
        Iter<Cluster<Model>> iter = hier.iterChildren(cluster);
        for (int i = 0; iter.valid(); iter.advance(), i++) {
          mids[i] = drawHierarchy(svgp, ml, size, subpos, depth, iter.get(), cnum, hier);
        }
        // Center:
        posy = (pos.second + subpos.second) * .5;
        for (int i = 0; i < numc; i++) {
          Element line = svgp.svgLine(pos.first + maxwidth - 1., posy + .5, pos.first + maxwidth, mids[i] + .5);
          SVGUtil.setCSSClass(line, KEY_HIERLINE);
          layer.appendChild(line);
        }
        // Use vertical extends of children:
        pos.second = subpos.second;
      } else {
        posy = pos.second + .5;
        pos.second += 1.;
      }
      ml.useMarker(svgp, layer, 0.3 + pos.first, posy + 0.5, cnum.get(cluster), 0.3);
      Element label = svgp.svgText(0.7 + pos.first, posy + 0.7, cluster.getNameAutomatic());
      SVGUtil.setCSSClass(label, KEY_ENTRY);
      layer.appendChild(label);
      size.first = Math.max(size.first, pos.first + maxwidth);
      size.second = Math.max(size.second, pos.second);
      return posy;
    }

    /**
     * Trigger a style change.
     */
    protected void setStylePolicy() {
      context.getStyleResult().setStylingPolicy(new ClusterStylingPolicy(clustering, context.getStyleResult().getStyleLibrary()));
      context.getHierarchy().resultChanged(context.getStyleResult());
    }

    /**
     * Registers the Tooltip-CSS-Class at a SVGPlot.
     * 
     * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
     */
    protected void setupCSS(SVGPlot svgp) {
      final StyleLibrary style = context.getStyleResult().getStyleLibrary();
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
