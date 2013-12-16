package de.lmu.ifi.dbs.elki.index.projected;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.projection.Projection;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPairList;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.distance.ModifiableDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.ModifiableDoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.generic.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.AbstractDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.ProjectedView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Counter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * 
 * @apiviz.composedOf ProjectedKNNQuery
 * @apiviz.composedOf ProjectedRangeQuery
 * @apiviz.composedOf ProjectedRKNNQuery
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
  Relation<O> relation;

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
  public ProjectedIndex(Relation<O> relation, Projection<O, I> proj, Relation<I> view, Index inner, boolean norefine, double kmulti) {
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
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(!(inner instanceof KNNIndex)) {
      return null;
    }
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    for(Object o : hints) {
      if(o == DatabaseQuery.HINT_EXACT) {
        return null;
      }
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I, D> innerQuery = ((DistanceFunction<? super I, D>) distanceQuery.getDistanceFunction()).instantiate(view);
    @SuppressWarnings("unchecked")
    KNNQuery<I, D> innerq = ((KNNIndex<I>) inner).getKNNQuery(innerQuery, hints);
    if(innerq == null) {
      return null;
    }
    return new ProjectedKNNQuery<>(distanceQuery, innerq);
  }

  @Override
  public <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(!(inner instanceof RangeIndex)) {
      return null;
    }
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    for(Object o : hints) {
      if(o == DatabaseQuery.HINT_EXACT) {
        return null;
      }
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I, D> innerQuery = ((DistanceFunction<? super I, D>) distanceQuery.getDistanceFunction()).instantiate(view);
    @SuppressWarnings("unchecked")
    RangeQuery<I, D> innerq = ((RangeIndex<I>) inner).getRangeQuery(innerQuery, hints);
    if(innerq == null) {
      return null;
    }
    return new ProjectedRangeQuery<>(distanceQuery, innerq);
  }

  @Override
  public <D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(!(inner instanceof RKNNIndex)) {
      return null;
    }
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    for(Object o : hints) {
      if(o == DatabaseQuery.HINT_EXACT) {
        return null;
      }
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I, D> innerQuery = ((DistanceFunction<? super I, D>) distanceQuery.getDistanceFunction()).instantiate(view);
    @SuppressWarnings("unchecked")
    RKNNQuery<I, D> innerq = ((RKNNIndex<I>) inner).getRKNNQuery(innerQuery, hints);
    if(innerq == null) {
      return null;
    }
    return new ProjectedRKNNQuery<>(distanceQuery, innerq);
  }

  /**
   * Class to proxy kNN queries.
   * 
   * @author Erich Schubert
   * 
   * @param <D> Distance type
   */
  class ProjectedKNNQuery<D extends Distance<D>> implements KNNQuery<O, D> {
    /**
     * Inner kNN query.
     */
    KNNQuery<I, D> inner;

    /**
     * Distance query for refinement.
     */
    DistanceQuery<O, D> distq;

    /**
     * Constructor.
     * 
     * @param inner Inner kNN query.
     */
    public ProjectedKNNQuery(DistanceQuery<O, D> distanceQuery, KNNQuery<I, D> inner) {
      super();
      this.inner = inner;
      this.distq = distanceQuery;
    }

    @Override
    public KNNList<D> getKNNForDBID(DBIDRef id, int k) {
      // So we have to project the query point only once:
      return getKNNForObject(relation.get(id), k);
    }

    @Override
    public List<? extends KNNList<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
      return inner.getKNNForBulkDBIDs(ids, k);
    }

    @SuppressWarnings("unchecked")
    @Override
    public KNNList<D> getKNNForObject(O obj, int k) {
      final I pobj = proj.project(obj);
      if(norefine) {
        return inner.getKNNForObject(pobj, k);
      }
      KNNList<D> ilist = inner.getKNNForObject(pobj, (int) Math.ceil(k * kmulti));
      if(distq.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction) {
        PrimitiveDoubleDistanceFunction<? super O> df = (PrimitiveDoubleDistanceFunction<? super O>) distq.getDistanceFunction();
        DoubleDistanceKNNHeap heap = DBIDUtil.newDoubleDistanceHeap(k);
        for(DistanceDBIDListIter<D> iter = ilist.iter(); iter.valid(); iter.advance()) {
          heap.insert(df.doubleDistance(obj, distq.getRelation().get(iter)), iter);
          countRefinement();
        }
        return (KNNList<D>) heap.toKNNList();
      }
      else {
        KNNHeap<D> heap = DBIDUtil.newHeap(distq.getDistanceFactory(), k);
        for(DistanceDBIDListIter<D> iter = ilist.iter(); iter.valid(); iter.advance()) {
          heap.insert(distq.distance(obj, iter), iter);
          countRefinement();
        }
        return heap.toKNNList();
      }
    }
  }

  /**
   * Class to proxy range queries.
   * 
   * @author Erich Schubert
   * 
   * @param <D> Distance type
   */
  class ProjectedRangeQuery<D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
    /**
     * Inner range query.
     */
    RangeQuery<I, D> inner;

    /**
     * Constructor.
     * 
     * @param inner Inner range query.
     */
    public ProjectedRangeQuery(DistanceQuery<O, D> distanceQuery, RangeQuery<I, D> inner) {
      super(distanceQuery);
      this.inner = inner;
    }

    @Override
    public DistanceDBIDList<D> getRangeForDBID(DBIDRef id, D range) {
      // So we have to project the query point only once:
      return getRangeForObject(relation.get(id), range);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public DistanceDBIDList<D> getRangeForObject(O obj, D range) {
      final I pobj = proj.project(obj);
      DistanceDBIDList<D> ilist = inner.getRangeForObject(pobj, range);
      if(norefine) {
        return ilist;
      }
      if(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction) {
        PrimitiveDoubleDistanceFunction<? super O> df = (PrimitiveDoubleDistanceFunction<? super O>) distanceQuery.getDistanceFunction();
        double drange = ((DoubleDistance) range).doubleValue();
        ModifiableDoubleDistanceDBIDList olist = new DoubleDistanceDBIDPairList(ilist.size());
        for(DistanceDBIDListIter<D> iter = ilist.iter(); iter.valid(); iter.advance()) {
          final double dist = df.doubleDistance(obj, distanceQuery.getRelation().get(iter));
          countRefinement();
          if(dist <= drange) {
            olist.add(dist, iter);
          }
        }
        return (DistanceDBIDList<D>) olist;
      }
      else {
        ModifiableDistanceDBIDList<D> olist = new GenericDistanceDBIDList<>(ilist.size());
        for(DistanceDBIDListIter<D> iter = ilist.iter(); iter.valid(); iter.advance()) {
          D dist = distanceQuery.distance(obj, iter);
          countRefinement();
          if(range.compareTo(dist) <= 0) {
            olist.add(dist, iter);
          }
        }
        return olist;
      }
    }
  }

  /**
   * Class to proxy RkNN queries.
   * 
   * @author Erich Schubert
   * 
   * @param <D> Distance type
   */
  class ProjectedRKNNQuery<D extends Distance<D>> implements RKNNQuery<O, D> {
    /**
     * Inner RkNN query.
     */
    RKNNQuery<I, D> inner;

    /**
     * Distance query for refinement.
     */
    DistanceQuery<O, D> distq;

    /**
     * Constructor.
     * 
     * @param inner Inner RkNN query.
     */
    public ProjectedRKNNQuery(DistanceQuery<O, D> distanceQuery, RKNNQuery<I, D> inner) {
      super();
      this.inner = inner;
      this.distq = distanceQuery;
    }

    @Override
    public DistanceDBIDList<D> getRKNNForDBID(DBIDRef id, int k) {
      // So we have to project the query point only once:
      return getRKNNForObject(relation.get(id), k);
    }

    @SuppressWarnings("unchecked")
    @Override
    public DistanceDBIDList<D> getRKNNForObject(O obj, int k) {
      final I pobj = proj.project(obj);
      if(norefine) {
        return inner.getRKNNForObject(pobj, k);
      }
      DistanceDBIDList<D> ilist = inner.getRKNNForObject(pobj, (int) Math.ceil(k * kmulti));
      if(distq.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction) {
        PrimitiveDoubleDistanceFunction<? super O> df = (PrimitiveDoubleDistanceFunction<? super O>) distq.getDistanceFunction();
        ModifiableDoubleDistanceDBIDList olist = new DoubleDistanceDBIDPairList(ilist.size());
        for(DistanceDBIDListIter<D> iter = ilist.iter(); iter.valid(); iter.advance()) {
          final double dist = df.doubleDistance(obj, distq.getRelation().get(iter));
          countRefinement();
          olist.add(dist, iter);
        }
        return (DistanceDBIDList<D>) olist;
      }
      else {
        ModifiableDistanceDBIDList<D> olist = new GenericDistanceDBIDList<>(ilist.size());
        for(DistanceDBIDListIter<D> iter = ilist.iter(); iter.valid(); iter.advance()) {
          D dist = distq.distance(obj, iter);
          countRefinement();
          olist.add(dist, iter);
        }
        return olist;
      }
    }

    @Override
    public List<? extends DistanceDBIDList<D>> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
      return inner.getRKNNForBulkDBIDs(ids, k);
    }
  }

  /**
   * Index factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has ProjectedIndex
   * 
   * @param <O> Outer object type.
   * @param <I> Inner object type.
   */
  public static class Factory<O, I> implements IndexFactory<O, ProjectedIndex<O, I>> {
    /**
     * Projection to use.
     */
    Projection<O, I> proj;

    /**
     * Inner index factory.
     */
    IndexFactory<I, ?> inner;

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
    public Factory(Projection<O, I> proj, IndexFactory<I, ?> inner, boolean materialize, boolean norefine, double kmulti) {
      super();
      this.proj = proj;
      this.inner = inner;
      this.materialize = materialize;
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
        view = new MaterializedRelation<>("Projected Index", "projected-index", proj.getOutputDataTypeInformation(), content, ids);
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
     * @apiviz.exclude
     * 
     * @param <O> Outer object type.
     * @param <I> Inner object type.
     */
    public static class Parameterizer<O, I> extends AbstractParameterizer {
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
      IndexFactory<I, ?> inner;

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
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        ObjectParameter<Projection<O, I>> projP = new ObjectParameter<>(PROJ_ID, Projection.class);
        if(config.grab(projP)) {
          proj = projP.instantiateClass(config);
        }
        ObjectParameter<IndexFactory<I, ?>> innerP = new ObjectParameter<>(INDEX_ID, IndexFactory.class);
        if(config.grab(innerP)) {
          inner = innerP.instantiateClass(config);
        }
        Flag materializeF = new Flag(MATERIALIZE_FLAG);
        if(config.grab(materializeF)) {
          materialize = materializeF.isTrue();
        }
        Flag norefineF = new Flag(DISABLE_REFINE_FLAG);
        if(config.grab(norefineF)) {
          norefine = norefineF.isTrue();
        }
        if(!norefine) {
          DoubleParameter kmultP = new DoubleParameter(K_MULTIPLIER_ID);
          kmultP.setDefaultValue(1.0);
          kmultP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE);
          if(config.grab(kmultP)) {
            kmulti = kmultP.doubleValue();
          }
        }
      }

      @Override
      protected Factory<O, I> makeInstance() {
        return new Factory<>(proj, inner, materialize, norefine, kmulti);
      }
    }
  }
}
