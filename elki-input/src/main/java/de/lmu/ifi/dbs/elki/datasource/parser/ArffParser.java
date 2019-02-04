/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.datasource.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.ExternalID;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.SparseDoubleVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Parser to load WEKA .arff files into ELKI.
 * <p>
 * This parser is quite hackish, and contains lots of not yet configurable
 * magic.
 * <p>
 * TODO: Allow configuration of the vector types (double, float)
 * <p>
 * TODO: when encountering integer columns, produce integer vectors.
 * <p>
 * TODO: allow optional class labels.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
@Title("ARFF File Format Parser")
public class ArffParser implements Parser {
  /**
   * Logger.
   */
  private static final Logging LOG = Logging.getLogger(ArffParser.class);

  /**
   * Arff file marker.
   */
  public static final Matcher ARFF_HEADER_RELATION = Pattern.compile("@relation\\s+(.*)", Pattern.CASE_INSENSITIVE).matcher("");

  /**
   * Arff attribute declaration marker.
   */
  public static final Matcher ARFF_HEADER_ATTRIBUTE = Pattern.compile("@attribute\\s+([^ ]+|['\"].*?['\"])\\s+(numeric|real|integer|string|double|date(\\s.*)|\\{.*\\})\\s*", Pattern.CASE_INSENSITIVE).matcher("");

  /**
   * Arff data marker.
   */
  public static final Matcher ARFF_HEADER_DATA = Pattern.compile("@data\\s*", Pattern.CASE_INSENSITIVE).matcher("");

  /**
   * Comment pattern.
   */
  public static final Matcher ARFF_COMMENT = Pattern.compile("^\\s*%.*").matcher("");

  /**
   * Pattern to auto-convert columns to external ids.
   */
  public static final String DEFAULT_ARFF_MAGIC_EID = "(External-?ID)";

  /**
   * Pattern to auto-convert columns to class labels.
   */
  public static final String DEFAULT_ARFF_MAGIC_CLASS = "(Class|Class-?Label)";

  /**
   * Pattern for numeric columns.
   */
  public static final Matcher ARFF_NUMERIC = Pattern.compile("(numeric|real|integer|double)", Pattern.CASE_INSENSITIVE).matcher("");

  /**
   * Empty line pattern.
   */
  public static final Matcher EMPTY = Pattern.compile("^\\s*$").matcher("");

  /**
   * Pattern to recognize external ids.
   */
  Matcher magic_eid;

  /**
   * Pattern to recognize class label columns.
   */
  Matcher magic_class;

  /**
   * (Reused) buffer for building label lists.
   */
  ArrayList<String> labels = new ArrayList<>();

  /**
   * Factory for dense vectors.
   *
   * TODO: Make parameterizable
   */
  NumberVector.Factory<?> denseFactory = DoubleVector.FACTORY;

  /**
   * Constructor.
   *
   * @param magic_eid Magic to recognize external IDs
   * @param magic_class Magic to recognize class labels
   */
  public ArffParser(Pattern magic_eid, Pattern magic_class) {
    super();
    this.magic_eid = magic_eid.matcher("");
    this.magic_class = magic_class.matcher("");
  }

  /**
   * Constructor.
   *
   * @param magic_eid Magic to recognize external IDs
   * @param magic_class Magic to recognize class labels
   */
  public ArffParser(String magic_eid, String magic_class) {
    this(Pattern.compile(magic_eid, Pattern.CASE_INSENSITIVE), Pattern.compile(magic_class, Pattern.CASE_INSENSITIVE));
  }

