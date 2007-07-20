package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides a hierarchical correlation cluster in an arbitrary subspace
 * that holds the PCA, the ids of the objects
 * belonging to this cluster and the children and parents of this cluster.
 *
 * @author Elke Achtert 
 */
public class HierarchicalCorrelationCluster<V extends RealVector<V,?>> extends HierarchicalCluster<HierarchicalCorrelationCluster<V>> {
  /**
   * The PCA of this cluster.
   */
  private final LocalPCA<V> pca;

  /**
   * The centroid of this cluster.
   */
  private V centroid;

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
  public HierarchicalCorrelationCluster(LocalPCA<V> pca,
                                        Set<Integer> ids,
                                        String label, int level, int levelIndex) {
    this(pca, ids,
         new ArrayList<HierarchicalCorrelationCluster<V>>(),
         new ArrayList<HierarchicalCorrelationCluster<V>>(),
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
  public HierarchicalCorrelationCluster(LocalPCA<V> pca,
                                        Set<Integer> ids,
                                        List<HierarchicalCorrelationCluster<V>> children,
                                        List<HierarchicalCorrelationCluster<V>> parents,
                                        String label, int level, int levelIndex) {
    super(ids, children, parents, label, level, levelIndex);
    this.pca = pca;
  }

  /**
   * Returns the PCA of this cluster.
   *
   * @return the PCA of this cluster
   */
  public LocalPCA<V> getPCA() {
    return pca;
  }

  /**
   * Returns a hash code value for this cluster.
   *
   * @return a hash code value for this cluster
   */
  @Override
public int hashCode() {
    return pca.hashCode();
  }

  /**
   * Sets the centroid of this cluster.
   *
   * @param centroid the centroid to be set
   */
  public void setCentroid(V centroid) {
    this.centroid = centroid;
  }

  /**
   * Returns the centroid of this cluster.
   *
   * @return the centroid of this clusterx
   */
  public V getCentroid() {
    return centroid;
  }
}
