package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.AbstractLocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.FourCPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;

/**
 * 4C identifies local subgroups of data objects sharing a uniform correlation.
 * The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourC<O extends RealVector<O,? extends Number>,D extends Distance<D>> extends ProjectedDBSCAN<O, FourCPreprocessor<? extends AbstractLocallyWeightedDistanceFunction<O,?>,O>> {

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("4C", "Computing Correlation Connected Clusters",
                           "4C identifies local subgroups of data objects sharing a uniform correlation. " +
                           "The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).",
                           "Christian B\u00F6hm, Karin Kailing, Peer Kr\u00F6ger, Arthur Zimek: " +
                           "Computing Clusters of Correlation Connected Objects, " +
                           "In Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466");
  }


  @Override
  public Class<FourCPreprocessor> preprocessorClass() {
    return FourCPreprocessor.class;
  }
}
