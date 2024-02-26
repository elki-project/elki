package elki.clustering.neighborhood;

import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.neighborhood.helper.ClosedNeighborhoodSetGenerator;
import elki.clustering.neighborhood.helper.MutualNeighborClosedNeighborhoodSetGenerator;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * 
 * @author Niklas Strahmann
 */
public class CNSClustering<O> implements ClusteringAlgorithm<Clustering<Model>> {
  private final ClosedNeighborhoodSetGenerator<O> closedNeighborhoodSetGenerator;

  public CNSClustering(ClosedNeighborhoodSetGenerator<O> closedNeighborhoodSetGenerator) {
    this.closedNeighborhoodSetGenerator = closedNeighborhoodSetGenerator;
  }

  public Clustering<Model> run(Relation<O> relation) {
    List<DBIDs> CNSs = closedNeighborhoodSetGenerator.getClosedNeighborhoods(relation);
    Clustering<Model> clustering = new Clustering<>();
    for(DBIDs cns : CNSs) {
      clustering.addToplevelCluster(new Cluster<>(cns));
    }
    return clustering;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(closedNeighborhoodSetGenerator.getInputTypeRestriction());
  }

  /**
   * 
   * @author Niklas Strahmann
   */
  public static class Par<O> implements Parameterizer {
    protected ClosedNeighborhoodSetGenerator<O> closedNeighborhoodSetGenerator;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<ClosedNeighborhoodSetGenerator<O>>(ClosedNeighborhoodSetGenerator.CNS_GENERATOR_ID, ClosedNeighborhoodSetGenerator.class, MutualNeighborClosedNeighborhoodSetGenerator.class).grab(config, x -> closedNeighborhoodSetGenerator = x);
    }

    @Override
    public CNSClustering<O> make() {
      return new CNSClustering<>(closedNeighborhoodSetGenerator);
    }
  }
}
