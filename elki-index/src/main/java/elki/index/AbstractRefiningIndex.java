/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index;

import elki.database.ids.DBID;
import elki.database.ids.DBIDRef;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.statistics.Counter;

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
public abstract class AbstractRefiningIndex<O> implements Index {
  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

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
    this.relation = relation;
    this.refinements = getLogger().isStatistics() ? getLogger().newCounter(this.getClass().getName() + ".refinements") : null;
  }

  /**
   * Get the class logger.
   * 
   * @return Logger
   */
  public abstract Logging getLogger();

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
   * Abstract query for this index.
   * 
   * @author Erich Schubert
   */
  public abstract class AbstractRefiningQuery {
    /**
     * Distance query.
     */
    protected DistanceQuery<O> distanceQuery;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     */
    public AbstractRefiningQuery(DistanceQuery<O> distanceQuery) {
      super();
      this.distanceQuery = distanceQuery;
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
