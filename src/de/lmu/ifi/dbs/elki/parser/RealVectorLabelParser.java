package de.lmu.ifi.dbs.elki.parser;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FloatVector;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Provides a parser for parsing one point per line, attributes separated by
 * whitespace.</p>
 * <p/>
 * <p>The parser provides a parameter for parsing the real values as
 * doubles (default) (resulting in a {@link ParsingResult} of
 * {@link DoubleVector}s) or float (resulting in a {@link ParsingResult} of
 * {@link FloatVector}s).</p>
 * <p/>
 * <p>Several labels may be given per point. A label must not be parseable as
 * double (or float). Lines starting with &quot;#&quot; will be ignored.</p>
 * <p/>
 * <p>An index can be specified to identify an entry to be treated as class label.
 * This index counts all entries (numeric and labels as well) starting with 0.</p>
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector expected in the {@link ParsingResult}
 */
public class RealVectorLabelParser<V extends RealVector<V, ?>> extends AbstractParser<V> {

  /**
   * Option string for parameter float.
   */
  public static final String FLOAT_F = "float";

  /**
   * Description for parameter float.
   */
  public static final String FLOAT_D = "flag to specify parsing the real values as floats (default is double)";

  /**
   * If true, the real values are parsed as floats.
   */
  protected boolean parseFloat;

  /**
   * The parameter name for an index of a numerical class label.
   */
  public static final String CLASS_LABEL_INDEX_P = "numericalClassLabelIndex";

  /**
   * Description for the parameter numerical class label.
   */
  public static final String CLASS_LABEL_INDEX_D = "(optional) index of a class label (may be numeric), " +
      "counting whitespace separated entries in a line starting with 0 - " +
      "the corresponding entry will be treated as a label. " +
      "To actually set this label as class label, use also the parametrization of " +
      AbstractDatabaseConnection.class.getCanonicalName() + " -" +
      AbstractDatabaseConnection.CLASS_LABEL_INDEX_ID.getName() + " -" +
      AbstractDatabaseConnection.CLASS_LABEL_CLASS_ID.getName();

  /**
   * The parameter for an index of a numerical class label.
   * The corresponding numerical value is treated as string label
   * an can be selected as class label by the {@link AbstractDatabaseConnection}.
   * A non-numerical class label can be directly selected from the labels after parsing via the
   * corresponding parameter of the {@link AbstractDatabaseConnection}:
   * {@link AbstractDatabaseConnection#CLASS_LABEL_INDEX_PARAM}.
   * <p/>
   * The parameter is optional and the default value is set to -1.
   */
  public static final IntParameter CLASS_LABEL_INDEX_PARAM = new IntParameter(CLASS_LABEL_INDEX_P, CLASS_LABEL_INDEX_D);

  static {
    CLASS_LABEL_INDEX_PARAM.setDefaultValue(-1);
    CLASS_LABEL_INDEX_PARAM.setOptional(true);
  }

  /**
   * Keeps the index of an attribute to be treated as a string label.
   */
  private int classLabelIndex;


  /**
   * Provides a parser for parsing one point per line, attributes separated by
   * whitespace. <p/> Several labels may be given per point. A label must not
   * be parseable as double (or float). Lines starting with &quot;#&quot; will
   * be ignored.
   */
  public RealVectorLabelParser() {
    super();
    debug = true;
    optionHandler.put(new Flag(FLOAT_F, FLOAT_D));
    optionHandler.put(CLASS_LABEL_INDEX_PARAM);
  }

  public ParsingResult<V> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    int dimensionality = -1;
    List<ObjectAndLabels<V>> objectAndLabelsList = new ArrayList<ObjectAndLabels<V>>();
    try {
      for (String line; (line = reader.readLine()) != null; lineNumber++) {
        if (!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          List<Double> attributes = new ArrayList<Double>();
          List<String> labels = new ArrayList<String>();
          for (int i = 0; i < entries.length; i++) {
            if (i != classLabelIndex) {
              try {
                Double attribute = Double.valueOf(entries[i]);
                attributes.add(attribute);
              }
              catch (NumberFormatException e) {
                labels.add(entries[i]);
              }
            }
            else {
              labels.add(entries[i]);
            }
          }

          if (dimensionality < 0) {
            dimensionality = attributes.size();
          }
          else if (dimensionality != attributes.size()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ":" + attributes.size() + " != " + dimensionality);
          }

          //V featureVector;
          ObjectAndLabels<V> objectAndLabel;
          if (parseFloat) {
            //featureVector = (V) new FloatVector(Util.convertToFloat(attributes));
            objectAndLabel = new ObjectAndLabels(new FloatVector(Util.convertToFloat(attributes)), labels);
          }
          else {
            //featureVector = (V) new DoubleVector(attributes);
            objectAndLabel = new ObjectAndLabels(new DoubleVector(attributes), labels);
          }
          //ObjectAndLabels<V> objectAndLabel = new ObjectAndLabels<V>(featureVector, labels);
          objectAndLabelsList.add(objectAndLabel);
        }
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<V>(objectAndLabelsList);
  }

  @Override
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(RealVectorLabelParser.class.getName());
    description.append(" expects following format of parsed lines:\n");
    description.append("A single line provides a single point. Attributes are separated by whitespace (");
    description.append(WHITESPACE_PATTERN.pattern() + "). ");
    description.append("If parameter " + FLOAT_F + " is set, the real values will be parsed as floats (resulting in a set of FloatVectors), " + "otherwise the real values will be parsed as as doubles (resulting in a set of DoubleVectors -- default).");
    description.append("Any substring not containing whitespace is tried to be read as double (or float). " + "If this fails, it will be appended to a label. (Thus, any label must not be parseable " + "as double nor as float.) Empty lines and lines beginning with \"");
    description.append(COMMENT);
    description.append("\" will be ignored. If any point differs in its dimensionality from other points, " + "the parse method will fail with an Exception.\n");

    return usage(description.toString());
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParams = super.setParameters(args);
    parseFloat = optionHandler.isSet(FLOAT_F);
    if (optionHandler.isSet(CLASS_LABEL_INDEX_PARAM)) {
      classLabelIndex = getParameterValue(CLASS_LABEL_INDEX_PARAM);
    }
    else {
      classLabelIndex = CLASS_LABEL_INDEX_PARAM.getDefaultValue();
    }

    return remainingParams;
  }
}
