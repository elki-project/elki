package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a hierarchical correlation cluster in an arbitrary subspace
 * that holds the PCA, the ids of the objects
 * belonging to this cluster and the children and parents of this cluster.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HierarchicalCorrelationCluster extends HierarchicalCluster<HierarchicalCorrelationCluster> {
  /**
   * The PCA of this cluster.
   */
  private final LocalPCA pca;

  /**
   * The centroid of this cluster.
   */
  private RealVector centroid;

  /**
   * Provides a new hierarchical correlation cluster with the
   * specified parameters.
   *
   * @param pca        the PCA of this cluster
   * @param ids        the ids of the objects belonging to this cluster
   * @param label      the label of this cluster
   * @param level      the level of this cluster in the graph
   * @param levelIndex the index of this cluster within the level
   */
  public HierarchicalCorrelationCluster(LocalPCA pca,
                                        List<Integer> ids,
                                        String label, int level, int levelIndex) {
    this(pca, ids,
         new ArrayList<HierarchicalCorrelationCluster>(),
         new ArrayList<HierarchicalCorrelationCluster>(),
         label, level, levelIndex);
  }

  /**
   * Provides a hierarchical correlation cluster in an arbitrary subspace
   * that holds the basis vectors of this cluster, the similarity matrix for
   * distance computations, the ids of the objects
   * belonging to this cluster and the children and parents of this cluster.
   *
   * @param pca        the PCA of this cluster
   * @param ids        the ids of the objects belonging to this cluster
   * @param children   the list of children of this cluster
   * @param parents    the list of parents of this cluster
   * @param label      the label of this cluster
   * @param level      the level of this cluster in the graph
   * @param levelIndex the index of this cluster within the level
   */
  public HierarchicalCorrelationCluster(LocalPCA pca,
                                        List<Integer> ids,
                                        List<HierarchicalCorrelationCluster> children,
                                        List<HierarchicalCorrelationCluster> parents,
                                        String label, int level, int levelIndex) {
    super(ids, children, parents, label, level, levelIndex);
    this.pca = pca;
  }

  /**
   * Returns the PCA of this cluster.
   *
   * @return the PCA of this cluster
   */
  public LocalPCA getPCA() {
    return pca;
  }

  /**
   * Returns a hash code value for this cluster.
   *
   * @return a hash code value for this cluster
   */
  public int hashCode() {
    return pca.hashCode();
  }

  /**
   * Sets the centroid of this cluster.
   *
   * @param centroid the centroid to be set
   */
  public void setCentroid(RealVector centroid) {
    this.centroid = centroid;
  }

  /**
   * Returns the centroid of this cluster.
   *
   * @return the centroid of this clusterx
   */
  public RealVector getCentroid() {
    return centroid;
  }
}
