package de.lmu.ifi.dbs.elki.parser.meta;

import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * <p>A RandomProjectionParser selects a subset of attributes randomly for projection of a ParsingResult.</p>
 * 
 * The cardinality of the subset of attributes is specified as a parameter.
 *
 *
 * @author Arthur Zimek
 *
 * @param <V> the type of RealVector contained in both the ParsingResult<V> of the base parser and the projected ParsingResult<V> of this ProjectionParser
 */
public abstract class RandomProjectionParser<V extends RealVector<V,?>> extends MetaParser<V> {

  /**
   * Holds the desired cardinality of the subset of attributes selected for projection.
   */
  protected int k;
  
  /**
   * ID for the parameter {@link #NUMBER_SELECTED_ATTRIBUTES_PARAM}.
   */
  public static final OptionID NUMBER_SELECTED_ATTRIBUTES_ID = OptionID.getOrCreateOptionID("randomprojection.numberselected", "number of selected attributes");
  
  /**
   * Parameter for the desired cardinality of the subset of attributes selected for projection.
   * 
   * <p>Key: <code>-randomprojection.numberselected</code></p>
   * <p>Default: <code>1</code></p>
   * <p>Constraint: &ge;1</p> 
   */
  private final IntParameter NUMBER_SELECTED_ATTRIBUTES_PARAM = new IntParameter(NUMBER_SELECTED_ATTRIBUTES_ID, new GreaterEqualConstraint(1), 1);
  
  /**
   * Holds a random object.
   */
  protected final Random random = new Random();
  
  /**
   * Adds the parameter {@link #NUMBER_SELECTED_ATTRIBUTES_PARAM}.
   */
  protected RandomProjectionParser(){
    addOption(NUMBER_SELECTED_ATTRIBUTES_PARAM);
  }
  
  /**
   * Calls the super method
   * and sets additionally the value of the parameter
   * {@link #NUMBER_SELECTED_ATTRIBUTES_PARAM}.
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
      List<String> remainingParameters = super.setParameters(args);

      // k
      k = NUMBER_SELECTED_ATTRIBUTES_PARAM.getValue();

      return remainingParameters;
  }


  
  

}
