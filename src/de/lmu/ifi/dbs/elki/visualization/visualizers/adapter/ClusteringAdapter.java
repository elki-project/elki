package de.lmu.ifi.dbs.elki.visualization.visualizers.adapter;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerTree;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.ClusterMeanVisualizer;
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
public class ClusteringAdapter<NV extends NumberVector<NV, ?>> implements AlgorithmAdapter<NV> {
  /**
   * Visualizer to do data dots (e.g. for outlier visualization)
   */
  private DataDotVisualizer<NV> dataDotVisualizer;

  /**
   * Visualizer for clusterings (unless doing outliers ...)
   */
  private ClusteringVisualizer<NV> clusteringVisualizer;

  /**
   * Cluster mean visualizer (when available)
   */
  private ClusterMeanVisualizer<NV> meanVisualizer;

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
    config = config.descend(this);
    dataDotVisualizer = new DataDotVisualizer<NV>();
    clusteringVisualizer = new ClusteringVisualizer<NV>();
    keyVisualizer = new KeyVisualizer();
    meanVisualizer = new ClusterMeanVisualizer<NV>();    
  }

  @Override
  public boolean canVisualize(VisualizerContext<? extends NV> context) {
    if (!VisualizerUtil.isNumberVectorDatabase(context.getDatabase())) {
      return false;
    }
    // FIXME: we can visualize the cluster key even for non-NV databases!
    return true;
  }

  @Override
  public Collection<Visualizer> getProvidedVisualizers() {
    ArrayList<Visualizer> providedVisualizers = new ArrayList<Visualizer>(4);
    providedVisualizers.add(clusteringVisualizer);
    providedVisualizers.add(keyVisualizer);
    providedVisualizers.add(dataDotVisualizer);
    providedVisualizers.add(meanVisualizer);
    return providedVisualizers;
  }

  @Override
  public void addVisualizers(VisualizerContext<? extends NV> context, VisualizerTree<? extends NV> vistree) {
    // Find clusterings we can visualize:
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(context.getResult(), Clustering.class);

    // FIXME: find a better logic for this!
    // Decide on whether to show cluster markers or dots:
    boolean preferDots = false;
    // If we have outlier results, hide default clustering and prefer tiny dots
    if(ResultUtil.filterResults(context.getResult(), OutlierResult.class).size() > 0) {
      preferDots = true;
    }
    for(Clustering<?> c : clusterings) {
      if(c.getAllClusters().size() > 0) {
        preferDots = false;
      }
      ClusteringVisualizer<NV> cv = new ClusteringVisualizer<NV>();
      KeyVisualizer kv = new KeyVisualizer();
      cv.init(context, c);
      kv.init(context, c);
      vistree.addVisualization(c, cv);
      vistree.addVisualization(c, kv);

      // Does the cluster have a model with cluster means?
      Clustering<MeanModel<NV>> mcls = findMeanModel(c);
      if(mcls != null) {
        ClusterMeanVisualizer<NV> kmv = new ClusterMeanVisualizer<NV>();
        kmv.init(context, mcls);
        vistree.addVisualization(c, kmv);
      }
    }
    // If we don't have a clustering, create a default clustering.
    if(clusterings.size() == 0) {
      Clustering<Model> c = context.getOrCreateDefaultClustering();
      ClusteringVisualizer<NV> cv = new ClusteringVisualizer<NV>();
      KeyVisualizer kv = new KeyVisualizer();
      cv.init(context, c);
      kv.init(context, c);
      // but don't show it by default for Outlier visualizations
      if(preferDots) {
        cv.getMetadata().put(Visualizer.META_VISIBLE_DEFAULT, false);
      }
      vistree.addVisualization(c, cv);
      vistree.addVisualization(c, kv);
    }

    // Add the dot visualizer
    dataDotVisualizer.init(context);
    if(!preferDots) {
      dataDotVisualizer.getMetadata().put(Visualizer.META_VISIBLE_DEFAULT, false);
    }
    vistree.addVisualization(context.getDatabase(), dataDotVisualizer);
  }

  /**
   * Test if the given clustering has a mean model.
   * 
   * @param <NV> Vector type
   * @param c Clustering to inspect
   * @return the clustering cast to return a mean model, null otherwise.
   */
  @SuppressWarnings("unchecked")
  private static <NV extends NumberVector<NV, ?>> Clustering<MeanModel<NV>> findMeanModel(Clustering<?> c) {
    if(c.getAllClusters().get(0).getModel() instanceof MeanModel<?>) {
      return (Clustering<MeanModel<NV>>) c;
    }
    return null;
  }
}