package de.lmu.ifi.dbs.elki.parser.meta;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.parser.Parser;
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
 * onto a selected subset of attributes.</p>
 * 
 * @author Arthur Zimek
 */
public class DoubleVectorProjectionParser extends ProjectionParser<DoubleVector> {
  /**
   * Constructor.
   *
   * @param baseparser
   * @param selectedAttributes
   */
  public DoubleVectorProjectionParser(Parser<DoubleVector> baseparser, BitSet selectedAttributes) {
    super(baseparser, selectedAttributes);
  }

  /**
   * <p>Returns as ParsingResult a projection on a selected subset of attributes.</p>
   * 
   * The specified InputStream is parsed by a base parser, the resulting ParsingResult is projected
   * on a selected subset of attributes.
   * 
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
    return new ParsingResult<DoubleVector>(projectedResult, new DoubleVector(new double[getDimensionality()]));
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends ProjectionParser.Parameterizer<DoubleVector> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected DoubleVectorProjectionParser makeInstance() {
      return new DoubleVectorProjectionParser(baseparser, selectedAttributes);
    }
  }
}