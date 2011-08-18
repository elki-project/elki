package experimentalcode.erich.arff;

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
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.parser.Parser;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Parser to load WEKA .arff files into ELKI.
 * 
 * Sparse vectors are not yet supported.
 * 
 * This parser is quite hackish, and contains lots of not yet configurable magic.
 * 
 * @author Erich Schubert
 */
public class ArffParser implements Parser {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(ArffParser.class);

  /**
   * Arff file marker
   */
  public static final Pattern ARFF_HEADER_RELATION = Pattern.compile("@relation\\s+(.*)", Pattern.CASE_INSENSITIVE);

  /**
   * Arff attribute declaration marker
   */
  public static final Pattern ARFF_HEADER_ATTRIBUTE = Pattern.compile("@attribute\\s+([^ ]+|['\"].*?['\"])\\s+(numeric|real|integer|string|double|date(\\s.*)|\\{.*\\})\\s*", Pattern.CASE_INSENSITIVE);

  /**
   * Arff data marker
   */
  public static final Pattern ARFF_HEADER_DATA = Pattern.compile("@data\\s*", Pattern.CASE_INSENSITIVE);

  /**
   * Comment pattern.
   */
  public static final Pattern ARFF_COMMENT = Pattern.compile("^\\s*%.*");

  /**
   * Pattern to auto-convert columns to external ids.
   */
  public static final Pattern ARFF_MAGIC_EID = Pattern.compile("(ID|External-?ID)", Pattern.CASE_INSENSITIVE);

  /**
   * Pattern to auto-convert columns to class labels.
   */
  public static final Pattern ARFF_MAGIC_CLASS = Pattern.compile("(Class|Class-?Label)", Pattern.CASE_INSENSITIVE);

  /**
   * Pattern for numeric columns
   */
  public static final Pattern ARFF_NUMERIC = Pattern.compile("(numeric|real|integer|double)", Pattern.CASE_INSENSITIVE);

  /**
   * Empty line pattern.
   */
  public static final Pattern EMPTY = Pattern.compile("^\\s*$");

