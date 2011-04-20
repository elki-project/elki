package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Abstract query bound to a certain representation.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 */
public abstract class AbstractDataBasedQuery<O> implements DatabaseQuery {
  /**
   * The data to use for this query
   */
  final protected Relation<? extends O> rep;

  /**
   * Database this query works on.
   * 
   * @param rep Representation
   */
  public AbstractDataBasedQuery(Relation<? extends O> rep) {
    super();
    this.rep = rep;
  }

  /**
   * Give access to the underlying data query.
   * 
   * @return data query instance
   */
  public Relation<? extends O> getRelation() {
    return rep;
  }
}