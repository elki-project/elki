package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.AbstractDistance;
import de.lmu.ifi.dbs.elki.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.HiSCDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.HiSCPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Implementation of the HiSC algorithm, an algorithm for detecting hierarchies
 * of subspace clusters.
 * <p>
 * Reference: E. Achtert, C. B&ouml;hm, H.-P. Kriegel, P. Kr&ouml;ger, I.
 * M&uuml;ller-Gorman, A. Zimek: Finding Hierarchies of Subspace Clusters. <br>
 * In: Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery
 * in Databases (PKDD'06), Berlin, Germany, 2006.
 * </p>
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Finding Hierarchies of Subspace Clusters")
@Description("Algorithm for detecting hierarchies of subspace clusters.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek", title = "Finding Hierarchies of Subspace Clusters", booktitle = "Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in Databases (PKDD'06), Berlin, Germany, 2006", url = "http://www.dbs.ifi.lmu.de/Publikationen/Papers/PKDD06-HiSC.pdf")
public class HiSC<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, ClusterOrderResult<PreferenceVectorBasedCorrelationDistance>> {

  /**
   * The number of nearest neighbors considered to determine the preference
   * vector. If this value is not defined, k is set to three times of the
   * dimensionality of the database objects.
   * <p>
   * Key: {@code -hisc.k}
   * </p>
   * <p>
   * Default value: three times of the dimensionality of the database objects
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(HiSCPreprocessor.K_ID, new GreaterConstraint(0), true);

  /**
   * Holds the value of parameter {@link #K_PARAM}.
   */
  private Integer k;

  /**
   * The maximum absolute variance along a coordinate axis. Must be in the range
   * of [0.0, 1.0).
   * <p>
   * Default value: {@link #DEFAULT_ALPHA}
   * </p>
   * <p>
   * Key: {@code -hisc.alpha}
   * </p>
   */
  private final DoubleParameter ALPHA_PARAM = new DoubleParameter(HiSCPreprocessor.ALPHA_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.OPEN), HiSCPreprocessor.DEFAULT_ALPHA);

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public HiSC(Parameterization config) {
    super(config);

    // parameter k
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }

    // parameter alpha
    config.grab(ALPHA_PARAM);
  }

  @Override
  protected ClusterOrderResult<PreferenceVectorBasedCorrelationDistance> runInTime(Database<V> database) throws IllegalStateException {
    // OPTICS
    ListParameterization opticsParameters = new ListParameterization();
    // epsilon and minpts
    opticsParameters.addParameter(OPTICS.EPSILON_ID, AbstractDistance.INFINITY_PATTERN);
    opticsParameters.addParameter(OPTICS.MINPTS_ID, 2);
    // distance function
    opticsParameters.addParameter(OPTICS.DISTANCE_FUNCTION_ID, HiSCDistanceFunction.class.getName());
    opticsParameters.addParameter(HiSCDistanceFunction.EPSILON_ID, ALPHA_PARAM.getValue());
    opticsParameters.addFlag(PreprocessorHandler.OMIT_PREPROCESSING_ID);
    // preprocessor
    opticsParameters.addParameter(PreprocessorHandler.PREPROCESSOR_ID, HiSCPreprocessor.class.getName());
    opticsParameters.addParameter(HiSCPreprocessor.ALPHA_ID, ALPHA_PARAM.getValue());
    if(k != null) {
      opticsParameters.addParameter(HiSCPreprocessor.K_ID, k);
    }

    OPTICS<V, PreferenceVectorBasedCorrelationDistance> optics = new OPTICS<V, PreferenceVectorBasedCorrelationDistance>(opticsParameters);
    optics.setVerbose(isVerbose());
    optics.setTime(isTime());

    return optics.run(database);
  }

}
