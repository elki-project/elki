package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;

import java.util.Map;

/**
 * TODO comment
 *
 * @author Arthur Zimek
 */
public interface ClusteringResult<D extends DatabaseObject> extends Result<D> {
    
    /**
     * Returns the clusters as array of arrays of object ids.
     * 
     * The array must not contain noise objects.
     * 
     * @return the clusters as defined in this result
     */
    Cluster<D>[] getClusters();
    
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
  public <L extends ClassLabel<L>> Map<L, Database<D>> clustering(Class<L> classLabel);

  /**
   * Returns a new Database containing only
   * non-noise Objects with a clusterID
   * associated as {@link AssociationID#CLASS AssociationID#CLASS}.
   *
   * @param classLabel the class to be used as ClassLabel
   * @return a new Database of only non-noise objects
   */
  public <L extends ClassLabel<L>> Database<D> associate(Class<L> classLabel);

  /**
   * todo comment
   *
   * @param clusterID
   * @param model
   */
  public <L extends ClassLabel<L>> void appendModel(L clusterID, Result<D> model);
  

  /**
   * Returns a database containing only noise objects.
   *
   * @return a database containing only noise objects
   */
  public Database<D> noise();
}
