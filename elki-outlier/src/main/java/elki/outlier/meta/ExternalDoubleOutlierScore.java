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
package elki.outlier.meta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import elki.outlier.OutlierAlgorithm;
import elki.AbstractAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.datasource.parser.CSVReaderFormat;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.FileUtil;
import elki.utilities.io.ParseUtil;
import elki.utilities.io.TokenizedReader;
import elki.utilities.io.Tokenizer;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.PatternParameter;
import elki.utilities.scaling.IdentityScaling;
import elki.utilities.scaling.ScalingFunction;
import elki.utilities.scaling.outlier.OutlierScaling;

/**
 * External outlier detection scores, loading outlier scores from an external
 * file. This class is meant to be able to read the default output of ELKI, i.e.
 * one object per line, with the DBID specified as <tt>ID=</tt> and the outlier
 * score specified with an algorithm-specific prefix.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - ScalingFunction
 * @has - - - File
 * @composed - - - CSVReaderFormat
 */
public class ExternalDoubleOutlierScore extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ExternalDoubleOutlierScore.class);

  /**
   * The comment character.
   */
  public static final String COMMENT = "#";

  /**
   * The default pattern for matching ID lines.
   */
  public static final String ID_PATTERN_DEFAULT = "^ID=";

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
   * Scaling function to use
   */
  private ScalingFunction scaling;

  /**
   * Inversion flag.
   */
  private boolean inverted = false;

  /**
   * Constructor.
   *
   * @param file File to load
   * @param idpattern Pattern to match IDs
   * @param scorepattern Pattern to match scores with
   * @param inverted Inversion flag
   * @param scaling Score scaling function
   */
  public ExternalDoubleOutlierScore(File file, Pattern idpattern, Pattern scorepattern, boolean inverted, ScalingFunction scaling) {
    super();
    this.file = file;
    this.idpattern = idpattern;
    this.scorepattern = scorepattern;
    this.inverted = inverted;
    this.scaling = scaling;
  }

  /**
   * Run the algorithm.
   *
   * @param database Database to use
   * @param relation Relation to use
   * @return Result
   */
  public OutlierResult run(Database database, Relation<?> relation) {
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);

    DoubleMinMax minmax = new DoubleMinMax();

    try (FileInputStream fis = new FileInputStream(file); //
        InputStream in = FileUtil.tryGzipInput(fis); //
        TokenizedReader reader = CSVReaderFormat.DEFAULT_FORMAT.makeReader()) {
      Tokenizer tokenizer = reader.getTokenizer();
      CharSequence buf = reader.getBuffer();
      Matcher mi = idpattern.matcher(buf), ms = scorepattern.matcher(buf);
      reader.reset(in);
      while(reader.nextLineExceptComments()) {
        Integer id = null;
        double score = Double.NaN;
        for(/* initialized by nextLineExceptComments */; tokenizer.valid(); tokenizer.advance()) {
          mi.region(tokenizer.getStart(), tokenizer.getEnd());
          ms.region(tokenizer.getStart(), tokenizer.getEnd());
          final boolean mif = mi.find(), msf = ms.find();
          if(mif && msf) {
            throw new AbortException("ID pattern and score pattern both match value: " + tokenizer.getSubstring());
          }
          if(mif) {
            if(id != null) {
              throw new AbortException("ID pattern matched twice: previous value " + id + " second value: " + tokenizer.getSubstring());
            }
            id = ParseUtil.parseIntBase10(buf, mi.end(), tokenizer.getEnd());
          }
          if(msf) {
            if(!Double.isNaN(score)) {
              throw new AbortException("Score pattern matched twice: previous value " + score + " second value: " + tokenizer.getSubstring());
            }
            score = ParseUtil.parseDouble(buf, ms.end(), tokenizer.getEnd());
          }
        }
        if(id != null && !Double.isNaN(score)) {
          scores.putDouble(DBIDUtil.importInteger(id), score);
          minmax.put(score);
        }
        else if(id == null && Double.isNaN(score)) {
          LOG.warning("Line did not match either ID nor score nor comment: " + reader.getLineNumber());
        }
        else {
          throw new AbortException("Line matched only ID or only SCORE patterns: " + reader.getLineNumber());
        }
      }
    }
    catch(IOException e) {
      throw new AbortException("Could not load outlier scores: " + e.getMessage() + " when loading " + file, e);
    }

    OutlierScoreMeta meta = inverted //
        ? new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax()) //
        : new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    DoubleRelation scoresult = new MaterializedDoubleRelation("External Outlier", relation.getDBIDs(), scores);
    OutlierResult or = new OutlierResult(meta, scoresult);

    // Apply scaling
    if(scaling instanceof OutlierScaling) {
      ((OutlierScaling) scaling).prepare(or);
    }
    DoubleMinMax mm = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double val = scoresult.doubleValue(iditer);
      val = scaling.getScaled(val);
      scores.putDouble(iditer, val);
      mm.put(val);
    }
    meta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax());
    return new OutlierResult(meta, scoresult);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter that specifies the name of the file to be re-parsed.
     */
    public static final OptionID FILE_ID = new OptionID("externaloutlier.file", "The file name containing the (external) outlier scores.");

    /**
     * Parameter that specifies the object ID pattern
     */
    public static final OptionID ID_ID = new OptionID("externaloutlier.idpattern", "The pattern to match object ID prefix");

    /**
     * Parameter that specifies the object score pattern
     */
    public static final OptionID SCORE_ID = new OptionID("externaloutlier.scorepattern", "The pattern to match object score prefix");

    /**
     * Parameter to specify a scaling function to use.
     */
    public static final OptionID SCALING_ID = new OptionID("externaloutlier.scaling", "Class to use as scaling function.");

    /**
     * Flag parameter for inverted scores.
     */
    public static final OptionID INVERTED_ID = new OptionID("externaloutlier.inverted", "Flag to signal an inverted outlier score.");

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
     * Scaling function to use
     */
    private ScalingFunction scaling;

    /**
     * Inversion flag.
     */
    private boolean inverted = false;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(FILE_ID, FileParameter.FileType.INPUT_FILE) //
          .grab(config, x -> file = x);
      new PatternParameter(ID_ID, ID_PATTERN_DEFAULT).grab(config, x -> idpattern = x);
      new PatternParameter(SCORE_ID).grab(config, x -> scorepattern = x);
      new Flag(INVERTED_ID).grab(config, x -> inverted = x);
      new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class) //
          .grab(config, x -> scaling = x);
    }

    @Override
    public ExternalDoubleOutlierScore make() {
      return new ExternalDoubleOutlierScore(file, idpattern, scorepattern, inverted, scaling);
    }
  }
}
