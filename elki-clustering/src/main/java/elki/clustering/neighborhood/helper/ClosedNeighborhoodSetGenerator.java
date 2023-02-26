package elki.clustering.neighborhood.helper;

import elki.data.type.TypeInformation;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;

public interface ClosedNeighborhoodSetGenerator<O> {


    DBIDs[] getClosedNeighborhoods(Relation<O> relation);

    interface Factory<O> {
        ClosedNeighborhoodSetGenerator<O> instantiate();
        TypeInformation getInputTypeRestriction();

    }

}
