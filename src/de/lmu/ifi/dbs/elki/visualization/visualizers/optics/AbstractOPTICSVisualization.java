package de.lmu.ifi.dbs.elki.visualization.visualizers.optics;

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

import java.util.List;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projector.OPTICSProjector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;

/**
 * Abstract base class for OPTICS visualizer
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses OPTICSProjector
 * 
 * @param <D>
 */
public abstract class AbstractOPTICSVisualization<D extends Distance<D>> extends AbstractVisualization {
  /**
   * The plot
   */
  final protected OPTICSProjector<D> optics;

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
   * @param task Visualization task.
   */
  public AbstractOPTICSVisualization(VisualizationTask task) {
    super(task);
    this.optics = task.getResult();
  }

  /**
   * Produce a new layer element.
   */
  protected void makeLayerElement() {
    plotwidth = StyleLibrary.SCALE;
    plotheight = StyleLibrary.SCALE / optics.getOPTICSPlot(context).getRatio();
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), plotwidth, plotheight, margin / 2);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
  }

  /**
   * Access the raw cluster order
   * 
   * @return Cluster order
   */
  protected List<ClusterOrderEntry<D>> getClusterOrder() {
    return optics.getResult().getClusterOrder();
  }
}