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
import java.util.List;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.Clustering;
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
import de.lmu.ifi.dbs.elki.visualization.svg.SVGScoreBar;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Pseudo-Visualizer, that lists the cluster evaluation results found.
 * 
 * @author Erich Schubert
 * @author Sascha Goldhofer
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualization oneway - - «create»
 * @apiviz.has de.lmu.ifi.dbs.elki.evaluation.clustering.EvaluateClustering.ScoreResult oneway - - visualizes
 */
public class ClusterEvaluationVisFactory extends AbstractVisFactory {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Cluster Evaluation";

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
  public ClusterEvaluationVisFactory() {
    super();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    final ArrayList<EvaluateClustering.ScoreResult> srs = ResultUtil.filterResults(newResult, EvaluateClustering.ScoreResult.class);
    for(EvaluateClustering.ScoreResult sr : srs) {
      final VisualizationTask task = new VisualizationTask(NAME, sr, null, this);
      task.width = .5;
      task.height = 2.0;
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
      baseResult.getHierarchy().add(sr, task);
    }
  }

  private double addBarChart(SVGPlot svgp, Element parent, double ypos, String label, double maxValue, double value) {
    SVGScoreBar barchart = new SVGScoreBar();
    barchart.setFill(value, maxValue);
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
    
    double ypos = -.5; // Skip space before first header
    SVGPlot svgp = task.getPlot();
    Element parent = svgp.svgElement(SVGConstants.SVG_G_TAG);
    EvaluateClustering.ScoreResult sr = task.getResult();
    ClusterContingencyTable cont = sr.getContingencyTable();

    List<Result> parents = task.getContext().getHierarchy().getParents(sr);

    for(Result r : parents) {
      if(r instanceof Clustering) {
        ypos = addHeader(svgp, parent, ypos, r.getLongName());
      }
    }
    // TODO: use CSSClass and StyleLibrary

    ypos = addHeader(svgp, parent, ypos, "Pair counting measures");

    PairCounting paircount = cont.getPaircount();
    ypos = addBarChart(svgp, parent, ypos, "Jaccard", 1, paircount.jaccard());
    ypos = addBarChart(svgp, parent, ypos, "F1-Measure", 1, paircount.f1Measure());
    ypos = addBarChart(svgp, parent, ypos, "Precision", 1, paircount.precision());
    ypos = addBarChart(svgp, parent, ypos, "Recall", 1, paircount.recall());
    ypos = addBarChart(svgp, parent, ypos, "Rand", 1, paircount.randIndex());
    ypos = addBarChart(svgp, parent, ypos, "ARI", 1, paircount.adjustedRandIndex());
    ypos = addBarChart(svgp, parent, ypos, "FowlkesMallows", 1, paircount.fowlkesMallows());

    ypos = addHeader(svgp, parent, ypos, "Entropy based measures");

    Entropy entropy = cont.getEntropy();
    ypos = addBarChart(svgp, parent, ypos, "NMI Joint", 1, entropy.entropyNMIJoint());
    ypos = addBarChart(svgp, parent, ypos, "NMI Sqrt", 1, entropy.entropyNMISqrt());

    ypos = addHeader(svgp, parent, ypos, "BCubed-based measures");

    BCubed bcubed = cont.getBCubed();
    ypos = addBarChart(svgp, parent, ypos, "F1-Measure", 1, bcubed.f1Measure());
    ypos = addBarChart(svgp, parent, ypos, "Recall", 1, bcubed.recall());
    ypos = addBarChart(svgp, parent, ypos, "Precision", 1, bcubed.precision());

    ypos = addHeader(svgp, parent, ypos, "Set-Matching-based measures");

    SetMatchingPurity setm = cont.getSetMatching();
    ypos = addBarChart(svgp, parent, ypos, "F1-Measure", 1, setm.f1Measure());
    ypos = addBarChart(svgp, parent, ypos, "Purity", 1, setm.purity());
    ypos = addBarChart(svgp, parent, ypos, "Inverse Purity", 1, setm.inversePurity());

    ypos = addHeader(svgp, parent, ypos, "Editing-distance measures");

    EditDistance edit = cont.getEdit();
    ypos = addBarChart(svgp, parent, ypos, "F1-Measure", 1, edit.f1Measure());
    ypos = addBarChart(svgp, parent, ypos, "Precision", 1, edit.editDistanceFirst());
    ypos = addBarChart(svgp, parent, ypos, "Recall", 1, edit.editDistanceSecond());

    ypos = addHeader(svgp, parent, ypos, "Gini measures");

    final MeanVariance gini = cont.averageSymmetricGini();
    ypos = addBarChart(svgp, parent, ypos, "Mean +-" + FormatUtil.format(gini.getSampleStddev(), FormatUtil.NF4), 1, gini.getMean());

    // scale vis
    double cols = 10; // Math.max(10, (int) (i * task.getHeight() /
    // task.getWidth()));
    double rows = ypos;
    final double margin = task.getContext().getStyleLibrary().getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), cols, rows, margin / StyleLibrary.SCALE);
    SVGUtil.setAtt(parent, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    return new StaticVisualization(task, parent);
  }

  @Override
  public boolean allowThumbnails(VisualizationTask task) {
    // Don't use thumbnails
    return false;
  }
}