/**
 * 
 */
package experimentalcode.arthur;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arthur Zimek
 *
 */
public class DoubleVectorProjectionParser extends ProjectionParser<DoubleVector> {

  
  /**
   * @see de.lmu.ifi.dbs.elki.parser.Parser#parse(java.io.InputStream)
   */
  @Override
  public ParsingResult<DoubleVector> parse(InputStream in) {
    ParsingResult<DoubleVector> baseresult = this.retrieveBaseParsingresult(in);
    List<Pair<DoubleVector,List<String>>> projectedResult = new ArrayList<Pair<DoubleVector,List<String>>>(baseresult.size());
    int index = 0;
    for(Pair<DoubleVector,List<String>> pair : baseresult.getObjectAndLabelList()){
      Pair<DoubleVector,List<String>> newPair = new Pair<DoubleVector,List<String>>(Util.project(pair.getFirst(),getSelectedAttributes()),pair.getSecond());
      projectedResult.add(index, newPair);
      index++;
    }
    return new ParsingResult<DoubleVector>(projectedResult);
  }

}
