package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Outlier have smallest GMOD_PROB: the outlier scores is the
 * <em>probability density</em> of the assumed distribution.
 * 
 * @author Lisa Reichert
 * 
 * @param <V> Vector type
 */
@Title("Gaussian Model Outlier Detection")
@Description("Fit a multivariate gaussian model onto the data, and use the PDF to compute an outlier score.")
public class GaussianModel<V extends NumberVector<V, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(GaussianModel.class);

  /**
   * OptionID for inversion flag.
   */
  public static final OptionID INVERT_ID = OptionID.getOrCreateOptionID("gaussod.invert", "Invert the value range to [0:1], with 1 being outliers instead of 0.");

  /**
   * Small value to increment diagonally of a matrix in order to avoid
   * singularity before building the inverse.
   */
  private static final double SINGULARITY_CHEAT = 1E-9;

  /**
   * Invert the result
   */
  private boolean invert = false;

  /**
   * Association ID for the Gaussian model outlier probability
   */
  public static final AssociationID<Double> GMOD_PROB = AssociationID.getOrCreateAssociationID("gmod.prob", TypeUtil.DOUBLE);

  /**
   * Constructor with actual parameters.
   * 
   * @param invert inversion flag.
   */
  public GaussianModel(boolean invert) {
    super();
    this.invert = invert;
  }

  public OutlierResult run(Relation<V> relation) throws IllegalStateException {
    DoubleMinMax mm = new DoubleMinMax();
    // resulting scores
    WritableDataStore<Double> oscores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);

    // Compute mean and covariance Matrix
    CovarianceMatrix temp = CovarianceMatrix.make(relation);
    V mean = temp.getMeanVector(relation);
    // debugFine(mean.toString());
    Matrix covarianceMatrix = temp.destroyToNaiveMatrix();
    // debugFine(covarianceMatrix.toString());
    Matrix covarianceTransposed = covarianceMatrix.cheatToAvoidSingularity(SINGULARITY_CHEAT).inverse();

    // Normalization factors for Gaussian PDF
    final double fakt = (1.0 / (Math.sqrt(Math.pow(MathUtil.TWOPI, DatabaseUtil.dimensionality(relation)) * covarianceMatrix.det())));

    // for each object compute Mahalanobis distance
    for(DBID id : relation.iterDBIDs()) {
      V x = relation.get(id);
      Vector x_minus_mean = x.minus(mean).getColumnVector();
      // Gaussian PDF
      final double mDist = x_minus_mean.transposeTimes(covarianceTransposed).times(x_minus_mean).get(0, 0);
      final double prob = fakt * Math.exp(-mDist / 2.0);

      mm.put(prob);
      oscores.put(id, prob);
    }

    final OutlierScoreMeta meta;
    if(invert) {
      double max = mm.getMax() != 0 ? mm.getMax() : 1.;
      for(DBID id : relation.iterDBIDs()) {
        oscores.put(id, (max - oscores.get(id)) / max);
      }
      meta = new BasicOutlierScoreMeta(0.0, 1.0);
    }
    else {
      meta = new InvertedOutlierScoreMeta(mm.getMin(), mm.getMax(), 0.0, Double.POSITIVE_INFINITY);
    }
    Relation<Double> res = new AnnotationFromDataStore<Double>("Gaussian Model Outlier Score", "gaussian-model-outlier", GMOD_PROB, oscores, relation.getDBIDs());
    return new OutlierResult(meta, res);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    protected boolean invert = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final Flag flag = new Flag(INVERT_ID);
      if(config.grab(flag)) {
        invert = flag.getValue();
      }
    }

    @Override
    protected GaussianModel<V> makeInstance() {
      return new GaussianModel<V>(invert);
    }
  }
}