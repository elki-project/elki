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
package elki.index.projected;

import elki.data.projection.Projection;
import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.query.rknn.RKNNQuery;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.ProjectedView;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.*;
import elki.logging.Logging;
import elki.logging.statistics.Counter;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Class to index data in an arbitrary projection only.
 * 
 * Note: be <b>careful</b> when using this class, as it may/will yield incorrect
 * distances, depending on your projection! It may be desirable to use a
 * modified index that corrects for this error, or supports specific
 * combiantions only.
 * 
 * See {@link LatLngAsECEFIndex} and {@link LngLatAsECEFIndex} for example
 * indexes that support only a specific (good) combination.
 * 
 * FIXME: add refinement to bulk queries!
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @composed - - - Projection
 * @has - - - ProjectedKNNQuery
 * @has - - - ProjectedRangeQuery
 * @has - - - ProjectedRKNNQuery
 * 
 * @param <O> Outer object type.
 * @param <I> Inner object type.
 */
public class ProjectedIndex<O, I> implements KNNIndex<O>, RKNNIndex<O>, RangeIndex<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ProjectedIndex.class);

  /**
   * Inner index.
   */
  Index inner;

  /**
   * Projection.
   */
  Projection<O, I> proj;

  /**
   * The relation we predend to index.
   */
  Relation<? extends O> relation;

  /**
   * The view that we really index.
   */
  Relation<I> view;

  /**
   * Refinement disable flag.
   */
  boolean norefine;

  /**
   * Multiplier for k.
   */
  double kmulti = 1.0;

  /**
   * Count the number of distance refinements computed.
   */
  final Counter refinements;

  /**
   * Constructor.
   * 
   * @param relation Relation to index.
   * @param proj Projection to use.
   * @param view View to use.
   * @param inner Index to wrap.
   * @param norefine Refinement disable flag.
   * @param kmulti Multiplicator for k
   */
  public ProjectedIndex(Relation<? extends O> relation, Projection<O, I> proj, Relation<I> view, Index inner, boolean norefine, double kmulti) {
    super();
    this.relation = relation;
    this.view = view;
    this.inner = inner;
    this.proj = proj;
    this.norefine = norefine;
    this.kmulti = kmulti;
    this.refinements = LOG.isStatistics() ? LOG.newCounter(this.getClass().getName() + ".refinements") : null;
  }

  /**
   * Count a single distance refinement.
   */
  private void countRefinement() {
    if(refinements != null) {
      refinements.increment();
    }
  }

  @Override
  public void initialize() {
    inner.initialize();
  }

  @Override
  public String getLongName() {
    return "projected " + inner.getLongName();
  }

  @Override
  public String getShortName() {
    return "proj-" + inner.getShortName();
  }

  @Override
  public void logStatistics() {
    if(refinements != null) {
      LOG.statistics(refinements);
    }
    inner.logStatistics();
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if(!(inner instanceof KNNIndex) || distanceQuery.getRelation() != relation || //
        (flags & QueryBuilder.FLAG_EXACT_ONLY) == 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I> innerQuery = ((Distance<? super I>) distanceQuery.getDistance()).instantiate(view);
    @SuppressWarnings("unchecked")
    KNNQuery<I> innerq = ((KNNIndex<I>) inner).getKNNQuery(innerQuery, maxk, flags);
    return innerq != null ? new ProjectedKNNQuery(distanceQuery, innerq) : null;
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    if(!(inner instanceof RangeIndex) || distanceQuery.getRelation() != relation || //
        (flags & QueryBuilder.FLAG_EXACT_ONLY) == 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I> innerQuery = ((Distance<? super I>) distanceQuery.getDistance()).instantiate(view);
    @SuppressWarnings("unchecked")
    RangeQuery<I> innerq = ((RangeIndex<I>) inner).getRangeQuery(innerQuery, maxradius, flags);
    return innerq != null ? new ProjectedRangeQuery(distanceQuery, innerq) : null;
  }

  @Override
  public RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if(!(inner instanceof RKNNIndex) || distanceQuery.getRelation() != relation || //
        (flags & QueryBuilder.FLAG_EXACT_ONLY) == 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I> innerQuery = ((Distance<? super I>) distanceQuery.getDistance()).instantiate(view);
    @SuppressWarnings("unchecked")
    RKNNQuery<I> innerq = ((RKNNIndex<I>) inner).getRKNNQuery(innerQuery, maxk, flags);
    return innerq != null ? new ProjectedRKNNQuery(distanceQuery, innerq) : null;
  }

  /**
   * Class to proxy kNN queries.
   * 
   * @author Erich Schubert
   * 
   * @param Distance type
   */
  class ProjectedKNNQuery implements KNNQuery<O> {
    /**
     * Inner kNN query.
     */
    KNNQuery<I> inner;

    /**
     * Distance query for refinement.
     */
    DistanceQuery<O> distq;

    /**
     * Constructor.
     * 
     * @param inner Inner kNN query.
     */
    public ProjectedKNNQuery(DistanceQuery<O> distanceQuery, KNNQuery<I> inner) {
      super();
      this.inner = inner;
      this.distq = distanceQuery;
    }

    @Override
    public KNNList getKNNForDBID(DBIDRef id, int k) {
      // So we have to project the query point only once:
      return getKNNForObject(relation.get(id), k);
    }

    @Override
    public KNNList getKNNForObject(O obj, int k) {
      final I pobj = proj.project(obj);
      if(norefine) {
        return inner.getKNNForObject(pobj, k);
      }
      KNNList ilist = inner.getKNNForObject(pobj, (int) Math.ceil(k * kmulti));
      KNNHeap heap = DBIDUtil.newHeap(k);
      for(DoubleDBIDListIter iter = ilist.iter(); iter.valid(); iter.advance()) {
        heap.insert(distq.distance(obj, iter), iter);
        countRefinement();
      }
      return heap.toKNNList();
    }
  }

  /**
   * Class to proxy range queries.
   * 
   * @author Erich Schubert
   * 
   * @param Distance type
   */
  class ProjectedRangeQuery implements RangeQuery<O> {
    /**
     * Hold the distance function to be used.
     */
    protected final DistanceQuery<O> distanceQuery;

    /**
     * Inner range query.
     */
    RangeQuery<I> inner;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query to use
     * @param inner Inner range query
     */
    public ProjectedRangeQuery(DistanceQuery<O> distanceQuery, RangeQuery<I> inner) {
      super();
      this.distanceQuery = distanceQuery;
      this.inner = inner;
    }

    @Override
    public ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
      return getRangeForObject(relation.get(id), range, result);
    }

    @Override
    public ModifiableDoubleDBIDList getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
      final I pobj = proj.project(obj);
      DoubleDBIDList ilist = inner.getRangeForObject(pobj, range);
      if(norefine) {
        // Copy without refinement:
        for(DoubleDBIDListIter iter = ilist.iter(); iter.valid(); iter.advance()) {
          result.add(iter.doubleValue(), iter);
        }
        return result;
      }
      for(DoubleDBIDListIter iter = ilist.iter(); iter.valid(); iter.advance()) {
        double dist = distanceQuery.distance(obj, iter);
        countRefinement();
        if(range <= dist) {
          result.add(dist, iter);
        }
      }
      return result;
    }
  }

  /**
   * Class to proxy RkNN queries.
   * 
   * @author Erich Schubert
   * 
   * @param Distance type
   */
  class ProjectedRKNNQuery implements RKNNQuery<O> {
    /**
     * Inner RkNN query.
     */
    RKNNQuery<I> inner;

    /**
     * Distance query for refinement.
     */
    DistanceQuery<O> distq;

    /**
     * Constructor.
     * 
     * @param inner Inner RkNN query.
     */
    public ProjectedRKNNQuery(DistanceQuery<O> distanceQuery, RKNNQuery<I> inner) {
      super();
      this.inner = inner;
      this.distq = distanceQuery;
    }

    @Override
    public DoubleDBIDList getRKNNForDBID(DBIDRef id, int k) {
      // So we have to project the query point only once:
      return getRKNNForObject(relation.get(id), k);
    }

    @Override
    public DoubleDBIDList getRKNNForObject(O obj, int k) {
      final I pobj = proj.project(obj);
      if(norefine) {
        return inner.getRKNNForObject(pobj, k);
      }
      DoubleDBIDList ilist = inner.getRKNNForObject(pobj, (int) Math.ceil(k * kmulti));
      ModifiableDoubleDBIDList olist = DBIDUtil.newDistanceDBIDList(ilist.size());
      for(DoubleDBIDListIter iter = ilist.iter(); iter.valid(); iter.advance()) {
        double dist = distq.distance(obj, iter);
        countRefinement();
        olist.add(dist, iter);
      }
      return olist;
    }
  }

  /**
   * Index factory.
   * 
   * @author Erich Schubert
   * 
   * @has - - - ProjectedIndex
   * 
   * @param <O> Outer object type.
   * @param <I> Inner object type.
   */
  public static class Factory<O, I> implements IndexFactory<O> {
    /**
     * Projection to use.
     */
    Projection<O, I> proj;

    /**
     * Inner index factory.
     */
    IndexFactory<I> inner;

    /**
     * Whether to use a materialized view, or a virtual view.
     */
    boolean materialize = false;

    /**
     * Disable refinement of distances.
     */
    boolean norefine = false;

    /**
     * Multiplier for k.
     */
    double kmulti = 1.0;

    /**
     * Constructor.
     * 
     * @param proj Projection
     * @param inner Inner index
     * @param materialize Flag for materializing
     * @param norefine Disable refinement of distances
     * @param kmulti Multiplicator for k.
     */
    public Factory(Projection<O, I> proj, IndexFactory<I> inner, boolean materialize, boolean norefine, double kmulti) {
      super();
      this.proj = proj;
      this.inner = inner;
      this.materialize = materialize;
      this.norefine = norefine;
      this.kmulti = kmulti;
    }

    @Override
    public ProjectedIndex<O, I> instantiate(Relation<O> relation) {
      if(!proj.getInputDataTypeInformation().isAssignableFromType(relation.getDataTypeInformation())) {
        return null;
      }
      // FIXME: non re-entrant!
      proj.initialize(relation.getDataTypeInformation());
      Index inneri = null;
      Relation<I> view = null;
      if(materialize) {
        DBIDs ids = relation.getDBIDs();
        WritableDataStore<I> content = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, proj.getOutputDataTypeInformation().getRestrictionClass());
        for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          content.put(iter, proj.project(relation.get(iter)));
        }
        view = new MaterializedRelation<>("Projected Index", proj.getOutputDataTypeInformation(), ids, content);
      }
      else {
        view = new ProjectedView<>(relation, proj);
      }
      inneri = inner.instantiate(view);
      if(inneri == null) {
        return null;
      }
      return new ProjectedIndex<>(relation, proj, view, inneri, norefine, kmulti);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return proj.getInputDataTypeInformation();
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @hidden
     * 
     * @param <O> Outer object type.
     * @param <I> Inner object type.
     */
    public static class Par<O, I> implements Parameterizer {
      /**
       * Option ID for the projection to use.
       */
      public static final OptionID PROJ_ID = new OptionID("projindex.proj", "Projection to use for the projected index.");

      /**
       * Option ID for the inner index to use.
       */
      public static final OptionID INDEX_ID = new OptionID("projindex.inner", "Index to use on the projected data.");

      /**
       * Option ID for materialization.
       */
      public static final OptionID MATERIALIZE_FLAG = new OptionID("projindex.materialize", "Flag to materialize the projected data.");

      /**
       * Option ID for disabling refinement.
       */
      public static final OptionID DISABLE_REFINE_FLAG = new OptionID("projindex.disable-refine", "Flag to disable refinement of distances.");

      /**
       * Option ID for querying a larger k.
       */
      public static final OptionID K_MULTIPLIER_ID = new OptionID("projindex.kmulti", "Multiplier for k.");

      /**
       * Projection to use.
       */
      Projection<O, I> proj;

      /**
       * Inner index factory.
       */
      IndexFactory<I> inner;

      /**
       * Whether to use a materialized view, or a virtual view.
       */
      boolean materialize = false;

      /**
       * Disable refinement of distances.
       */
      boolean norefine = false;

      /**
       * Multiplier for k.
       */
      double kmulti = 1.0;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<Projection<O, I>>(PROJ_ID, Projection.class) //
            .grab(config, x -> proj = x);
        new ObjectParameter<IndexFactory<I>>(INDEX_ID, IndexFactory.class) //
            .grab(config, x -> inner = x);
        new Flag(MATERIALIZE_FLAG).grab(config, x -> materialize = x);
        new Flag(DISABLE_REFINE_FLAG).grab(config, x -> norefine = x);
        if(!norefine) {
          new DoubleParameter(K_MULTIPLIER_ID) //
              .setDefaultValue(1.0) //
              .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE) //
              .grab(config, x -> kmulti = x);
        }
      }

      @Override
      public Factory<O, I> make() {
        return new Factory<>(proj, inner, materialize, norefine, kmulti);
      }
    }
  }
}
