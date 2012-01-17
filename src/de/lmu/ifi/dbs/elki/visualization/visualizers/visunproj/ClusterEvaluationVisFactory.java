package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

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

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.evaluation.clustering.BCubed;
import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.evaluation.clustering.EditDistance;
import de.lmu.ifi.dbs.elki.evaluation.clustering.Entropy;
import de.lmu.ifi.dbs.elki.evaluation.clustering.EvaluateClustering;
import de.lmu.ifi.dbs.elki.evaluation.clustering.PairCounting;
import de.lmu.ifi.dbs.elki.evaluation.clustering.SetMatchingPurity;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
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
 * @apiviz.has EvaluateClustering.ScoreResult oneway - - visualizes
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
    final ArrayList<EvaluateClustering.ScoreResult> srs = ResultUtil.filterResults(newResult, EvaluateClustering.ScoreResult.class);
    for(EvaluateClustering.ScoreResult sr : srs) {
      final VisualizationTask task = new VisualizationTask(NAME, sr, null, this);
      task.width = 1.0;
      task.height = 2.0;
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
      baseResult.getHierarchy().add(sr, task);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    SVGPlot svgp = task.getPlot();
    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    EvaluateClustering.ScoreResult sr = task.getResult();
    ClusterContingencyTable cont = sr.getContingencyTable();

    // TODO: use CSSClass and StyleLibrary

    int i = 0;
    // Pair-counting measures:
    {
      Element object = svgp.svgText(0, i + 0.7, "Pair-counting measures:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      Element object = svgp.svgText(0.3, i + 0.7, "F1-Measure, Precision and Recall:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    PairCounting paircount = cont.getPaircount();
    {
      StringBuffer buf = new StringBuffer();
      buf.append(FormatUtil.format(paircount.f1Measure(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(paircount.precision(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(paircount.recall(), FormatUtil.NF6));
      Element object = svgp.svgText(0.3, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }
    {
      Element object = svgp.svgText(0.3, i + 0.7, "Rand, Adjusted Rand and Jaccard:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      StringBuffer buf = new StringBuffer();
      buf.append(FormatUtil.format(paircount.randIndex(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(paircount.adjustedRandIndex(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(paircount.jaccard(), FormatUtil.NF6));
      Element object = svgp.svgText(0.3, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }
    {
      Element object = svgp.svgText(0.3, i + 0.7, "Fowlkes-Mallows, Mirkin-Index:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      StringBuffer buf = new StringBuffer();
      buf.append(FormatUtil.format(paircount.fowlkesMallows(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(paircount.mirkin()));
      Element object = svgp.svgText(0.3, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }
    // Entropy-based measures
    {
      Element object = svgp.svgText(0, i + 0.7, "Entropy measures:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      Element object = svgp.svgText(0.3, i + 0.7, "VI, NormalizedVI:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      Entropy entropy = cont.getEntropy();
      StringBuffer buf = new StringBuffer();
      buf.append(FormatUtil.format(entropy.variationOfInformation(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(entropy.normalizedVariationOfInformation(), FormatUtil.NF6));
      Element object = svgp.svgText(0.3, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }
    // BCubed-based measures
    {
      Element object = svgp.svgText(0, i + 0.7, "BCubed measures:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      Element object = svgp.svgText(0.3, i + 0.7, "F-Measure / Precision / Recall:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      BCubed bcubed = cont.getBCubed();
      StringBuffer buf = new StringBuffer();
      buf.append(FormatUtil.format(bcubed.f1Measure(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(bcubed.precision(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(bcubed.recall(), FormatUtil.NF6));
      Element object = svgp.svgText(0.3, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }
    // Set-Matching-based measures
    {
      Element object = svgp.svgText(0, i + 0.7, "Set-Matching measures:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      Element object = svgp.svgText(0.3, i + 0.7, "F-Measure / Inverse Purity / Purity:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      SetMatchingPurity setm = cont.getSetMatching();
      StringBuffer buf = new StringBuffer();
      buf.append(FormatUtil.format(setm.f1Measure(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(setm.inversePurity(), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(setm.purity(), FormatUtil.NF6));
      Element object = svgp.svgText(0.3, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }
    // Edit-distance measures
    {
      Element object = svgp.svgText(0, i + 0.7, "Edit-distance measures:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      EditDistance edit = cont.getEdit();
      StringBuffer buf = new StringBuffer();
      buf.append("Edit F1: ");
      buf.append(FormatUtil.format(edit.f1Measure(), FormatUtil.NF6));
      Element object = svgp.svgText(0.3, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }
    // Gini measures
    {
      Element object = svgp.svgText(0, i + 0.7, "Gini measures:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    }
    {
      StringBuffer buf = new StringBuffer();
      buf.append("Mean: ");
      final MeanVariance gini = cont.averageSymmetricGini();
      buf.append(FormatUtil.format(gini.getMean(), FormatUtil.NF6));
      buf.append(" +- ");
      buf.append(FormatUtil.format(gini.getSampleStddev(), FormatUtil.NF6));
      Element object = svgp.svgText(0.3, i + 0.7, buf.toString());
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }

    int cols = 10; // Math.max(10, (int) (i * task.getHeight() /
                   // task.getWidth()));
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