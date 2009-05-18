/**
 * 
 */
package experimentalcode.arthur;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * @author Arthur Zimek
 *
 */
public class DoubleVectorProjectionParser extends MetaParser<DoubleVector> {

  private BitSet selectedAttributes = new BitSet();
  
  public static final OptionID SELECTED_ATTRIBUTES_ID = OptionID.getOrCreateOptionID("doublevectorprojectionparser.selectedattributes",
      "a comma separated array of integer values d_i, where 1 <= d_i <= the " +
      "dimensionality of the feature space " +
      "specifying the dimensions to be considered " +
      "for projection. If this parameter is not set, " +
      "no dimensions will be considered, i.e. the projection is a zero-dimensional feature space");
  
  /**
   * Selected attributes parameter.
   */
  private final IntListParameter SELECTED_ATTRIBUTES_PARAM = new IntListParameter(SELECTED_ATTRIBUTES_ID, new ListGreaterEqualConstraint<Integer>(1), true, null);

  public DoubleVectorProjectionParser(){
    addOption(SELECTED_ATTRIBUTES_PARAM);
  }
  
  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters(java.lang.String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    if (SELECTED_ATTRIBUTES_PARAM.isSet()) {
      this.selectedAttributes.clear();
      List<Integer> dimensionList = SELECTED_ATTRIBUTES_PARAM.getValue();
      for (int d : dimensionList) {
        this.selectedAttributes.set(d-1);
      }
    }
    return remainingParameters;
  }
  
  public void setSelectedDimensions(BitSet selectedAttributes) throws ParameterException{
    this.SELECTED_ATTRIBUTES_PARAM.setValue(Util.parseSelectedBits(selectedAttributes, ""));
    this.selectedAttributes.clear();
    this.selectedAttributes.or(selectedAttributes);
  }
  
  /**
   * @see de.lmu.ifi.dbs.elki.parser.Parser#parse(java.io.InputStream)
   */
  @Override
  public ParsingResult<DoubleVector> parse(InputStream in) {
    ParsingResult<DoubleVector> baseresult = this.retrieveBaseParsingresult(in);
    List<Pair<DoubleVector,List<String>>> projectedResult = new ArrayList<Pair<DoubleVector,List<String>>>(baseresult.size());
    int index = 0;
    for(Pair<DoubleVector,List<String>> pair : baseresult.getObjectAndLabelList()){
      Pair<DoubleVector,List<String>> newPair = new Pair<DoubleVector,List<String>>(Util.project(pair.getFirst(),selectedAttributes),pair.getSecond());
      projectedResult.add(index, newPair);
      index++;
    }
    return new ParsingResult<DoubleVector>(projectedResult);
  }

}
