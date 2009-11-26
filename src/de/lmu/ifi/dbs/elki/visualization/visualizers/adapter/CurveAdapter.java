package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.CurveVisualizer;

/**
 * Adapter that will look for visualizable 2D curves and create visualizations for them.
 * 
 * @author Erich Schubert
 */
public class CurveAdapter implements AlgorithmAdapter {
  /**
   * Prototype for parameterization
   */
  private CurveVisualizer curveVisualizer = new CurveVisualizer();
  
  /**
   * Constructor, following the {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style.
   */
  public CurveAdapter() {
    super();
  }

  @Override
  public boolean canVisualize(Result result) {
    Collection<IterableResult<Pair<Double, Double>>> curves = CurveVisualizer.findCurveResult(result);
    return (curves.size() > 0);
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    // FIXME: parameter handling is not very nice here.
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(curveVisualizer);
    return providedVisualizers;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    Collection<IterableResult<Pair<Double, Double>>> curves = CurveVisualizer.findCurveResult(context.getResult());
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(curves.size());
    ArrayList<String> params = curveVisualizer.getParameters();
    for (IterableResult<Pair<Double, Double>> curve : curves) {
      CurveVisualizer curveVis = new CurveVisualizer();
      // setup parameters.
      try {
        curveVis.setParameters(params);
      }
      catch(ParameterException e) {
        LoggingUtil.exception("Error setting parameters for curve visualizers.", e);
      }
      curveVis.init(context, curve);
      usableVisualizers.add(curveVis);
    }
    return usableVisualizers;
  }
}
