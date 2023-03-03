package elki.clustering.neighborhood.helper;

import elki.data.type.TypeInformation;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

public class NearestNeighborClosedNeighborhoodSetGenerator<O> extends AbstractClosedNeighborhoodSetGenerator<O> {

    int k;
    int kPlus;
    Distance<? super O> distance;

    KNNSearcher<DBIDRef> knn;
    RKNNSearcher<DBIDRef> rknn;

    /**
     * Generate a closed neighborhood set for the KNN neighborhood relation.
     * This is a weak-connected component in the corresponding neighborhood graph.
     * @param k number of neighbors (excluding the origin)
     * @param distance rank neighbors based on this distance
     */
    public NearestNeighborClosedNeighborhoodSetGenerator(int k, Distance<? super O> distance){
       this.k = k;
       this.distance = distance;
       this.kPlus = k+1;
    }

    @Override
    public StaticDBIDs[] getClosedNeighborhoods(Relation<O> relation) {
        knn = new QueryBuilder<>(relation, distance).precomputed().kNNByDBID(kPlus);
        rknn = new QueryBuilder<>(relation, distance).precomputed().rKNNByDBID(kPlus);
        return super.getClosedNeighborhoods(relation);
    }

    @Override
    protected DBIDs getNeighbors(DBIDRef element) {
        ModifiableDBIDs neighbors = DBIDUtil.newArray(2*k);
        neighbors.addDBIDs(knn.getKNN(element, kPlus));
        neighbors.addDBIDs(rknn.getRKNN(element, kPlus));
        return neighbors;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
        return distance.getInputTypeRestriction();
    }

    public static class Par<O> extends AbstractClosedNeighborhoodSetGenerator.Par<O>{

        OptionID K_NEIGHBORS = new OptionID("closedNeighborhoodSet.k", "The amount of neighbors to consider to create the closed neighborhood set.");
        private int k;

        @Override
        public void  configure(Parameterization config) {
            super.configure(config);
            new IntParameter(K_NEIGHBORS)
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)
                    .grab(config, p-> k = p);
        }

        @Override
        public NearestNeighborClosedNeighborhoodSetGenerator<O> make() {
            return new NearestNeighborClosedNeighborhoodSetGenerator<>(k, distance);
        }
    }

}
