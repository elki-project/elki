package experimentalcode.noemi;

import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractDatabaseSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.DatabaseSimilarityFunction;

public class ClusterSimilarityFunction<M extends Model, C extends Cluster<M>> extends AbstractDatabaseSimilarityFunction<Cluster<M>, IntegerDistance> implements DatabaseSimilarityFunction<Cluster<M>, IntegerDistance> {
  /**
   * @param database
   */
  public ClusterSimilarityFunction(Database<Cluster<M>> database) {
    super(database);
  }

  @Override
  public IntegerDistance similarity(DBID id1, DBID id2) {
    ArrayModifiableDBIDs data1 = DBIDUtil.newArray(database.get(id1).getIDs());
    ArrayModifiableDBIDs data2 = DBIDUtil.newArray(database.get(id2).getIDs());
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

  @Override
  public IntegerDistance getDistanceFactory() {
    return IntegerDistance.FACTORY;
  }
}
