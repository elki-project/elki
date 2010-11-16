package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Default linear scan KNN query class.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class LinearScanKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public LinearScanKNNQuery(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database) {
    DistanceQuery<T, D> distanceQuery = distanceFunction.instantiate(database);
    return new Instance<T, D>(database, distanceQuery, k);
  }

  /**
   * Instance of this query for a particular database.
   * 
   * @author Erich Schubert
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceKNNQuery.Instance<O, D> {
    /**
     * The query k
     */
    final int k;

    /**
     * Constructor.
     * 
     * @param database Database to query
     * @param distanceQuery Distance function to use
     */
    public Instance(Database<O> database, DistanceQuery<O, D> distanceQuery, int k) {
      super(database, distanceQuery);
      this.k = k;
    }

    @Override
    public List<DistanceResultPair<D>> getForDBID(DBID id) {
      KNNHeap<D> heap = new KNNHeap<D>(k);
      for(DBID candidateID : database) {
        heap.add(new DistanceResultPair<D>(distanceQuery.distance(id, candidateID), candidateID));
      }
      return heap.toSortedArrayList();
    }

    @Override
    public List<DistanceResultPair<D>> getForObject(O obj) {
      KNNHeap<D> heap = new KNNHeap<D>(k);
      for(DBID candidateID : database) {
        O candidate = database.get(candidateID);
        heap.add(new DistanceResultPair<D>(distanceQuery.distance(obj, candidate), candidateID));
      }
      return heap.toSortedArrayList();
    }
  }
}