  @Override
  public MultipleObjectsBundle parse(InputStream instream) {
    try (InputStreamReader ir = new InputStreamReader(instream);
        BufferedReader br = new BufferedReader(ir)) {
      ArrayList<String> names = new ArrayList<>(), types = new ArrayList<>();

      readHeader(br);
      parseAttributeStatements(br, names, types);

      // Convert into column mapping. Prepare arrays to fill
      int[] targ = new int[names.size()];
      TypeInformation[] elkitypes = new TypeInformation[names.size()];
      int[] dimsize = new int[names.size()];
      processColumnTypes(names, types, targ, elkitypes, dimsize);

      // Prepare bundle:
      // This is a bit complicated to produce vector fields.
      MultipleObjectsBundle bundle = new MultipleObjectsBundle();
      StreamTokenizer tokenizer = makeArffTokenizer(br);

      int state = 0;

      nextToken(tokenizer);
      while(tokenizer.ttype != StreamTokenizer.TT_EOF) {
        // Parse instance
        if(tokenizer.ttype == StreamTokenizer.TT_EOL) {
          // ignore empty lines
        }
        else if(tokenizer.ttype != '{') {
          if(state == 0) {
            setupBundleHeaders(names, targ, elkitypes, dimsize, bundle, false);
            state = 1; // dense
          }
          else if(state != 1) {
            throw new AbortException("Mixing dense and sparse vectors is currently not allowed.");
          }
          // Load a dense instance
          bundle.appendSimple(loadDenseInstance(tokenizer, dimsize, elkitypes, bundle.metaLength()));
        }
        else {
          if(state == 0) {
            setupBundleHeaders(names, targ, elkitypes, dimsize, bundle, true);
            state = 2; // sparse
          }
          else if(state != 2) {
            throw new AbortException("Mixing dense and sparse vectors is currently not allowed.");
          }
          bundle.appendSimple(loadSparseInstance(tokenizer, targ, dimsize, elkitypes, bundle.metaLength()));
        }
        nextToken(tokenizer);
      }
      return bundle;
    }
    catch(IOException e) {
      throw new AbortException("IO error in parser", e);
    }
  }

  private Object[] loadSparseInstance(StreamTokenizer tokenizer, int[] targ, int[] dimsize, TypeInformation[] elkitypes, int metaLength) throws IOException {
    Int2ObjectOpenHashMap<Object> map = new Int2ObjectOpenHashMap<>();
    while(true) {
      nextToken(tokenizer);
      assert (tokenizer.ttype != StreamTokenizer.TT_EOF && tokenizer.ttype != StreamTokenizer.TT_EOL);
      if(tokenizer.ttype == '}') {
        nextToken(tokenizer);
        assert (tokenizer.ttype == StreamTokenizer.TT_EOF || tokenizer.ttype == StreamTokenizer.TT_EOL);
        break;
      }
      else {
        // sparse token
        if(tokenizer.ttype != StreamTokenizer.TT_WORD) {
          throw new AbortException("Unexpected token type encountered: " + tokenizer.toString() + " type: " + tokenizer.ttype);
        }
        int dim = ParseUtil.parseIntBase10(tokenizer.sval);
        if(map.containsKey(dim)) {
          throw new AbortException("Duplicate key in sparse vector: " + tokenizer.toString());
        }
        nextToken(tokenizer);
        if(tokenizer.ttype == StreamTokenizer.TT_WORD) {
          map.put(dim, TypeUtil.NUMBER_VECTOR_FIELD.equals(elkitypes[targ[dim]]) //
              ? (Double) ParseUtil.parseDouble(tokenizer.sval) : tokenizer.sval);
        }
        else {
          throw new AbortException("Unexpected token type encountered: " + tokenizer.toString());
        }
      }
    }
    Object[] data = new Object[metaLength];
    for(int out = 0; out < metaLength; out++) {
      // Find the first index
      int s = -1;
      for(int i = 0; i < targ.length; i++) {
        if(targ[i] == out) {
          s = i;
          break;
        }
      }
      assert (s >= 0);
      if(TypeUtil.NUMBER_VECTOR_FIELD.equals(elkitypes[out])) {
        Int2DoubleOpenHashMap f = new Int2DoubleOpenHashMap(dimsize[out]);
        for(ObjectIterator<Int2ObjectMap.Entry<Object>> iter = map.int2ObjectEntrySet().fastIterator(); iter.hasNext();) {
          Int2ObjectMap.Entry<Object> entry = iter.next();
          int i = entry.getIntKey();
          if(i < s || i >= s + dimsize[out]) {
            continue;
          }
          double v = ((Double) entry.getValue()).doubleValue();
          f.put(i - s, v);
        }
        data[out] = new SparseDoubleVector(f, dimsize[out]);
      }
      else if(TypeUtil.LABELLIST.equals(elkitypes[out])) {
        // Build a label list out of successive labels
        labels.clear();
        for(ObjectIterator<Int2ObjectMap.Entry<Object>> iter = map.int2ObjectEntrySet().fastIterator(); iter.hasNext();) {
          Int2ObjectMap.Entry<Object> entry = iter.next();
          int i = entry.getIntKey();
          if(i < s) {
            continue;
          }
          if(i >= s + dimsize[out]) {
            break;
          }
          if(labels.size() < i - s) {
            LOG.warning("Sparse consecutive labels are currently not correctly supported.");
          }
          labels.add((String) entry.getValue());
        }
        data[out] = LabelList.make(labels);
      }
      else if(TypeUtil.EXTERNALID.equals(elkitypes[out])) {
        String val = (String) map.get(s);
        if(val == null) {
          throw new AbortException("External ID column not set in sparse instance." + tokenizer.toString());
        }
        data[out] = new ExternalID(val);
      }
      else if(TypeUtil.CLASSLABEL.equals(elkitypes[out])) {
        Object val = map.get(s);
        if(val == null) {
          throw new AbortException("Class label column not set in sparse instance." + tokenizer.toString());
        }
        // TODO: support other class label types.
        ClassLabel lbl = new SimpleClassLabel(String.valueOf(val));
        data[out] = lbl;
      }
      else {
        throw new AbortException("Unsupported type for column " + "->" + out + ": " + ((elkitypes[out] != null) ? elkitypes[out].toString() : "null"));
      }
    }
    return data;
  }

