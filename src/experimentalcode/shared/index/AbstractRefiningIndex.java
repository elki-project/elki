package experimentalcode.shared.index;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;

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
}