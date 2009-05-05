package de.lmu.ifi.dbs.elki.parser;

import de.lmu.ifi.dbs.elki.data.FloatVector;
import de.lmu.ifi.dbs.elki.utilities.Util;

import java.util.List;

/**
 * <p>
 * Provides a parser for parsing one point per line, attributes separated by
 * whitespace.
 * </p>
 * <p>Numerical values in a line will be parsed as double values but used in float precision only.</p>
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
public class FloatVectorLabelParser extends RealVectorLabelParser<FloatVector> {

  /**
   * Creates a FloatVector out of the given attribute values.
   * 
   * @see de.lmu.ifi.dbs.elki.parser.RealVectorLabelParser#createDBObject(java.util.List)
   */
  @Override
  public FloatVector createDBObject(List<Double> attributes) {
    return new FloatVector(Util.convertToFloat(attributes));
  }
  
  @Override
  protected String descriptionLineType() {
    return "The values will be parsed as floats (resulting in a set of FloatVectors).";
  }
}
