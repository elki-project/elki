package de.lmu.ifi.dbs.converter;

import de.lmu.ifi.dbs.parser.AbstractParser;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.wrapper.StandAloneWrapper;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Converts an arff sparse instance file to a file readable with a
 * SparseBitVectorLabelParser. All lines beginning with @ will be ignored, there
 * is no check if the specified arff file is in a valid format.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ArffSparseInstanceToSparseBitVector extends StandAloneWrapper {
  /**
     * Holds the class specific debug status.
     */
    @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
    private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

    /**
     * The logger of this class.
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

  static {
    INPUT_D = "<filename>the arff-file to convert";
    OUTPUT_D = "<filename>the txt-file to write the converted arff-file in";
  }

  /**
   * A keyword used to denote a relation declaration.
   */
  private static final String ARFF_RELATION = "@relation";

  /**
   * A keyword used to denote an attribute declaration.
   */
  private static final String ARFF_ATTRIBUTE = "@attribute";

  /**
   * A keyword used to denote a data declaration.
   */
  private static final String ARFF_DATA = "@data";

  /**
   * Constant set for numeric attributes.
   */
  private static final int ARFF_ATTRIBUTE_TYPE_NUMERIC = 0;

  /**
   * Constant set for nominal attributes.
   */
  private static final int ARFF_ATTRIBUTE_TYPE_NOMINAL = 1;

  /**
   * Constant set for attributes with string values.
   */
  private static final int ARFF_ATTRIBUTE_TYPE_STRING = 2;

  /**
   * Constant set for attributes with date values.
   */
  private static final int ARFF_ATTRIBUTE_TYPE_DATE = 3;

  /**
   * A keyword used to denote a numeric attribute
   */
  static final String ARFF_ATTRIBUTE_INTEGER = "integer";

  /**
   * A keyword used to denote a numeric attribute
   */
  static final String ARFF_ATTRIBUTE_REAL = "real";

  /**
   * A keyword used to denote a numeric attribute
   */
  static final String ARFF_ATTRIBUTE_NUMERIC = "numeric";

  /**
   * The keyword used to denote a string attribute
   */
  static final String ARFF_ATTRIBUTE_STRING = "string";

  /**
   * The keyword used to denote a date attribute
   */
  static final String ARFF_ATTRIBUTE_DATE = "date";

  /**
   * A map containing the types of attributes.
   */
  private Map<Integer, Integer> attributeTypes;

  /**
   * A map containing the different nominal values for nominal attributes.
   */
  private Map<Integer, List<String>> nominalValues;

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    ArffSparseInstanceToSparseBitVector wrapper = new ArffSparseInstanceToSparseBitVector();
    try {
      wrapper.run(args);
    }
    catch (UnableToComplyException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
    catch (ParameterException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), cause);
    }
  }

  /**
   * Runs the wrapper with the specified arguments.
   *
   * @param args parameter list
   */
  public void run(String[] args) throws UnableToComplyException,
                                        ParameterException {
    try {
      optionHandler.grabOptions(args);

      BufferedReader reader = new BufferedReader(new InputStreamReader(
      new FileInputStream(getInput())));
      PrintStream writer = new PrintStream(new FileOutputStream(
      getOutput()));

      StreamTokenizer tokenizer = new StreamTokenizer(reader);
      initTokenizer(tokenizer);

      writeHeader(tokenizer, writer);
      while (writeSparseInstances(tokenizer, writer)) {
      }
    }
    catch (FileNotFoundException e) {
      throw new UnableToComplyException("FileNotFoundException occured."
                                        + e);
    }
    catch (IOException e) {
      throw new UnableToComplyException("IOException occured." + e);
    }
  }

  /**
   * Initializes the StreamTokenizer used for reading the ARFF sparse instance
   * file.
   *
   * @param tokenizer the stream tokenizer
   */
  private void initTokenizer(StreamTokenizer tokenizer) {
    tokenizer.resetSyntax();
    tokenizer.whitespaceChars(0, ' ');
    tokenizer.wordChars(' ' + 1, '\u00FF');
    tokenizer.whitespaceChars(',', ',');
    tokenizer.commentChar('%');
    tokenizer.quoteChar('"');
    tokenizer.quoteChar('\'');
    tokenizer.ordinaryChar('{');
    tokenizer.ordinaryChar('}');
    tokenizer.eolIsSignificant(true);
  }

  /**
   * Reads and stores header of an ARFF file.
   *
   * @param tokenizer the stream tokenizer
   * @throws java.io.IOException if the information is not read successfully
   */
  private void writeHeader(StreamTokenizer tokenizer, PrintStream writer)
  throws IOException {
    // get name of relation
    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      throw new IOException("premature end of file" + ", read "
                            + tokenizer.toString());
    }
    if (ArffSparseInstanceToSparseBitVector.ARFF_RELATION
    .equalsIgnoreCase(tokenizer.sval)) {
      getNextToken(tokenizer);
      String relationName = tokenizer.sval;
      getLastToken(tokenizer, false);
      writer.print(AbstractParser.COMMENT);
      writer.print(" relation: ");
      writer.println(relationName);
    }
    else {
      throw new IOException("keyword "
                            + ArffSparseInstanceToSparseBitVector.ARFF_RELATION
                            + " expected" + ", read " + tokenizer.toString());
    }

    // get attribute declarations
    writer.print(AbstractParser.COMMENT);
    writer.println(" Attributes as declared in arff-file:");
    writer.println(AbstractParser.COMMENT);
    attributeTypes = new HashMap<Integer, Integer>();
    nominalValues = new HashMap<Integer, List<String>>();
    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      throw new IOException("premature end of file" + ", read "
                            + tokenizer.toString());
    }

    int index = 0;
    while (ArffSparseInstanceToSparseBitVector.ARFF_ATTRIBUTE
    .equalsIgnoreCase(tokenizer.sval)) {
      // get attribute name
      getNextToken(tokenizer);
      String attributeName = tokenizer.sval;
      getNextToken(tokenizer);

      if (tokenizer.ttype == StreamTokenizer.TT_WORD) {
        String attributeType = tokenizer.sval;
        // Attribute is real, integer, or string.
        if (tokenizer.sval
        .equalsIgnoreCase(ArffSparseInstanceToSparseBitVector.ARFF_ATTRIBUTE_REAL)
            || tokenizer.sval
        .equalsIgnoreCase(ArffSparseInstanceToSparseBitVector.ARFF_ATTRIBUTE_INTEGER)
            || tokenizer.sval
        .equalsIgnoreCase(ArffSparseInstanceToSparseBitVector.ARFF_ATTRIBUTE_NUMERIC)) {
          attributeTypes.put(index, ARFF_ATTRIBUTE_TYPE_NUMERIC);
          readTillEOL(tokenizer);
        }
        else if (tokenizer.sval
        .equalsIgnoreCase(ArffSparseInstanceToSparseBitVector.ARFF_ATTRIBUTE_STRING)) {
          attributeTypes.put(index, ARFF_ATTRIBUTE_TYPE_STRING);
          readTillEOL(tokenizer);
        }
        else if (tokenizer.sval
        .equalsIgnoreCase(ArffSparseInstanceToSparseBitVector.ARFF_ATTRIBUTE_DATE)) {
          attributeTypes.put(index, ARFF_ATTRIBUTE_TYPE_DATE);
          if (tokenizer.nextToken() != StreamTokenizer.TT_EOL) {
            if ((tokenizer.ttype != StreamTokenizer.TT_WORD)
                && (tokenizer.ttype != '\'')
                && (tokenizer.ttype != '\"')) {
              throw new IOException("not a valid date format"
                                    + ", read " + tokenizer.toString());
            }
            readTillEOL(tokenizer);
          }
          else {
            tokenizer.pushBack();
          }
        }
        else {
          throw new IOException(
          "no valid attribute type or invalid enumeration"
          + ", read " + tokenizer.toString());
        }

        writer.print(AbstractParser.COMMENT);
        writer.print(" attribute ");
        writer.print(attributeName);
        writer.print(" ");
        writer.println(attributeType);
      }
      else {
        attributeTypes.put(index, ARFF_ATTRIBUTE_TYPE_NOMINAL);
        // attribute is nominal
        tokenizer.pushBack();
        List<String> attributeValues = new ArrayList<String>();
        nominalValues.put(index, attributeValues);

        // get values for nominal attribute
        if (tokenizer.nextToken() != '{') {
          throw new IOException(
          "{ expected at beginning of enumeration"
          + ", read " + tokenizer.toString());
        }
        while (tokenizer.nextToken() != '}') {
          if (tokenizer.ttype == StreamTokenizer.TT_EOL) {
            throw new IOException(
            "} expected at end of enumeration" + ", read "
            + tokenizer.toString());
          }
          else {
            attributeValues.add(tokenizer.sval);
          }
        }
        if (attributeValues.size() == 0) {
          throw new IOException("no nominal values found" + ", read "
                                + tokenizer.toString());
        }

        writer.print(AbstractParser.COMMENT);
        writer.print(" attribute ");
        writer.print(attributeName);
        writer.print(" ");
        writer.println(attributeValues);
      }

      index++;

      getLastToken(tokenizer, false);
      getFirstToken(tokenizer);
      if (tokenizer.ttype == StreamTokenizer.TT_EOF)
        throw new IOException("premature end of file" + ", read "
                              + tokenizer.toString());
    }

    // Check if data part follows. We can't easily check for EOL.
    if (!ArffSparseInstanceToSparseBitVector.ARFF_DATA
    .equalsIgnoreCase(tokenizer.sval)) {
      throw new IOException("keyword "
                            + ArffSparseInstanceToSparseBitVector.ARFF_DATA
                            + " expected" + ", read " + tokenizer.toString());
    }

    // Check if any attributes have been declared.
    if (attributeTypes.size() == 0) {
      throw new IOException("no attributes declared" + ", read "
                            + tokenizer.toString());
    }
  }

  /**
   * Gets next token, skipping empty lines.
   *
   * @param tokenizer the stream tokenizer
   * @throws IOException if reading the next token fails
   */
  private void getFirstToken(StreamTokenizer tokenizer) throws IOException {

    while (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
    }

    if ((tokenizer.ttype == '\'') || (tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    }
    else if ((tokenizer.ttype == StreamTokenizer.TT_WORD)
             && (tokenizer.sval.equals("?"))) {
      tokenizer.ttype = '?';
    }
  }

  /**
   * Gets next token, checking for a premature and of line.
   *
   * @param tokenizer the stream tokenizer
   * @throws IOException if it finds a premature end of line
   */
  private void getNextToken(StreamTokenizer tokenizer) throws IOException {

    if (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
      throw new IOException("premature end of file" + ", read "
                            + tokenizer.toString());
    }
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      throw new IOException("premature end of file" + ", read "
                            + tokenizer.toString());
    }
    else if ((tokenizer.ttype == '\'') || (tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    }
    else if ((tokenizer.ttype == StreamTokenizer.TT_WORD)
             && (tokenizer.sval.equals("?"))) {
      tokenizer.ttype = '?';
    }
  }

  /**
   * Gets token and checks if its end of line.
   *
   * @param tokenizer the stream tokenizer
   * @throws IOException if it doesn't find an end of line
   */
  private void getLastToken(StreamTokenizer tokenizer, boolean endOfFileOk)
  throws IOException {
    if ((tokenizer.nextToken() != StreamTokenizer.TT_EOL)
        && ((tokenizer.ttype != StreamTokenizer.TT_EOF) || !endOfFileOk)) {
      throw new IOException("end of line expected" + ", read "
                            + tokenizer.toString());
    }
  }

  /**
   * Reads and skips all tokens before next end of line token.
   *
   * @param tokenizer the stream tokenizer
   */
  private void readTillEOL(StreamTokenizer tokenizer) throws IOException {
    while (tokenizer.nextToken() != StreamTokenizer.TT_EOL) {
    }
    tokenizer.pushBack();
  }

  /**
   * Gets index, checking for a premature and of line.
   *
   * @param tokenizer the stream tokenizer
   * @throws IOException if it finds a premature end of line
   */
  private void getIndex(StreamTokenizer tokenizer) throws IOException {

    if (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
      throw new IOException("premature end of file" + ", read "
                            + tokenizer.toString());
    }
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      throw new IOException("premature end of file" + ", read "
                            + tokenizer.toString());
    }
  }

  /**
   * Reads a single instance using the tokenizer and appends it to the
   * dataset. Automatically expands the dataset if it is not large enough to
   * hold the instance.
   *
   * @param tokenizer the tokenizer to be used
   * @return false if end of file has been reached
   * @throws IOException if the information is not read successfully
   */
  private boolean writeSparseInstances(StreamTokenizer tokenizer,
                                       PrintStream writer) throws IOException {
    // Check if end of file reached.
    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      return false;
    }

    // Parse instance
    if (tokenizer.ttype == '{') {
      return writeSparseInstance(tokenizer, writer);
    }
    else {
      throw new IOException("{ expected" + ", read "
                            + tokenizer.toString());
    }
  }

  /**
   * Reads a single instance using the tokenizer and appends it to the
   * dataset. Automatically expands the dataset if it is not large enough to
   * hold the instance.
   *
   * @param tokenizer the tokenizer to be used
   * @return false if end of file has been reached
   * @throws IOException if the information is not read successfully
   */
  protected boolean writeSparseInstance(StreamTokenizer tokenizer,
                                        PrintStream writer) throws IOException {
    int column = 0;
    int maxIndex = -1;
    int index;
    StringBuffer label = new StringBuffer();
    List<Integer> indices = new ArrayList<Integer>();

    // Get values
    do {
      // get index
      getIndex(tokenizer);
      if (tokenizer.ttype == '}') {
        break;
      }

      // is index valid?
      try {
        index = Integer.valueOf(tokenizer.sval);
      }
      catch (NumberFormatException e) {
        throw new IOException("index number expected" + ", read "
                              + tokenizer.toString());
      }

      if (index <= maxIndex) {
        throw new IOException("indices have to be ordered" + ", read "
                              + tokenizer.toString());
      }
      if ((index < 0) || (index >= attributeTypes.size())) {
        throw new IOException("index out of bounds" + ", read "
                              + tokenizer.toString());
      }
      maxIndex = index;

      // Get value;
      getNextToken(tokenizer);

      // Check if value is missing.
      if (tokenizer.ttype == '?') {
        throw new IOException("missing values are not supported"
                              + ", read " + tokenizer.toString());
      }
      else {
        // Check if token is valid.
        if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
          throw new IOException("not a valid value" + ", read "
                                + tokenizer.toString());
        }
        int attributeType = attributeTypes.get(index);
        switch (attributeType) {
          case ARFF_ATTRIBUTE_TYPE_NOMINAL:
            // Check if value appears in header.
            int valIndex = nominalValues.get(index).indexOf(
            tokenizer.sval);
            if (valIndex == -1) {
              throw new IOException(
              "nominal value not declared in header"
              + ", read " + tokenizer.toString());
            }
            if (label.length() != 0) {
              label.append(AbstractParser.ATTRIBUTE_CONCATENATION
                           + tokenizer.sval);
            }
            else {
              label.append(tokenizer.sval);
            }
            break;

          case ARFF_ATTRIBUTE_TYPE_NUMERIC:
            // Check if value is really a number.
            try {
              Double value = Double.valueOf(tokenizer.sval);
              if (value == 1.0)
                indices.add(index);
              else if (value != 0.0) {
                throw new IOException("1.0 or 0.0 expected"
                                      + ", read " + tokenizer.toString());
              }
            }
            catch (NumberFormatException e) {
              throw new IOException("1.0 or 0.0 expected" + ", read "
                                    + tokenizer.toString());
            }
            break;

          case ARFF_ATTRIBUTE_TYPE_STRING:
            if (label.length() != 0) {
              label.append(AbstractParser.ATTRIBUTE_CONCATENATION
                           + tokenizer.sval);
            }
            else {
              label.append(tokenizer.sval);
            }
            break;

          case ARFF_ATTRIBUTE_TYPE_DATE:
            if (label.length() != 0) {
              label.append(AbstractParser.ATTRIBUTE_CONCATENATION
                           + tokenizer.sval);
            }
            else {
              label.append(tokenizer.sval);
            }
            break;

          default:
            throw new IOException("unknown attribute type in column "
                                  + column + ", read " + tokenizer.toString());
        }
      }
      column++;
    }
    while (true);

    getLastToken(tokenizer, true);

    for (int i = 0; i < indices.size(); i++) {
      Integer id = indices.get(i);
      if (i != 0) {
        writer.print(AbstractParser.ATTRIBUTE_CONCATENATION);
      }
      writer.print(id);
    }

    if (indices.size() != 0 && label.length() != 0)
      writer.print(AbstractParser.ATTRIBUTE_CONCATENATION);

    writer.print(label.toString());

    writer.println();

    return true;
  }
}