  private Object[] loadDenseInstance(StreamTokenizer tokenizer, int[] dimsize, TypeInformation[] etyp, int outdim) throws IOException {
    Object[] data = new Object[outdim];
    for(int out = 0; out < outdim; out++) {
      if(TypeUtil.NUMBER_VECTOR_FIELD.equals(etyp[out])) {
        // For multi-column vectors, read successive columns
        double[] cur = new double[dimsize[out]];
        for(int k = 0; k < dimsize[out]; k++) {
          if(tokenizer.ttype == '?') {
            cur[k] = Double.NaN;
          }
          else if(tokenizer.ttype == StreamTokenizer.TT_WORD) {
            try {
              cur[k] = ParseUtil.parseDouble(tokenizer.sval);
            }
            catch(NumberFormatException e) {
              throw new AbortException("Expected number value, got: " + tokenizer.sval);
            }
          }
          else {
            throw new AbortException("Expected word token, got: " + tokenizer.toString());
          }
          nextToken(tokenizer);
        }
        data[out] = denseFactory.newNumberVector(cur);
      }
      else if(TypeUtil.LABELLIST.equals(etyp[out])) {
        // Build a label list out of successive labels
        labels.clear();
        for(int k = 0; k < dimsize[out]; k++) {
          if(tokenizer.ttype != StreamTokenizer.TT_WORD) {
            throw new AbortException("Expected word token, got: " + tokenizer.toString());
          }
          labels.add(tokenizer.sval);
          nextToken(tokenizer);
        }
        data[out] = LabelList.make(labels);
      }
      else if(TypeUtil.EXTERNALID.equals(etyp[out])) {
        if(tokenizer.ttype != StreamTokenizer.TT_WORD) {
          throw new AbortException("Expected word token, got: " + tokenizer.toString());
        }
        data[out] = new ExternalID(tokenizer.sval);
        nextToken(tokenizer);
      }
      else if(TypeUtil.CLASSLABEL.equals(etyp[out])) {
        if(tokenizer.ttype != StreamTokenizer.TT_WORD) {
          throw new AbortException("Expected word token, got: " + tokenizer.toString());
        }
        // TODO: support other class label types.
        ClassLabel lbl = new SimpleClassLabel(tokenizer.sval);
        data[out] = lbl;
        nextToken(tokenizer);
      }
      else {
        throw new AbortException("Unsupported type for column " + "->" + out + ": " + ((etyp[out] != null) ? etyp[out].toString() : "null"));
      }
    }
    return data;
  }

  /**
   * Make a StreamTokenizer for the ARFF format.
   *
   * @param br Buffered reader
   * @return Tokenizer
   */
  private StreamTokenizer makeArffTokenizer(BufferedReader br) {
    // Setup tokenizer
    StreamTokenizer tokenizer = new StreamTokenizer(br);
    {
      tokenizer.resetSyntax();
      tokenizer.whitespaceChars(0, ' ');
      tokenizer.ordinaryChars('0', '9'); // Do not parse numbers
      tokenizer.ordinaryChar('-');
      tokenizer.ordinaryChar('.');
      tokenizer.wordChars(' ' + 1, '\u00FF');
      tokenizer.whitespaceChars(',', ',');
      tokenizer.commentChar('%');
      tokenizer.quoteChar('"');
      tokenizer.quoteChar('\'');
      tokenizer.ordinaryChar('{');
      tokenizer.ordinaryChar('}');
      tokenizer.eolIsSignificant(true);
    }
    return tokenizer;
  }

