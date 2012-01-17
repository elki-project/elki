package de.lmu.ifi.dbs.elki.algorithm.outlier.meta;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.parser.AbstractParser;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * External outlier detection scores, loading outlier scores from an external
 * file.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ScalingFunction
 * @apiviz.has File
 */
public class ExternalDoubleOutlierScore extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ExternalDoubleOutlierScore.class);

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

    Pattern colSep = Pattern.compile(AbstractParser.WHITESPACE_PATTERN);
    DoubleMinMax minmax = new DoubleMinMax();
    InputStream in;
    try {
      in = FileUtil.tryGzipInput(new FileInputStream(file));
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      for(String line; (line = reader.readLine()) != null;) {
        if(line.startsWith(COMMENT)) {
          continue;
        }
        else if(line.length() > 0) {
          String[] cols = colSep.split(line);
          Integer id = null;
          double score = Double.NaN;
          for(String str : cols) {
            Matcher mi = idpattern.matcher(str);
            Matcher ms = scorepattern.matcher(str);
            final boolean mif = mi.find();
            final boolean msf = ms.find();
            if(mif && msf) {
              throw new AbortException("ID pattern and score pattern both match value: " + str);
            }
            if(mif) {
              if(id != null) {
                throw new AbortException("ID pattern matched twice: previous value " + id + " second value: " + str);
              }
              id = Integer.parseInt(str.substring(mi.end()));
            }
            if(msf) {
              if(!Double.isNaN(score)) {
                throw new AbortException("Score pattern matched twice: previous value " + score + " second value: " + str);
              }
              score = Double.parseDouble(str.substring(ms.end()));
            }
          }
          if(id != null && !Double.isNaN(score)) {
            scores.putDouble(DBIDUtil.importInteger(id), score);
            minmax.put(score);
          }
          else if(id == null && Double.isNaN(score)) {
            logger.warning("Line did not match either ID nor score nor comment: " + line);
          }
          else {
            throw new AbortException("Line matched only ID or only SCORE patterns: " + line);
          }
        }
      }
    }
    catch(IOException e) {
      throw new AbortException("Could not load outlier scores: " + e.getMessage() + " when loading " + file, e);
    }

    OutlierScoreMeta meta;
    if(inverted) {
      meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    }
    else {
      meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    }
    Relation<Double> scoresult = new MaterializedRelation<Double>("External Outlier", "external-outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierResult or = new OutlierResult(meta, scoresult);

    // Apply scaling
    if(scaling instanceof OutlierScalingFunction) {
      ((OutlierScalingFunction) scaling).prepare(or);
    }
    DoubleMinMax mm = new DoubleMinMax();
    for(DBID id : relation.iterDBIDs()) {
      double val = scoresult.get(id); // scores.get(id);
      val = scaling.getScaled(val);
      scores.putDouble(id, val);
      mm.put(val);
    }
    meta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax());
    or = new OutlierResult(meta, scoresult);

    return or;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter that specifies the name of the file to be re-parsed.
     * <p>
     * Key: {@code -externaloutlier.file}
     * </p>
     */
    public static final OptionID FILE_ID = OptionID.getOrCreateOptionID("externaloutlier.file", "The file name containing the (external) outlier scores.");

    /**
     * Parameter that specifies the object ID pattern
     * <p>
     * Key: {@code -externaloutlier.idpattern}<br />
     * Default: ^ID=
     * </p>
     */
    public static final OptionID ID_ID = OptionID.getOrCreateOptionID("externaloutlier.idpattern", "The pattern to match object ID prefix");

    /**
     * Parameter that specifies the object score pattern
     * <p>
     * Key: {@code -externaloutlier.scorepattern}<br />
     * </p>
     */
    public static final OptionID SCORE_ID = OptionID.getOrCreateOptionID("externaloutlier.scorepattern", "The pattern to match object score prefix");

    /**
     * Parameter to specify a scaling function to use.
     * <p>
     * Key: {@code -externaloutlier.scaling}
     * </p>
     */
    public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("externaloutlier.scaling", "Class to use as scaling function.");

    /**
     * Flag parameter for inverted scores.
     */
    public static final OptionID INVERTED_ID = OptionID.getOrCreateOptionID("externaloutlier.inverted", "Flag to signal an inverted outlier score.");

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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      FileParameter fileP = new FileParameter(FILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(fileP)) {
        file = fileP.getValue();
      }

      PatternParameter idP = new PatternParameter(ID_ID, ID_PATTERN_DEFAULT);
      if(config.grab(idP)) {
        idpattern = idP.getValue();
      }

      PatternParameter scoreP = new PatternParameter(SCORE_ID);
      if(config.grab(scoreP)) {
        scorepattern = scoreP.getValue();
      }

      Flag inverstedF = new Flag(INVERTED_ID);
      if(config.grab(inverstedF)) {
        inverted = inverstedF.getValue();
      }

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }
    }

    @Override
    protected ExternalDoubleOutlierScore makeInstance() {
      return new ExternalDoubleOutlierScore(file, idpattern, scorepattern, inverted, scaling);
    }
  }
}