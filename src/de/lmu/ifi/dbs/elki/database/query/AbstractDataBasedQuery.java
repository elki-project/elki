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
  final protected Relation<? extends O> relation;

  /**
   * Database this query works on.
   * 
   * @param relation Representation
   */
  public AbstractDataBasedQuery(Relation<? extends O> relation) {
    super();
    this.relation = relation;
  }

  /**
   * Give access to the underlying data query.
   * 
   * @return data query instance
   */
  public Relation<? extends O> getRelation() {
    return relation;
  }
}