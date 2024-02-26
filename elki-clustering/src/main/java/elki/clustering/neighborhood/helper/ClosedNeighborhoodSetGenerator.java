package elki.clustering.neighborhood.helper;

import java.util.List;

import elki.data.type.TypeInformation;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.utilities.optionhandling.OptionID;

/**
 * Closed neighborhood set generator.
 * 
 * @author Niklas Strahmann
 *
 * @param <O> Object type
 */
public interface ClosedNeighborhoodSetGenerator<O> {
  /**
   * Option ID for parameterization
   */
  static final OptionID CNS_GENERATOR_ID = new OptionID("clustering.neighborhood.generators", "Type of neighborhood relation to generate sets containing closed neighborhoods.");

  /**
   * Get the closed neighborhoods
   * 
   * @param relation Data relation
   * @return Neighborhood sets
   */
  List<DBIDs> getClosedNeighborhoods(Relation<? extends O> relation);

  /**
   * Input data type restriction.
   * 
   * @return Data type restriction
   */
  TypeInformation getInputTypeRestriction();
}
