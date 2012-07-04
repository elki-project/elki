package de.lmu.ifi.dbs.elki.index;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.AbstractDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;

/**
 * Abstract base class for Filter-refinement indexes.
 * 
 * The number of refinements will be counted as individual page accesses.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has AbstractRangeQuery
 * @apiviz.has AbstractKNNQuery
 * 
 * @param <O> Object type
 */
public abstract class AbstractRefiningIndex<O> extends AbstractIndex<O> implements PageFileStatistics {
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
    super(relation);
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
  public void insertAll(DBIDs ids) {
    initialize(relation, ids);
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
    public DistanceDBIDResult<D> getRangeForDBID(DBIDRef id, D range) {
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

    /**
     * Count extra refinements.
     * 
     * @param c Refinements
     */
    protected void incRefinements(int c) {
      AbstractRefiningIndex.this.refinements += c;
    }
  }

  /**
   * KNN query for this index.
   * 
   * @author Erich Schubert
   */
  abstract public class AbstractKNNQuery<D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query object
     */
    public AbstractKNNQuery(DistanceQuery<O, D> distanceQuery) {
      super(distanceQuery);
    }

    @Override
    public List<KNNResult<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
      throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public KNNResult<D> getKNNForDBID(DBIDRef id, int k) {
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

    /**
     * Count extra refinements.
     * 
     * @param c Refinements
     */
    protected void incRefinements(int c) {
      AbstractRefiningIndex.this.refinements += c;
    }
  }
}