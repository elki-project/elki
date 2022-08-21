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

import elki.visualization.VisualizationTask;
import elki.visualization.VisualizerContext;
import elki.visualization.gui.VisualizationPlot;
import elki.visualization.projections.Projection;
import elki.visualization.projections.SilhouetteProjection;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGUtil;
import elki.visualization.visualizers.AbstractVisualization;

/**
 * Abstract base class for silhouette visualizer
 *
 * @author Robert Gehde
 * @since 0.8.0
 * 
 * @assoc - - - SilhouetteProjection
 */
public abstract class AbstractSilhouetteVisualization extends AbstractVisualization {
  /**
   * The plot
   */
  protected final SilhouetteProjection silhouette;

  /**
   * Width of plot (in display units)
   */
  protected double plotwidth;

  /**
   * Height of plot (in display units)
   */
  protected double plotheight;

  /**
   * Constructor.
   *
   * @param context Visualizer context
   * @param task Visualization task.
   * @param plot Plot to draw to
   * @param width Embedding width
   * @param height Embedding height
   * @param proj Projection
   */
  public AbstractSilhouetteVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    super(context, task, plot, width, height);
    this.silhouette = (SilhouetteProjection) proj;
  }

  /**
   * Produce a new layer element.
   */
  protected void makeLayerElement() {
    plotwidth = StyleLibrary.SCALE;
    plotheight = StyleLibrary.SCALE / silhouette.getSilhouettePlot(context).getRatio();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(getWidth(), getHeight(), plotwidth, plotheight, margin * .5, margin * .5, margin * 1.5, margin * .5);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
  }
}
