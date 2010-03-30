package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.ClusteringVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.DataDotVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.KeyVisualizer;

/**
 * Class to add generic clustering visualizations.
 * 
 * @author Erich Schubert
 *
 * @param <NV> Vector type
 */
public class ClusteringAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter {
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
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public ClusteringAdapter(Parameterization config) {
    super();
    dataDotVisualizer = new DataDotVisualizer<NV>();
    clusteringVisualizer = new ClusteringVisualizer<NV>();
    keyVisualizer = new KeyVisualizer();
  }

  @Override
  public boolean canVisualize(@SuppressWarnings("unused") VisualizerContext context) {
    // TODO: check the database has number vectors?
    return true;
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(6);
    providedVisualizers.add(clusteringVisualizer);
    providedVisualizers.add(keyVisualizer);
    providedVisualizers.add(dataDotVisualizer);
    return providedVisualizers;
  }

  @Override
  public Collection<Visualizer> getUsableVisualizers(VisualizerContext context) {
    // Find clusterings we can visualize:
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(context.getResult(), Clustering.class);
    // We'll at least add one clustering.
    final int numclus = Math.max(clusterings.size(), 1);
    // Store the usable visualizers
    ArrayList<Visualizer> usableVisualizers = new ArrayList<Visualizer>(numclus*2 + 1);
    
    // Decide on whether to show cluster markers or dots:
    boolean preferDots = false;
    // If we have outlier results, hide default clustering and prefer tiny dots
    if (ResultUtil.filterResults(context.getResult(), OutlierResult.class).size() > 0) {
      preferDots = true;
    }
    for (Clustering<?> c : clusterings) {
      if (c.getAllClusters().size() > 0) {
        preferDots = false;
      }
      ClusteringVisualizer<NV> cv = new ClusteringVisualizer<NV>();
      KeyVisualizer kv = new KeyVisualizer();
      cv.init(context, c);
      kv.init(context, c);
      
      usableVisualizers.add(cv);
      usableVisualizers.add(kv);
    }
    // If we don't have a clustering, create a default clustering.
    if (clusterings.size() == 0) {
      Clustering<Model> c = context.getOrCreateDefaultClustering();
      ClusteringVisualizer<NV> cv = new ClusteringVisualizer<NV>();
      KeyVisualizer kv = new KeyVisualizer();
      cv.init(context, c);
      kv.init(context, c);
      // but don't show it by default for Outlier visualizations
      if (preferDots) {
        cv.getMetadata().put(Visualizer.META_VISIBLE_DEFAULT, false);        
      }
      
      usableVisualizers.add(cv);
      usableVisualizers.add(kv);
    }
    
    // Add the dot visualizer
    dataDotVisualizer.init(context);
    if (!preferDots) {
      dataDotVisualizer.getMetadata().put(Visualizer.META_VISIBLE_DEFAULT, false);
    }
    usableVisualizers.add(dataDotVisualizer);
    
    return usableVisualizers;
  }
}