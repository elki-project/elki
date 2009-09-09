package de.lmu.ifi.dbs.elki.parser;

import de.lmu.ifi.dbs.elki.data.DoubleVector;

import java.util.List;

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
   * Creates a DoubleVector out of the given attribute values.
   * 
   * @see de.lmu.ifi.dbs.elki.parser.NumberVectorLabelParser#createDBObject(java.util.List)
   */
  @Override
  public DoubleVector createDBObject(List<Double> attributes) {
    return new DoubleVector(attributes);
  }

  @Override
  protected String descriptionLineType() {
    return "The values will be parsed as doubles (resulting in a set of DoubleVectors).";
  }

  
  
}
