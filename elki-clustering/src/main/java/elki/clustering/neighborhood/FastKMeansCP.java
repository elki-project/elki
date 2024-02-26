package elki.clustering.neighborhood;

import java.util.ArrayList;
import java.util.List;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
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
 * 
 * @author Niklas Strahmann
 */
@Reference(authors = "Lars Lenssen, Niklas Strahmann, Erich Schubert", //
    title = "Fast k-Nearest-Neighbor-Consistent Clustering", //
    booktitle = "Lernen, Wissen, Daten, Analysen (LWDA)", //
    url = "https://ceur-ws.org/Vol-3630/LWDA2023-paper34.pdf", bibkey = "DBLP:conf/lwa/LenssenSS23")
public class FastKMeansCP<V extends NumberVector> extends KMeansCP<V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FastKMeansCP.class);

  /**
   * Constructor.
   *
   * @param kCluster Number of clusters
   * @param maxiter Maximum number of iterations
   * @param initializer Initialization method
   * @param closedNeighborhoodSetGenerator Neighborhood generator
   */
  public FastKMeansCP(int kCluster, int maxiter, KMeansInitialization initializer, ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator) {
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
  protected class Instance extends KMeansCP<V>.Instance {
    /**
     * Summarizes of each clused neighborhood set.
     */
    protected List<CNSrepresentor> summaries;

    /**
     * Cluster assignment of each neighborhood set.
     */
    protected List<List<CNSrepresentor>> cnsClusters;

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
      summaries = initalizeCNSrepresentors(closedNeighborhoods);
      cnsClusters = new ArrayList<>(k);
      for(int i = 0; i < k; i++) {
        cnsClusters.add(new ArrayList<>());
      }
      super.run(maxiter);
    }

    @Override
    protected int iterate(int iteration) {
      means = iteration == 1 ? means : weightedMeans(cnsClusters, means);
      return assignToNearestCluster(summaries, means);
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }

    /**
     * Creates a representative for each closed neighborhood set.
     * 
     * @param closedNeighborhoodSets closed neighborhood sets to operate on
     * @return representative consisting of mean and sizer of set
     */
    private List<CNSrepresentor> initalizeCNSrepresentors(List<DBIDs> closedNeighborhoodSets) {
      final int dim = RelationUtil.dimensionality(relation);
      List<CNSrepresentor> summaries = new ArrayList<>(closedNeighborhoodSets.size());
      for(DBIDs cns : closedNeighborhoodSets) {
        double[] sum = new double[dim];
        for(DBIDIter element = cns.iter(); element.valid(); element.advance()) {
          plusEquals(sum, relation.get(element));
        }
        summaries.add(new CNSrepresentor(VMath.times(sum, 1.0 / cns.size()), sum, cns.size(), cns));
      }
      return summaries;
    }

    protected double[][] weightedMeans(List<List<CNSrepresentor>> clusters, double[][] means) {
      final int k = means.length, dim = means[0].length;
      double[][] newMeans = new double[k][dim];
      for(int clusterIndex = 0; clusterIndex < k; clusterIndex++) {
        List<CNSrepresentor> cluster = clusters.get(clusterIndex);
        if(cluster.size() == 0) {
          newMeans[clusterIndex] = means[clusterIndex];
          continue;
        }

        double[] sum = newMeans[clusterIndex]; // local ref
        int size = 0;
        for(CNSrepresentor summary : cluster) {
          VMath.plusTimesEquals(sum, summary.cnsMean, summary.size);
          size += summary.size;
        }
        VMath.timesEquals(sum, 1.0 / size);
      }
      return newMeans;
    }

    protected int assignToNearestCluster(List<CNSrepresentor> summaries, double[][] means) {
      int changed = 0;
      for(List<CNSrepresentor> cluster : cnsClusters) {
        cluster.clear();
      }

      for(CNSrepresentor representative : summaries) {
        int minIndex = 0;
        double minDist = distance(representative.cnsMean, means[0]);
        for(int i = 1; i < k; i++) {
          double dist = distance(representative.cnsMean, means[i]);
          if(dist < minDist) {
            minIndex = i;
            minDist = dist;
          }
        }
        minDist *= representative.size;
        varsum[minIndex] += isSquared ? minDist : (minDist * minDist);
        cnsClusters.get(minIndex).add(representative);
        if(minIndex != representative.assignment) {
          representative.assignment = minIndex;
          changed += representative.size;
        }
      }
      return changed;
    }

    @Override
    public Clustering<KMeansModel> buildResult() {
      for(int i = 0; i < cnsClusters.size(); i++) {
        for(CNSrepresentor cns : cnsClusters.get(i)) {
          clusters.get(i).addDBIDs(cns.cnsElements);
        }
      }
      return super.buildResult();
    }
  }

  /**
   * Summary of a closed neighborhood set for FastKMeansCP
   * 
   * @author Niklas Strahmann
   */
  protected static class CNSrepresentor {
    protected int size;

    protected double[] cnsMean;

    protected double[] elementSum;

    protected DBIDs cnsElements;

    protected int assignment;

    protected CNSrepresentor(double[] cnsMean, double[] elementSum, int size, DBIDs cnsElements) {
      this.cnsMean = cnsMean;
      this.size = size;
      this.cnsElements = cnsElements;
      this.elementSum = elementSum;
      this.assignment = -1;
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
    public FastKMeansCP<V> make() {
      return new FastKMeansCP<>(k, maxiter, initializer, closedNeighborhoodSetGenerator);
    }
  }
}
