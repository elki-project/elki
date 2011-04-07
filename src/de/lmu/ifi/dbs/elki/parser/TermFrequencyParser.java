package de.lmu.ifi.dbs.elki.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * A parser to load term frequency data, which essentially are sparse vectors
 * with text keys.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has SparseFloatVector
 */
// TODO: add a flag to perform TF normalization when using term counts
@Title("Term frequency parser")
@Description("Parse a file containing term frequencies. The expected format is 'label term1 <freq> term2 <freq> ...'. Terms must not contain the separator character!")
public class TermFrequencyParser extends NumberVectorLabelParser<SparseFloatVector> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(TermFrequencyParser.class);
  
  /**
   * Maximum dimension used
   */
  int maxdim;

  /**
   * Map
   */
  HashMap<String, Integer> keymap;
  
  /**
   * Constructor.
   *
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public TermFrequencyParser(Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar, labelIndices);
    this.maxdim = 0;
    this.keymap = new HashMap<String, Integer>();
  }

  @Override
  @SuppressWarnings("unused")
  protected SparseFloatVector createDBObject(List<Double> attributes) {
    throw new UnsupportedOperationException("This method should never be reached.");
  }

  @Override
  public Pair<SparseFloatVector, List<String>> parseLine(String line) {
    List<String> entries = tokenize(line);

    Map<Integer, Float> values = new TreeMap<Integer, Float>();
    List<String> labels = new ArrayList<String>();

    String curterm = null;
    for(int i = 0; i < entries.size(); i++) {
      if(curterm == null) {
        curterm = entries.get(i);
      }
      else {
        try {
          Float attribute = Float.valueOf(entries.get(i));
          Integer curdim = keymap.get(curterm);
          if(curdim == null) {
            curdim = maxdim + 1;
            keymap.put(curterm, curdim);
            maxdim += 1;
          }
          values.put(curdim, attribute);
          curterm = null;
        }
        catch(NumberFormatException e) {
          if(curterm != null) {
            labels.add(curterm);
          }
          curterm = entries.get(i);
        }
      }
    }
    if(curterm != null) {
      labels.add(curterm);
    }

    return new Pair<SparseFloatVector, List<String>>(new SparseFloatVector(values, maxdim), labels);
  }

  @Override
  public ParsingResult<SparseFloatVector> parse(InputStream in) {
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
      pair.getFirst().setDimensionality(maxdim);
    }
    return new ParsingResult<SparseFloatVector>(objectAndLabelsList, getPrototype(maxdim));
  }

  @Override
  protected SparseFloatVector getPrototype(int dimensionality) {
    return new SparseFloatVector(new int[] {}, new float[] {}, dimensionality);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends NumberVectorLabelParser.Parameterizer<SparseFloatVector> {
    @Override
    protected TermFrequencyParser makeInstance() {
      return new TermFrequencyParser(colSep, quoteChar, labelIndices);
    }
  }
}