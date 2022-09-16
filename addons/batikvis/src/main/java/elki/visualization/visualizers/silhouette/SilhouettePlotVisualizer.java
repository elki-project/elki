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
package elki.visualization.visualizers.silhouette;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.logging.LoggingUtil;
import elki.math.scales.LinearScale;
import elki.visualization.VisualizationTask;
import elki.visualization.VisualizationTree;
import elki.visualization.VisualizerContext;
import elki.visualization.css.CSSClassManager.CSSNamingConflict;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.projector.SilhouettePlotProjector;
import elki.visualization.silhouette.SilhouettePlot;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGSimpleLinearAxis;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.VisFactory;
import elki.visualization.visualizers.Visualization;

/**
 * Visualize a Silhouette result by constructing a Silhouette plot for it.
 *
 * @author Robert Gehde
 * @since 0.8.0
 *
 * @stereotype factory
 * @navassoc - create - Instance
 */
public class SilhouettePlotVisualizer implements VisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Silhouette Plot";

  /**
   * Constructor.
   */
  public SilhouettePlotVisualizer() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object result) {
    VisualizationTree.findVis(context, result).filter(SilhouettePlotProjector.class).forEach(p -> {
      context.addVis(p, new VisualizationTask(this, NAME, p.getResult(), null) //
          // FIXME: .with(UpdateFlag.ON_STYLEPOLICY);
          .level(VisualizationTask.LEVEL_DATA));
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
   * Instance.
   *
   * @author Robert Gehde
   */
  public static class Instance extends AbstractSilhouetteVisualization {
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
      addListeners();
    }

    @Override
    public void fullRedraw() {
      makeLayerElement();
      // addCSSClasses();

      SilhouettePlot silhouettesplot = silhouette.getSilhouettePlot(context);
      String ploturi = silhouettesplot.getSVGPlotURI();

      Element itag = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
      SVGUtil.setAtt(itag, SVGConstants.SVG_PRESERVE_ASPECT_RATIO_ATTRIBUTE, SVGConstants.SVG_NONE_VALUE);
      SVGUtil.setAtt(itag, SVGConstants.SVG_IMAGE_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_SPEED_VALUE);
      SVGUtil.setAtt(itag, SVGConstants.SVG_X_ATTRIBUTE, 0);
      SVGUtil.setAtt(itag, SVGConstants.SVG_Y_ATTRIBUTE, 0);
      SVGUtil.setAtt(itag, SVGConstants.SVG_WIDTH_ATTRIBUTE, plotwidth);
      SVGUtil.setAtt(itag, SVGConstants.SVG_HEIGHT_ATTRIBUTE, plotheight);
      itag.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, ploturi);

      layer.appendChild(itag);

      LinearScale scale = silhouettesplot.getScale();
      double y1 = plotheight * silhouettesplot.scaleToPixel(scale.getMin()) / silhouettesplot.getHeight();
      double y2 = plotheight * silhouettesplot.scaleToPixel(scale.getMax()) / silhouettesplot.getHeight();
      double y3 = plotheight * silhouettesplot.scaleToPixel(0) / (silhouettesplot.getHeight()-1);
      // -1 because in testing the line was a bit off in the thumbnail.
      try {
        final StyleLibrary style = context.getStyleLibrary();
        SVGSimpleLinearAxis.drawAxis(svgp, layer, scale, 0, y1, 0, y2, SVGSimpleLinearAxis.LabelStyle.LEFTHAND, style);
        SVGSimpleLinearAxis.drawAxis(svgp, layer, scale, plotwidth, y1, plotwidth, y2, SVGSimpleLinearAxis.LabelStyle.RIGHTHAND, style);
        SVGSimpleLinearAxis.drawAxis(svgp, layer, scale, 0, y3, plotwidth, y3, SVGSimpleLinearAxis.LabelStyle.NOTHING, style);
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception("CSS naming conflict for axes on Silhouette plot", e);
      }
    }
  }
}
