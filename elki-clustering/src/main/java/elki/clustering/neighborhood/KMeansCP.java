package elki.clustering.neighborhood;

import java.util.Arrays;
import java.util.List;

import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.MutualNeighborClosedNeighborhoodSetGenerator;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Create neighborhood consistent clusterings by calculating closed neighborhood
 * sets and running standard k-means iterations for these sets of points.
 * <p>
 * This is a "naive" implementation of the basic algorithm, for a version that
 * exploits properties of squared Euclidean distance, see {@link FastKMeansCP}
 * instead.
 * <p>
 * Reference:
 * <p>
 * C. H. Q. Ding, X. He<br>
 * K-nearest-neighbor consistency in data clustering: incorporating local
 * information into global optimization<br>
 * Proc. Symposium on Applied Computing (SAC) 2004
 * 
 * @author Niklas Strahmann
 */
@Reference(authors = "C. H. Q. Ding, X. He", //
    title = "K-nearest-neighbor consistency in data clustering: incorporating local information into global optimization", //
    booktitle = "Proc. Symposium on Applied Computing (SAC) 2004", //
    url = "https://doi.org/10.1145/967900.968021", bibkey = "DBLP:conf/sac/DingH04")
public class KMeansCP<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(KMeansCP.class);

  /**
   * Neighborhood generator
   */
  protected final ClosedNeighborhoodSetGenerator<? super V> closedNeighborhoodSetGenerator;

  /**
   * Constructor.
   *
   * @param kCluster Number of clusters
   * @param maxiter Maximum number of iterations
   * @param initializer Initialization method
   * @param closedNeighborhoodSetGenerator Neighborhood generator
   */
  public KMeansCP(int kCluster, int maxiter, KMeansInitialization initializer, ClosedNeighborhoodSetGenerator<? super V> closedNeighborhoodSetGenerator) {
    super(kCluster, maxiter, initializer);
    this.closedNeighborhoodSetGenerator = closedNeighborhoodSetGenerator;
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
  protected class Instance extends AbstractKMeans.Instance {
    /**
     * Neighborhoods
     */
    protected List<DBIDs> closedNeighborhoods;

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

    @SuppressWarnings("unchecked")
    @Override
    public void run(int maxiter) {
      closedNeighborhoods = closedNeighborhoodSetGenerator.getClosedNeighborhoods((Relation<? extends V>) relation);
      super.run(maxiter);
    }

    @Override
    protected int iterate(int iteration) {
      means = iteration == 1 ? means : means(clusters, means, relation);
      return assignToNearestCluster();
    }

    @Override
    protected int assignToNearestCluster() {
      int changed = 0;
      Arrays.fill(varsum, 0.);
      for(ModifiableDBIDs cluster : clusters) {
        cluster.clear();
      }

      for(DBIDs cns : closedNeighborhoods) {
        int minIndex = 0;
        double minDist = distanceSum(cns, means[0]);
        for(int i = 1; i < k; i++) {
          double currentDistance = distanceSum(cns, means[i]);
          if(currentDistance < minDist) {
            minDist = currentDistance;
            minIndex = i;
          }
        }
        varsum[minIndex] += isSquared ? minDist : (minDist * minDist);
        for(DBIDIter element = cns.iter(); element.valid(); element.advance()) {
          if(assignment.putInt(element, minIndex) != minIndex) {
            changed++;
          }
        }
        clusters.get(minIndex).addDBIDs(cns);
      }
      return changed;
    }

    /**
     * Compute the distance sum for one neighborhood set.
     *
     * @param cns Closed neighborhood set
     * @param mean Cluster mean
     * @return Distance sum
     */
    protected double distanceSum(DBIDs cns, double[] mean) {
      double distanceSum = 0;
      for(DBIDIter element = cns.iter(); element.valid(); element.advance()) {
        distanceSum += distance(relation.get(element), mean);
      }
      return distanceSum;
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
  public static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
    /**
     * Neighborhood generator
     */
    protected ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<ClosedNeighborhoodSetGenerator<V>>(ClosedNeighborhoodSetGenerator.CNS_GENERATOR_ID, ClosedNeighborhoodSetGenerator.class, MutualNeighborClosedNeighborhoodSetGenerator.class) //
          .grab(config, x -> closedNeighborhoodSetGenerator = x);
    }

    @Override
    public KMeansCP<V> make() {
      return new KMeansCP<>(k, maxiter, initializer, closedNeighborhoodSetGenerator);
    }
  }
}
