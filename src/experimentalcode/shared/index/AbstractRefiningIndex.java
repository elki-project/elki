package experimentalcode.shared.index;

import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * Abstract base class for Filter-refinement indexes.
 * 
 * The number of refinements will be counted as individual page accesses.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public abstract class AbstractRefiningIndex<O> implements Index, PageFileStatistics {
  /**
   * The representation we are bound to.
   */
  private final Relation<O> relation;

  /**
   * Refinement counter.
   */
  private int refinements;

  /**
   * Constructor.
   * 
   * @param relation Relation indexed
   */
  public AbstractRefiningIndex(Relation<O> relation) {
    super();
    this.relation = relation;
  }

  /**
   * Initialize the index.
   * 
   * @param relation Relation to index
   * @param ids database ids
   */
  abstract protected void initialize(Relation<O> relation, DBIDs ids);

  /**
   * Refine a given object (and count the refinement!)
   * 
   * @param id Object id
   * @return refined object
   */
  protected O refine(DBID id) {
    refinements++;
    return relation.get(id);
  }

  @Override
  abstract public String getLongName();

  @Override
  abstract public String getShortName();

  @Override
  public PageFileStatistics getPageFileStatistics() {
    return this;
  }

  @Override
  public long getReadOperations() {
    return refinements;
  }

  @Override
  public long getWriteOperations() {
    return 0;
  }

  @Override
  public void resetPageAccess() {
    refinements = 0;
  }

  @Override
  public PageFileStatistics getInnerStatistics() {
    return null;
  }

  @Override
  public void insert(DBID id) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @Override
  public void insertAll(DBIDs ids) {
    initialize(relation, ids);
  }

  @Override
  public boolean delete(DBID id) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @Override
  public void deleteAll(DBIDs id) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  /**
   * Range query for this index.
   * 
   * @author Erich Schubert
   */
  public abstract class AbstractRangeQuery<D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
    /**
     * Hold the distance function to be used.
     */
    private DistanceQuery<O, D> distanceQuery;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     */
    public AbstractRangeQuery(DistanceQuery<O, D> distanceQuery) {
      super(distanceQuery);
      this.distanceQuery = distanceQuery;
    }

    @Override
    public List<DistanceResultPair<D>> getRangeForDBID(DBID id, D range) {
      return getRangeForObject(relation.get(id), range);
    }

    /**
     * Refinement distance computation.
     * 
     * @param id Candidate ID
     * @param q Query object
     * @return Distance
     */
    protected D refine(DBID id, O q) {
      AbstractRefiningIndex.this.refinements++;
      return distanceQuery.distance(q, id);
    }
  }

  /**
   * KNN query for this index.
   * 
   * @author Erich Schubert
   */
  abstract public class AbstractKNNQuery<D extends Distance<D>> implements KNNQuery<O, D> {
    /**
     * Hold the distance function to be used.
     */
    private DistanceQuery<O, D> distanceQuery;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     */
    public AbstractKNNQuery(DistanceQuery<O, D> distanceQuery) {
      super();
      this.distanceQuery = distanceQuery;
    }

    @Override
    public List<List<DistanceResultPair<D>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
      throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void getKNNForBulkHeaps(Map<DBID, KNNHeap<D>> heaps) {
      throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k) {
      return getKNNForObject(relation.get(id), k);
    }

    /**
     * Refinement distance computation.
     * 
     * @param id Candidate ID
     * @param q Query object
     * @return Distance
     */
    protected D refine(DBID id, O q) {
      AbstractRefiningIndex.this.refinements++;
      return distanceQuery.distance(q, id);
    }
  }
}