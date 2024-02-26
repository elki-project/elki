package elki.clustering.neighborhood;

import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.MutualNeighborClosedNeighborhoodSetGenerator;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * ENFORCE: clustering, then enforcing neighborhood consistency
 * <p>
 * Reference:
 * <p>
 * C. H. Q. Ding, X. He<br>
 * K-nearest-neighbor consistency in data clustering: incorporating local
 * information into global optimization<br>
 * Proc. Symposium on Applied Computing (SAC) 2004
 * 
 * @author Niklas Strahmann
 *
 * @param <O> Input data type
 */
@Reference(authors = "C. H. Q. Ding, X. He", //
    title = "K-nearest-neighbor consistency in data clustering: incorporating local information into global optimization", //
    booktitle = "Proc. Symposium on Applied Computing (SAC) 2004", //
    url = "https://doi.org/10.1145/967900.968021", bibkey = "DBLP:conf/sac/DingH04")
public class ENFORCE<O> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Base clustering algorithm.
   */
  private final ClusteringAlgorithm<?> baseAlgorithm;

  /**
   * Closed neighborhood set generator
   */
  private final ClosedNeighborhoodSetGenerator<? super O> closedNeighborhoodSetGenerator;

  /**
   * Constructor.
   *
   * @param baseAlgorithm Base algorithm
   * @param closedNeighborhoodSetGenerator Neighborhood generator
   */
  public ENFORCE(ClusteringAlgorithm<?> baseAlgorithm, ClosedNeighborhoodSetGenerator<? super O> closedNeighborhoodSetGenerator) {
    this.baseAlgorithm = baseAlgorithm;
    this.closedNeighborhoodSetGenerator = closedNeighborhoodSetGenerator;
  }

  /**
   * Run the clustering algorithm.
   * 
   * @param db Database
   * @return Clustering result
   */
  public Clustering<Model> run(Database db) {
    Clustering<?> baseResult = baseAlgorithm.autorun(db);

    Relation<O> relation = db.getRelation(closedNeighborhoodSetGenerator.getInputTypeRestriction());
    List<DBIDs> closedNeighborhoods = closedNeighborhoodSetGenerator.getClosedNeighborhoods(relation);

    int clusterAmount = baseResult.getAllClusters().size();
    ModifiableDBIDs[] finalCluster = new ModifiableDBIDs[clusterAmount];
    for(int i = 0; i < clusterAmount; i++) {
      finalCluster[i] = DBIDUtil.newArray();
    }

    for(DBIDs closedNeighborhood : closedNeighborhoods) {
      int[] clusterCounter = new int[clusterAmount];
      int clusterIndex = 0;

      for(It<? extends Cluster<?>> cluster = baseResult.iterToplevelClusters(); cluster.valid(); cluster.advance()) {
        DBIDs clusterDBIDs = cluster.get().getIDs();
        for(DBIDIter cnsElement = closedNeighborhood.iter(); cnsElement.valid(); cnsElement.advance()) {
          if(clusterDBIDs.contains(cnsElement)) {
            clusterCounter[clusterIndex]++;
          }
        }
        clusterIndex++;
      }

      int modeClusterIndex = argmax(clusterCounter);
      finalCluster[modeClusterIndex].addDBIDs(closedNeighborhood);
    }

    Clustering<Model> clustering = new Clustering<>();
    for(int i = 0; i < clusterAmount; i++) {
      if(finalCluster[i].size() > 0) {
        clustering.addToplevelCluster(new Cluster<>(finalCluster[i]));
      }
    }
    return clustering;
  }

  /**
   * Find the index of the maximum.
   * 
   * @param values Counts
   * @return Index
   */
  private int argmax(int[] values) {
    int maxIndex = 0, maxValue = values[0];
    for(int i = 1; i < values.length; i++) {
      if(values[i] > maxValue) {
        maxIndex = i;
        maxValue = values[i];
      }
    }
    return maxIndex;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return new TypeInformation[0];
  }

  /**
   * Parameterizer.
   * 
   * @author Niklas Strahmann
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Underlying base clustering algorithm.
     */
    protected ClusteringAlgorithm<?> baseAlgorithm;

    /**
     * Neighborhood generator for enforcement.
     */
    protected ClosedNeighborhoodSetGenerator<? super O> closedNeighborhoodSetGenerator;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<ClosedNeighborhoodSetGenerator<? super O>>(ClosedNeighborhoodSetGenerator.CNS_GENERATOR_ID, ClosedNeighborhoodSetGenerator.class, MutualNeighborClosedNeighborhoodSetGenerator.class) //
          .grab(config, x -> closedNeighborhoodSetGenerator = x);
      new ObjectParameter<ClusteringAlgorithm<?>>(Utils.ALGORITHM_ID, ClusteringAlgorithm.class) //
          .grab(config, x -> baseAlgorithm = x);
    }

    @Override
    public ENFORCE<O> make() {
      return new ENFORCE<>(baseAlgorithm, closedNeighborhoodSetGenerator);
    }
  }
}
