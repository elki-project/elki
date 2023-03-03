package elki.clustering.neighborhood.helper;

import elki.Algorithm;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.PrimitiveDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

public abstract class AbstractClosedNeighborhoodSetGenerator<V> implements ClosedNeighborhoodSetGenerator<V>{
    @Override
    public DBIDs[] getClosedNeighborhoods(Relation<V> relation) {
        return new DBIDs[0];
    }

    public abstract static class Par<V> implements Parameterizer {

        protected Distance<? super V> distance;

        @Override
        public void configure(Parameterization config) {
            new ObjectParameter<Distance<? super V>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, PrimitiveDistance.class, SquaredEuclideanDistance.class)
                    .grab(config, x -> distance = x);

        }

        @Override
        public abstract Object make();
    }
}
