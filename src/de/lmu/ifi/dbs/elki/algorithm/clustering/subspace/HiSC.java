package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.IndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.HiSCDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.HiSCPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
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
 * @apiviz.uses HiSCPreferenceVectorIndex
 * @apiviz.uses HiSCDistanceFunction
 * 
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Finding Hierarchies of Subspace Clusters")
@Description("Algorithm for detecting hierarchies of subspace clusters.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek", title = "Finding Hierarchies of Subspace Clusters", booktitle = "Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in Databases (PKDD'06), Berlin, Germany, 2006", url = "http://www.dbs.ifi.lmu.de/Publikationen/Papers/PKDD06-HiSC.pdf")
public class HiSC<V extends NumberVector<V, ?>> extends OPTICS<V, PreferenceVectorBasedCorrelationDistance> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(HiSC.class);

  /**
   * Constructor.
   *
   * @param distanceFunction HiSC distance function used
   */
  public HiSC(HiSCDistanceFunction<V> distanceFunction) {
    super(distanceFunction, distanceFunction.getDistanceFactory().infiniteDistance(), 2);
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
    HiSCDistanceFunction<V> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter alphaP = new DoubleParameter(HiSCPreferenceVectorIndex.Factory.ALPHA_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.OPEN), HiSCPreferenceVectorIndex.Factory.DEFAULT_ALPHA);
      double alpha = 0.0;
      if(config.grab(alphaP)) {
        alpha = alphaP.getValue();
      }

      // Configure HiSC distance function
      ListParameterization opticsParameters = new ListParameterization();

      // distance function
      opticsParameters.addParameter(HiSCDistanceFunction.EPSILON_ID, alpha);
      // preprocessor
      opticsParameters.addParameter(IndexBasedDistanceFunction.INDEX_ID, HiSCPreferenceVectorIndex.Factory.class);
      opticsParameters.addParameter(HiSCPreferenceVectorIndex.Factory.ALPHA_ID, alpha);

      ChainedParameterization chain = new ChainedParameterization(opticsParameters, config);
      chain.errorsTo(config);
      Class<HiSCDistanceFunction<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(HiSCDistanceFunction.class);
      distanceFunction = chain.tryInstantiate(cls);
    }

    @Override
    protected HiSC<V> makeInstance() {
      return new HiSC<V>(distanceFunction);
    }
  }
}