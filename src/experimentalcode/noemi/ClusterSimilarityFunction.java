package experimentalcode.noemi;

import java.util.Arrays;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.distance.IntegerDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractSimilarityFunction;

public class ClusterSimilarityFunction<M extends Model, C extends Cluster<M>> extends AbstractSimilarityFunction<Cluster<M>, IntegerDistance> {
  public ClusterSimilarityFunction() {
    super(new IntegerDistance());
  }
  
  @Override
  public IntegerDistance similarity(Integer id1, Integer id2) {
    Collection<Integer> collection1 = getDatabase().get(id1).getIDs();
    Collection<Integer> collection2 = getDatabase().get(id2).getIDs();
    Integer[] data1 = collection1.toArray(new Integer[collection1.size()]);
    Integer[] data2 = collection2.toArray(new Integer[collection2.size()]);
    Arrays.sort(data1);
    Arrays.sort(data2);
    int intersection = 0;
    for(Integer id: data1){
      if(Arrays.binarySearch(data2, id)>=0){
        intersection++;
      }
    }
    
    return new IntegerDistance(intersection);
  }

}
