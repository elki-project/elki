package experimentalcode.students.andor;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractPrimitiveSimilarityFunction;

public class ClusterSimilarityFunction<C extends Cluster<?>> extends AbstractPrimitiveSimilarityFunction<C, DoubleDistance> {
  public ClusterSimilarityFunction(Database database) {
    super();
  }

  @Override
  public DoubleDistance similarity(C o1, C o2) {
    return new DoubleDistance(DBIDUtil.intersectionSize(o1.getIDs(), o2.getIDs()));
  }

  @Override
  public SimpleTypeInformation<? super Cluster<?>> getInputTypeRestriction() {
    return new SimpleTypeInformation<>(Cluster.class);
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }
}