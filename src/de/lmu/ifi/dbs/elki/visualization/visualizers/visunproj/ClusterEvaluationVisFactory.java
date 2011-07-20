package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.evaluation.paircounting.EvaluatePairCountingFMeasure;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;

/**
 * Pseudo-Visualizer, that lists the cluster evaluation results found.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses StaticVisualization oneway - - «create»
 * @apiviz.has de.lmu.ifi.dbs.elki.evaluation.paircounting.EvaluatePairCountingFMeasure.ScoreResult oneway - - visualizes
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
    final ArrayList<EvaluatePairCountingFMeasure.ScoreResult> srs = ResultUtil.filterResults(newResult, EvaluatePairCountingFMeasure.ScoreResult.class);
    for(EvaluatePairCountingFMeasure.ScoreResult sr : srs) {
      final VisualizationTask task = new VisualizationTask(NAME, sr, null, this, null);
      task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_STATIC);
      baseResult.getHierarchy().add(sr, task);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    SVGPlot svgp = task.getPlot();
    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    EvaluatePairCountingFMeasure.ScoreResult sr = task.getResult();
    
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
    for(Vector vec : sr) {
      StringBuffer buf = new StringBuffer();
      double fmeasure = vec.get(0);
      double inboth = vec.get(1);
      double infirst = vec.get(2);
      double insecond = vec.get(3);
      buf.append(FormatUtil.format(fmeasure, FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(inboth / (inboth + infirst), FormatUtil.NF6));
      buf.append(" / ");
      buf.append(FormatUtil.format(inboth / (inboth + insecond), FormatUtil.NF6));
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
  public Class<? extends Projection> getProjectionType() {
    return null;
  }
}