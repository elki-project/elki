package de.lmu.ifi.dbs.elki.visualization.visualizers.scatterplot;

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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SamplingResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.projections.CanvasSize;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;

/**
 * Default class to handle 2D projected visualizations.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has Projection2D
 */
public abstract class AbstractScatterplotVisualization extends AbstractVisualization {
  /**
   * The current projection
   */
  final protected Projection2D proj;

  /**
   * The representation we visualize
   */
  final protected Relation<? extends NumberVector<?, ?>> rel;

  /**
   * The DBID sample
   */
  final protected SamplingResult sample;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public AbstractScatterplotVisualization(VisualizationTask task) {
    super(task);
    this.proj = task.getProj();
    this.rel = task.getRelation();
    this.sample = ResultUtil.getSamplingResult(rel);
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    this.layer = setupCanvas(svgp, proj, margin, task.getWidth(), task.getHeight());
  }

  /**
   * Utility function to setup a canvas element for the visualization.
   * 
   * @param svgp Plot element
   * @param proj Projection to use
   * @param margin Margin to use
   * @param width Width
   * @param height Height
   * @return wrapper element with appropriate view box.
   */
  public static Element setupCanvas(SVGPlot svgp, Projection2D proj, double margin, double width, double height) {
    final CanvasSize canvas = proj.estimateViewport();
    final double sizex = canvas.getDiffX();
    final double sizey = canvas.getDiffY();
    String transform = SVGUtil.makeMarginTransform(width, height, sizex, sizey, margin) + " translate(" + SVGUtil.fmt(sizex / 2) + " " + SVGUtil.fmt(sizey / 2) + ")";

    final Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    return layer;
  }
  
  @Override
  public void resultChanged(Result current) {
    super.resultChanged(current);
    if(current == proj) {
      synchronizedRedraw();
    }
  }
}