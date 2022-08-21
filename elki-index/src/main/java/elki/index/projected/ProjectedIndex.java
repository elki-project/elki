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
package elki.index.projected;

import elki.data.projection.Projection;
import elki.data.type.TypeInformation;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.ProjectedView;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.*;
import elki.logging.Logging;
import elki.logging.statistics.Counter;
import elki.result.Metadata;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Index data in an arbitrary projection.
 * <p>
 * Note: be <b>careful</b> when using this class, as it may/will yield incorrect
 * distances, depending on your projection! It may be desirable to use a
 * modified index that corrects for this error, or supports specific
 * combinations only.
 * <p>
 * See {@link LatLngAsECEFIndex} and {@link LngLatAsECEFIndex} for example
 * indexes that support only a specific (good) combination.
 * <p>
 * FIXME: add refinement to bulk queries!
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @composed - - - Projection
 * @has - - - ProjectedKNNByObject
 * @has - - - ProjectedRangeByObject
 * @has - - - ProjectedRKNNByObject
 * 
 * @param <O> Outer object type
 * @param <I> Inner object type
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
    Metadata.of(this).setLongName("Projected " + Metadata.of(inner).getLongName());
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
  public void logStatistics() {
    if(refinements != null) {
      LOG.statistics(refinements);
    }
    inner.logStatistics();
  }

  @Override
  public KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if(!(inner instanceof KNNIndex) || distanceQuery.getRelation() != relation || //
        (flags & QueryBuilder.FLAG_EXACT_ONLY) == 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I> innerQuery = ((Distance<? super I>) distanceQuery.getDistance()).instantiate(view);
    @SuppressWarnings("unchecked")
    KNNSearcher<I> innerq = ((KNNIndex<I>) inner).kNNByObject(innerQuery, maxk, flags);
    return innerq != null ? new ProjectedKNNByObject(distanceQuery, innerq) : null;
  }

  @Override
  public KNNSearcher<DBIDRef> kNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if(!(inner instanceof KNNIndex) || distanceQuery.getRelation() != relation || //
        (flags & QueryBuilder.FLAG_EXACT_ONLY) == 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I> innerQuery = ((Distance<? super I>) distanceQuery.getDistance()).instantiate(view);
    @SuppressWarnings("unchecked")
    KNNSearcher<I> innerq = ((KNNIndex<I>) inner).kNNByObject(innerQuery, maxk, flags);
    return innerq != null ? new ProjectedKNNByDBID(distanceQuery, innerq) : null;
  }

  @Override
  public RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    if(!(inner instanceof RangeIndex) || distanceQuery.getRelation() != relation || //
        (flags & QueryBuilder.FLAG_EXACT_ONLY) == 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I> innerQuery = ((Distance<? super I>) distanceQuery.getDistance()).instantiate(view);
    @SuppressWarnings("unchecked")
    RangeSearcher<I> innerq = ((RangeIndex<I>) inner).rangeByObject(innerQuery, maxradius, flags);
    return innerq != null ? new ProjectedRangeByObject(distanceQuery, innerq) : null;
  }

  @Override
  public RangeSearcher<DBIDRef> rangeByDBID(DistanceQuery<O> distanceQuery, double maxradius, int flags) {
    if(!(inner instanceof RangeIndex) || distanceQuery.getRelation() != relation || //
        (flags & QueryBuilder.FLAG_EXACT_ONLY) == 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I> innerQuery = ((Distance<? super I>) distanceQuery.getDistance()).instantiate(view);
    @SuppressWarnings("unchecked")
    RangeSearcher<I> innerq = ((RangeIndex<I>) inner).rangeByObject(innerQuery, maxradius, flags);
    return innerq != null ? new ProjectedRangeByDBID(distanceQuery, innerq) : null;
  }

  @Override
  public RKNNSearcher<O> rkNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if(!(inner instanceof RKNNIndex) || distanceQuery.getRelation() != relation || //
        (flags & QueryBuilder.FLAG_EXACT_ONLY) == 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I> innerQuery = ((Distance<? super I>) distanceQuery.getDistance()).instantiate(view);
    @SuppressWarnings("unchecked")
    RKNNSearcher<I> innerq = ((RKNNIndex<I>) inner).rkNNByObject(innerQuery, maxk, flags);
    return innerq != null ? new ProjectedRKNNByObject(distanceQuery, innerq) : null;
  }

  @Override
  public RKNNSearcher<DBIDRef> rkNNByDBID(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    if(!(inner instanceof RKNNIndex) || distanceQuery.getRelation() != relation || //
        (flags & QueryBuilder.FLAG_EXACT_ONLY) == 0) {
      return null;
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I> innerQuery = ((Distance<? super I>) distanceQuery.getDistance()).instantiate(view);
    @SuppressWarnings("unchecked")
    RKNNSearcher<I> innerq = ((RKNNIndex<I>) inner).rkNNByObject(innerQuery, maxk, flags);
    return innerq != null ? new ProjectedRKNNByDBID(distanceQuery, innerq) : null;
  }

  /**
   * Class to proxy kNN queries.
   * 
   * @author Erich Schubert
   */
  class ProjectedKNNByObject implements KNNSearcher<O> {
    /**
     * Inner kNN query.
     */
    KNNSearcher<I> inner;

    /**
     * Distance query for refinement.
     */
    DistanceQuery<O> distq;

    /**
     * Constructor.
     * 
     * @param inner Inner kNN query.
     */
    public ProjectedKNNByObject(DistanceQuery<O> distanceQuery, KNNSearcher<I> inner) {
      super();
      this.inner = inner;
      this.distq = distanceQuery;
    }

    @Override
    public KNNList getKNN(O obj, int k) {
      final I pobj = proj.project(obj);
      if(norefine) {
        return inner.getKNN(pobj, k);
      }
      KNNList ilist = inner.getKNN(pobj, (int) Math.ceil(k * kmulti));
      KNNHeap heap = DBIDUtil.newHeap(k);
      for(DoubleDBIDListIter iter = ilist.iter(); iter.valid(); iter.advance()) {
        heap.insert(distq.distance(obj, iter), iter);
        countRefinement();
      }
      return heap.toKNNList();
    }
  }

  /**
   * Class to proxy kNN queries.
   * 
   * @author Erich Schubert
   */
  class ProjectedKNNByDBID implements KNNSearcher<DBIDRef> {
    /**
     * Inner kNN query.
     */
    KNNSearcher<I> inner;

    /**
     * Distance query for refinement.
     */
    DistanceQuery<O> distq;

    /**
     * Constructor.
     * 
     * @param inner Inner kNN query.
     */
    public ProjectedKNNByDBID(DistanceQuery<O> distanceQuery, KNNSearcher<I> inner) {
      super();
      this.inner = inner;
      this.distq = distanceQuery;
    }

    @Override
    public KNNList getKNN(DBIDRef id, int k) {
      final O obj = relation.get(id);
      final I pobj = proj.project(obj);
      if(norefine) {
        return inner.getKNN(pobj, k);
      }
      KNNList ilist = inner.getKNN(pobj, (int) Math.ceil(k * kmulti));
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
   */
  class ProjectedRangeByObject implements RangeSearcher<O> {
    /**
     * Hold the distance function to be used.
     */
    protected final DistanceQuery<O> distanceQuery;

    /**
     * Inner range query.
     */
    RangeSearcher<I> inner;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query to use
     * @param inner Inner range query
     */
    public ProjectedRangeByObject(DistanceQuery<O> distanceQuery, RangeSearcher<I> inner) {
      super();
      this.distanceQuery = distanceQuery;
      this.inner = inner;
    }

    @Override
    public ModifiableDoubleDBIDList getRange(O obj, double range, ModifiableDoubleDBIDList result) {
      DoubleDBIDList ilist = inner.getRange(proj.project(obj), range);
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
   * Class to proxy range queries.
   * 
   * @author Erich Schubert
   */
  class ProjectedRangeByDBID implements RangeSearcher<DBIDRef> {
    /**
     * Hold the distance function to be used.
     */
    protected final DistanceQuery<O> distanceQuery;

    /**
     * Inner range query.
     */
    RangeSearcher<I> inner;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query to use
     * @param inner Inner range query
     */
    public ProjectedRangeByDBID(DistanceQuery<O> distanceQuery, RangeSearcher<I> inner) {
      super();
      this.distanceQuery = distanceQuery;
      this.inner = inner;
    }

    @Override
    public ModifiableDoubleDBIDList getRange(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
      final O obj = relation.get(id);
      DoubleDBIDList ilist = inner.getRange(proj.project(obj), range);
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
   */
  class ProjectedRKNNByObject implements RKNNSearcher<O> {
    /**
     * Inner RkNN query.
     */
    RKNNSearcher<I> inner;

    /**
     * Distance query for refinement.
     */
    DistanceQuery<O> distq;

    /**
     * Constructor.
     * 
     * @param inner Inner RkNN query.
     */
    public ProjectedRKNNByObject(DistanceQuery<O> distanceQuery, RKNNSearcher<I> inner) {
      super();
      this.inner = inner;
      this.distq = distanceQuery;
    }

    @Override
    public DoubleDBIDList getRKNN(O obj, int k) {
      final I pobj = proj.project(obj);
      if(norefine) {
        return inner.getRKNN(pobj, k);
      }
      DoubleDBIDList ilist = inner.getRKNN(pobj, (int) Math.ceil(k * kmulti));
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
   * Class to proxy RkNN queries.
   * 
   * @author Erich Schubert
   */
  class ProjectedRKNNByDBID implements RKNNSearcher<DBIDRef> {
    /**
     * Inner RkNN query.
     */
    RKNNSearcher<I> inner;

    /**
     * Distance query for refinement.
     */
    DistanceQuery<O> distq;

    /**
     * Constructor.
     * 
     * @param inner Inner RkNN query.
     */
    public ProjectedRKNNByDBID(DistanceQuery<O> distanceQuery, RKNNSearcher<I> inner) {
      super();
      this.inner = inner;
      this.distq = distanceQuery;
    }

    @Override
    public DoubleDBIDList getRKNN(DBIDRef id, int k) {
      final O obj = relation.get(id);
      final I pobj = proj.project(obj);
      if(norefine) {
        return inner.getRKNN(pobj, k);
      }
      DoubleDBIDList ilist = inner.getRKNN(pobj, (int) Math.ceil(k * kmulti));
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
