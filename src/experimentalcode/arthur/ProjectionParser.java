/**
 * 
 */
package experimentalcode.arthur;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.parser.meta.MetaParser;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;

import java.util.BitSet;
import java.util.List;

/**
 * @author Arthur Zimek
 *
 */
public abstract class ProjectionParser<V extends RealVector<V,?>> extends MetaParser<V> {

  private BitSet selectedAttributes = new BitSet();
  
  public static final OptionID SELECTED_ATTRIBUTES_ID = OptionID.getOrCreateOptionID("projectionparser.selectedattributes",
      "a comma separated array of integer values d_i, where 1 <= d_i <= the " +
      "dimensionality of the feature space " +
      "specifying the dimensions to be considered " +
      "for projection. If this parameter is not set, " +
      "no dimensions will be considered, i.e. the projection is a zero-dimensional feature space");
  
  /**
   * Selected attributes parameter.
   */
  private final IntListParameter SELECTED_ATTRIBUTES_PARAM = new IntListParameter(SELECTED_ATTRIBUTES_ID, new ListGreaterEqualConstraint<Integer>(1), true, null);

  public ProjectionParser(){
    addOption(SELECTED_ATTRIBUTES_PARAM);
  }
  
  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters(java.lang.String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    if (SELECTED_ATTRIBUTES_PARAM.isSet()) {
      this.getSelectedAttributes().clear();
      List<Integer> dimensionList = SELECTED_ATTRIBUTES_PARAM.getValue();
      for (int d : dimensionList) {
        this.getSelectedAttributes().set(d-1);
      }
    }
    return remainingParameters;
  }
  
  public void setSelectedAttributes(BitSet selectedAttributes) throws ParameterException{
    this.SELECTED_ATTRIBUTES_PARAM.setValue(Util.parseSelectedBits(selectedAttributes, ""));
    this.selectedAttributes.clear();
    this.selectedAttributes.or(selectedAttributes);
  }

  /**
   * @return the selectedAttributes
   */
  public BitSet getSelectedAttributes() {
    return selectedAttributes;
  }
  
  

}
