/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGScoreBar;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualizationInstance;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Pseudo-Visualizer, that lists the cluster evaluation results found.
 *
 * TODO: add indication/warning when values are out-of-bounds.
 *
 * TODO: Find a nicer solution than the current hack to only display the
 * evaluation results for the currently active clustering.
 *
 * @author Erich Schubert
 * @author Sascha Goldhofer
 * @since 0.4.0
 *
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualizationInstance oneway - - «create»
 * @apiviz.has EvaluationResult oneway - - visualizes
 */
public class EvaluationVisualization extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Evaluation Bar Chart";

  /**
   * Constant: width of score bars
   */
  private static final double BARLENGTH = 5;

  /**
   * Constant: height of score bars
   */
  private static final double BARHEIGHT = 0.7;

  /**
   * Constructor.
   */
  public EvaluationVisualization() {
    super();
  }

  @Override
  public void processNewResult(VisualizerContext context, Object start) {
    Hierarchy.Iter<EvaluationResult> it = VisualizationTree.filterResults(context, start, EvaluationResult.class);
    candidate: for(; it.valid(); it.advance()) {
      EvaluationResult sr = it.get();
      // Avoid duplicates:
      Hierarchy.Iter<VisualizationTask> it2 = VisualizationTree.filter(context, sr, VisualizationTask.class);
      for(; it2.valid(); it2.advance()) {
        if(it2.get().getFactory() instanceof EvaluationVisualization) {
          continue candidate;
        }
      }
      // Hack: for clusterings, only show the currently visible clustering.
      if(sr.visualizeSingleton()) {
        Class<? extends EvaluationResult> c = sr.getClass();
        // Ensure singleton.
        Hierarchy.Iter<?> it3 = context.getVisHierarchy().iterChildren(context.getBaseResult());
        for(; it3.valid(); it3.advance()) {
          Object o = it3.get();
          if(!(o instanceof VisualizationTask)) {
            continue;
          }
          final VisualizationTask otask = (VisualizationTask) o;
          if(otask.getFactory() instanceof EvaluationVisualization && otask.getResult() == c) {
            continue candidate;
          }
        }
        final VisualizationTask task = new VisualizationTask(NAME, context, c, null, EvaluationVisualization.this);
        task.reqwidth = .5;
        task.reqheight = sr.numLines() * .05;
        task.level = VisualizationTask.LEVEL_STATIC;
        task.addUpdateFlags(VisualizationTask.ON_STYLEPOLICY);
        context.addVis(context.getBaseResult(), task);
        continue candidate;
      }
      final VisualizationTask task = new VisualizationTask(NAME, context, sr, null, EvaluationVisualization.this);
      task.reqwidth = .5;
      task.reqheight = sr.numLines() * .05;
      task.level = VisualizationTask.LEVEL_STATIC;
      context.addVis(sr, task);
    }
  }

  private double addBarChart(SVGPlot svgp, Element parent, double ypos, String label, double value, double minValue, double maxValue, double baseValue, boolean reversed) {
    SVGScoreBar barchart = new SVGScoreBar();
    barchart.setFill(value, baseValue == baseValue ? baseValue : minValue, maxValue);
    barchart.setReversed(reversed);
    barchart.showValues(FormatUtil.NF4);
    barchart.addLabel(label);
    parent.appendChild(barchart.build(svgp, 0.0, ypos, BARLENGTH, BARHEIGHT));
    ypos += 1;
    return ypos;
  }

  private double addHeader(SVGPlot svgp, Element parent, double ypos, String text) {
    ypos += .5;
    Element object = svgp.svgText(0, ypos + BARHEIGHT * 0.5, text);
    object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
    parent.appendChild(object);
    ypos += 1;
    return ypos;
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj) {
    // TODO: make a utility class to wrap SVGPlot + parent layer + ypos.
    // TODO: use CSSClass and StyleLibrary

    double ypos = -.5; // Skip space before first header
    Element parent = plot.svgElement(SVGConstants.SVG_G_TAG);
    Object o = task.getResult();
    EvaluationResult sr = null;
    if(o instanceof EvaluationResult) {
      sr = (EvaluationResult) o;
    }
    else if(o instanceof Class) {
      // Use cluster evaluation of current style instead.
      VisualizerContext context = task.getContext();
      StylingPolicy spol = context.getStylingPolicy();
      if(spol instanceof ClusterStylingPolicy) {
        ClusterStylingPolicy cpol = (ClusterStylingPolicy) spol;
        @SuppressWarnings("unchecked")
        final Class<Object> c = (Class<Object>) o;
        Iter<?> it = VisualizationTree.filterResults(context, cpol.getClustering(), c);
        candidates: for(; it.valid(); it.advance()) {
          // This could be attached to a child clustering, in which case we
          // may end up displaying the wrong evaluation.
          Iter<Result> it2 = context.getHierarchy().iterAncestors((EvaluationResult) it.get());
          for(; it2.valid(); it2.advance()) {
            if(it2.get() instanceof Clustering && it2.get() != cpol.getClustering()) {
              continue candidates;
            }
          }
          sr = (EvaluationResult) it.get();
          break;
        }
      }
    }
    if(sr == null) {
      return new StaticVisualizationInstance(task, plot, width, height, parent); // Failed.
    }

    for(String header : sr.getHeaderLines()) {
      ypos = addHeader(plot, parent, ypos, header);
    }

    for(EvaluationResult.MeasurementGroup g : sr) {
      ypos = addHeader(plot, parent, ypos, g.getName());
      for(EvaluationResult.Measurement m : g) {
        ypos = addBarChart(plot, parent, ypos, m.getName(), m.getVal(), m.getMin(), m.getMax(), m.getExp(), m.lowerIsBetter());
      }
    }

    // scale vis
    double cols = 10;
    final StyleLibrary style = task.getContext().getStyleLibrary();
    final double margin = style.getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(width, height, cols, ypos, margin / StyleLibrary.SCALE);
    SVGUtil.setAtt(parent, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    return new StaticVisualizationInstance(task, plot, width, height, parent);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }
}
