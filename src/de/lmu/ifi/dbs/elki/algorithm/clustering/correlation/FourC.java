package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj.FourCSubspaceIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * 4C identifies local subgroups of data objects sharing a uniform correlation.
 * The algorithm is based on a combination of PCA and density-based clustering
 * (DBSCAN).
 * <p>
 * Reference: Christian Böhm, Karin Kailing, Peer Kröger, Arthur Zimek:
 * Computing Clusters of Correlation Connected Objects. <br>
 * In Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses FourCSubspaceIndex
 * 
 * @param <V> type of NumberVector handled by this Algorithm
 */
@Title("4C: Computing Correlation Connected Clusters")
@Description("4C identifies local subgroups of data objects sharing a uniform correlation. " + "The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).")
@Reference(authors = "C. Böhm, K. Kailing, P. Kröger, A. Zimek", title = "Computing Clusters of Correlation Connected Objects", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466", url = "http://dx.doi.org/10.1145/1007568.1007620")
public class FourC<V extends NumberVector<V, ?>> extends AbstractProjectedDBSCAN<V> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(FourC.class);

  /**
   * Constructor.
   * 
   * @param epsilon Epsilon value
   * @param minpts MinPts value
   * @param distanceFunction Distance function
   * @param lambda Lambda value
   */
  public FourC(DoubleDistance epsilon, int minpts, LocallyWeightedDistanceFunction<V> distanceFunction, int lambda) {
    super(epsilon, minpts, distanceFunction, lambda);
  }

  @Override
  public String getLongResultName() {
    return "4C Clustering";
  }

  @Override
  public String getShortResultName() {
    return "4c-clustering";
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
  public static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractProjectedDBSCAN.Parameterizer<O, DoubleDistance> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configInnerDistance(config);
      configEpsilon(config, innerdist);
      configMinPts(config);
      configOuterDistance(config, epsilon, minpts, FourCSubspaceIndex.Factory.class, innerdist);
      configLambda(config);
    }

    @Override
    protected FourC<O> makeInstance() {
      return new FourC<O>(epsilon, minpts, outerdist, lambda);
    }
  }
}