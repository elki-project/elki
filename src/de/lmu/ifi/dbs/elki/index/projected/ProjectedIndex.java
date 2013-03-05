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
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.ProjectedView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
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
 * @author Erich Schubert
 * 
 * @param <O> Outer object type.
 * @param <I> Inner object type.
 */
public class ProjectedIndex<O, I> implements KNNIndex<O>, RKNNIndex<O>, RangeIndex<O> {
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
   * Constructor.
   * 
   * @param inner Index to wrap.
   */
  public ProjectedIndex(Relation<O> relation, Projection<O, I> proj, Relation<I> view, Index inner) {
    super();
    this.relation = relation;
    this.view = view;
    this.inner = inner;
    this.proj = proj;
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
    // No statistics to log.
  }

  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if (!(inner instanceof KNNIndex)) {
      return null;
    }
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    for (Object o : hints) {
      if (o == DatabaseQuery.HINT_EXACT) {
        return null;
      }
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I, D> innerQuery = ((DistanceFunction<? super I, D>) distanceQuery.getDistanceFunction()).instantiate(view);
    @SuppressWarnings("unchecked")
    KNNQuery<I, D> innerq = ((KNNIndex<I>) inner).getKNNQuery(innerQuery, hints);
    if (innerq == null) {
      return null;
    }
    return new ProjectedKNNQuery<>(innerq);
  }

  @Override
  public <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if (!(inner instanceof RangeIndex)) {
      return null;
    }
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    for (Object o : hints) {
      if (o == DatabaseQuery.HINT_EXACT) {
        return null;
      }
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I, D> innerQuery = ((DistanceFunction<? super I, D>) distanceQuery.getDistanceFunction()).instantiate(view);
    @SuppressWarnings("unchecked")
    RangeQuery<I, D> innerq = ((RangeIndex<I>) inner).getRangeQuery(innerQuery, hints);
    if (innerq == null) {
      return null;
    }
    return new ProjectedRangeQuery<>(innerq);
  }

  @Override
  public <D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if (!(inner instanceof RKNNIndex)) {
      return null;
    }
    if (distanceQuery.getRelation() != relation) {
      return null;
    }
    for (Object o : hints) {
      if (o == DatabaseQuery.HINT_EXACT) {
        return null;
      }
    }
    @SuppressWarnings("unchecked")
    DistanceQuery<I, D> innerQuery = ((DistanceFunction<? super I, D>) distanceQuery.getDistanceFunction()).instantiate(view);
    @SuppressWarnings("unchecked")
    RKNNQuery<I, D> innerq = ((RKNNIndex<I>) inner).getRKNNQuery(innerQuery, hints);
    if (innerq == null) {
      return null;
    }
    return new ProjectedRKNNQuery<>(innerq);
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
     * Constructor.
     * 
     * @param inner Inner kNN query.
     */
    public ProjectedKNNQuery(KNNQuery<I, D> inner) {
      super();
      this.inner = inner;
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

    @Override
    public KNNList<D> getKNNForObject(O obj, int k) {
      final I pobj = proj.project(obj);
      return inner.getKNNForObject(pobj, k);
    }
  }

  /**
   * Class to proxy range queries.
   * 
   * @author Erich Schubert
   * 
   * @param <D> Distance type
   */
  class ProjectedRangeQuery<D extends Distance<D>> implements RangeQuery<O, D> {
    /**
     * Inner range query.
     */
    RangeQuery<I, D> inner;

    /**
     * Constructor.
     * 
     * @param inner Inner range query.
     */
    public ProjectedRangeQuery(RangeQuery<I, D> inner) {
      super();
      this.inner = inner;
    }

    @Override
    public DistanceDBIDList<D> getRangeForDBID(DBIDRef id, D range) {
      // So we have to project the query point only once:
      return getRangeForObject(relation.get(id), range);
    }

    @Override
    public DistanceDBIDList<D> getRangeForObject(O obj, D range) {
      final I pobj = proj.project(obj);
      return inner.getRangeForObject(pobj, range);
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
     * Constructor.
     * 
     * @param inner Inner RkNN query.
     */
    public ProjectedRKNNQuery(RKNNQuery<I, D> inner) {
      super();
      this.inner = inner;
    }

    @Override
    public DistanceDBIDList<D> getRKNNForDBID(DBIDRef id, int k) {
      // So we have to project the query point only once:
      return getRKNNForObject(relation.get(id), k);
    }

    @Override
    public DistanceDBIDList<D> getRKNNForObject(O obj, int k) {
      final I pobj = proj.project(obj);
      return inner.getRKNNForObject(pobj, k);
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
     * Constructor.
     * 
     * @param proj Projection
     * @param inner Inner index
     * @param materialize Flag for materializing
     */
    public Factory(Projection<O, I> proj, IndexFactory<I, ?> inner, boolean materialize) {
      super();
      this.proj = proj;
      this.inner = inner;
      this.materialize = materialize;
    }

    @Override
    public ProjectedIndex<O, I> instantiate(Relation<O> relation) {
      if (!proj.getInputDataTypeInformation().isAssignableFromType(relation.getDataTypeInformation())) {
        return null;
      }
      // FIXME: non re-entrant!
      proj.initialize(relation.getDataTypeInformation());
      Index inneri = null;
      Relation<I> view = null;
      if (materialize) {
        DBIDs ids = relation.getDBIDs();
        WritableDataStore<I> content = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, proj.getOutputDataTypeInformation().getRestrictionClass());
        for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
          content.put(iter, proj.project(relation.get(iter)));
        }
        view = new MaterializedRelation<>(relation.getDatabase(), proj.getOutputDataTypeInformation(), ids, "projected data", content);
      } else {
        view = new ProjectedView<>(relation, proj);
      }
      inneri = inner.instantiate(view);
      if (inneri == null) {
        return null;
      }
      return new ProjectedIndex<>(relation, proj, view, inneri);
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

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        ObjectParameter<Projection<O, I>> projP = new ObjectParameter<>(PROJ_ID, Projection.class);
        if (config.grab(projP)) {
          proj = projP.instantiateClass(config);
        }
        ObjectParameter<IndexFactory<I, ?>> innerP = new ObjectParameter<>(INDEX_ID, IndexFactory.class);
        if (config.grab(innerP)) {
          inner = innerP.instantiateClass(config);
        }
        Flag materializeF = new Flag(MATERIALIZE_FLAG);
        if (config.grab(materializeF)) {
          materialize = materializeF.isTrue();
        }
      }

      @Override
      protected Factory<O, I> makeInstance() {
        return new Factory<>(proj, inner, materialize);
      }
    }
  }
}
