package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.Clustering;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.Cluster;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.SubspaceClusterModel;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.elki.database.SequentialDatabase;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;

import java.util.BitSet;
import java.util.Map;

/**
 * Provides FIRES, a generic framework for Subspace Clustering.
 * <p/>
 * Reference:
 * <br>H.-P. Kriegel, P. Kroeger, M. Renz, S. Wurst:
 * A Generic Framework for Efficient Subspace Clustering of High-Dimensional Data.
 * <br>In: Proc. 5th IEEE International Conference on Data Mining (ICDM), Houston, TX, 2005).
 * </P>
 *
 * @author Arthur Zimek
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class FIRES<V extends RealVector<V, Double>> extends AbstractAlgorithm<V> implements Clustering<V> {

    // todo arthur comment
    private int kMostSimilar;

    // todo arthur comment
    private Clustering<V> preclustering;

    // todo arthur comment
    public FIRES() {
        // TODO arthur
    }

    /**
     * Performs the FIRES algorithm on the given database.
     *
     */
    protected void runInTime(Database<V> database) throws IllegalStateException {
        if (database.size() == 0) {
            throw new IllegalArgumentException("database empty: must contain elements");
        }
        int dim = database.dimensionality();

        // preclustering
        Database<Cluster<V>> clusterDB = new SequentialDatabase<Cluster<V>>();
        for (int d = 0; d < dim; d++) {
            BitSet attributes = new BitSet();
            attributes.set(d + 1);
            // set attributewise distance function
            // TODO distance function
            // TODO parameterize preclustering
            // run attributewise clustering
            preclustering.run(database);
            // append subspace cluster model
            ClusteringResult<V> result = preclustering.getResult();
            Map<SimpleClassLabel, Database<V>> clustering = result.clustering(SimpleClassLabel.class);
            SubspaceClusterModel<V> model = new SubspaceClusterModel<V>(database, attributes);
            for (SimpleClassLabel label : clustering.keySet()) {
                result.appendModel(label, model);
            }
            // save attributewise clusteringresult
            Cluster<V>[] clusters = result.getClusters();
            for (Cluster<V> cluster : clusters) {
                try {
                    clusterDB.insert(new ObjectAndAssociations<Cluster<V>>(cluster, new Associations()));
                }
                catch (UnableToComplyException e) {
                    exception(e.getMessage(), e);
                }
            }
        }
        // cluster approximations

        // pruning

        // refinement

        // TODO Auto-generated method stub

    }

    public Description getDescription() {
        return new Description("FIRES",
            "FIRES",
            "Generic Framework for Subspace Clustering",
            "H.-P. Kriegel, P. Kr\u00F6ger, M. Renz, S. Wurst: " +
                "A Generic Framework for Efficient Subspace Clustering of High-Dimensional Data. " +
                "In: Proc. 5th IEEE International Conference on Data Mining (ICDM), Houston, TX, 2005.");
    }

    public ClusteringResult<V> getResult() {
        // TODO arthur
        return null;
    }
}
