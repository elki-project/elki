package experimentalcode.students.frankenb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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
    
    KNNHeap<DoubleDistance> newDistanceList = new KNNHeap<DoubleDistance>(k);
    for (DistanceResultPair<DoubleDistance> distance : projectedDistanceList) {
      NumberVector<?, ?> otherVector = dataBase.get(distance.getDBID());
      newDistanceList.add(new DoubleDistanceResultPair(EuclideanDistanceFunction.STATIC.doubleDistance(vector, otherVector), distance.getDBID()));
    }
    
    if (++requested % 100000 == 0) {
      logger.debug(String.format("%d distances requested from index", requested));
    }
    
    return newDistanceList.toKNNList();
  }

  @Override
  public List<KNNResult<DoubleDistance>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    List<KNNResult<DoubleDistance>> results = new ArrayList<KNNResult<DoubleDistance>>();
    for (DBID dbid : ids) {
      results.add(getKNNForDBID(dbid, k));
    }
    return results;
  }

  @Override
  public KNNResult<DoubleDistance> getKNNForObject(NumberVector<?, ?> obj, int k) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void getKNNForBulkHeaps(Map<DBID, KNNHeap<DoubleDistance>> heaps) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }
}
