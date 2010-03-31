package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.AbstractDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PCACorrelationDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.PCABasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedLocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.DefaultValueGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Implementation of the HiCO algorithm, an algorithm for detecting hierarchies
 * of correlation clusters.
 * <p>
 * Reference: E. Achtert, C. B&ouml;hm, P. Kr&ouml;ger, A. Zimek: Mining
 * Hierarchies of Correlation Clusters. <br>
 * In: Proc. Int. Conf. on Scientific and Statistical Database Management (SSDBM
 * 2006), Vienna, Austria, 2006.
 * </p>
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Mining Hierarchies of Correlation Clusters")
@Description("Algorithm for detecting hierarchies of correlation clusters.")
@Reference(authors = "E. Achtert, C. Böhm, P. Kröger, A. Zimek", title = "Mining Hierarchies of Correlation Clusterse", booktitle = "Proc. Int. Conf. on Scientific and Statistical Database Management (SSDBM'06), Vienna, Austria, 2006", url = "http://dx.doi.org/10.1109/SSDBM.2006.35")
public class HiCO<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, ClusterOrderResult<PCACorrelationDistance>> {

  /**
   * OptionID for {@link #MU_PARAM}.
   */
  public static final OptionID MU_ID = OptionID.getOrCreateOptionID("hico.mu", "Specifies the smoothing factor. The mu-nearest neighbor is used to compute the correlation reachability of an object.");

  /**
   * Parameter to specify the smoothing factor, must be an integer greater than
   * 0. The {link {@link #MU_PARAM}-nearest neighbor is used to compute the
   * correlation reachability of an object.
   * 
   * <p>
   * Key: {@code -hico.mu}
   * </p>
   */
  private final IntParameter MU_PARAM = new IntParameter(MU_ID, new GreaterConstraint(0));

  /**
   * OptionID for {@link #K_PARAM}.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("hico.k", "Optional parameter to specify the number of nearest neighbors considered in the PCA. If this parameter is not set, k is set to the value of parameter mu.");

  /**
   * Optional parameter to specify the number of nearest neighbors considered in
   * the PCA, must be an integer greater than 0. If this parameter is not set, k
   * is set to the value of {@link #MU_PARAM}.
   * <p>
   * Key: {@code -hico.k}
   * </p>
   * <p>
   * Default value: {@link #MU_PARAM}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0), true);

  /**
   * OptionID for {@link #DELTA_PARAM}.
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("hico.delta", "Threshold of a distance between a vector q and a given space that indicates that " + "q adds a new dimension to the space.");

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
  private final DoubleParameter DELTA_PARAM = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0), 0.25);

  /**
   * The default value for {@link #ALPHA_PARAM}.
   */
  public static final double DEFAULT_ALPHA = 0.85;

  /**
   * OptionID for {@link #ALPHA_PARAM}.
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("hico.alpha", "The threshold for 'strong' eigenvectors: the 'strong' eigenvectors explain a portion of at least alpha of the total variance.");

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
  private final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.OPEN), DEFAULT_ALPHA);

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public HiCO(Parameterization config) {
    super(config);

    // parameter mu
    config.grab(MU_PARAM);

    // parameter k
    config.grab(K_PARAM);

    // global constraint k <-> mu
    GlobalParameterConstraint gpc = new DefaultValueGlobalConstraint<Integer>(K_PARAM, MU_PARAM);
    config.checkConstraint(gpc);

    // parameter delta
    config.grab(DELTA_PARAM);

    // parameter alpha
    config.grab(ALPHA_PARAM);
  }

  @Override
  protected ClusterOrderResult<PCACorrelationDistance> runInTime(Database<V> database) throws IllegalStateException {
    // OPTICS
    ListParameterization opticsParameters = new ListParameterization();
    // epsilon and minpts
    opticsParameters.addParameter(OPTICS.EPSILON_ID, AbstractDistance.INFINITY_PATTERN);
    opticsParameters.addParameter(OPTICS.MINPTS_ID, MU_PARAM.getValue());
    // distance function
    opticsParameters.addParameter(OPTICS.DISTANCE_FUNCTION_ID, PCABasedCorrelationDistanceFunction.class.getName());
    opticsParameters.addFlag(PreprocessorHandler.OMIT_PREPROCESSING_ID);
    // preprocessor
    opticsParameters.addParameter(PreprocessorHandler.PREPROCESSOR_ID, KnnQueryBasedLocalPCAPreprocessor.class.getName());
    opticsParameters.addParameter(KnnQueryBasedLocalPCAPreprocessor.K_ID, K_PARAM.getValue());
    opticsParameters.addParameter(PercentageEigenPairFilter.ALPHA_ID, ALPHA_PARAM.getValue());
    opticsParameters.addParameter(PCABasedCorrelationDistanceFunction.DELTA_ID, DELTA_PARAM.getValue());

    // run OPTICS
    OPTICS<V, PCACorrelationDistance> optics = new OPTICS<V, PCACorrelationDistance>(opticsParameters);
    optics.setVerbose(isVerbose());
    optics.setTime(isTime());
    return optics.run(database);
  }

}
