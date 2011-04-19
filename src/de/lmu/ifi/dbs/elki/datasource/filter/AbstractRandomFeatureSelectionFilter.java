package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.BitSet;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * A RandomProjectionParser selects a subset of attributes randomly for
 * projection of a ParsingResult.
 * </p>
 * 
 * The cardinality of the subset of attributes is specified as a parameter.
 * 
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * 
 * @param <V> the type of FeatureVector contained in both the original data of
 *        the base parser and the projected data of this ProjectionParser
 */
public abstract class AbstractRandomFeatureSelectionFilter<V extends FeatureVector<?, ?>> extends AbstractConversionFilter<V, V> {
  /**
   * The selected attributes
   */
  protected BitSet selectedAttributes = null;

  /**
   * Parameter for the desired cardinality of the subset of attributes selected
   * for projection.
   * 
   * <p>
   * Key: <code>-randomprojection.numberselected</code>
   * </p>
   * <p>
   * Default: <code>1</code>
   * </p>
   * <p>
   * Constraint: &ge;1
   * </p>
   */
  public static final OptionID NUMBER_SELECTED_ATTRIBUTES_ID = OptionID.getOrCreateOptionID("randomprojection.numberselected", "number of selected attributes");

  /**
   * Holds the desired cardinality of the subset of attributes selected for
   * projection.
   */
  protected int k;

  /**
   * Holds a random object.
   */
  protected final Random random = new Random();

  /**
   * Constructor.
   * 
   * @param dim dimensionality
   */
  public AbstractRandomFeatureSelectionFilter(int dim) {
    super();
    this.k = dim;
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<V> in) {
    int d = ((VectorFieldTypeInformation<V>) in).dimensionality();
    selectedAttributes = Util.randomBitSet(k, d, random);
    // We don't need the full loop, so return false.
    return false;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(NUMBER_SELECTED_ATTRIBUTES_ID, new GreaterEqualConstraint(1), 1);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }
  }
}