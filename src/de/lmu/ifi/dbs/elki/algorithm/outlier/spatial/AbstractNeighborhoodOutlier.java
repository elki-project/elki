package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for spatial outlier detection methods using a spatial
 * neighborhood.
 * 
 * @author Ahmed Hettab
 * 
 * @param <O> Object type
 */
public abstract class AbstractNeighborhoodOutlier<O> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * Parameter to specify the neighborhood predicate to use.
   */
  public static final OptionID NEIGHBORHOOD_ID = OptionID.getOrCreateOptionID("neighborhood", "The neighborhood predicate to use in comparison step.");

  /**
   * Our predicate to obtain the neighbors
   */
  private NeighborSetPredicate.Factory<O> npredf = null;

  /**
   * Constructor
   * 
   * @param npredf Neighborhood predicate
   */
  public AbstractNeighborhoodOutlier(NeighborSetPredicate.Factory<O> npredf) {
    super();
    this.npredf = npredf;
  }

  /**
   * Get the predicate to obtain the neighbors.
   * 
   * @return predicate to obtain the neighbors
   */
  protected NeighborSetPredicate.Factory<O> getNeighborSetPredicateFactory() {
    return npredf;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<O> extends AbstractParameterizer {
    /**
     * The predicate to obtain the neighbors.
     */
    protected NeighborSetPredicate.Factory<O> npredf = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<NeighborSetPredicate.Factory<O>> param = new ObjectParameter<NeighborSetPredicate.Factory<O>>(NEIGHBORHOOD_ID, NeighborSetPredicate.Factory.class);
      if(config.grab(param)) {
        npredf = param.instantiateClass(config);
      }
    }
  }
}