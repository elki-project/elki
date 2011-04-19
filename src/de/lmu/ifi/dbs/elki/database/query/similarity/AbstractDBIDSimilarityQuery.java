package de.lmu.ifi.dbs.elki.database.query.similarity;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type.
 * @param <D> Distance result type.
 */
public abstract class AbstractDBIDSimilarityQuery<O, D extends Distance<D>> extends AbstractSimilarityQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param rep Representation to use.
   */
  public AbstractDBIDSimilarityQuery(Relation<? extends O> rep) {
    super(rep);
  }

  @SuppressWarnings("unused")
  @Override
  public D similarity(O o1, DBID id2) {
    throw new UnsupportedOperationException("This distance function can only be used for objects when referenced by ID.");
  }

  @SuppressWarnings("unused")
  @Override
  public D similarity(DBID id1, O o2) {
    throw new UnsupportedOperationException("This distance function can only be used for objects when referenced by ID.");
  }

  @SuppressWarnings("unused")
  @Override
  public D similarity(O o1, O o2) {
    throw new UnsupportedOperationException("This distance function can only be used for objects when referenced by ID.");
  }
}