package experimentalcode.students.andor;

import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractPrimitiveSimilarityFunction;

public class ClusterSimilarityFunction<C extends Cluster<?>> extends AbstractPrimitiveSimilarityFunction<C, IntegerDistance> {
  /**
   * @param rep
   */
  public ClusterSimilarityFunction(Database<C> database) {
    super();
  }

  @Override
  public IntegerDistance similarity(C o1, C o2) {
    ArrayModifiableDBIDs data1 = DBIDUtil.newArray(o1.getIDs());
    ArrayModifiableDBIDs data2 = DBIDUtil.newArray(o2.getIDs());
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
  public Class<? super C> getInputTypeRestriction() {
    return Cluster.class;
  }

  @Override
  public IntegerDistance getDistanceFactory() {
    return IntegerDistance.FACTORY;
  }
}