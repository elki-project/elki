package experimentalcode.noemi;

import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractSimilarityFunction;

public class ClusterSimilarityFunction<M extends Model, C extends Cluster<M>> extends AbstractSimilarityFunction<Cluster<M>, IntegerDistance> {
  public ClusterSimilarityFunction() {
    super(IntegerDistance.FACTORY);
  }
  
  @Override
  public IntegerDistance similarity(DBID id1, DBID id2) {
    ArrayModifiableDBIDs data1 = DBIDUtil.newArray(getDatabase().get(id1).getIDs());
    ArrayModifiableDBIDs data2 = DBIDUtil.newArray(getDatabase().get(id2).getIDs());
    Collections.sort(data1);
    Collections.sort(data2);
    int intersection = 0;
    for(DBID id: data1){
      if(Collections.binarySearch(data2, id)>=0){
        intersection++;
      }
    }
    
    return new IntegerDistance(intersection);
  }

  @Override
  public Class<? super Cluster<M>> getInputDatatype() {
    return Cluster.class;
  }
}
