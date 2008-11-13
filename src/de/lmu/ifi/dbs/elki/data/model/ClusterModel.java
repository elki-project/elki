package de.lmu.ifi.dbs.elki.data.model;

/**
 * Generic cluster model. Does not supply additional meta information except that it is a cluster.
 * Since there is no meta information, you should use the static {@link CLUSTER} object.
 * 
 * @author Erich Schubert
 *
 */
public final class ClusterModel extends BaseModel {
  /**
   * Implementation of {@link Model} interface.
   */
  @Override
  public String getSuggestedLabel() {
    return "Cluster";
  }

  /**
   * Static cluster model that can be shared for all clusters (since the object doesn't include meta information.
   */
  public final static ClusterModel CLUSTER = new ClusterModel();
}
