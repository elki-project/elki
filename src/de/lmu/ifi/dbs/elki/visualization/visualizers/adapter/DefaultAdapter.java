package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerTree;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.SettingsVisualizer;

/**
 * Class to add various default visualizations.
 * 
 * @author Erich Schubert
 */
public class DefaultAdapter implements AlgorithmAdapter<DatabaseObject> {
  /**
   * Visualizer to show the settings.
   */
  private SettingsVisualizer settingsVisualizer;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public DefaultAdapter(Parameterization config) {
    super();
    config = config.descend(this);
    settingsVisualizer = new SettingsVisualizer(null);
  }

  @Override
  public boolean canVisualize(@SuppressWarnings("unused") VisualizerContext<? extends DatabaseObject> context) {
    return true;
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(1);
    providedVisualizers.add(settingsVisualizer);
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends DatabaseObject> context, VisualizerTree<? extends DatabaseObject> vistree) {
    for(SettingsResult sr : ResultUtil.getSettingsResults(context.getResult())) {
      SettingsVisualizer v = new SettingsVisualizer(sr);
      v.init(context);
      vistree.addVisualization(sr, v);
    }
  }
}