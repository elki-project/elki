package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.result.IterableResult;
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
   * Constructor.
   */
  public CurveAdapter() {
    super();
  }

  @Override
  public boolean canVisualize(VisualizerContext context) {
    Collection<IterableResult<Pair<Double, Double>>> curves = CurveVisualizer.findCurveResult(context.getResult());
    return (curves.size() > 0);
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(curveVisualizer);
    return providedVisualizers;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    Collection<IterableResult<Pair<Double, Double>>> curves = CurveVisualizer.findCurveResult(context.getResult());
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(curves.size());
    for (IterableResult<Pair<Double, Double>> curve : curves) {
      CurveVisualizer curveVis = new CurveVisualizer();
      curveVis.init(context, curve);
      usableVisualizers.add(curveVis);
    }
    return usableVisualizers;
  }
}