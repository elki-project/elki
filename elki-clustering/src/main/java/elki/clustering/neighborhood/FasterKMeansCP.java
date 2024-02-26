package elki.clustering.neighborhood;

import static elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator.CNS_GENERATOR_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.MutualNeighborClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.model.CNSrepresentor;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.math.linearalgebra.VMath;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ClassParameter;

/**
 * Improves {@link FastKMeansCP} by not recompute
 * <code>cns.avg * cns.size</code> of all CNS every Iteration but instead uses
 * the precomputed <code>cns.sum</code>.
 * 
 * @bug Currently not always converging.
 */
public class FasterKMeansCP<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  private static final Logging LOG = Logging.getLogger(FasterKMeansCP.class);

  private final ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator;

  public FasterKMeansCP(int kCluster, int maxiter, KMeansInitialization initializer, ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator) {
    super(kCluster, maxiter, initializer);
    this.closedNeighborhoodSetGenerator = closedNeighborhoodSetGenerator;
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> rel) {
    Instance instance = new Instance(rel, distance, initialMeans(rel));
    instance.run(maxiter);
    return instance.buildResult();
  }

  protected List<DBIDs> getCNS(Relation<V> relation) {
    Duration cnsTime = LOG.newDuration(closedNeighborhoodSetGenerator.getClass().getName() + ".time").begin();
    List<DBIDs> dbids = closedNeighborhoodSetGenerator.getClosedNeighborhoods(relation);
    LOG.statistics(cnsTime.end());
    return dbids;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * 
   * @author Niklas Strahmann
   */
  protected class Instance extends AbstractKMeans.Instance {

    protected List<DBIDs> CNSs;

    protected CNSrepresentor[] cnsRepresentors;

    protected List<List<CNSrepresentor>> CnsClusters;

    protected Map<CNSrepresentor, Integer> cnsAssignment;

    /**
     * Constructor.
     *
     * @param relation Relation to process
     * @param df Distance function
     * @param means Initial mean
     */
    public Instance(Relation<V> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
      CNSs = getCNS(relation);
      cnsRepresentors = initalizeCNSrepresentors(CNSs);
      CnsClusters = new ArrayList<>(k);
      for(int i = 0; i < k; i++) {
        CnsClusters.add(new ArrayList<>());
      }
      cnsAssignment = new HashMap<>(CNSs.size());
    }

    @Override
    protected int iterate(int iteration) {
      means = iteration == 1 ? means : weightedMeans(CnsClusters, means);
      return assignToNearestCluster(cnsRepresentors, means);
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
    private CNSrepresentor[] initalizeCNSrepresentors(List<DBIDs> closedNeighborhoodSets) {
      int dim = RelationUtil.dimensionality(relation);

      CNSrepresentor[] representors = new CNSrepresentor[closedNeighborhoodSets.size()];

      for(int currentCNS = 0; currentCNS < closedNeighborhoodSets.size(); currentCNS++) {
        double[] sum = new double[dim];
        int CNSsize = closedNeighborhoodSets.get(currentCNS).size();
        for(DBIDIter element = closedNeighborhoodSets.get(currentCNS).iter(); element.valid(); element.advance()) {
          VMath.plusEquals(sum, relation.get(element).toArray());
        }
        representors[currentCNS] = new CNSrepresentor(VMath.times(sum, 1.0 / CNSsize), sum, CNSsize, closedNeighborhoodSets.get(currentCNS));
      }

      return representors;
    }

    protected double[][] weightedMeans(List<List<CNSrepresentor>> clusters, double[][] means) {
      final int k = means.length;
      double[][] newMeans = new double[k][];
      for(int clusterIndex = 0; clusterIndex < k; clusterIndex++) {
        List<CNSrepresentor> cluster = clusters.get(clusterIndex);
        if(cluster.size() == 0) {
          newMeans[clusterIndex] = means[clusterIndex];
          continue;
        }

        int amountElements = cluster.get(0).size;
        double[] sum = cluster.get(0).elementSum;

        for(int i = 1; i < cluster.size(); i++) {
          VMath.plusEquals(sum, cluster.get(i).elementSum);
          amountElements += cluster.get(i).size;
        }
        newMeans[clusterIndex] = VMath.timesEquals(sum, 1.0 / amountElements);
      }
      return newMeans;
    }

    protected int assignToNearestCluster(CNSrepresentor[] representatives, double[][] means) {
      int changed = 0;

      for(List<CNSrepresentor> cluster : CnsClusters) {
        cluster.clear();
      }

      for(CNSrepresentor representative : representatives) {
        NumberVector cnsMean = DoubleVector.wrap(representative.cnsMean);

        double minDist = distance.distance(cnsMean, DoubleVector.wrap(means[0]));
        int minIndex = 0;
        for(int i = 1; i < k; i++) {
          double dist = distance.distance(cnsMean, DoubleVector.wrap(means[i]));
          if(dist < minDist) {
            minIndex = i;
            minDist = dist;
          }
        }
        varsum[minIndex] += isSquared ? minDist : (minDist * minDist);
        CnsClusters.get(minIndex).add(representative);
        if(!((Integer) minIndex).equals(cnsAssignment.put(representative, minIndex))) {
          changed += representative.size;
        }
      }
      return changed;
    }

    @Override
    public Clustering<KMeansModel> buildResult() {
      for(int i = 0; i < CnsClusters.size(); i++) {
        for(CNSrepresentor cns : CnsClusters.get(i)) {
          clusters.get(i).addDBIDs(cns.cnsElements);
        }
      }
      return super.buildResult();
    }
  }

  /**
   * 
   * @author Niklas Strahmann
   */
  public static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
    protected ClosedNeighborhoodSetGenerator<V> closedNeighborhoodSetGenerator;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);

      new ClassParameter<ClosedNeighborhoodSetGenerator<V>>(CNS_GENERATOR_ID, ClosedNeighborhoodSetGenerator.class, MutualNeighborClosedNeighborhoodSetGenerator.class).grab(config, x -> closedNeighborhoodSetGenerator = x);
    }

    @Override
    public FasterKMeansCP<V> make() {
      return new FasterKMeansCP<>(k, maxiter, initializer, closedNeighborhoodSetGenerator);
    }
  }

}
