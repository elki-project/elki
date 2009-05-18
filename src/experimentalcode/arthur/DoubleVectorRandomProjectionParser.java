/**
 * 
 */
package experimentalcode.arthur;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

/**
 * @author Arthur Zimek
 *
 */
public class DoubleVectorRandomProjectionParser extends RandomProjectionParser<DoubleVector> {


  /**
   * @see de.lmu.ifi.dbs.elki.parser.Parser#parse(java.io.InputStream)
   */
  @Override
  public ParsingResult<DoubleVector> parse(InputStream in) {
    ParsingResult<DoubleVector> baseparsingresult = this.retrieveBaseParsingresult(in);
    int d = baseparsingresult.getObjectAndLabelList().get(0).getFirst().getDimensionality();
    BitSet selectedAttributes = Util.randomBitSet(k, d, random);
    List<Pair<DoubleVector,List<String>>> projectedResult = new ArrayList<Pair<DoubleVector,List<String>>>(baseparsingresult.size());
    int index = 0;
    for(Pair<DoubleVector,List<String>> pair : baseparsingresult.getObjectAndLabelList()){
      Pair<DoubleVector,List<String>> newPair = new Pair<DoubleVector,List<String>>(Util.project(pair.getFirst(),selectedAttributes),pair.getSecond());
      projectedResult.add(index, newPair);
    }
    return new ParsingResult<DoubleVector>(projectedResult);
  }
  
  

}
