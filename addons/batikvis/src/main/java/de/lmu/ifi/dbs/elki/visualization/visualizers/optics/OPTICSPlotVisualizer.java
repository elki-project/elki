package de.lmu.ifi.dbs.elki.visualization.visualizers.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Visualize an OPTICS result by constructing an OPTICS plot for it.
 *
 * @author Erich Schubert
 *
 * @apiviz.stereotype factory
 * @apiviz.uses Instance oneway - - «create»
 */
public class OPTICSPlotVisualizer extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "OPTICS Plot";

  /**
   * Constructor.
   */
  public OPTICSPlotVisualizer() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    Hierarchy.Iter<OPTICSProjector> it = VisualizationTree.filter(context, result, OPTICSProjector.class);
    for(; it.valid(); it.advance()) {
      OPTICSProjector p = it.get();
      // Add plots, attach visualizer
      final VisualizationTask task = new VisualizationTask(NAME, context, p.getResult(), null, this);
      task.level = VisualizationTask.LEVEL_DATA;
      // FIXME: task.setUpdates(VisualizationTask.ON_STYLEPOLICY);
      context.addVis(p, task);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
    return new Instance(task, plot, width, height, proj);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }

  /**
   * Instance.
   *
   * @author Erich Schubert
   */
  public class Instance extends AbstractOPTICSVisualization {
    /**
     * Constructor.
     *
     * @param task Visualization task
     * @param plot Plot to draw to
     * @param width Embedding width
     * @param height Embedding height
     * @param proj Projection
     */
    public Instance(VisualizationTask task, SVGPlot plot, double width, double height, Projection proj) {
      super(task, plot, width, height, proj);
      addListeners();
    }

    @Override
    protected void redraw() {
      makeLayerElement();
      // addCSSClasses();

      OPTICSPlot opticsplot = optics.getOPTICSPlot(context);
      String ploturi = opticsplot.getSVGPlotURI();

      Element itag = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
      SVGUtil.setAtt(itag, SVGConstants.SVG_PRESERVE_ASPECT_RATIO_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      SVGUtil.setAtt(itag, SVGConstants.SVG_IMAGE_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_SPEED_VALUE);
      SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, 0);
      SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, 0);
      SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, plotwidth);
      SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, plotheight);
      itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, ploturi);

      layer.appendChild(itag);

      LinearScale scale = opticsplot.getScale();
      double y1 = plotheight * opticsplot.scaleToPixel(scale.getMin()) / opticsplot.getHeight();
      double y2 = plotheight * opticsplot.scaleToPixel(scale.getMax()) / opticsplot.getHeight();
      try {
        final StyleLibrary style = context.getStyleLibrary();
        SVGSimpleLinearAxis.drawAxis(svgp, layer, scale, 0, y1, 0, y2, SVGSimpleLinearAxis.LabelStyle.LEFTHAND, style);
        SVGSimpleLinearAxis.drawAxis(svgp, layer, scale, plotwidth, y1, plotwidth, y2, SVGSimpleLinearAxis.LabelStyle.RIGHTHAND, style);
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception("CSS naming conflict for axes on OPTICS plot", e);
      }
    }
  }
}