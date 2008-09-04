package de.lmu.ifi.dbs.elki.algorithm.result.clustering;

import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;

/**
 * Provides a primitive hierarchical correlation cluster.
 *
 * @author Erich Schubert
 * @param <V> the type of RealVector handled by this Result
 */
public class HierarchicalPlainCluster<O extends DatabaseObject>
    extends HierarchicalCluster<HierarchicalPlainCluster<O>> {

  public HierarchicalPlainCluster() {
    super();
  }

  public HierarchicalPlainCluster(Set<Integer> ids, List<HierarchicalPlainCluster<O>> children, List<HierarchicalPlainCluster<O>> parents, String label, int level, int levelIndex) {
    super(ids, children, parents, label, level, levelIndex);
  }
}
