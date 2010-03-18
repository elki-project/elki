package de.lmu.ifi.dbs.elki.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Bit;
import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a parser for parsing one BitVector per line, bits separated by
 * whitespace.
 * <p/>
 * Several labels may be given per BitVector. A label must not be parseable as
 * Bit. Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Arthur Zimek
 */
@Title("Bit Vector Label Parser")
@Description("Parses the following format of lines:\n" + "A single line provides a single BitVector. Bits are separated by whitespace. Any substring not containing whitespace is tried to be read as Bit. If this fails, it will be appended to a label. (Thus, any label must not be parseable as Bit.) Empty lines and lines beginning with \"#\" will be ignored. If any BitVector differs in its dimensionality from other BitVectors, the parse method will fail with an Exception.")
public class BitVectorLabelParser extends AbstractParser<BitVector> implements Parameterizable {
  /**
   * Provides a parser for parsing one BitVector per line, bits separated by
   * whitespace.
   * <p/>
   * Several labels may be given per BitVector. A label must not be parseable as
   * Bit. Lines starting with &quot;#&quot; will be ignored.
   */
  public BitVectorLabelParser() {
    super();
  }

  public ParsingResult<BitVector> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    int dimensionality = -1;
    List<Pair<BitVector, List<String>>> objectAndLabelsList = new ArrayList<Pair<BitVector, List<String>>>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          List<Bit> attributes = new ArrayList<Bit>();
          List<String> labels = new ArrayList<String>();
          for(String entry : entries) {
            try {
              Bit attribute = Bit.valueOf(entry);
              attributes.add(attribute);
            }
            catch(NumberFormatException e) {
              labels.add(entry);
            }
          }

          if(dimensionality < 0) {
            dimensionality = attributes.size();
          }
          else if(dimensionality != attributes.size()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ".");
          }

          Pair<BitVector, List<String>> objectAndLabels = new Pair<BitVector, List<String>>(new BitVector(attributes.toArray(new Bit[attributes.size()])), labels);
          objectAndLabelsList.add(objectAndLabels);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<BitVector>(objectAndLabelsList);
  }
}