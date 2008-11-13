package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.cluster.BaseCluster;
import de.lmu.ifi.dbs.elki.distance.IntegerDistance;

/**
 * @author Arthur Zimek
 */
public class ClusterSimilarity extends AbstractIntegerSimilarityFunction<BaseCluster<?, ?>> {

  public IntegerDistance similarity(Integer id1, Integer id2) {
    BaseCluster<?, ?> cluster1 = getDatabase().get(id1);
    BaseCluster<?, ?> cluster2 = getDatabase().get(id2);
    return similarity(cluster1, cluster2);
  }

  @Override
  public IntegerDistance similarity(BaseCluster<?, ?> o1, BaseCluster<?, ?> o2) {
    Collection<Integer> ids1 = o1.getIDs();
    Collection<Integer> ids2 = o2.getIDs();
    int intersection = 0;
    for(Integer i : ids1) {
      if(ids2.contains(i)) {
        intersection++;
      }
    }
    return new IntegerDistance(intersection);
  }

  @Override
  public IntegerDistance similarity(Integer id1, BaseCluster<?, ?> cluster2) {
    BaseCluster<?, ?> cluster1 = getDatabase().get(id1);
    return super.similarity(cluster1, cluster2);
  }

}
