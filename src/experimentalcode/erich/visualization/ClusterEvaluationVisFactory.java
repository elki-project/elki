package experimentalcode.erich.visualization;

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.EvaluatePairCountingFMeasure;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.StaticVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.AbstractUnprojectedVisFactory;

/**
 * Pseudo-Visualizer, that lists the cluster evaluation results found.
 * 
 * @author Erich Schubert
 */
public class ClusterEvaluationVisFactory extends AbstractUnprojectedVisFactory<DatabaseObject> {
  /**
   * Name for this visualizer.
   */
  private static final String NAME = "Cluster Evaluation";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public ClusterEvaluationVisFactory() {
    super(NAME);
  }
  
  @Override
  public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, AnyResult result) {
    final ArrayList<EvaluatePairCountingFMeasure.ScoreResult> srs = ResultUtil.filterResults(result, EvaluatePairCountingFMeasure.ScoreResult.class);
    for(EvaluatePairCountingFMeasure.ScoreResult sr : srs) {
      context.addVisualizer(sr, this);
    }
  }

  @Override
  public Visualization makeVisualization(VisualizationTask task) {
    SVGPlot svgp = task.getPlot();
    Element layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    EvaluatePairCountingFMeasure.ScoreResult sr = task.getResult();
    
    // FIXME: use CSSClass and StyleLibrary

    int i = 0;
    {
      Element object = svgp.svgText(0, i + 0.7, "F-Measure of cluster pair counts:");
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6; font-weight: bold");
      layer.appendChild(object);
      i++;
    
    }
    
    for(Vector vec : sr) {
      Element object = svgp.svgText(0, i + 0.7, FormatUtil.format(vec.get(0), FormatUtil.NF8));
      object.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.6");
      layer.appendChild(object);
      i++;
    }

    int cols = Math.max(30, (int) (i * task.getHeight() / task.getWidth()));
    int rows = i;
    final double margin = task.getContext().getStyleLibrary().getSize(StyleLibrary.MARGIN);
    final String transform = SVGUtil.makeMarginTransform(task.getWidth(), task.getHeight(), cols, rows, margin / StyleLibrary.SCALE);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);

    return new StaticVisualization(task, layer, getLevel());
  }
}