package elki.clustering.neighborhood;

import java.util.List;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;

/**
 * Improves {@link KMeansCP} by using the center of each closed neighborhood set
 * instead of working with individual data points. This improved the runtime
 * over the original {@link KMeansCP} algorithm.
 * <p>
 * Reference:
 * <p>
 * Lars Lenssen, Niklas Strahmann, Erich Schubert<br>
 * Fast k-Nearest-Neighbor-Consistent Clustering<br>
 * Lernen, Wissen, Daten, Analysen (LWDA), 2023
 */
@Reference(authors = "Lars Lenssen, Niklas Strahmann, Erich Schubert", //
    title = "Fast k-Nearest-Neighbor-Consistent Clustering", //
    booktitle = "Lernen, Wissen, Daten, Analysen (LWDA)", //
    url = "https://ceur-ws.org/Vol-3630/LWDA2023-paper34.pdf", bibkey = "DBLP:conf/lwa/LenssenSS23")
public class FasterKMeansCP<V extends NumberVector> extends FastKMeansCP<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FasterKMeansCP.class);

  /**
   * Constructor.
   *
   * @param kCluster Number of clusters
   * @param maxiter Maximum number of iterations
   * @param initializer Initialization method
   * @param closedNeighborhoodSetGenerator Neighborhood generator
   */
  public FasterKMeansCP(int kCluster, int maxiter, KMeansInitialization initializer, ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator) {
    super(kCluster, maxiter, initializer, closedNeighborhoodSetGenerator);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> rel) {
    Instance instance = new Instance(rel, distance, initialMeans(rel));
    instance.run(maxiter);
    return instance.buildResult();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Instance for a particular data set
   * 
   * @author Niklas Strahmann
   */
  protected class Instance extends FastKMeansCP<V>.Instance {
    /**
     * Constructor.
     *
     * @param relation Relation to process
     * @param df Distance function
     * @param means Initial mean
     */
    public Instance(Relation<? extends V> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
    }

    @Override
    protected double[][] weightedMeans(List<List<CNSrepresentor>> clusters, double[][] means) {
      final int k = means.length;
      double[][] newMeans = new double[k][means[0].length];
      for(int clusterIndex = 0; clusterIndex < k; clusterIndex++) {
        List<CNSrepresentor> cluster = clusters.get(clusterIndex);
        if(cluster.size() == 0) {
          newMeans[clusterIndex] = means[clusterIndex];
          continue;
        }

        double[] sum = newMeans[clusterIndex]; // local ref
        int size = 0;
        for(CNSrepresentor summary : cluster) {
          VMath.plusEquals(sum, summary.elementSum);
          size += summary.size;
        }
        VMath.timesEquals(sum, 1.0 / size);
      }
      return newMeans;
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  /**
   * Class parameterizer
   * 
   * @author Niklas Strahmann
   *
   * @param <V> Vector type
   */
  public static class Par<V extends NumberVector> extends KMeansCP.Par<V> {
    @Override
    public FasterKMeansCP<V> make() {
      return new FasterKMeansCP<>(k, maxiter, initializer, closedNeighborhoodSetGenerator);
    }
  }
}
