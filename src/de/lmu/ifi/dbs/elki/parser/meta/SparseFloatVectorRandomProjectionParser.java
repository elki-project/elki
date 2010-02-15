package de.lmu.ifi.dbs.elki.parser.meta;

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * <p>Parser to project the ParsingResult obtained by a suitable base parser
 * onto a randomly selected subset of attributes.</p>
 *
 * @author Arthur Zimek
 */
public class SparseFloatVectorRandomProjectionParser extends RandomProjectionParser<SparseFloatVector> {
  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  protected SparseFloatVectorRandomProjectionParser(Parameterization config) {
    super(config);
  }

  /**
   * <p>Returns as ParsingResult a projection on a randomly selected subset of attributes.</p>
   * 
   * The specified InputStream is parsed by a base parser, the resulting ParsingResult is projected
   * on a randomly selected subset of attributes.
   * 
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
