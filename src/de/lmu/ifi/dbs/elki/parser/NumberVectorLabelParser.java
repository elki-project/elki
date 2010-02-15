package de.lmu.ifi.dbs.elki.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
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
 * An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.
 * </p>
 * 
 * @author Arthur Zimek
 * @param <V> the type of NumberVector expected in the {@link ParsingResult}
 */
public abstract class NumberVectorLabelParser<V extends NumberVector<?, ?>> extends AbstractParser<V> implements LinebasedParser<V>, Parameterizable {
  /**
   * OptionID for {@link #CLASS_LABEL_INDEX_PARAM}
   */
  private static final OptionID CLASS_LABEL_INDEX_ID = OptionID.getOrCreateOptionID("parser.classLabelIndex", "Index of a class label (may be numeric), " + "counting whitespace separated entries in a line starting with 0 - " + "the corresponding entry will be treated as a label. ");

  /**
   * The parameter for an index of a numerical class label. The corresponding
   * numerical value is treated as string label an can be selected as class
   * label by the {@link AbstractDatabaseConnection}. A non-numerical class
   * label can be directly selected from the labels after parsing via the
   * corresponding parameter of the {@link AbstractDatabaseConnection}:
   * {@link AbstractDatabaseConnection#CLASS_LABEL_INDEX_PARAM}.
   * <p/>
   * The parameter is optional and the default value is set to -1.
   */
  private final IntListParameter CLASS_LABEL_INDEX_PARAM = new IntListParameter(CLASS_LABEL_INDEX_ID, true);

  /**
   * Keeps the index of an attribute to be treated as a string label.
   */
  protected BitSet classLabelIndex;

  /**
   * Provides a parser for parsing one point per line, attributes separated by
   * whitespace.
   * <p/>
   * Several labels may be given per point. A label must not be parseable as
   * double (or float). Lines starting with &quot;#&quot; will be ignored.
   */
  public NumberVectorLabelParser(Parameterization config) {
    super();
    classLabelIndex = new BitSet();
    if (config.grab(this, CLASS_LABEL_INDEX_PARAM)) {
      List<Integer> labelcols = CLASS_LABEL_INDEX_PARAM.getValue();
      for (Integer idx : labelcols) {
        classLabelIndex.set(idx);
      }
    }
  }

  public ParsingResult<V> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    int dimensionality = -1;
    List<Pair<V, List<String>>> objectAndLabelsList = new ArrayList<Pair<V, List<String>>>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          Pair<V, List<String>> objectAndLabels = parseLine(line);
          if(dimensionality < 0) {
            dimensionality = objectAndLabels.getFirst().getDimensionality();
          }
          else if(dimensionality != objectAndLabels.getFirst().getDimensionality()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ":" + objectAndLabels.getFirst().getDimensionality() + " != " + dimensionality);
          }
          objectAndLabelsList.add(objectAndLabels);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<V>(objectAndLabelsList);
  }

  /*
   * Parse a single line into an object and labels
   */
  public Pair<V, List<String>> parseLine(String line) {
    String[] entries = WHITESPACE_PATTERN.split(line);
    List<Double> attributes = new ArrayList<Double>();
    List<String> labels = new ArrayList<String>();
    for(int i = 0; i < entries.length; i++) {
      if(!classLabelIndex.get(i)) {
        try {
          Double attribute = Double.valueOf(entries[i]);
          attributes.add(attribute);
        }
        catch(NumberFormatException e) {
          labels.add(entries[i]);
        }
      }
      else {
        labels.add(entries[i]);
      }
    }

    Pair<V, List<String>> objectAndLabels;
    V vec = createDBObject(attributes);
    /*
    if(parseFloat) {
      vec = (V) new FloatVector(Util.convertToFloat(attributes));
    }
    else {
      vec = (V) new DoubleVector(attributes);
    }
    */
    objectAndLabels = new Pair<V, List<String>>(vec, labels);
    return objectAndLabels;
  }
  
  /**
   * <p>Creates a database object of type V.</p>
   * 
   * @param attributes the attributes of the vector to create.
   * @return a RalVector of type V containing the given attribute values
   */
  protected abstract V createDBObject(List<Double> attributes);

  @Override
  public String shortDescription() {
    StringBuffer description = new StringBuffer();
    description.append(this.getClass().getName());
    description.append(" expects following format of parsed lines:\n");
    description.append("A single line provides a single point. Attributes are separated by whitespace (");
    description.append(WHITESPACE_PATTERN.pattern()).append("). ");
    description.append(descriptionLineType());
    description.append("Any substring not containing whitespace is tried to be read as double (or float). " + "If this fails, it will be appended to a label. (Thus, any label must not be parseable " + "as double nor as float.) Empty lines and lines beginning with \"");
    description.append(COMMENT);
    description.append("\" will be ignored. If any point differs in its dimensionality from other points, " + "the parse method will fail with an Exception.\n");

    return description.toString();
  }
  
  protected abstract String descriptionLineType();
}
