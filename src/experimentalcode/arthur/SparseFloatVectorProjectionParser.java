/**
 * 
 */
package experimentalcode.arthur;

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
public class SparseFloatVectorProjectionParser extends ProjectionParser<SparseFloatVector> {

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.parser.Parser#parse(java.io.InputStream)
   */
  @Override
  public ParsingResult<SparseFloatVector> parse(InputStream in) {
    ParsingResult<SparseFloatVector> baseresult = this.retrieveBaseParsingresult(in);
    List<Pair<SparseFloatVector,List<String>>> projectedResult = new ArrayList<Pair<SparseFloatVector,List<String>>>(baseresult.size());
    int index = 0;
    for(Pair<SparseFloatVector,List<String>> pair : baseresult.getObjectAndLabelList()){
      Pair<SparseFloatVector,List<String>> newPair = new Pair<SparseFloatVector,List<String>>(Util.project(pair.getFirst(),getSelectedAttributes()),pair.getSecond());
      projectedResult.add(index, newPair);
      index++;
    }
    return new ParsingResult<SparseFloatVector>(projectedResult);
  }

}
