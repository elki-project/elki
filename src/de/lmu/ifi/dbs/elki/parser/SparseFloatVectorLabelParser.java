package de.lmu.ifi.dbs.elki.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p>
 * Provides a parser for parsing one point per line, attributes separated by
 * whitespace.
 * </p>
 * <p>
 * Several labels may be given per point. A label must not be parseable as
 * double. Lines starting with &quot;#&quot; will be ignored.
 * </p>
 * <p>
 * A line is expected in the following format: The first entry of each line is
 * the number of attributes with coordinate value not zero. Subsequent entries
 * are of the form (index, value), where index is the number of the
 * corresponding dimension, and value is the value of the corresponding
 * attribute.
 * </p>
 * <p>
 * An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.
 * </p>
 * 
 * @author Arthur Zimek
 */
@Title("Sparse Float Vector Label Parser")
@Description("Parser for the following line format:\n" + "A single line provides a single point. Entries are separated by whitespace. " + "The values will be parsed as floats (resulting in a set of SparseFloatVectors). A line is expected in the following format: The first entry of each line is the number of attributes with coordinate value not zero. Subsequent entries are of the form (index, value), where index is the number of the corresponding dimension, and value is the value of the corresponding attribute." + "Any pair of two subsequent substrings not containing whitespace is tried to be read as int and float. If this fails for the first of the pair (interpreted ans index), it will be appended to a label. (Thus, any label must not be parseable as Integer.) If the float component is not parseable, an exception will be thrown. Empty lines and lines beginning with \"#\" will be ignored. Having the file parsed completely, the maximum occuring dimensionality is set as dimensionality to all created SparseFloatvectors.")
public class SparseFloatVectorLabelParser extends NumberVectorLabelParser<SparseFloatVector> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SparseFloatVectorLabelParser(Parameterization config) {
    super(config);
  }

  /**
   * Holds the dimensionality of the parsed data which is the maximum occurring
   * index of any attribute.
   */
  private int dimensionality = -1;

  /**
   * Creates a DoubleVector out of the given attribute values.
   * 
   * @see de.lmu.ifi.dbs.elki.parser.NumberVectorLabelParser#createDBObject(java.util.List)
   */
  @Override
  public SparseFloatVector createDBObject(List<Double> attributes) {
    return new SparseFloatVector(Util.unboxToFloat(ClassGenericsUtil.toArray(attributes, Double.class)));
  }

  /**
   * @see de.lmu.ifi.dbs.elki.parser.NumberVectorLabelParser#parseLine(java.lang.String)
   */
  @Override
  public Pair<SparseFloatVector, List<String>> parseLine(String line) {
    String[] entries = WHITESPACE_PATTERN.split(line);
    int cardinality = Integer.parseInt(entries[0]);

    Map<Integer, Float> values = new HashMap<Integer, Float>(cardinality, 1);
    List<String> labels = new ArrayList<String>();

    for(int i = 1; i < entries.length - 1; i++) {
      if(!classLabelIndex.get(i)) {
        Integer index;
        Float attribute;
        try {
          index = Integer.valueOf(entries[i]);
          if(index > dimensionality) {
            dimensionality = index;
          }
          i++;
        }
        catch(NumberFormatException e) {
          labels.add(entries[i]);
          continue;
        }
        attribute = Float.valueOf(entries[i]);
        values.put(index, attribute);
      }
      else {
        labels.add(entries[i]);
      }
    }
    return new Pair<SparseFloatVector, List<String>>(new SparseFloatVector(values, dimensionality), labels);
  }

  /**
   * 
   * @see de.lmu.ifi.dbs.elki.parser.NumberVectorLabelParser#parse(java.io.InputStream)
   */
  @Override
  public ParsingResult<SparseFloatVector> parse(InputStream in) {
    dimensionality = -1;
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    List<Pair<SparseFloatVector, List<String>>> objectAndLabelsList = new ArrayList<Pair<SparseFloatVector, List<String>>>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          objectAndLabelsList.add(parseLine(line));
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }
    for(Pair<SparseFloatVector, List<String>> pair : objectAndLabelsList) {
      pair.getFirst().setDimensionality(dimensionality);
    }
    return new ParsingResult<SparseFloatVector>(objectAndLabelsList);
  }
}