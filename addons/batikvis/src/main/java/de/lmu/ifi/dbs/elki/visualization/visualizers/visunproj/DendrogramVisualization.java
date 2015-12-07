package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.database.datastore.DBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.IntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Dendrogram visualizer.
 *
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class DendrogramVisualization extends AbstractVisFactory {
  /**
   * Visualizer name.
   */
  private static final String NAME = "Dendrogram";

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    // Ensure there is a clustering result:
    Hierarchy.Iter<PointerHierarchyRepresentationResult> it = VisualizationTree.filterResults(context, start, PointerHierarchyRepresentationResult.class);
    for(; it.valid(); it.advance()) {
      PointerHierarchyRepresentationResult pi = it.get();
      final VisualizationTask task = new VisualizationTask(NAME, context, pi, null, this);
      task.level = VisualizationTask.LEVEL_STATIC;
      task.addUpdateFlags(VisualizationTask.ON_STYLEPOLICY);
      task.reqwidth = 1.;
      task.reqheight = 1.;
      context.addVis(context.getStylingPolicy(), task);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height);
  }

  /**
   * Visualization instance.
   *
   * @author Erich Schubert
   */
  public static class Instance extends AbstractVisualization {
    /**
     * CSS class for key captions.
     */
    private static final String KEY_CAPTION = "key-caption";

    /**
     * CSS class for hierarchy plot lines
     */
    private static final String KEY_HIERLINE = "key-hierarchy";

    /**
     * Constructor.
     *
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     */
    public Instance(VisualizationTask task, VisualizationPlot plot, double width, double height) {
      super(task, plot, width, height);
      addListeners();
    }

    @Override
    public void fullRedraw() {
      layer = svgp.svgElement(SVGConstants.SVG_G_TAG);

      StyleLibrary style = context.getStyleLibrary();
      StylingPolicy spol = context.getStylingPolicy();

      PointerHierarchyRepresentationResult p = task.getResult();

      DBIDs ids = p.getDBIDs();
      DBIDDataStore par = p.getParentStore();
      DoubleDataStore pdi = p.getParentDistanceStore();
      IntegerDataStore pos = p.getPositions();
      DBIDVar pa = DBIDUtil.newVar();

      int size = ids.size();
      double width = 16. * Math.log1p(size),
          height = width / getWidth() * getHeight();
      double xscale = width / size, xoff = xscale * .5;
      double maxh = Double.MIN_NORMAL;
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        if(DBIDUtil.equal(it, par.assignVar(it, pa))) {
          continue; // Root
        }
        double v = pdi.doubleValue(it);
        if(v == Double.POSITIVE_INFINITY) {
          continue;
        }
        maxh = v > maxh ? v : maxh;
      }
      double yscale = height / maxh;

      // Initial positions:
      double[] xy = new double[size << 1];
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        final int off = pos.intValue(it);
        xy[off] = off * xscale + xoff;
        xy[off + size] = height;
      }
      // Draw ascending by distance
      ArrayModifiableDBIDs order = DBIDUtil.newArray(ids);
      order.sort(new DataStoreUtil.AscendingByDoubleDataStoreAndId(pdi));

      if(spol instanceof ClassStylingPolicy) {
        ClassStylingPolicy cspol = (ClassStylingPolicy) spol;
        setupCSS(svgp, cspol);
        int mins = cspol.getMinStyle() - 1, maxs = cspol.getMaxStyle();
        SVGPath[] paths = new SVGPath[maxs - mins + 1];
        for(int i = 0; i < paths.length; i++) {
          paths[i] = new SVGPath();
        }
        for(DBIDIter it = order.iter(); it.valid(); it.advance()) {
          par.assignVar(it, pa); // Get parent.
          final int o1 = pos.intValue(it);
          final int p1 = cspol.getStyleForDBID(it);
          double x1 = xy[o1], y1 = xy[o1 + size];
          if(DBIDUtil.equal(it, pa)) {
            paths[p1 - mins + 1].moveTo(x1, y1).verticalLineTo(0);
            continue; // Root
          }
          final int o2 = pos.intValue(pa);
          final int p2 = cspol.getStyleForDBID(pa);
          double x2 = xy[o2], y2 = xy[o2 + size];
          double x3 = (x1 + x2) * .5,
              y3 = height - pdi.doubleValue(it) * yscale;
          if(p1 == p2) {
            paths[p1 - mins + 1].moveTo(x1, y1).verticalLineTo(y3).horizontalLineTo(x2).verticalLineTo(y2);
          }
          else {
            paths[y1 == height ? p1 - mins + 1 : 0].moveTo(x1, y1).verticalLineTo(y3);
            paths[y2 == height ? p2 - mins + 1 : 0].moveTo(x2, y2).verticalLineTo(y3);
            paths[0].moveTo(x1, y3).horizontalLineTo(x2);
          }
          xy[o2] = x3;
          xy[o2 + size] = y3;
        }
        for(int i = 0; i < paths.length; i++) {
          SVGPath path = paths[i];
          if(!path.isStarted()) {
            continue;
          }
          Element elem = path.makeElement(svgp);
          SVGUtil.setCSSClass(elem, (i > 0 ? KEY_HIERLINE + "_" + (i + mins - 1) : KEY_HIERLINE));
          layer.appendChild(elem);
        }
      }
      else {
        setupCSS(svgp);
        SVGPath dendrogram = new SVGPath();

        for(DBIDIter it = order.iter(); it.valid(); it.advance()) {
          final int o1 = pos.intValue(it);
          double x1 = xy[o1], y1 = xy[o1 + size];
          if(DBIDUtil.equal(it, par.assignVar(it, pa))) {
            dendrogram.moveTo(x1, y1).verticalLineTo(0);
            continue; // Root
          }
          final int o2 = pos.intValue(pa);
          double x2 = xy[o2], y2 = xy[o2 + size];
          double x3 = (x1 + x2) * .5,
              y3 = height - pdi.doubleValue(it) * yscale;
          dendrogram.moveTo(x1, y1).verticalLineTo(y3).horizontalLineTo(x2).verticalLineTo(y2);
          xy[o2] = x3;
          xy[o2 + size] = y3;
        }
        Element elem = dendrogram.makeElement(svgp);
        SVGUtil.setCSSClass(elem, KEY_HIERLINE);
        layer.appendChild(elem);
      }

      final double margin = style.getSize(StyleLibrary.MARGIN);
      final String transform = SVGUtil.makeMarginTransform(getWidth(), getHeight(), width, height, margin / StyleLibrary.SCALE);
      SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    }

    /**
     * Register the CSS classes.
     *
     * @param svgp the SVGPlot to register the CSS classes.
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

      CSSClass hierline = new CSSClass(svgp, KEY_HIERLINE);
      hierline.setStatement(SVGConstants.CSS_STROKE_PROPERTY, color);
      hierline.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth("key.hierarchy") / StyleLibrary.SCALE);
      hierline.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
      svgp.addCSSClassOrLogError(hierline);

      svgp.updateStyleElement();
    }

    /**
     * Register the CSS classes.
     *
     * @param svgp the SVGPlot to register the CSS classes.
     * @param cspol Class styling policy
     */
    protected void setupCSS(SVGPlot svgp, ClassStylingPolicy cspol) {
      setupCSS(svgp);
      StyleLibrary style = context.getStyleLibrary();
      ColorLibrary colors = style.getColorSet(StyleLibrary.PLOT);

      for(int i = cspol.getMinStyle(); i <= cspol.getMaxStyle(); i++) {
        CSSClass hierline = new CSSClass(svgp, KEY_HIERLINE + "_" + i);
        hierline.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colors.getColor(i));
        hierline.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth("key.hierarchy") / StyleLibrary.SCALE);
        hierline.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        svgp.addCSSClassOrLogError(hierline);
      }

      svgp.updateStyleElement();
    }
  }
}