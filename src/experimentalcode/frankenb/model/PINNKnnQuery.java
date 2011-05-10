package experimentalcode.frankenb.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
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
  private Set<Integer> alreadyRequestedIDs = new HashSet<Integer>();
  
  @Override
  public List<DistanceResultPair<DoubleDistance>> getKNNForDBID(DBID id, int k) {
    NumberVector<?, ?> vector = dataBase.get(id);
    
    DistanceList projectedDistanceList = tree.findNearestNeighbors(id, this.kFactor*k, EuclideanDistanceFunction.STATIC);
    List<DistanceResultPair<DoubleDistance>> list = new ArrayList<DistanceResultPair<DoubleDistance>>();
    
    if (!alreadyRequestedIDs.contains(id.getIntegerID())) {
      //calculations += tree.getLastMeasure().getCalculations();
      alreadyRequestedIDs.add(id.getIntegerID());
      if (alreadyRequestedIDs.size() == dataBase.size()) {
        logger.verbose(String.format("Calculations used: %,d", calculations));
      }
    }
    
    DistanceList newDistanceList = new DistanceList(id, k);
    for (Pair<DBID, Double> distance : projectedDistanceList) {
      NumberVector<?, ?> otherVector = dataBase.get(distance.first);
      newDistanceList.addDistance(distance.first, EuclideanDistanceFunction.STATIC.doubleDistance(vector, otherVector));
    }
    
    for (Pair<DBID, Double> distance : newDistanceList) {
      list.add(new DistanceResultPair<DoubleDistance>(new DoubleDistance(distance.second), distance.first));
    }
    
    if (++requested % 100000 == 0) {
      logger.debug(String.format("%d distances requested from index", requested));
    }
    
    
    return list;
  }

  @Override
  public List<List<DistanceResultPair<DoubleDistance>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    List<List<DistanceResultPair<DoubleDistance>>> results = new ArrayList<List<DistanceResultPair<DoubleDistance>>>();
    for (DBID dbid : ids) {
      results.add(getKNNForDBID(dbid, k));
    }
    return results;
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getKNNForObject(NumberVector<?, ?> obj, int k) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DistanceQuery<NumberVector<?, ?>, DoubleDistance> getDistanceQuery() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Relation<? extends NumberVector<?, ?>> getRelation() {
    return dataBase;
  }
}
