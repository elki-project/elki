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
package de.lmu.ifi.dbs.elki.visualization.visualizers.optics;

import java.awt.Color;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.OPTICSXi;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize the steep areas found in an OPTICS plot
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class OPTICSSteepAreaVisualization implements VisFactory {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "OPTICS Steep Areas";

  /**
   * Constructor.
   */
  public OPTICSSteepAreaVisualization() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizationTree.findVis(context, result).filter(OPTICSProjector.class).forEach(p -> {
      ClusterOrder co = p.getResult();
      It<OPTICSXi.SteepAreaResult> r = co.getHierarchy().iterChildren(co).filter(OPTICSXi.SteepAreaResult.class);
      if(r.valid()) {
        final VisualizationTask task = new VisualizationTask(this, NAME, p.getResult(), null) //
            .level(VisualizationTask.LEVEL_DATA + 1);
        context.addVis(p, task);
        context.addVis(r.get(), task);
      }
    });
  }

  @Override
  public Visualization makeVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    return new Instance(context, task, plot, width, height, proj);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance
   *
   * @author Erich Schubert
   *
   * @assoc - - - SteepAreaResult
   */
  public class Instance extends AbstractOPTICSVisualization {
    /**
     * CSS class for markers
     */
    protected static final String CSS_STEEP_UP = "opticsSteepUp";

    /**
     * CSS class for markers
     */
    protected static final String CSS_STEEP_DOWN = "opticsSteepDown";

    /**
     * Our clustering
     */
    OPTICSXi.SteepAreaResult areas;

    /**
     * Constructor.
     *
     * @param context Visualizer context
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
      super(context, task, plot, width, height, proj);
      ClusterOrder co = this.optics.getResult();
      this.areas = co.getHierarchy().iterChildren(co).filter(OPTICSXi.SteepAreaResult.class).get();
    }

    @Override
    public void fullRedraw() {
      makeLayerElement();
      addCSSClasses();

      final OPTICSPlot opticsplot = optics.getOPTICSPlot(context);
      final ClusterOrder co = getClusterOrder();
      final int oheight = opticsplot.getHeight();
      final double xscale = plotwidth / (double) co.size();
      final double yscale = plotheight / (double) oheight;

      DBIDArrayIter tmp = co.iter();

      for(OPTICSXi.SteepArea area : areas) {
        boolean down = area instanceof OPTICSXi.SteepDownArea;
        final int st = area.getStartIndex();
        final int en = area.getEndIndex() + 1;
        final double x1 = st + (down ? 1. : .5);
        final double x2 = en + (down ? .5 : 0.);
        final double y1 = opticsplot.scaleToPixel(co.getReachability(tmp.seek(st)));
        final double y2 = opticsplot.scaleToPixel(en < co.size() ? co.getReachability(tmp.seek(en)) : Double.POSITIVE_INFINITY);
        Element e = svgp.svgLine(x1 * xscale, y1 * yscale, x2 * xscale, y2 * yscale);
        SVGUtil.addCSSClass(e, down ? CSS_STEEP_DOWN : CSS_STEEP_UP);
        layer.appendChild(e);
      }
    }

    /**
     * Adds the required CSS-Classes
     */
    private void addCSSClasses() {
      // Class for the markers
      final StyleLibrary style = context.getStyleLibrary();
      if(!svgp.getCSSClassManager().contains(CSS_STEEP_DOWN)) {
        final CSSClass cls = new CSSClass(this, CSS_STEEP_DOWN);
        Color color = SVGUtil.stringToColor(style.getColor(StyleLibrary.PLOT));
        if(color == null) {
          color = Color.BLACK;
        }
        color = new Color((int) (color.getRed() * 0.6), (int) (color.getGreen() * 0.6 + 0.4 * 256.), (int) (color.getBlue() * 0.6));
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGUtil.colorToString(color));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) * .5);
        svgp.addCSSClassOrLogError(cls);
      }
      if(!svgp.getCSSClassManager().contains(CSS_STEEP_UP)) {
        final CSSClass cls = new CSSClass(this, CSS_STEEP_UP);
        Color color = SVGUtil.stringToColor(style.getColor(StyleLibrary.PLOT));
        if(color == null) {
          color = Color.BLACK;
        }
        color = new Color((int) (color.getRed() * 0.6 + 0.4 * 256.), (int) (color.getGreen() * 0.6), (int) (color.getBlue() * 0.6));
        cls.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGUtil.colorToString(color));
        cls.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.PLOT) * .5);
        svgp.addCSSClassOrLogError(cls);
      }
    }
  }
}
