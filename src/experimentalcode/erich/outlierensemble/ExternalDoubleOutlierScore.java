package experimentalcode.erich.outlierensemble;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.parser.AbstractParser;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * External outlier detection, loading outlier scores from an external file.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 */
public class ExternalDoubleOutlierScore<O extends DatabaseObject> extends AbstractAlgorithm<O, OutlierResult> {
  /**
   * The comment character.
   */
  public static final String COMMENT = "#";

  /**
   * OptionID for {@link #FILE_PARAM}
   */
  public static final OptionID FILE_ID = OptionID.getOrCreateOptionID("externaloutlier.file", "The file name containing the (external) outlier scores.");

  /**
   * Parameter that specifies the name of the file to be re-parsed.
   * <p>
   * Key: {@code -externaloutlier.file}
   * </p>
   */
  private final FileParameter FILE_PARAM = new FileParameter(FILE_ID, FileParameter.FileType.INPUT_FILE);

  /**
   * OptionID for {@link #ID_PARAM}
   */
  public static final OptionID ID_ID = OptionID.getOrCreateOptionID("externaloutlier.idpattern", "The pattern to match object ID prefix");

  /**
   * Parameter that specifies the object ID pattern
   * <p>
   * Key: {@code -externaloutlier.idpattern}<br />
   * Default: ^ID=
   * </p>
   */
  private final PatternParameter ID_PARAM = new PatternParameter(ID_ID, "^ID=");
  
  /**
   * OptionID for {@link #SCORE_PARAM}
   */
  public static final OptionID SCORE_ID = OptionID.getOrCreateOptionID("externaloutlier.scorepattern", "The pattern to match object score prefix");

  private static final AssociationID<Double> EXTERNAL_OUTLIER_SCORES_ID = null;

  /**
   * Parameter that specifies the object score pattern
   * <p>
   * Key: {@code -externaloutlier.scorepattern}<br />
   * </p>
   */
  private final PatternParameter SCORE_PARAM = new PatternParameter(SCORE_ID);
  
  /**
   * The file to be reparsed
   */
  private File file;
  
  /**
   * object id pattern
   */
  private Pattern idpattern;
  
  /**
   * object score pattern
   */
  private Pattern scorepattern;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public ExternalDoubleOutlierScore(Parameterization config) {
    super(config);
    if(config.grab(FILE_PARAM)) {
      file = FILE_PARAM.getValue();
    }
    if(config.grab(ID_PARAM)) {
      idpattern = ID_PARAM.getValue();
    }
    if(config.grab(SCORE_PARAM)) {
      scorepattern = SCORE_PARAM.getValue();
    }
  }

  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);
    
    MinMax<Double> minmax = new MinMax<Double>();
    InputStream in;
    try {
      in = FileUtil.tryGzipInput(new FileInputStream(file));
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      for(String line; (line = reader.readLine()) != null;) {
        if(line.startsWith(COMMENT)) {
          continue;
        }
        else if(line.length() > 0) {
          String[] cols = AbstractParser.WHITESPACE_PATTERN.split(line);
          Integer id = null;
          Double score = null;
          for (String str : cols) {
            Matcher mi = idpattern.matcher(str);
            Matcher ms = scorepattern.matcher(str);
            final boolean mif = mi.find();
            final boolean msf = ms.find();
            if (mif && msf) {
              throw new AbortException("ID pattern and score pattern both match value: "+str);
            }
            if (mif) {
              if (id != null) {
                throw new AbortException("ID pattern matched twice: previous value "+id+" second value: "+str);
              }
              id = Integer.parseInt(str.substring(mi.end()));
            }
            if (msf) {
              if (score != null) {
                throw new AbortException("Score pattern matched twice: previous value "+score+" second value: "+str);
              }
              score = Double.parseDouble(str.substring(ms.end()));
            }
          }
          if (id != null && score != null) {
            scores.put(DBIDUtil.importInteger(id), score);
            minmax.put(score);
          } else if (id == null && score == null) {
            logger.warning("Line did not match either ID nor score nor comment: "+line);
          } else {
            throw new AbortException("Line matched only ID or only SCORE patterns: "+line);
          }
        }
      }
    }
    catch(IOException e) {
      throw new AbortException("Could not load outlier scores.", e);
    }

    // FIXME: ordering - descending or ascending!
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    AnnotationResult<Double> scoresult = new AnnotationFromDataStore<Double>(EXTERNAL_OUTLIER_SCORES_ID, scores);
    OrderingResult ordering = new OrderingFromDataStore<Double>(scores, true);
    return new OutlierResult(meta , scoresult , ordering);
  }

}
