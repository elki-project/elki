package de.lmu.ifi.dbs.elki.parser.meta;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p>
 * Parser to project the ParsingResult obtained by a suitable base parser onto a
 * selected subset of attributes.
 * </p>
 * 
 * 
 * @author Arthur Zimek
 * 
 */
public class SparseFloatVectorProjectionParser extends ProjectionParser<SparseFloatVector> {
  /**
   * Constructor.
   * 
   * @param baseparser
   * @param selectedAttributes
   */
  public SparseFloatVectorProjectionParser(Parser<SparseFloatVector> baseparser, BitSet selectedAttributes) {
    super(baseparser, selectedAttributes);
  }

  /**
   * <p>
   * Returns as ParsingResult a projection on a selected subset of attributes.
   * </p>
   * 
   * The specified InputStream is parsed by a base parser, the resulting
   * ParsingResult is projected on a selected subset of attributes.
   * 
   * @see de.lmu.ifi.dbs.elki.parser.Parser#parse(java.io.InputStream)
   */
  @Override
  public ParsingResult<SparseFloatVector> parse(InputStream in) {
    ParsingResult<SparseFloatVector> baseresult = this.retrieveBaseParsingresult(in);
    List<Pair<SparseFloatVector, List<String>>> projectedResult = new ArrayList<Pair<SparseFloatVector, List<String>>>(baseresult.size());
    int index = 0;
    for(Pair<SparseFloatVector, List<String>> pair : baseresult.getObjectAndLabelList()) {
      Pair<SparseFloatVector, List<String>> newPair = new Pair<SparseFloatVector, List<String>>(Util.project(pair.getFirst(), getSelectedAttributes()), pair.getSecond());
      projectedResult.add(index, newPair);
      index++;
    }
    final Map<Integer, Float> emptyMap = Collections.emptyMap();
    return new ParsingResult<SparseFloatVector>(projectedResult, new SparseFloatVector(emptyMap, getDimensionality()));
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends ProjectionParser.Parameterizer<SparseFloatVector> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected SparseFloatVectorProjectionParser makeInstance() {
      return new SparseFloatVectorProjectionParser(baseparser, selectedAttributes);
    }
  }
}