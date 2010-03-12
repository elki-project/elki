package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.preprocessing.PreDeConPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * <p/>
 * PreDeCon computes clusters of subspace preference weighted connected points.
 * The algorithm searches for local subgroups of a set of feature vectors having
 * a low variance along one or more (but not all) attributes.
 * </p>
 * <p/>
 * Reference: <br>
 * C. B&ouml;hm, K. Kailing, H.-P. Kriegel, P. Kr&ouml;ger: Density Connected
 * Clustering with Local Subspace Preferences. <br>
 * In Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04), Brighton, UK, 2004.
 * </p>
 * 
 * @author Peer Kr&ouml;ger
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("PreDeCon: Subspace Preference weighted Density Connected Clustering")
@Description("PreDeCon computes clusters of subspace preference weighted connected points. " + "The algorithm searches for local subgroups of a set of feature vectors having " + "a low variance along one or more (but not all) attributes.")
@Reference(authors = "C. Böhm, K. Kailing, H.-P. Kriegel, P. Kröger", title = "Density Connected Clustering with Local Subspace Preferences", booktitle = "Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04), Brighton, UK, 2004", url="http://dx.doi.org/10.1109/ICDM.2004.10087")
public class PreDeCon<V extends NumberVector<V, ?>> extends ProjectedDBSCAN<V> {
  /**
   * Constructor.
   * 
   * @param config Configuration
   */
  public PreDeCon(Parameterization config) {
    super(config);
  }

  @Override
  public Class<?> preprocessorClass() {
    return PreDeConPreprocessor.class;
  }
}