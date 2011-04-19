package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.IndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.PCABasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.AbstractDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PCACorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.KNNQueryFilteredPCAIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Implementation of the HiCO algorithm, an algorithm for detecting hierarchies
 * of correlation clusters.
 * <p>
 * Reference: E. Achtert, C. Böhm, P. Kröger, A. Zimek: Mining Hierarchies of
 * Correlation Clusters. <br>
 * In: Proc. Int. Conf. on Scientific and Statistical Database Management
 * (SSDBM'06), Vienna, Austria, 2006.
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses KNNQueryFilteredPCAIndex
 * @apiviz.uses PCABasedCorrelationDistanceFunction
 * 
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Mining Hierarchies of Correlation Clusters")
@Description("Algorithm for detecting hierarchies of correlation clusters.")
@Reference(authors = "E. Achtert, C. Böhm, P. Kröger, A. Zimek", title = "Mining Hierarchies of Correlation Clusterse", booktitle = "Proc. Int. Conf. on Scientific and Statistical Database Management (SSDBM'06), Vienna, Austria, 2006", url = "http://dx.doi.org/10.1109/SSDBM.2006.35")
public class HiCO<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, ClusterOrderResult<PCACorrelationDistance>> {
  // TODO: make this a subclass of OPTICS.

  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(HiCO.class);

  /**
   * Parameter to specify the smoothing factor, must be an integer greater than
   * 0. The {link {@link #MU_ID}-nearest neighbor is used to compute the
   * correlation reachability of an object.
   * 
   * <p>
   * Key: {@code -hico.mu}
   * </p>
   */
  public static final OptionID MU_ID = OptionID.getOrCreateOptionID("hico.mu", "Specifies the smoothing factor. The mu-nearest neighbor is used to compute the correlation reachability of an object.");

  /**
   * Optional parameter to specify the number of nearest neighbors considered in
   * the PCA, must be an integer greater than 0. If this parameter is not set, k
   * is set to the value of {@link #MU_ID}.
   * <p>
   * Key: {@code -hico.k}
   * </p>
   * <p>
   * Default value: {@link #MU_ID}
   * </p>
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("hico.k", "Optional parameter to specify the number of nearest neighbors considered in the PCA. If this parameter is not set, k is set to the value of parameter mu.");

  /**
   * Parameter to specify the threshold of a distance between a vector q and a
   * given space that indicates that q adds a new dimension to the space, must
   * be a double equal to or greater than 0.
   * <p>
   * Default value: {@code 0.25}
   * </p>
   * <p>
   * Key: {@code -hico.delta}
   * </p>
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("hico.delta", "Threshold of a distance between a vector q and a given space that indicates that " + "q adds a new dimension to the space.");

  /**
   * The threshold for 'strong' eigenvectors: the 'strong' eigenvectors explain
   * a portion of at least alpha of the total variance.
   * <p>
   * Default value: {@link #DEFAULT_ALPHA}
   * </p>
   * <p>
   * Key: {@code -hico.alpha}
   * </p>
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("hico.alpha", "The threshold for 'strong' eigenvectors: the 'strong' eigenvectors explain a portion of at least alpha of the total variance.");

  /**
   * The default value for {@link #DELTA_ID}.
   */
  public static final double DEFAULT_DELTA = 0.25;

  /**
   * The default value for {@link #ALPHA_ID}.
   */
  public static final double DEFAULT_ALPHA = 0.85;

  /**
   * Internal OPTICS used by HiCO
   */
  private OPTICS<V, PCACorrelationDistance> optics;

  public HiCO(OPTICS<V, PCACorrelationDistance> optics) {
    super();
    this.optics = optics;
  }

  @Override
  protected ClusterOrderResult<PCACorrelationDistance> runInTime(Database database) throws IllegalStateException {
    return optics.run(database);
  }

  @Override
  public VectorFieldTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
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
    protected OPTICS<V, PCACorrelationDistance> optics;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter muP = new IntParameter(MU_ID, new GreaterConstraint(0));
      config.grab(muP);

      IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(0), true);
      config.grab(kP);
      final int k = kP.isDefined() ? kP.getValue() : muP.getValue();

      DoubleParameter deltaP = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0), DEFAULT_DELTA);
      config.grab(deltaP);

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.OPEN), DEFAULT_ALPHA);
      config.grab(alphaP);

      // Configure OPTICS
      ListParameterization opticsParameters = new ListParameterization();
      // epsilon and minpts
      opticsParameters.addParameter(OPTICS.EPSILON_ID, AbstractDistance.INFINITY_PATTERN);
      opticsParameters.addParameter(OPTICS.MINPTS_ID, muP.getValue());
      // distance function
      opticsParameters.addParameter(OPTICS.DISTANCE_FUNCTION_ID, PCABasedCorrelationDistanceFunction.class);
      // opticsParameters.addFlag(PreprocessorHandler.OMIT_PREPROCESSING_ID);
      // preprocessor
      opticsParameters.addParameter(IndexBasedDistanceFunction.INDEX_ID, KNNQueryFilteredPCAIndex.Factory.class);
      opticsParameters.addParameter(KNNQueryFilteredPCAIndex.Factory.K_ID, k);
      opticsParameters.addParameter(PercentageEigenPairFilter.ALPHA_ID, alphaP.getValue());
      opticsParameters.addParameter(PCABasedCorrelationDistanceFunction.DELTA_ID, deltaP.getValue());

      ChainedParameterization chain = new ChainedParameterization(opticsParameters, config);
      chain.errorsTo(config);
      Class<OPTICS<V, PCACorrelationDistance>> cls = ClassGenericsUtil.uglyCastIntoSubclass(OPTICS.class);
      optics = chain.tryInstantiate(cls);
    }

    @Override
    protected HiCO<V> makeInstance() {
      return new HiCO<V>(optics);
    }
  }
}