package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SettingsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.MergedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerTree;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d.Projection1DHistogramVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.AxisVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.ToolBox2D;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.SettingsVisualizer;

/**
 * Class to add various default visualizations.
 * 
 * @author Erich Schubert
 * 
 * @param <NV> Vector type
 */
public class DefaultAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter<NV> {
  /**
   * Visualizer to show the settings.
   */
  private SettingsVisualizer settingsVisualizer;

  /**
   * Visualizer to draw the axes on 2D projections
   */
  private AxisVisualizer<NV> axisVisualizer;

  /**
   * Visualizer to draw 1D distribution histograms
   */
  private Projection1DHistogramVisualizer<NV> histoVisualizer;

  /**
   * Visualizer for drawing a tool box in the 2D projections
   */
  private ToolBox2D<NV> toolBoxVisualizer;

  /**
   * Track parameters for subclasses for "replay".
   */
  private MergedParameterization reconfig;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public DefaultAdapter(Parameterization config) {
    super();
    config = config.descend(this);
    reconfig = new MergedParameterization(config);
    axisVisualizer = new AxisVisualizer<NV>();
    histoVisualizer = new Projection1DHistogramVisualizer<NV>(reconfig);
    settingsVisualizer = new SettingsVisualizer(null);
    toolBoxVisualizer = new ToolBox2D<NV>();
  }

  @Override
  public boolean canVisualize(@SuppressWarnings("unused") VisualizerContext<? extends NV> context) {
    // TODO: check the database has number vectors?
    return true;
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(4);
    providedVisualizers.add(axisVisualizer);
    providedVisualizers.add(histoVisualizer);
    providedVisualizers.add(settingsVisualizer);
    providedVisualizers.add(toolBoxVisualizer);
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends NV> context, VisualizerTree<? extends NV> vistree) {
    // Create and init new visualizers.
    AxisVisualizer<NV> aVis = new AxisVisualizer<NV>();
    aVis.init(context);
    vistree.addVisualization(context.getDatabase(), aVis);

    reconfig.rewind();
    Projection1DHistogramVisualizer<NV> hVis = new Projection1DHistogramVisualizer<NV>(reconfig);
    hVis.init(context);
    vistree.addVisualization(context.getDatabase(), hVis);

    ToolBox2D<NV> tbVis = new ToolBox2D<NV>();
    tbVis.init(context);
    vistree.addVisualization(context.getDatabase(), tbVis);

    for(SettingsResult sr : ResultUtil.getSettingsResults(context.getResult())) {
      SettingsVisualizer v = new SettingsVisualizer(sr);
      v.init(context);
      vistree.addVisualization(sr, v);
    }
  }
}