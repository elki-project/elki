package de.lmu.ifi.dbs.elki.algorithm.result.clustering;

import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;

import java.util.Map;

/**
 * A Result that provides a set of disjunct clusters and a mapping from
 * classlabels (supposedly assigned by the algorithm) to databases.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Result
 */
public interface ClusteringResult<O extends DatabaseObject> extends Result<O> {

    /**
     * Returns the clusters as array of arrays of object ids.
     * <p/>
     * The array must not contain noise objects.
     *
     * @return the clusters as defined in this result
     */
    Cluster<O>[] getClusters();

    /**
     * Returns a Map of ClassLabel to Database,
     * comprising a separate database for each cluster.
     * Note that if the clustering result contains noise,
     * the noise-objects are not comprised in this map.
     *
     * @param classLabel the class to be used as ClassLabel
     * @return Map of ClassLabel to Database,
     *         comprising a separate database for each cluster
     *         without noise.
     */
    public <L extends ClassLabel<L>> Map<L, Database<O>> clustering(Class<L> classLabel);

    /**
     * Returns a new Database containing only
     * non-noise Objects with a clusterID
     * associated as {@link AssociationID#CLASS AssociationID#CLASS}.
     *
     * @param classLabel the class to be used as ClassLabel
     * @return a new Database of only non-noise objects
     */
    public <L extends ClassLabel<L>> Database<O> associate(Class<L> classLabel);

    /**
     * Appends a model the designated cluster.
     *
     * @param clusterID ClassLabel assigned to the cluster a model should be appended to
     * @param model     the model describing the designated cluster
     */
    public <L extends ClassLabel<L>> void appendModel(L clusterID, Result<O> model);


    /**
     * Returns a database containing only noise objects.
     *
     * @return a database containing only noise objects
     */
    public Database<O> noise();
}
