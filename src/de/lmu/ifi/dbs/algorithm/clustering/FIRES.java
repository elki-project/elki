package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.Cluster;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.SubspaceClusterModel;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.Associations;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.util.BitSet;
import java.util.Map;

/**
 * todo arthur comment class, parameters, constructor
 *
 * @author Arthur Zimek
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class FIRES<V extends RealVector<V, Double>> extends AbstractAlgorithm<V> implements Clustering<V> {
    private int kMostSimilar;

    private Clustering<V> preclustering;

    /**
     *
     */
    public FIRES() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
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


    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("FIRES",
            "FIRES",
            "Generic Framework for Subspace Clustering",
            "H.-P. Kriegel, P. Kr\u00F6ger, M. Renz, S. Wurst: " +
                "A Generic Framework for Efficient Subspace Clustering of High-Dimensional Data, " +
                "In: Proc. 5th IEEE International Conference on Data Mining (ICDM), Houston, TX, 2005");
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public ClusteringResult<V> getResult() {
        // TODO Auto-generated method stub
        return null;
    }
}
