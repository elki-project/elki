package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.MergedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d.Projection1DHistogramVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.AxisVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.ClusteringVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.DataDotVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.KeyVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.SettingsVisualizer;

/**
 * Class to add various default visualizations.
 * 
 * @author Erich Schubert
 *
 * @param <NV> Vector type
 */
// TODO: Support more than one clustering.
public class DefaultAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter {
  /**
   * Visualizer to do data dots (e.g. for outlier visualization)
   */
  private DataDotVisualizer<NV> dataDotVisualizer;

  /**
   * Visualizer for clusterings (unless doing outliers ...)
   */
  private ClusteringVisualizer<NV> clusteringVisualizer;

  /**
   * Visualizer to show the clustering key.
   */
  private KeyVisualizer keyVisualizer;

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
    reconfig = new MergedParameterization(config);
    dataDotVisualizer = new DataDotVisualizer<NV>();
    clusteringVisualizer = new ClusteringVisualizer<NV>();
    axisVisualizer = new AxisVisualizer<NV>();
    histoVisualizer = new Projection1DHistogramVisualizer<NV>(reconfig);
    keyVisualizer = new KeyVisualizer();
    settingsVisualizer = new SettingsVisualizer();
  }

  @Override
  public boolean canVisualize(@SuppressWarnings("unused") VisualizerContext context) {
    // TODO: check the database has number vectors?
    return true;
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(6);
    providedVisualizers.add(dataDotVisualizer);
    providedVisualizers.add(clusteringVisualizer);
    providedVisualizers.add(axisVisualizer);
    providedVisualizers.add(histoVisualizer);
    providedVisualizers.add(keyVisualizer);
    providedVisualizers.add(settingsVisualizer);
    return providedVisualizers;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>();
    axisVisualizer.init(context);
    dataDotVisualizer.init(context);
    clusteringVisualizer.init(context, context.getOrCreateDefaultClustering());
    histoVisualizer.init(context);
    keyVisualizer.init(context);
    settingsVisualizer.init(context);
    
    usableVisualizers.add(axisVisualizer);
    usableVisualizers.add(clusteringVisualizer);
    usableVisualizers.add(keyVisualizer);
    usableVisualizers.add(settingsVisualizer);
    usableVisualizers.add(dataDotVisualizer);
    usableVisualizers.add(histoVisualizer);
    
    // Decide on whether to show cluster markers or dots:
    boolean preferDots = false;
    // If we have outlier results, hide default clustering and prefer tiny dots
    if (ResultUtil.filterResults(context.getResult(), OutlierResult.class).size() > 0) {
      preferDots = true;
    }
    // If we have a (non-empty) clustering, we'll prefer it to dots.
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(context.getResult(), Clustering.class);
    for (Clustering<?> c : clusterings) {
      if (c.getAllClusters().size() > 0) {
        preferDots = false;
      }
    }
    if (preferDots) {
      //keyVisualizer.getMetadata().put(Visualizer.META_VISIBLE_DEFAULT, false);
      clusteringVisualizer.getMetadata().put(Visualizer.META_VISIBLE_DEFAULT, false);
    } else {
      dataDotVisualizer.getMetadata().put(Visualizer.META_VISIBLE_DEFAULT, false);
    }
    return usableVisualizers;
  }
}