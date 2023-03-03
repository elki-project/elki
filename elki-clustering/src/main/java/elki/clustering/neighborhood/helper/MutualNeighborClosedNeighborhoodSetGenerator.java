package elki.clustering.neighborhood.helper;

import elki.data.type.TypeInformation;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;
import elki.database.ids.StaticDBIDs;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.helper.MutualNeighborQuery;
import elki.helper.MutualNeighborQueryBuilder;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

public class MutualNeighborClosedNeighborhoodSetGenerator<O> extends AbstractClosedNeighborhoodSetGenerator<O> {

    private final int k;
    private final int kPlus;
    private final Distance<? super O> distance;
    MutualNeighborQuery<DBIDRef> kmn;

    public MutualNeighborClosedNeighborhoodSetGenerator(int k, Distance<? super O> distance){
        this.k = k;
        this.kPlus = k+1;
        this.distance = distance;
    }
    @Override
    public StaticDBIDs[] getClosedNeighborhoods(Relation<O> relation) {
        kmn = new MutualNeighborQueryBuilder<>(relation, distance).precomputed().byDBID(kPlus);
        return super.getClosedNeighborhoods(relation);
    }

    @Override
    protected DBIDs getNeighbors(DBIDRef element) {
        return kmn.getMutualNeighbors(element, kPlus);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
        return distance.getInputTypeRestriction();
    }

    public static class Par<O> extends AbstractClosedNeighborhoodSetGenerator.Par<O>{

        OptionID K_NEIGHBORS = new OptionID("closedNeighborhoodSet.k", "The amount of neighbors to consider to create the closed neighborhood set.");
        private int k;

        @Override
        public void configure(Parameterization config){
            super.configure(config);

            new IntParameter(K_NEIGHBORS)
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)
                    .grab(config, p-> k = p);

        }

        @Override
        public MutualNeighborClosedNeighborhoodSetGenerator<O> make() {
            return new MutualNeighborClosedNeighborhoodSetGenerator<>(k, distance) ;
        }
    }
}