  @Override
  public MultipleObjectsBundle parse(InputStream instream) {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(instream));
      String line;
      // Locate header line
      while(true) {
        line = br.readLine();
        if(line == null) {
          throw new AbortException(ARFF_HEADER_RELATION + " not found in file.");
        }
        // Skip comments and empty lines
        if(ARFF_COMMENT.matcher(line).matches() || EMPTY.matcher(line).matches()) {
          continue;
        }
        // Break on relation statement
        if(ARFF_HEADER_RELATION.matcher(line).matches()) {
          break;
        }
        throw new AbortException("Expected relation declaration: " + line);
      }
      ArrayList<String> names = new ArrayList<String>();
      ArrayList<String> types = new ArrayList<String>();
      // Load attribute metadata
      while(true) {
        line = br.readLine();
        if(line == null) {
          throw new AbortException(ARFF_HEADER_DATA + " not found in file.");
        }
        // Skip comments and empty lines
        if(ARFF_COMMENT.matcher(line).matches() || EMPTY.matcher(line).matches()) {
          continue;
        }
        // Break on data statement to continue
        if(ARFF_HEADER_DATA.matcher(line).matches()) {
          break;
        }
        // Expect an attribute specification
        Matcher matcher = ARFF_HEADER_ATTRIBUTE.matcher(line);
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

      int[] targ = new int[names.size()];
      TypeInformation[] etyp = new TypeInformation[names.size()];
      int[] dims = new int[names.size()];

      int next = 0;
      for(int i = 0; i < targ.length; i++) {
        // Turn into an external ID column.
        if(ARFF_MAGIC_EID.matcher(names.get(i)).matches()) {
          targ[i] = next;
          etyp[next] = TypeUtil.EXTERNALID;
          dims[next] = 1;
          next++;
          continue;
        }
        else if(ARFF_MAGIC_CLASS.matcher(names.get(i)).matches()) {
          targ[i] = next;
          etyp[next] = TypeUtil.CLASSLABEL;
          dims[next] = 1;
          next++;
          continue;
        }
        else if(ARFF_NUMERIC.matcher(types.get(i)).matches()) {
          if(next > 0 && etyp[next - 1] == TypeUtil.NUMBER_VECTOR_FIELD) {
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
          if(next > 0 && etyp[next - 1] == TypeUtil.LABELLIST) {
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

      // Prepare bundle:
      // This is a bit complicated to produce vector fields.
      MultipleObjectsBundle bundle = new MultipleObjectsBundle();
      for(int in = 0, out = 0; in < targ.length; out++) {
        int nin = in + 1;
        for(; nin < targ.length; nin++) {
          if(targ[nin] != targ[in]) {
            break;
          }
        }
        if(etyp[out] == TypeUtil.NUMBER_VECTOR_FIELD) {
          bundle.appendColumn(VectorFieldTypeInformation.get(DoubleVector.class, dims[out]), new ArrayList<DoubleVector>());
        }
        else if(etyp[out] == TypeUtil.LABELLIST) {
          bundle.appendColumn(TypeUtil.LABELLIST, new ArrayList<LabelList>());
        }
        else if(etyp[out] == TypeUtil.EXTERNALID) {
          bundle.appendColumn(TypeUtil.EXTERNALID, new ArrayList<ExternalID>());
        }
        else if(etyp[out] == TypeUtil.CLASSLABEL) {
          bundle.appendColumn(TypeUtil.CLASSLABEL, new ArrayList<ClassLabel>());
        }
        else {
          throw new AbortException("Unsupported type for column " + in + "->" + out + ": " + ((etyp[out] != null) ? etyp[out].toString() : "null"));
        }
        assert (out == bundle.metaLength() - 1);
        // logger.warning("Added meta: " + bundle.meta(bundle.metaLength() - 1));
        in = nin;
      }
      // Setup tokenizer
      StreamTokenizer tokenizer = new StreamTokenizer(br);
      {
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

      final int outdim = bundle.metaLength();
      nextToken(tokenizer);
      while(tokenizer.ttype != StreamTokenizer.TT_EOF) {
        // Parse instance
        if(tokenizer.ttype == StreamTokenizer.TT_EOL) {
          // ignore empty lines
        }
        else if(tokenizer.ttype != '{') {
          // logger.warning("Regular instance.");
          Object[] data = new Object[outdim];
          for(int out = 0; out < outdim; out++) {
            if(etyp[out] == TypeUtil.NUMBER_VECTOR_FIELD) {
              double[] cur = new double[dims[out]];
              for(int k = 0; k < dims[out]; k++) {
                if(tokenizer.ttype != StreamTokenizer.TT_NUMBER) {
                  throw new AbortException("Expected word token, got: " + tokenizer.toString());
                }
                cur[k] = tokenizer.nval;
                nextToken(tokenizer);
              }
              data[out] = new DoubleVector(cur);
            }
            else if(etyp[out] == TypeUtil.LABELLIST) {
              LabelList ll = new LabelList();
              for(int k = 0; k < dims[out]; k++) {
                if(tokenizer.ttype != StreamTokenizer.TT_WORD) {
                  throw new AbortException("Expected word token, got: " + tokenizer.toString());
                }
                ll.add(tokenizer.sval);
                nextToken(tokenizer);
              }
              data[out] = ll;
            }
            else if(etyp[out] == TypeUtil.EXTERNALID) {
              if(tokenizer.ttype != StreamTokenizer.TT_WORD) {
                throw new AbortException("Expected word token, got: " + tokenizer.toString());
              }
              data[out] = new ExternalID(tokenizer.sval);
              nextToken(tokenizer);
            }
            else if(etyp[out] == TypeUtil.CLASSLABEL) {
              if(tokenizer.ttype != StreamTokenizer.TT_WORD) {
                throw new AbortException("Expected word token, got: " + tokenizer.toString());
              }
              ClassLabel lbl = new SimpleClassLabel();
              lbl.init(tokenizer.sval);
              data[out] = lbl;
              nextToken(tokenizer);
            }
            else {
              throw new AbortException("Unsupported type for column " + "->" + out + ": " + ((etyp[out] != null) ? etyp[out].toString() : "null"));
            }
          }
          bundle.appendSimple(data);
        }
        else {
          logger.warning("Sparse instance.");
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
            }
          }
          throw new AbortException("Sparse ARFF are not (yet) supported.");
        }
        if(tokenizer.ttype != StreamTokenizer.TT_EOF) {
          nextToken(tokenizer);
        }
      }
      return bundle;
    }
    catch(IOException e) {
      throw new AbortException("IO error in parser", e);
    }
  }

  private void nextToken(StreamTokenizer tokenizer) throws IOException {
    tokenizer.nextToken();
    if((tokenizer.ttype == '\'') || (tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    }
    else if((tokenizer.ttype == StreamTokenizer.TT_WORD) && (tokenizer.sval.equals("?"))) {
      tokenizer.ttype = '?';
    }
    if(tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
      logger.debug("token: " + tokenizer.nval);
    }
    else if(tokenizer.ttype == StreamTokenizer.TT_WORD) {
      logger.debug("token: " + tokenizer.sval);
    }
    else if(tokenizer.ttype == StreamTokenizer.TT_EOF) {
      logger.debug("token: EOF");
    }
    else if(tokenizer.ttype == StreamTokenizer.TT_EOL) {
      logger.debug("token: EOL");
    }
    else {
      logger.debug("token type: " + tokenizer.ttype);
    }
  }
}