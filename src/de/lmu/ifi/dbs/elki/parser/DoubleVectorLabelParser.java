package de.lmu.ifi.dbs.elki.parser;

import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * <p>
 * Provides a parser for parsing one point per line, attributes separated by
 * whitespace.
 * </p>
 * <p>
 * Several labels may be given per point. A label must not be parseable as
 * double. Lines starting with &quot;#&quot; will be ignored.
 * </p>
 * <p/>
 * <p>
 * An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.
 * </p>
 * 
 * @author Arthur Zimek
 */
public class DoubleVectorLabelParser extends NumberVectorLabelParser<DoubleVector> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(DoubleVectorLabelParser.class);

  /**
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public DoubleVectorLabelParser(Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar, labelIndices);
  }

  /**
   * Creates a DoubleVector out of the given attribute values.
   * 
   * @see de.lmu.ifi.dbs.elki.parser.NumberVectorLabelParser#createDBObject(java.util.List)
   */
  @Override
  public DoubleVector createDBObject(List<Double> attributes) {
    return new DoubleVector(attributes);
  }

  @Override
  protected DoubleVector getPrototype(int dimensionality) {
    return new DoubleVector(new double[dimensionality]);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends NumberVectorLabelParser.Parameterizer<DoubleVector> {
    @Override
    protected DoubleVectorLabelParser makeInstance() {
      return new DoubleVectorLabelParser(colSep, quoteChar, labelIndices);
    }
  }
}