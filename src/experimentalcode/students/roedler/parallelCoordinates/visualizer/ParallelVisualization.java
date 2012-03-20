package experimentalcode.students.roedler.parallelCoordinates.visualizer;

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
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import experimentalcode.students.roedler.parallelCoordinates.projections.ProjectionParallel;

/**
 * Default class to handle parallel visualizations.
 * 
 * @author Robert Rödler
 * 
 */
public abstract class ParallelVisualization<NV extends NumberVector<?, ?>> extends AbstractVisualization {

  /**
   * The current projection
   */
  final protected ProjectionParallel proj;

  /**
   * The representation we visualize
   */
  final protected Relation<NV> rep;

  /**
   * Margin
   */
  protected double margin;
  
  /**
   * margin
   */
  final double[] margins;

  /**
   * space between two axis
   */
  double dist;

  /**
   * viewbox size
   */
  final double[] size;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public ParallelVisualization(VisualizationTask task) {
    super(task);
    this.proj = task.getProj();
    this.rep = task.getRelation();
    this.margin = 0; //context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    
    margins = new double[] { 0.071, 0.071 };
    size = new double[] { 1.8, 1. };
    dist = (size[0] - 2 * margins[0]) / (proj.getDimensions() - 1.);
    
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
  public Element setupCanvas(SVGPlot svgp, ProjectionParallel proj, double margin, double width, double height) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(width, height, getSizeX(), getSizeY(), 0.);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    return layer;
  }
  

  public double getSizeX() {
    return size[0];
  }

  public double getSizeY() {
    return size[1];
  }

  public double getDist() {
    return dist;
  }

  public double getMarginX() {
    return margins[0];
  }

  public double getMarginY() {
    return margins[1];
  }

  public void calcAxisPositions() {
    dist = (size[0] - 2 * margins[0]) / -(proj.getVisibleDimensions() - 1.);
  }
  
  public double getAxisHeight() {
    return 0.71;
  }
}