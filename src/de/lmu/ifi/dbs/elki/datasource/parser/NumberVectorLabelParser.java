package de.lmu.ifi.dbs.elki.datasource.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
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
 * @param <V> the type of NumberVector used
 */
public abstract class NumberVectorLabelParser<V extends NumberVector<?, ?>> extends AbstractParser implements LinebasedParser, Parser {
  /**
   * A comma separated list of the indices of labels (may be numeric), counting
   * whitespace separated entries in a line starting with 0. The corresponding
   * entries will be treated as a label.
   * <p>
   * Key: {@code -parser.labelIndices}
   * </p>
   */
  public static final OptionID LABEL_INDICES_ID = OptionID.getOrCreateOptionID("parser.labelIndices", "A comma separated list of the indices of labels (may be numeric), counting whitespace separated entries in a line starting with 0. The corresponding entries will be treated as a label.");

  /**
   * Keeps the indices of the attributes to be treated as a string label.
   */
  protected BitSet labelIndices;

  /**
   * Constructor
   * 
   * @param colSep
   * @param quoteChar
   * @param labelIndices
   */
  public NumberVectorLabelParser(Pattern colSep, char quoteChar, BitSet labelIndices) {
    super(colSep, quoteChar);
    this.labelIndices = labelIndices;
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    int dimensionality = -1;
    List<Object> folded = new ArrayList<Object>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          Pair<V, LabelList> objectAndLabels = parseLineInternal(line);
          if(dimensionality < 0) {
            dimensionality = objectAndLabels.getFirst().getDimensionality();
          }
          else if(dimensionality != objectAndLabels.getFirst().getDimensionality()) {
            throw new IllegalArgumentException("Differing dimensionality in line " + lineNumber + ":" + objectAndLabels.getFirst().getDimensionality() + " != " + dimensionality);
          }
          folded.add(objectAndLabels.first);
          folded.add(objectAndLabels.second);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    BundleMeta meta = new BundleMeta();
    meta.add(getTypeInformation(dimensionality));
    meta.add(TypeUtil.LABELLIST);
    return new MultipleObjectsBundle(meta, folded);
  }

  @Override
  public SingleObjectBundle parseLine(String line) {
    Pair<V, LabelList> objectAndLabels = parseLineInternal(line);
    SingleObjectBundle pkg = new SingleObjectBundle();
    pkg.append(getTypeInformation(objectAndLabels.first.getDimensionality()), objectAndLabels.first);
    pkg.append(TypeUtil.LABELLIST, objectAndLabels.second);
    return pkg;
  }

  /**
   * Internal method for parsing a single line. Used by both line based parsig
   * as well as block parsing. This saves the building of meta data for each
   * line.
   * 
   * @param line Line to process
   * @return parsing result
   */
  protected Pair<V, LabelList> parseLineInternal(String line) {
    List<String> entries = tokenize(line);

    // Split into numerical attributes and labels
    List<Double> attributes = new ArrayList<Double>(entries.size());
    LabelList labels = new LabelList();

    Iterator<String> itr = entries.iterator();
    for(int i = 0; itr.hasNext(); i++) {
      String ent = itr.next();
      if(!labelIndices.get(i)) {
        try {
          Double attribute = Double.valueOf(ent);
          attributes.add(attribute);
        }
        catch(NumberFormatException e) {
          labels.add(ent);
        }
      }
      else {
        labels.add(ent);
      }
    }

    Pair<V, LabelList> objectAndLabels;
    V vec = createDBObject(attributes);
    objectAndLabels = new Pair<V, LabelList>(vec, labels);
    return objectAndLabels;
  }

  /**
   * <p>
   * Creates a database object of type V.
   * </p>
   * 
   * @param attributes the attributes of the vector to create.
   * @return a RalVector of type V containing the given attribute values
   */
  protected abstract V createDBObject(List<Double> attributes);

  /**
   * Get a prototype object for the given dimensionality.
   * 
   * @param dimensionality Dimensionality
   * @return Prototype object
   */
  abstract protected VectorFieldTypeInformation<V> getTypeInformation(int dimensionality);

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<V extends NumberVector<?, ?>> extends AbstractParser.Parameterizer {
    /**
     * Keeps the indices of the attributes to be treated as a string label.
     */
    protected BitSet labelIndices = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntListParameter labelIndicesP = new IntListParameter(LABEL_INDICES_ID, true);

      labelIndices = new BitSet();
      if(config.grab(labelIndicesP)) {
        List<Integer> labelcols = labelIndicesP.getValue();
        for(Integer idx : labelcols) {
          labelIndices.set(idx);
        }
      }
    }

    @Override
    protected abstract NumberVectorLabelParser<V> makeInstance();
  }
}