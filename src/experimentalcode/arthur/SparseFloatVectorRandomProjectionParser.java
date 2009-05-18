/**
 * 
 */
package experimentalcode.arthur;

import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * @author Arthur Zimek
 *
 */
public class SparseFloatVectorRandomProjectionParser extends RandomProjectionParser<SparseFloatVector> {


  /**
   * @see de.lmu.ifi.dbs.elki.parser.Parser#parse(java.io.InputStream)
   */
  @Override
  public ParsingResult<SparseFloatVector> parse(InputStream in) {
    ParsingResult<SparseFloatVector> baseparsingresult = this.retrieveBaseParsingresult(in);
    int d = baseparsingresult.getObjectAndLabelList().get(0).getFirst().getDimensionality();
    BitSet selectedAttributes = Util.randomBitSet(k, d, random);
    List<Pair<SparseFloatVector,List<String>>> projectedResult = new ArrayList<Pair<SparseFloatVector,List<String>>>(baseparsingresult.size());
    int index = 0;
    for(Pair<SparseFloatVector,List<String>> pair : baseparsingresult.getObjectAndLabelList()){
      Pair<SparseFloatVector,List<String>> newPair = new Pair<SparseFloatVector,List<String>>(Util.project(pair.getFirst(),selectedAttributes),pair.getSecond());
      projectedResult.add(index, newPair);
    }
    return new ParsingResult<SparseFloatVector>(projectedResult);
  }
  
  

}
