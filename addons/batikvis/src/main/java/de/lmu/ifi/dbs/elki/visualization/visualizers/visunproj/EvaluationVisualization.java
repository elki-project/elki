package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

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

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGScoreBar;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualizationInstance;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Pseudo-Visualizer, that lists the cluster evaluation results found.
 * 
 * TODO: add indicator whether high values are better or low.
 * 
 * TODO: add indication/warning when values are out-of-bounds.
 * 
 * @author Erich Schubert
 * @author Sascha Goldhofer
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualizationInstance oneway - - «create»
 * @apiviz.has 
 *             de.lmu.ifi.dbs.elki.evaluation.clustering.EvaluateClustering.ScoreResult
 *             oneway - - visualizes
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
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    final ArrayList<EvaluationResult> srs = ResultUtil.filterResults(hier, newResult, EvaluationResult.class);
    for(EvaluationResult sr : srs) {
      final VisualizationTask task = new VisualizationTask(NAME, sr, null, this);
      task.width = .5;
      task.height = sr.numLines() * .05;
      task.level = VisualizationTask.LEVEL_STATIC;
      hier.add(sr, task);
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
  public Visualization makeVisualization(VisualizationTask task) {
    // TODO: make a utility class to wrap SVGPlot + parent layer + ypos.
    // TODO: use CSSClass and StyleLibrary

    double ypos = -.5; // Skip space before first header
    SVGPlot svgp = task.getPlot();
    Element parent = svgp.svgElement(SVGConstants.SVG_G_TAG);
    EvaluationResult sr = task.getResult();

    for(String header : sr.getHeaderLines()) {
      ypos = addHeader(svgp, parent, ypos, header);
    }

    for(EvaluationResult.MeasurementGroup g : sr) {
      ypos = addHeader(svgp, parent, ypos, g.getName());
      for(EvaluationResult.Measurement m : g) {
        ypos = addBarChart(svgp, parent, ypos, m.getName(), m.getVal(), m.getMin(), m.getMax(), m.getExp(), m.lowerIsBetter());
      }
    }

    // scale vis
    double cols = 10;
    final StyleLibrary style = task.getContext().getStyleResult().getStyleLibrary();
    final double margin = style.getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), cols, ypos, margin / StyleLibrary.SCALE);
    SVGUtil.setAtt(parent, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    return new StaticVisualizationInstance(task, parent);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }
}
