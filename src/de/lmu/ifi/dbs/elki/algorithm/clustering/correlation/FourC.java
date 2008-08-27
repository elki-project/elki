package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.preprocessing.FourCPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.Description;

/**
 * 4C identifies local subgroups of data objects sharing a uniform correlation.
 * The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).
 * <p>Reference:
 * Christian Boehm, Karin Kailing, Peer Kroeger, Arthur Zimek:
 * Computing Clusters of Correlation Connected Objects.
 * <br>In Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004.
 * </p>
 *
 * @author Arthur Zimek
 * @param <O> type of DatabaseObjects handled by this Algorithm
 */
public class FourC<O extends RealVector<O, ?>> extends ProjectedDBSCAN<O> {

    public Description getDescription() {
        return new Description("4C", "Computing Correlation Connected Clusters",
            "4C identifies local subgroups of data objects sharing a uniform correlation. " +
                "The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).",
            "Christian B\u00F6hm, Karin Kailing, Peer Kr\u00F6ger, Arthur Zimek: " +
                "Computing Clusters of Correlation Connected Objects., " +
                "In Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466.");
    }


    public Class<?> preprocessorClass() {
        return FourCPreprocessor.class;
    }
}
