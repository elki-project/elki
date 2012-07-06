package experimentalcode.students.frankenb;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class PINNKnnQuery implements KNNQuery<NumberVector<?, ?>, DoubleDistance> {
  /**
   * The logger
   */
  private static final Logging logger = Logging.getLogger(PINNKnnQuery.class);

  private final Relation<? extends NumberVector<?, ?>> dataBase;
  private final KDTree tree;
  private final int kFactor;
  
  protected PINNKnnQuery(Relation<? extends NumberVector<?, ?>> database, KDTree tree, int kFactor) {
    this.tree = tree;
    this.dataBase = database;
    this.kFactor = kFactor;
  }
  
  private int requested = 0;
  private long calculations = 0;
  private HashSetModifiableDBIDs alreadyRequestedIDs = DBIDUtil.newHashSet();
  
  @Override
  public KNNResult<DoubleDistance> getKNNForDBID(DBIDRef id, int k) {
    NumberVector<?, ?> vector = dataBase.get(id);
    
    KNNResult<DoubleDistance> projectedDistanceList = tree.findNearestNeighbors(id, this.kFactor*k, EuclideanDistanceFunction.STATIC);
    
    if (!alreadyRequestedIDs.contains(id)) {
      //calculations += tree.getLastMeasure().getCalculations();
      alreadyRequestedIDs.add(id);
      if (alreadyRequestedIDs.size() == dataBase.size()) {
        logger.verbose(String.format("Calculations used: %,d", calculations));
      }
    }
    
    DoubleDistanceKNNHeap newDistanceList = new DoubleDistanceKNNHeap(k);
    for (DistanceDBIDResultIter<DoubleDistance> distance = projectedDistanceList.iter(); distance.valid(); distance.advance()) {
      NumberVector<?, ?> otherVector = dataBase.get(distance);
      newDistanceList.add(DBIDFactory.FACTORY.newDistancePair(EuclideanDistanceFunction.STATIC.doubleDistance(vector, otherVector), distance));
    }
    
    if (++requested % 100000 == 0) {
      logger.debug(String.format("%d distances requested from index", requested));
    }
    
    return newDistanceList.toKNNList();
  }

  @Override
  public List<KNNResult<DoubleDistance>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    List<KNNResult<DoubleDistance>> results = new ArrayList<KNNResult<DoubleDistance>>();
    for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
      results.add(getKNNForDBID(id, k));
    }
    return results;
  }

  @Override
  public KNNResult<DoubleDistance> getKNNForObject(NumberVector<?, ?> obj, int k) {
    // TODO Auto-generated method stub
    return null;
  }
}
