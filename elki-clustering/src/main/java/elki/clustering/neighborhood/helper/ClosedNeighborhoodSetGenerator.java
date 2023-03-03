package elki.clustering.neighborhood.helper;

import elki.data.type.TypeInformation;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.utilities.optionhandling.OptionID;

public interface ClosedNeighborhoodSetGenerator<O> {

    OptionID CNS_GENERATOR_ID = new OptionID("clustering.neighborhood.generators", "Type of neighborhood relation to generate sets containing closed neighborhoods.");


    DBIDs[] getClosedNeighborhoods(Relation<O> relation);

    TypeInformation getInputTypeRestriction() ;
}
