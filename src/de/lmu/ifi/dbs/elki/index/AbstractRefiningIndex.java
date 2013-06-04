package de.lmu.ifi.dbs.elki.index;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Counter;

/**
 * Abstract base class for Filter-refinement indexes.
 * 
 * The number of refinements will be counted as individual page accesses.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.excludeSubtypes
 * @apiviz.has AbstractRangeQuery
 * @apiviz.has AbstractKNNQuery
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
    if (refinements != null) {
      refinements.increment(i);
    }
  }

  @Override
  public void logStatistics() {
    if (refinements != null) {
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
   * @apiviz.excludeSubtypes
   */
  public abstract class AbstractRangeQuery<D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     */
    public AbstractRangeQuery(DistanceQuery<O, D> distanceQuery) {
      super(distanceQuery);
    }

    /**
     * Refinement distance computation.
     * 
     * @param id Candidate ID
     * @param q Query object
     * @return Distance
     */
    protected D refine(DBIDRef id, O q) {
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
   * @apiviz.excludeSubtypes
   */
  public abstract class AbstractKNNQuery<D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     */
    public AbstractKNNQuery(DistanceQuery<O, D> distanceQuery) {
      super(distanceQuery);
    }

    /**
     * Refinement distance computation.
     * 
     * @param id Candidate ID
     * @param q Query object
     * @return Distance
     */
    protected D refine(DBID id, O q) {
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
