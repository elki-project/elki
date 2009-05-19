/**
 * 
 */
package experimentalcode.arthur;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.parser.meta.MetaParser;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.Random;

/**
 * @author Arthur Zimek
 *
 */
public abstract class RandomProjectionParser<V extends RealVector<V,?>> extends MetaParser<V> {
  
  protected int k;
  
  public static final OptionID NUMBER_SELECTED_ATTRIBUTES_ID = OptionID.getOrCreateOptionID("randomprojection.numberselected", "number of selected attributes");
  
  private final IntParameter NUMBER_SELECTED_ATTRIBUTES_PARAM = new IntParameter(NUMBER_SELECTED_ATTRIBUTES_ID, new GreaterEqualConstraint(1), 1);
  
  protected final Random random = new Random();
  
  public RandomProjectionParser(){
    addOption(NUMBER_SELECTED_ATTRIBUTES_PARAM);
  }
  
  /**
   * Calls the super method
   * and sets additionally the value of the parameter
   * {@link #NUMBER_SELECTED_ATTRIBUTES_PARAM}.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
      String[] remainingParameters = super.setParameters(args);

      // k
      k = NUMBER_SELECTED_ATTRIBUTES_PARAM.getValue();

      return remainingParameters;
  }


  
  

}
