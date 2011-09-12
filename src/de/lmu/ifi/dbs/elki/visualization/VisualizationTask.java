package de.lmu.ifi.dbs.elki.visualization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.AnyMap;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;

/**
 * Container class, with ugly casts to reduce generics crazyness.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf VisFactory
 * @apiviz.has SVGPlot
 * @apiviz.has VisFactory
 * @apiviz.has Projection oneway - 0:1
 * @apiviz.has Visualization oneway
 */
public class VisualizationTask extends AnyMap<String> implements Cloneable, Result, Comparable<VisualizationTask> {
  /**
   * Serial number
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constant for using thumbnail
   */
  public static final String THUMBNAIL = "thumbnail";

  /**
   * Thumbnail size
   */
  public static final String THUMBNAIL_RESOLUTION = "tres";

  /**
   * Meta data key: Level for visualizer ordering
   * 
   * Returns an integer indicating the "temporal position" of this Visualizer.
   * It is intended to impose an ordering on the execution of Visualizers as a
   * Visualizer may depend on another Visualizer running earlier. <br>
   * Lower numbers should result in a earlier use of this Visualizer, while
   * higher numbers should result in a later use. If more Visualizers have the
   * same level, no ordering is guaranteed. <br>
   * Note that this value is only a recommendation, as it is totally up to the
   * framework to ignore it.
   * 
   * Type: Integer
   */
  public final static String META_LEVEL = "level";

  /**
   * Flag to control visibility. Type: Boolean
   */
  public final static String META_VISIBLE = "visible";

  /**
   * Flag to signal there is no thumbnail needed. Type: Boolean
   */
  public final static String META_NOTHUMB = "no-thumbnail";

  /**
   * Mark as not having a (sensible) detail view.
   */
  public static final String META_NODETAIL = "no-detail";

  /**
   * Flag to signal the visualizer should not be exported. Type: Boolean
   */
  public final static String META_NOEXPORT = "no-export";

  /**
   * Flag to signal default visibility of a visualizer. Type: Boolean
   */
  public static final String META_VISIBLE_DEFAULT = "visible-default";

  /**
   * Flag to mark the visualizer as tool. Type: Boolean
   */
  public static final String META_TOOL = "tool";

  /**
   * Background layer
   */
  public final static int LEVEL_BACKGROUND = 0;

  /**
   * Data layer
   */
  public final static int LEVEL_DATA = 100;

  /**
   * Static plot layer
   */
  public final static int LEVEL_STATIC = 200;

  /**
   * Passive foreground layer
   */
  public final static int LEVEL_FOREGROUND = 300;

  /**
   * Active foreground layer (interactive elements)
   */
  public final static int LEVEL_INTERACTIVE = 1000;

  /**
   * Name
   */
  String name;

  /**
   * The active context
   */
  VisualizerContext context;

  /**
   * The factory
   */
  VisFactory factory;

  /**
   * The result we are attached to
   */
  Result result;

  /**
   * The current projection
   */
  Projection proj;

  /**
   * The main representation
   */
  Relation<?> relation;

  /**
   * The plot to draw onto
   */
  SVGPlot svgp;

  /**
   * Width
   */
  public double width;

  /**
   * Height
   */
  public double height;

  /**
   * Visualization task.
   * 
   * @param name Name
   * @param result Result
   * @param relation Relation to use
   * @param factory Factory
   */
  public VisualizationTask(String name, Result result, Relation<?> relation, VisFactory factory) {
    super();
    this.name = name;
    this.result = result;
    this.relation = relation;
    this.factory = factory;
  }

  /**
   * Constructor
   * 
   * @param name Name
   * @param context Context
   * @param result Result
   * @param relation Representation
   * @param factory Factory
   * @param proj Projection
   * @param svgp Plot
   * @param width Width
   * @param height Height
   */
  public VisualizationTask(String name, VisualizerContext context, Result result, Relation<?> relation, VisFactory factory, Projection proj, SVGPlot svgp, double width, double height) {
    super();
    this.name = name;
    this.context = context;
    this.result = result;
    this.factory = factory;
    this.proj = proj;
    this.relation = relation;
    this.svgp = svgp;
    this.width = width;
    this.height = height;
  }

  /**
   * Get the visualizer context.
   * 
   * @return context
   */
  public VisualizerContext getContext() {
    return context;
  }

  /**
   * Get the visualizer factory.
   * 
   * @return Visualizer factory
   */
  public VisFactory getFactory() {
    return factory;
  }

  @SuppressWarnings("unchecked")
  public <R extends Result> R getResult() {
    return (R) result;
  }

  @SuppressWarnings("unchecked")
  public <P extends Projection> P getProj() {
    return (P) proj;
  }

  @SuppressWarnings("unchecked")
  public <R extends Relation<?>> R getRelation() {
    return (R) relation;
  }

  public SVGPlot getPlot() {
    return svgp;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  @Override
  public VisualizationTask clone() {
    VisualizationTask obj = (VisualizationTask) super.clone();
    obj.name = name;
    obj.context = context;
    obj.result = result;
    obj.proj = proj;
    obj.factory = factory;
    obj.svgp = svgp;
    obj.width = width;
    obj.height = height;
    return obj;
  }

  /**
   * Special clone operation that replaces the target plot.
   * 
   * @param newplot Replacement plot to use
   * @param context Visualizer context
   * @return clone with different plot
   */
  public VisualizationTask clone(SVGPlot newplot, VisualizerContext context) {
    VisualizationTask obj = this.clone();
    obj.svgp = newplot;
    obj.context = context;
    return obj;
  }

  /**
   * Special clone operation to set projection and size.
   * 
   * @param plot new plot
   * @param p Projection to use
   * @param width Width
   * @param height Height
   * @return clone with different plot
   */
  public VisualizationTask clone(SVGPlot plot, VisualizerContext context, Projection p, double width, double height) {
    VisualizationTask obj = this.clone();
    obj.svgp = plot;
    obj.context = context;
    obj.proj = p;
    obj.width = width;
    obj.height = height;
    return obj;
  }

  @Override
  public String getLongName() {
    return name;
  }

  @Override
  public String getShortName() {
    return name;
  }

  @Override
  public int compareTo(VisualizationTask other) {
    // sort by levels first
    Integer level1 = this.get(VisualizationTask.META_LEVEL, Integer.class);
    Integer level2 = other.get(VisualizationTask.META_LEVEL, Integer.class);
    if(level1 != null && level2 != null && level1 != level2) {
      return level1 - level2;
    }
    // sort by name otherwise.
    String name1 = this.getShortName();
    String name2 = other.getShortName();
    if(name1 != null && name2 != null && name1 != name2) {
      return name1.compareTo(name2);
    }
    return 0;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("VisTask: ").append(factory.getClass().getName()).append(" ");
    if(result != null) {
      buf.append("Result: ").append(result.getLongName()).append(" ");
    }
    if(proj != null) {
      buf.append("Proj: ").append(proj.toString()).append(" ");
    }
    buf.append(super.toString());
    return buf.toString();
  }

  @Override
  public int hashCode() {
    // We can't have our hashcode change with the map contents!
    return System.identityHashCode(this);
  }

  @Override
  public boolean equals(Object o) {
    // Also don't inherit equals based on list contents!
    return (this == o);
  }
}