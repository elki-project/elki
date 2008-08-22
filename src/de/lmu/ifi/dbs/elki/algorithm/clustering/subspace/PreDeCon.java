package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.preprocessing.PreDeConPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.Description;

/**
 * <p/>
 * PreDeCon computes clusters of subspace preference weighted connected points.
 * The algorithm searches for local subgroups of a set of feature vectors having
 * a low variance along one or more (but not all) attributes.
 * </p>
 * <p/>
 * Reference:
 * <br>C. Boehm, K. Kailing, H.-P. Kriegel, P. Kroeger:
 * Density Connected Clustering with Local Subspace Preferences.
 * <br>In Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04), Brighton, UK, 2004.
 * </p>
 *
 * @author Peer Kr&ouml;ger
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class PreDeCon<V extends RealVector<V, ?>> extends ProjectedDBSCAN<V> {

    public Description getDescription() {
        return new Description("PreDeCon", "Subspace Preference weighted Density Connected Clustering",
            "PreDeCon computes clusters of subspace preference weighted connected points. " +
                "The algorithm searches for local subgroups of a set of feature vectors having " +
                "a low variance along one or more (but not all) attributes.",
            "C. B\u00F6hm, K. Kailing, H.-P. Kriegel, P. Kr\u00F6ger: " +
                "Density Connected Clustering with Local Subspace Preferences. " +
                "In Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04), Brighton, UK, 2004.");
    }

    public Class<PreDeConPreprocessor> preprocessorClass() {
        return PreDeConPreprocessor.class;
    }


}
