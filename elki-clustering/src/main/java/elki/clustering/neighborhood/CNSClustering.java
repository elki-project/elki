package elki.clustering.neighborhood;

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
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

public class CNSClustering<O> implements ClusteringAlgorithm<Clustering<Model>> {


    private final ClosedNeighborhoodSetGenerator<O> closedNeighborhoodSetGenerator;

    public CNSClustering(ClosedNeighborhoodSetGenerator<O> closedNeighborhoodSetGenerator) {
        this.closedNeighborhoodSetGenerator = closedNeighborhoodSetGenerator;
    }

    public Clustering<Model> run(Relation<O> relation){
        DBIDs[] CNSs = closedNeighborhoodSetGenerator.getClosedNeighborhoods(relation);

        Clustering<Model> clustering = new Clustering<>();

        for(DBIDs cns : CNSs){
            clustering.addToplevelCluster(new Cluster<>(cns));
        }
        return clustering;
    }



    @Override
    public TypeInformation[] getInputTypeRestriction() {
        return TypeUtil.array(closedNeighborhoodSetGenerator.getInputTypeRestriction());
    }

    public static class Par<O> implements Parameterizer{

        public static final OptionID CNS_TYPE = new OptionID("closedneighborhoodset.neighborhoodrelation", "Type of neighborhood - knn/kmn");
        protected ClosedNeighborhoodSetGenerator<O> closedNeighborhoodSetGenerator;

        @Override
        public void configure(Parameterization config) {
            new ObjectParameter<ClosedNeighborhoodSetGenerator<O>>(CNS_TYPE, ClosedNeighborhoodSetGenerator.class, MutualNeighborClosedNeighborhoodSetGenerator.class)
                    .grab(config, x -> closedNeighborhoodSetGenerator = x);
        }

        @Override
        public Object make() {
            return new CNSClustering<>(closedNeighborhoodSetGenerator);
        }
    }
}
