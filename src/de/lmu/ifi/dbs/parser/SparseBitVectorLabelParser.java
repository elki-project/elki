package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.BitVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Provides a parser for parsing one sparse BitVector per line,
 * where the indices of the one-bits are separated by whitespace.
 * The first index starts with zero.
 * <p/>
 * Several labels may be given per BitVector, a label must not be parseable as an Integer.
 * Lines starting with &quot;#&quot; will be ignored.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SparseBitVectorLabelParser extends AbstractParser<BitVector> {

  /**
   * Provides a parser for parsing one sparse BitVector per line,
   * where the indices of the one-bits are separated by whitespace.
   * <p/>
   * Several labels may be given per BitVector, a label must not be parseable as an Integer.
   * Lines starting with &quot;#&quot; will be ignored.
   */
  public SparseBitVectorLabelParser() {
    super();
  }

  /**
   * @see de.lmu.ifi.dbs.parser.Parser#parse(java.io.InputStream)
   */
  public ParsingResult<BitVector> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    int dimensionality = -1;
    List<BitVector> objects = new ArrayList<BitVector>();
    List<String> labels = new ArrayList<String>();
    try {
      List<BitSet> bitSets = new ArrayList<BitSet>();
      for (String line; (line = reader.readLine()) != null; lineNumber++) {
        if (!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          BitSet bitSet = new BitSet();
          StringBuffer label = new StringBuffer();
          for (String entry : entries) {
            try {
              Integer index = Integer.valueOf(entry);
              bitSet.set(index);
              dimensionality = Math.max(dimensionality, index);
            }
            catch (NumberFormatException e) {
              if (label.length() > 0) {
                label.append(LABEL_CONCATENATION);
              }
              label.append(entry);
            }
          }
          bitSets.add(bitSet);
          labels.add(label.toString());
        }
      }

      dimensionality++;
      for (BitSet bitSet : bitSets) {
        objects.add(new BitVector(bitSet, dimensionality));
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<BitVector>(objects, labels);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(SparseBitVectorLabelParser.class.getName());
    description.append(" expects following format of parsed lines:\n");
    description.append("A single line provides a single sparse BitVector. The indices of the one-bits are " +
                       "separated by whitespace (");
    description.append(WHITESPACE_PATTERN.pattern());
    description.append("). The first index starts with zero. Any substring not containing whitespace is tried to be read as an Integer. " +
                       "If this fails, it will be appended to a label. (Thus, any label must not be parseable as an Integer.) " +
                       "Empty lines and lines beginning with \"");
    description.append(COMMENT);
    description.append("\" will be ignored. \n");

    return usage(description.toString());
  }

}
