package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.distancefunction.AbstractLocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.PreDeConPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;

/**
 * PreDeCon computes clusters of subspace preference weighted connected points.
 * The algorithm searches for local subgroups of a set of feature vectors having
 * a low variance along one or more (but not all) attributes.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PreDeCon<O extends RealVector<O,?>> extends ProjectedDBSCAN<O, PreDeConPreprocessor<? extends AbstractLocallyWeightedDistanceFunction<O,?>>> {

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("PreDeCon", "Subspace Preference weighted Density Connected Clustering",
                           "PreDeCon computes clusters of subspace preference weighted connected points. " +
                           "The algorithm searches for local subgroups of a set of feature vectors having " +
                           "a low variance along one or more (but not all) attributes.",
                           "Christian B\u00F6hm, Karin Kailing, Hans-Peter Kriegel, Peer Kr\u00F6ger: " +
                           "Density Connected Clustering with Local Subspace Preferences, " +
                           "In Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04), Brighton, UK, 2004");
  }


  /**
   * @see ProjectedDBSCAN#preprocessorClass()
   */
  public Class preprocessorClass() {
    return PreDeConPreprocessor.class;
  }


}