  /**
   * Setup the headers for the object bundle.
   *
   * @param names Attribute names
   * @param targ Target columns
   * @param etyp ELKI type information
   * @param dimsize Number of dimensions in the individual types
   * @param bundle Output bundle
   * @param sparse Flag to create sparse vectors
   */
  private void setupBundleHeaders(ArrayList<String> names, int[] targ, TypeInformation[] etyp, int[] dimsize, MultipleObjectsBundle bundle, boolean sparse) {
    for(int in = 0, out = 0; in < targ.length; out++) {
      int nin = in + 1;
      for(; nin < targ.length; nin++) {
        if(targ[nin] != targ[in]) {
          break;
        }
      }
      if(TypeUtil.NUMBER_VECTOR_FIELD.equals(etyp[out])) {
        String[] labels = new String[dimsize[out]];
        // Collect labels:
        for(int i = 0; i < dimsize[out]; i++) {
          labels[i] = names.get(out + i);
        }
        if(!sparse) {
          VectorFieldTypeInformation<DoubleVector> type = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dimsize[out], labels);
          bundle.appendColumn(type, new ArrayList<DoubleVector>());
        }
        else {
          VectorFieldTypeInformation<SparseDoubleVector> type = new VectorFieldTypeInformation<>(SparseDoubleVector.FACTORY, dimsize[out], labels);
          bundle.appendColumn(type, new ArrayList<SparseDoubleVector>());
        }
      }
      else if(TypeUtil.LABELLIST.equals(etyp[out])) {
        StringBuilder label = new StringBuilder(names.get(out));
        for(int i = 1; i < dimsize[out]; i++) {
          label.append(' ').append(names.get(out + i));
        }
        bundle.appendColumn(new SimpleTypeInformation<>(LabelList.class, label.toString()), new ArrayList<LabelList>());
      }
      else if(TypeUtil.EXTERNALID.equals(etyp[out])) {
        bundle.appendColumn(new SimpleTypeInformation<>(ExternalID.class, names.get(out)), new ArrayList<ExternalID>());
      }
      else if(TypeUtil.CLASSLABEL.equals(etyp[out])) {
        bundle.appendColumn(new SimpleTypeInformation<>(ClassLabel.class, names.get(out)), new ArrayList<ClassLabel>());
      }
      else {
        throw new AbortException("Unsupported type for column " + in + "->" + out + ": " + ((etyp[out] != null) ? etyp[out].toString() : "null"));
      }
      assert (out == bundle.metaLength() - 1);
      in = nin;
    }
  }

  /**
   * Read the dataset header part of the ARFF file, to ensure consistency.
   *
   * @param br Buffered Reader
   * @throws IOException
   */
  private void readHeader(BufferedReader br) throws IOException {
    String line;
    // Locate header line
    while(true) {
      line = br.readLine();
      if(line == null) {
        throw new AbortException(ARFF_HEADER_RELATION + " not found in file.");
      }
      // Skip comments and empty lines
      if(ARFF_COMMENT.reset(line).matches() || EMPTY.reset(line).matches()) {
        continue;
      }
      // Break on relation statement
      if(ARFF_HEADER_RELATION.reset(line).matches()) {
        break;
      }
      throw new AbortException("Expected relation declaration: " + line);
    }
  }

  /**
   * Parse the "@attribute" section of the ARFF file.
   *
   * @param br Input
   * @param names List (to fill) of attribute names
   * @param types List (to fill) of attribute types
   * @throws IOException
   */
  private void parseAttributeStatements(BufferedReader br, ArrayList<String> names, ArrayList<String> types) throws IOException {
    String line;
    // Load attribute metadata
    while(true) {
      line = br.readLine();
      if(line == null) {
        throw new AbortException(ARFF_HEADER_DATA + " not found in file.");
      }
      // Skip comments and empty lines
      if(ARFF_COMMENT.reset(line).matches() || EMPTY.reset(line).matches()) {
        continue;
      }
      // Break on data statement to continue
      if(ARFF_HEADER_DATA.reset(line).matches()) {
        break;
      }
      // Expect an attribute specification
      Matcher matcher = ARFF_HEADER_ATTRIBUTE.reset(line);
      if(matcher.matches()) {
        String name = matcher.group(1);
        if(name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\'') {
          name = name.substring(1, name.length() - 1);
        }
        else if(name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"') {
          name = name.substring(1, name.length() - 1);
        }
        String type = matcher.group(2);
        names.add(name);
        types.add(type);
        // logger.warning("Attribute name: " + name + " type: " + type);
        continue;
      }
      throw new AbortException("Unrecognized line: " + line);
    }
    assert (names.size() == types.size());
  }

  /**
   * Process the column types (and names!) into ELKI relation style. Note that
   * this will for example merge successive numerical columns into a single
   * vector.
   *
   * @param names Attribute names
   * @param types Attribute types
   * @param targ Target dimension mapping (ARFF to ELKI), return value
   * @param etyp ELKI type information, return value
   * @param dims Number of successive dimensions, return value
   */
  private void processColumnTypes(ArrayList<String> names, ArrayList<String> types, int[] targ, TypeInformation[] etyp, int[] dims) {
    int next = 0;
    for(int i = 0; i < targ.length; i++) {
      if(magic_eid != null && magic_eid.reset(names.get(i)).matches()) {
        // Turn into an external ID column.
        targ[i] = next;
        etyp[next] = TypeUtil.EXTERNALID;
        dims[next] = 1;
        next++;
        continue;
      }
      else if(magic_class != null && magic_class.reset(names.get(i)).matches()) {
        // Type as ClassLabel
        targ[i] = next;
        etyp[next] = TypeUtil.CLASSLABEL;
        dims[next] = 1;
        next++;
        continue;
      }
      else if(ARFF_NUMERIC.reset(types.get(i)).matches()) {
        // Create a number vector field
        if(next > 0 && TypeUtil.NUMBER_VECTOR_FIELD.equals(etyp[next - 1])) {
          targ[i] = next - 1;
          dims[next - 1]++;
          continue;
        }
        else {
          targ[i] = next;
          etyp[next] = TypeUtil.NUMBER_VECTOR_FIELD;
          dims[next] = 1;
          next++;
          continue;
        }
      }
      else {
        // Use LabelList
        if(next > 0 && TypeUtil.LABELLIST.equals(etyp[next - 1])) {
          targ[i] = next - 1;
          dims[next - 1]++;
          continue;
        }
        else {
          targ[i] = next;
          etyp[next] = TypeUtil.LABELLIST;
          dims[next] = 1;
          next++;
          continue;
        }
      }
    }
  }

  /**
   * Helper function for token handling.
   *
   * @param tokenizer Tokenizer
   * @throws IOException
   */
  private void nextToken(StreamTokenizer tokenizer) throws IOException {
    tokenizer.nextToken();
    if((tokenizer.ttype == '\'') || (tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    }
    else if((tokenizer.ttype == StreamTokenizer.TT_WORD) && (tokenizer.sval.equals("?"))) {
      tokenizer.ttype = '?';
    }
    if(LOG.isDebugging()) {
      if(tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
        LOG.debug("token: " + tokenizer.nval);
      }
      else if(tokenizer.ttype == StreamTokenizer.TT_WORD) {
        LOG.debug("token: " + tokenizer.sval);
      }
      else if(tokenizer.ttype == StreamTokenizer.TT_EOF) {
        LOG.debug("token: EOF");
      }
      else if(tokenizer.ttype == StreamTokenizer.TT_EOL) {
        LOG.debug("token: EOL");
      }
      else {
        LOG.debug("token type: " + tokenizer.ttype);
      }
    }
  }

  @Override
  public void cleanup() {
    if(magic_eid != null) {
      magic_eid.reset("");
    }
    if(magic_class != null) {
      magic_class.reset("");
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Pattern for recognizing external ID attributes.
     */
    public static final OptionID MAGIC_EID_ID = new OptionID("arff.externalid", "Pattern to recognize external ID attributes.");

    /**
     * Pattern for recognizing class label attributes.
     */
    public static final OptionID MAGIC_CLASS_ID = new OptionID("arff.classlabel", "Pattern to recognize class label attributes.");

    /**
     * Pattern to recognize external ids
     */
    Pattern magic_eid;

    /**
     * Pattern to recognize class label columns
     */
    Pattern magic_class;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter eidP = new PatternParameter(MAGIC_EID_ID, DEFAULT_ARFF_MAGIC_EID);
      if(config.grab(eidP)) {
        magic_eid = eidP.getValue();
      }
      PatternParameter classP = new PatternParameter(MAGIC_CLASS_ID, DEFAULT_ARFF_MAGIC_CLASS);
      if(config.grab(classP)) {
        magic_class = classP.getValue();
      }
    }

    @Override
    protected ArffParser makeInstance() {
      return new ArffParser(magic_eid, magic_class);
    }
  }
}
