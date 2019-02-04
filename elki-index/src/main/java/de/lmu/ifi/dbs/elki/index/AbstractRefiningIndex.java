/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Counter;

/**
 * Abstract base class for Filter-refinement indexes.
 * 
 * The number of refinements will be counted as individual page accesses.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @has - - - AbstractRangeQuery
 * @has - - - AbstractKNNQuery
 * 
 * @param <O> Object type
 */
public abstract class AbstractRefiningIndex<O> extends AbstractIndex<O> {
  /**
   * Refinement counter.
   */
  private Counter refinements;

  /**
   * Constructor.
   * 
   * @param relation Relation indexed
   */
  public AbstractRefiningIndex(Relation<O> relation) {
    super(relation);
    Logging log = getLogger();
    refinements = log.isStatistics() ? log.newCounter(this.getClass().getName() + ".refinements") : null;
  }

  /**
   * Get the class logger.
   * 
   * @return Logger
   */
  abstract public Logging getLogger();

  /**
   * Increment the refinement counter, if in use.
   * 
   * @param i Increment.
   */
  protected void countRefinements(int i) {
    if(refinements != null) {
      refinements.increment(i);
    }
  }

  @Override
  public void logStatistics() {
    if(refinements != null) {
      getLogger().statistics(refinements);
    }
  }

  /**
   * Refine a given object (and count the refinement!).
   * 
   * @param id Object id
   * @return refined object
   */
  protected O refine(DBID id) {
    countRefinements(1);
    return relation.get(id);
  }

  /**
   * Range query for this index.
   * 
   * @author Erich Schubert
   * 
   */
  public abstract class AbstractRangeQuery extends AbstractDistanceRangeQuery<O> {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     */
    public AbstractRangeQuery(DistanceQuery<O> distanceQuery) {
      super(distanceQuery);
    }

    /**
     * Refinement distance computation.
     * 
     * @param id Candidate ID
     * @param q Query object
     * @return Distance
     */
    protected double refine(DBIDRef id, O q) {
      AbstractRefiningIndex.this.countRefinements(1);
      return distanceQuery.distance(q, id);
    }

    /**
     * Count extra refinements.
     * 
     * @param c Refinements
     */
    protected void incRefinements(int c) {
      AbstractRefiningIndex.this.countRefinements(c);
    }
  }

  /**
   * KNN query for this index.
   * 
   * @author Erich Schubert
   * 
   */
  public abstract class AbstractKNNQuery extends AbstractDistanceKNNQuery<O> {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     */
    public AbstractKNNQuery(DistanceQuery<O> distanceQuery) {
      super(distanceQuery);
    }

    /**
     * Refinement distance computation.
     * 
     * @param id Candidate ID
     * @param q Query object
     * @return Distance
     */
    protected double refine(DBIDRef id, O q) {
      AbstractRefiningIndex.this.countRefinements(1);
      return distanceQuery.distance(q, id);
    }

    /**
     * Count extra refinements.
     * 
     * @param c Refinements
     */
    protected void incRefinements(int c) {
      AbstractRefiningIndex.this.countRefinements(c);
    }
  }
}
