package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.SettingsVisualizer;

/**
 * Class to add various default visualizations.
 * 
 * @author Erich Schubert
 */
public class DefaultAdapter implements AlgorithmAdapter<DatabaseObject> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public DefaultAdapter() {
    super();
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(new SettingsVisualizer(null));
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, AnyResult result) {
    for(SettingsResult sr : ResultUtil.getSettingsResults(result)) {
      SettingsVisualizer v = new SettingsVisualizer(sr);
      v.init(context);
      context.addVisualization(sr, v);
    }
  }
}