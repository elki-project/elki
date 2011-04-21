package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.IndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.HiSCDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.AbstractDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.HiSCPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Implementation of the HiSC algorithm, an algorithm for detecting hierarchies
 * of subspace clusters.
 * <p>
 * Reference: E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman,
 * A. Zimek: Finding Hierarchies of Subspace Clusters. <br>
 * In: Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery
 * in Databases (PKDD'06), Berlin, Germany, 2006.
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses HiSCPreprocessor
 * @apiviz.uses HiSCDistanceFunction
 * 
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Finding Hierarchies of Subspace Clusters")
@Description("Algorithm for detecting hierarchies of subspace clusters.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek", title = "Finding Hierarchies of Subspace Clusters", booktitle = "Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in Databases (PKDD'06), Berlin, Germany, 2006", url = "http://www.dbs.ifi.lmu.de/Publikationen/Papers/PKDD06-HiSC.pdf")
public class HiSC<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V> {
  // TODO: make this a subclass of OPTICS.

  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(HiSC.class);

  /**
   * Internal OPTICS instance.
   */
  private OPTICS<V, PreferenceVectorBasedCorrelationDistance> optics;

  /**
   * Constructor.
   * 
   * @param optics OPTICS to use
   */
  public HiSC(OPTICS<V, PreferenceVectorBasedCorrelationDistance> optics) {
    this.optics = optics;
  }

  @Override
  public ClusterOrderResult<PreferenceVectorBasedCorrelationDistance> run(Database database) throws IllegalStateException {
    return optics.run(database);
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
    protected OPTICS<V, PreferenceVectorBasedCorrelationDistance> optics;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter alphaP = new DoubleParameter(HiSCPreferenceVectorIndex.Factory.ALPHA_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.OPEN), HiSCPreferenceVectorIndex.Factory.DEFAULT_ALPHA);
      double alpha = 0.0;
      if(config.grab(alphaP)) {
        alpha = alphaP.getValue();
      }

      // Configure OPTICS
      ListParameterization opticsParameters = new ListParameterization();

      // epsilon and minpts
      opticsParameters.addParameter(OPTICS.EPSILON_ID, AbstractDistance.INFINITY_PATTERN);
      opticsParameters.addParameter(OPTICS.MINPTS_ID, 2);
      // distance function
      opticsParameters.addParameter(OPTICS.DISTANCE_FUNCTION_ID, HiSCDistanceFunction.class);
      opticsParameters.addParameter(HiSCDistanceFunction.EPSILON_ID, alpha);
      // opticsParameters.addFlag(PreprocessorHandler.OMIT_PREPROCESSING_ID);
      // preprocessor
      opticsParameters.addParameter(IndexBasedDistanceFunction.INDEX_ID, HiSCPreferenceVectorIndex.Factory.class);
      opticsParameters.addParameter(HiSCPreferenceVectorIndex.Factory.ALPHA_ID, alpha);

      ChainedParameterization chain = new ChainedParameterization(opticsParameters, config);
      chain.errorsTo(config);
      Class<OPTICS<V, PreferenceVectorBasedCorrelationDistance>> cls = ClassGenericsUtil.uglyCastIntoSubclass(OPTICS.class);
      optics = chain.tryInstantiate(cls);
    }

    @Override
    protected HiSC<V> makeInstance() {
      return new HiSC<V>(optics);
    }
  }
}