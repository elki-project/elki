package experimentalcode.hettab.outlier;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;
/**
 * 
 * @author Ahmed Hettab
 *
 * @param <V>
 */
public abstract class AbstractNeighborhoodOutlier<V extends NumberVector<?, ?>>  extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm  {
  /**
   * Parameter to specify the neighborhood predicate to use.
   */
  public static final OptionID NEIGHBORHOOD_ID = OptionID.getOrCreateOptionID("neighborhood", "The neighborhood predicate to use in comparison step.");

  /**
   * Our predicate to obtain the neighbors
   */
  private NeighborSetPredicate.Factory<V> npredf = null;
  /**
   * Constructor
   * 
   * @param npredf
   */
  public AbstractNeighborhoodOutlier(NeighborSetPredicate.Factory<V> npredf) {
    super();
    this.npredf = npredf;
  }

  /**
   * return the Neighborsetpredicate Factory
   */
  public NeighborSetPredicate.Factory<V> getNeighborSetPredicateFactory() {
    return npredf;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<V extends NumberVector<?,?>> extends AbstractParameterizer {
    
    protected NeighborSetPredicate.Factory<V> npredf = null ;

    @Override
    protected void makeOptions(Parameterization config) {

      final ObjectParameter<NeighborSetPredicate.Factory<V>> param = new ObjectParameter<NeighborSetPredicate.Factory<V>>(NEIGHBORHOOD_ID, NeighborSetPredicate.Factory.class, true);
      if(config.grab(param)) {
        npredf = param.instantiateClass(config);
      }

    }
  }

}

