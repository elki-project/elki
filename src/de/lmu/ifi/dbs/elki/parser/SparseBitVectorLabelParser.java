package de.lmu.ifi.dbs.elki.parser;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Provides a parser for parsing one sparse BitVector per line, where the
 * indices of the one-bits are separated by whitespace. The first index starts
 * with zero.
 * <p/>
 * Several labels may be given per BitVector, a label must not be parseable as
 * an Integer. Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Elke Achtert
 */
public class SparseBitVectorLabelParser extends AbstractParser<BitVector> implements Parameterizable {
  /**
   * Provides a parser for parsing one sparse BitVector per line, where the
   * indices of the one-bits are separated by whitespace.
   * <p/>
   * Several labels may be given per BitVector, a label must not be parseable as
   * an Integer. Lines starting with &quot;#&quot; will be ignored.
   */
  public SparseBitVectorLabelParser() {
    super();
  }

  public ParsingResult<BitVector> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 0;
    int dimensionality = -1;
    List<Pair<BitVector, List<String>>> objectAndLabelsList = new ArrayList<Pair<BitVector, List<String>>>();
    try {
      List<BitSet> bitSets = new ArrayList<BitSet>();
      List<List<String>> allLabels = new ArrayList<List<String>>();
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          String[] entries = WHITESPACE_PATTERN.split(line);
          BitSet bitSet = new BitSet();
          List<String> labels = new ArrayList<String>();

          for(String entry : entries) {
            try {
              Integer index = Integer.valueOf(entry);
              bitSet.set(index);
              dimensionality = Math.max(dimensionality, index);
            }
            catch(NumberFormatException e) {
              labels.add(entry);
            }
          }

          bitSets.add(bitSet);
          allLabels.add(labels);
        }
      }

      dimensionality++;
      for(int i = 0; i < bitSets.size(); i++) {
        BitSet bitSet = bitSets.get(i);
        List<String> labels = allLabels.get(i);
        Pair<BitVector, List<String>> objectAndLabels = new Pair<BitVector, List<String>>(new BitVector(bitSet, dimensionality), labels);
        objectAndLabelsList.add(objectAndLabels);
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<BitVector>(objectAndLabelsList);
  }

  @Override
  public Description getDescription() {
    return new Description(SparseBitVectorLabelParser.class, "Sparse Bit Vector Label Parser", "Parser for the lines of the following format:\n" + "A single line provides a single sparse BitVector. The indices of the one-bits are " + "separated by whitespace (" + WHITESPACE_PATTERN.pattern() + "). The first index starts with zero. Any substring not containing whitespace is tried to be read as an Integer. " + "If this fails, it will be appended to a label. (Thus, any label must not be parseable as an Integer.) " + "Empty lines and lines beginning with \"" + COMMENT + "\" will be ignored.");
  }
}