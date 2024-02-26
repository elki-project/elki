package elki.evaluation.clustering.neighborhood;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.Evaluator;
import elki.helper.MutualNeighborQuery;
import elki.helper.MutualNeighborQueryBuilder;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import java.util.List;

/**
 * 
 * @author Niklas Strahmann
 */
public class MutualNeighborConsistency<O> implements Evaluator {
  private final Distance<? super O> distance;

  protected int k;

  int kplus;

  public MutualNeighborConsistency(Distance<? super O> distance, int k) {
    super();
    this.distance = distance;
    this.k = k;
    this.kplus = k + 1;
  }

  public double evaluateClustering(Clustering<?> clustering, Relation<O> relation) {
    WritableDoubleDataStore elementKMNConsistency = DataStoreFactory.FACTORY.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB, 0.);
    List<? extends Cluster<?>> clusters = clustering.getAllClusters();
    MutualNeighborQuery<DBIDRef> kmnQuery = new MutualNeighborQueryBuilder<>(relation, distance).precomputed().byDBID(kplus);

    int amountKMNConsistentElements = 0;

    for(Cluster<?> cluster : clusters) {
      DBIDs clusterElementIds = cluster.getIDs();

      clusterElementLoop: for(DBIDIter clusterElement = clusterElementIds.iter(); clusterElement.valid(); clusterElement.advance()) {

        DBIDs neighbors = kmnQuery.getMutualNeighbors(clusterElement, kplus);
        for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          if(!clusterElementIds.contains(neighbor)) {
            continue clusterElementLoop;
          }
        }
        elementKMNConsistency.put(clusterElement, 1);
        amountKMNConsistentElements++;
      }
    }

    double kMNc = (double) amountKMNConsistentElements / relation.size();

    EvaluationResult ev = EvaluationResult.findOrCreate(clustering, "Clustering Evaluation");
    EvaluationResult.MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    g.addMeasure(k + "-MN Consistency", kMNc, 0, 1., false);
    if(!Metadata.hierarchyOf(clustering).addChild(ev)) {
      Metadata.of(ev).notifyChanged();
    }
    Metadata.hierarchyOf(clustering).addChild(new MaterializedDoubleRelation(k + "-MN Consistency", relation.getDBIDs(), elementKMNConsistency));
    return kMNc;
  }

  @Override
  public void processNewResult(Object result) {
    List<Clustering<?>> clusters = Clustering.getClusteringResults(result);
    if(clusters.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(result);
    Relation<O> relation = db.getRelation(distance.getInputTypeRestriction());
    for(Clustering<?> cluster : clusters) {
      evaluateClustering(cluster, relation);
    }
  }

  /**
   * 
   * @author Niklas Strahmann
   */
  public static class Par<O> implements Parameterizer {
    public static final OptionID DISTANCE_ID = new OptionID("kmnc.distance", "Distance function to use for computing the kmnc.");

    public static final OptionID NUMBER_K = new OptionID("kmnc.k", "Number of Neighbors checked.");

    private Distance<? super O> distance;

    private int k;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(DISTANCE_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(NUMBER_K) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public MutualNeighborConsistency<O> make() {
      return new MutualNeighborConsistency<>(distance, k);
    }
  }
}
