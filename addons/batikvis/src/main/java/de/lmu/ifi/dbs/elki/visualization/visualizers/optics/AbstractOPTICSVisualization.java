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

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.OPTICSProjection;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;

/**
 * Abstract base class for OPTICS visualizer
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - OPTICSProjection
 */
public abstract class AbstractOPTICSVisualization extends AbstractVisualization {
  /**
   * The plot
   */
  final protected OPTICSProjection optics;

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
  public AbstractOPTICSVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    super(context, task, plot, width, height);
    this.optics = (OPTICSProjection) proj;
  }

  /**
   * Produce a new layer element.
   */
  protected void makeLayerElement() {
    plotwidth = StyleLibrary.SCALE;
    plotheight = StyleLibrary.SCALE / optics.getOPTICSPlot(context).getRatio();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(getWidth(), getHeight(), plotwidth, plotheight, margin * .5, margin * .5, margin * 1.5, margin * .5);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
  }

  /**
   * Access the raw cluster order
   *
   * @return Cluster order
   */
  protected ClusterOrder getClusterOrder() {
    return optics.getResult();
  }
}
