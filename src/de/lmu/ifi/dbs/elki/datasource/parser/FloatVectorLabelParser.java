package de.lmu.ifi.dbs.elki.datasource.parser;

import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.FloatVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Util;

/**
 * <p>
 * Provides a parser for parsing one point per line, attributes separated by
 * whitespace.
 * </p>
 * <p>
 * Numerical values in a line will be parsed as double values but used in float
 * precision only.
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
 * 
 * @apiviz.has FloatVector
 */
public class FloatVectorLabelParser extends NumberVectorLabelParser<FloatVector> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(FloatVectorLabelParser.class);

  /**
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public FloatVectorLabelParser(Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar, labelIndices);
  }

  /**
   * Creates a FloatVector out of the given attribute values.
   * 
   * @see de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser#createDBObject(java.util.List)
   */
  @Override
  public FloatVector createDBObject(List<Double> attributes) {
    return new FloatVector(Util.convertToFloat(attributes));
  }

  @Override
  protected VectorFieldTypeInformation<FloatVector> getTypeInformation(int dimensionality) {
    return new VectorFieldTypeInformation<FloatVector>(FloatVector.class, dimensionality, new FloatVector(new float[dimensionality]));
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
  public static class Parameterizer extends NumberVectorLabelParser.Parameterizer<FloatVector> {
    @Override
    protected FloatVectorLabelParser makeInstance() {
      return new FloatVectorLabelParser(colSep, quoteChar, labelIndices);
    }
  }
}