package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * Trivial algorithm that marks outliers by their label. Can be used as
 * reference algorithm in comparisons.
 * 
 * @author Erich Schubert
 */
public class ByLabelOutlier extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * Our logger.
   */
  private static final Logging logger = Logging.getLogger(ByLabelOutlier.class);

  /**
   * The default pattern to use.
   */
  public static final String DEFAULT_PATTERN = ".*(Outlier|Noise).*";

  /**
   * The pattern we match with.
   */
  final Pattern pattern;

  /**
   * Association id to associate
   */
  public static final AssociationID<Double> LABEL_OUT = AssociationID.getOrCreateAssociationID("label_outlier", Double.class);
  
  /**
   * Constructor.
   * 
   * @param pattern Pattern to match with.
   */
  public ByLabelOutlier(Pattern pattern) {
    super();
    this.pattern = pattern;
  }

  /**
   * Constructor.
   */
  public ByLabelOutlier() {
    this(Pattern.compile(DEFAULT_PATTERN));
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.GUESSED_LABEL);
  }

  public OutlierResult run(@SuppressWarnings("unused") Database database, Relation<?> relation) throws IllegalStateException {
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT, Double.class);
    for(DBID id : relation.iterDBIDs()) {
      String label = relation.get(id).toString();
      final double score;
      if (pattern.matcher(label).matches()) {
        score = 1.0;
      } else {
        score = 0.0;
      }
      scores.put(id, score);
    }
    AnnotationResult<Double> scoreres = new AnnotationFromDataStore<Double>("By label outlier scores", "label-outlier", LABEL_OUT, scores);
    OutlierScoreMeta meta = new ProbabilisticOutlierScore();
    return new OutlierResult(meta, scoreres);
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
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The pattern to match outliers with.
     * 
     * <p>
     * Default value: .*(Outlier|Noise).*
     * </p>
     * <p>
     * Key: {@code -outlier.pattern}
     * </p>
     */
    public static final OptionID OUTLIER_PATTERN_ID = OptionID.getOrCreateOptionID("outlier.pattern", "Label pattern to match outliers.");
    /**
     * Stores the "outlier" class.
     */
    private Pattern pattern;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter patternP = new PatternParameter(OUTLIER_PATTERN_ID, DEFAULT_PATTERN);
      if(config.grab(patternP)) {
        pattern = patternP.getValue();
      }
    }

    @Override
    protected ByLabelOutlier makeInstance() {
      return new ByLabelOutlier(pattern);
    }
  }
}