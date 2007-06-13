package de.lmu.ifi.dbs.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.data.Bit;
import de.lmu.ifi.dbs.data.BitVector;


/**
 * Provides a parser for parsing one BitVector per line, bits separated by whitespace.
 * <p/>
 * Several labels may be given per BitVector.
 * A label must not be parseable as Bit.
 * Lines starting with &quot;#&quot; will be ignored.
 *
 * @author Arthur Zimek
 */
public class BitVectorLabelParser extends AbstractParser<BitVector> {
  /**
   * Provides a parser for parsing one BitVector per line, bits separated by whitespace.
   * <p/>
   * Several labels may be given per BitVector.
   * A label must not be parseable as Bit.
   * Lines starting with &quot;#&quot; will be ignored.
   */
  public BitVectorLabelParser() {
    super();
  }

  /**
   * @see Parser#parse(java.io.InputStream)
   */
  public ParsingResult<BitVector> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    int dimensionality = -1;
    List<ObjectAndLabels<BitVector>> objectAndLabelsList = new ArrayList<ObjectAndLabels<BitVector>>();
    try {
      for (String line; (line = reader.readLine()) != null; lineNumber++) {
        if (!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          List<Bit> attributes = new ArrayList<Bit>();
          List<String> labels = new ArrayList<String>();
          for (String entry : entries) {
            try {
              Bit attribute = Bit.valueOf(entry);
              attributes.add(attribute);
            }
            catch (NumberFormatException e) {
              labels.add(entry);
            }
          }

          if (dimensionality < 0) {
            dimensionality = attributes.size();
          }
          else if (dimensionality != attributes.size()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ".");
          }

          ObjectAndLabels<BitVector> objectAndLabels = new ObjectAndLabels<BitVector>(new BitVector(attributes.toArray(new Bit[attributes.size()])), labels);
          objectAndLabelsList.add(objectAndLabels);
        }
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<BitVector>(objectAndLabelsList);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(BitVectorLabelParser.class.getName());
    description.append(" expects following format of parsed lines:\n");
    description.append("A single line provides a single BitVector. Bits are separated by whitespace (");
    description.append(WHITESPACE_PATTERN.pattern());
    description.append("). Any substring not containing whitespace is tried to be read as Bit. If this fails, it will be appended to a label. (Thus, any label must not be parseable as Bit.) Empty lines and lines beginning with \"");
    description.append(COMMENT);
    description.append("\" will be ignored. If any BitVector differs in its dimensionality from other BitVectors, the parse method will fail with an Exception.\n");

    return usage(description.toString());
  }

}
