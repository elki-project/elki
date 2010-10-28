package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.CurveVisualizer;

/**
 * Adapter that will look for visualizable 2D curves and create visualizations for them.
 * 
 * @author Erich Schubert
 */
public class CurveAdapter implements AlgorithmAdapter<DatabaseObject> {
  /**
   * Constructor.
   */
  public CurveAdapter() {
    super();
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(new CurveVisualizer());
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, AnyResult result) {
    Collection<IterableResult<DoubleDoublePair>> curves = CurveVisualizer.findCurveResult(result);
    for (IterableResult<DoubleDoublePair> curve : curves) {
      CurveVisualizer curveVis = new CurveVisualizer();
      curveVis.init(context, curve);
      context.addVisualization(curve, curveVis);
    }
  }
}