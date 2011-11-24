package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

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

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.evaluation.paircounting.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.EvaluatePairCounting;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Pseudo-Visualizer, that lists the cluster evaluation results found.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualization oneway - - «create»
 * @apiviz.has 
 *             de.lmu.ifi.dbs.elki.evaluation.paircounting.EvaluatePairCountingFMeasure
 *             .ScoreResult oneway - - visualizes
 */
public class ClusterEvaluationVisFactory extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Cluster Evaluation";

  /**
   * Constructor.
   */
  public ClusterEvaluationVisFactory() {
    super();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    final ArrayList<EvaluatePairCounting.ScoreResult> srs = ResultUtil.filterResults(newResult, EvaluatePairCounting.ScoreResult.class);
    for(EvaluatePairCounting.ScoreResult sr : srs) {
      final VisualizationTask task = new VisualizationTask(NAME, sr, null, this);
      task.width = 1.0;
      task.height = 0.5;
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
      baseResult.getHierarchy().add(sr, task);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    SVGPlot svgp = task.getPlot();
    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    EvaluatePairCounting.ScoreResult sr = task.getResult();
    ClusterContingencyTable cont = sr.getContingencyTable();
    
    // TODO: use CSSClass and StyleLibrary
    int i = 0;
    {
      Element object = svgp.svgText(0, i + 0.7, "Same-cluster object pairs");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      Element object = svgp.svgText(0, i + 0.7, "F1-Measure, Precision and Recall:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      StringBuffer buf = new StringBuffer();
      buf.append(FormatUtil.format(cont.pairF1Measure(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(cont.pairPrecision(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(cont.pairRecall(), FormatUtil.NF6));
      Element object = svgp.svgText(0, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }
    {
      Element object = svgp.svgText(0, i + 0.7, "Rand, Adjusted Rand and Jaccard:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      StringBuffer buf = new StringBuffer();
      buf.append(FormatUtil.format(cont.pairRandIndex(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(cont.pairAdjustedRandIndex(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(cont.pairJaccard(), FormatUtil.NF6));
      Element object = svgp.svgText(0, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }

    int cols = Math.max(10, (int) (i * task.getHeight() / task.getWidth()));
    int rows = i;
    final double margin = task.getContext().getStyleLibrary().getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), cols, rows, margin / StyleLibrary.SCALE);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    return new StaticVisualization(task, layer);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }
}