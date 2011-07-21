package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.weighted;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

/**
 * Neighbor predicate with weight support.
 * 
 * @author Erich Schubert
 */
public interface WeightedNeighborSetPredicate {
  /**
   * Get the neighbors of a reference object for DBSCAN.
   * 
   * @param reference Reference object
   * @return Weighted Neighborhood
   */
  public Collection<DoubleObjPair<DBID>> getWeightedNeighbors(DBID reference);

  /**
   * Factory interface to produce instances.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.has WeightedNeighborSetPredicate
   * 
   * @param <O> Input relation object type restriction
   */
  public static interface Factory<O> extends Parameterizable {
    /**
     * Instantiation method.
     * 
     * @param relation Relation to instantiate for.
     * 
     * @return instance
     */
    public WeightedNeighborSetPredicate instantiate(Relation<? extends O> relation);

    /**
     * Get the input type information
     * 
     * @return input type
     */
    public TypeInformation getInputTypeRestriction();
  }
}