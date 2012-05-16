package experimentalcode.erich.approxknn;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.AbstractSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.PeanoSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import experimentalcode.erich.HilbertSpatialSorter;

/**
 * Compute the nearest neighbors approximatively using space filling curves.
 * 
 * @author Erich Schubert
 */
public class SpacefillingKNNPreprocessor<O extends NumberVector<?, ?>> extends AbstractIndex<O> implements KNNIndex<O> {
  /**
   * Curve storage
   */
  List<List<SpatialRef>> curves = null;

  /**
   * Curve position storage
   */
  WritableDataStore<int[]> positions = null;

  /**
   * Curve window size
   */
  int window;

  /**
   * Constructor.
   * 
   * @param relation Relation to index.
   */
  public SpacefillingKNNPreprocessor(Relation<O> relation) {
    super(relation);
  }

  @Override
  public void insertAll(DBIDs ids) {
    if(curves == null) {
      if(ids.size() > 0) {
        preprocess();
      }
    }
    else {
      throw new UnsupportedOperationException("Preprocessor already ran.");
    }
  }

  protected void preprocess() {
    final int size = relation.size();

    int numcurves = 9;
    curves = new ArrayList<List<SpatialRef>>(numcurves);
    for(int i = 0; i < numcurves; i++) {
      curves.add(new ArrayList<SpatialRef>(size));
    }
    {
      for(DBID id : relation.iterDBIDs()) {
        final NumberVector<?, ?> v = relation.get(id);
        SpatialRef ref = new SpatialRef(id, v);
        for(List<SpatialRef> curve : curves) {
          curve.add(ref);
        }
      }

      // Sort spatially
      double[] mms = AbstractSpatialSorter.computeMinMax(curves.get(0));
      (new ZCurveSpatialSorter()).sort(curves.get(0), 0, size, mms);
      (new PeanoSpatialSorter()).sort(curves.get(1), 0, size, mms);
      (new HilbertSpatialSorter()).sort(curves.get(2), 0, size, mms);
      double[] mms2 = new double[mms.length];
      double[] mms3 = new double[mms.length];
      for(int i = 0; i < mms.length; i += 2) {
        double len = mms[i + 1] - mms[i];
        mms2[i] = mms[i] - len * .1234;
        mms2[i + 1] = mms[i + 1] + len * .3784123;
        mms3[i] = mms[i] - len * .321078;
        mms3[i + 1] = mms[i + 1] + len * .51824172;
      }
      (new ZCurveSpatialSorter()).sort(curves.get(3), 0, size, mms2);
      (new PeanoSpatialSorter()).sort(curves.get(4), 0, size, mms2);
      (new HilbertSpatialSorter()).sort(curves.get(5), 0, size, mms2);
      (new ZCurveSpatialSorter()).sort(curves.get(6), 0, size, mms3);
      (new PeanoSpatialSorter()).sort(curves.get(7), 0, size, mms3);
      (new HilbertSpatialSorter()).sort(curves.get(8), 0, size, mms3);
    }
    // Build position index, DBID -> position in the three curves
    positions = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, int[].class);
    for(int cnum = 0; cnum < numcurves; cnum++) {
      Iterator<SpatialRef> it = curves.get(cnum).iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        final int[] data;
        if(cnum == 0) {
          data = new int[numcurves];
          positions.put(r.id, data);
        }
        else {
          data = positions.get(r.id);
        }
        data[cnum] = i;
      }
    }
  }

  @Override
  public String getLongName() {
    return "Space Filling Curve KNN preprocessor";
  }

  @Override
  public String getShortName() {
    return "spacefilling-knn";
  }

  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    for(Object hint : hints) {
      if(DatabaseQuery.HINT_EXACT == hint) {
        return null;
      }
    }
    return new SpaceFillingKNNQuery<D>(distanceQuery);
  }

  /**
   * KNN Query processor for space filling curves
   * 
   * @author Erich Schubert
   * 
   * @param <D> Distance type
   */
  protected class SpaceFillingKNNQuery<D extends Distance<D>> implements KNNQuery<O, D> {
    /**
     * Distance query to use for refinement
     */
    DistanceQuery<O, D> distq;

    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query to use for refinement
     */
    public SpaceFillingKNNQuery(DistanceQuery<O, D> distanceQuery) {
      super();
      this.distq = distanceQuery;
    }

    @Override
    public KNNResult<D> getKNNForDBID(DBID id, int k) {
      // Build candidates
      ModifiableDBIDs cands = DBIDUtil.newHashSet(window * curves.size());
      final int[] posi = positions.get(id);
      for(int i = 0; i < posi.length; i++) {
        List<SpatialRef> curve = curves.get(i);
        final int start = Math.max(0, posi[i] - window);
        final int end = Math.min(posi[i] + window + 1, curve.size());
        for(int j = start; j < end; j++) {
          cands.add(curve.get(j).id);
        }
      }
      // Refine:
      KNNHeap<D> heap = new KNNHeap<D>(k);
      final O vec = relation.get(id);
      for(DBID cand : cands) {
        heap.add(new GenericDistanceResultPair<D>(distq.distance(vec, cand), cand));
      }
      return heap.toKNNList();
    }

    @Override
    public List<KNNResult<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
      throw new AbortException("Not yet implemented");
    }

    @Override
    public void getKNNForBulkHeaps(Map<DBID, KNNHeap<D>> heaps) {
      throw new AbortException("Not yet implemented");
    }

    @Override
    public KNNResult<D> getKNNForObject(O obj, int k) {
      throw new AbortException("Not yet implemented");
    }
  }

  /**
   * Object used in spatial sorting, combining the spatial object and the object
   * ID.
   * 
   * @author Erich Schubert
   */
  static class SpatialRef implements SpatialComparable {
    protected DBID id;

    protected NumberVector<?, ?> vec;

    /**
     * Constructor.
     * 
     * @param id
     * @param vec
     */
    protected SpatialRef(DBID id, NumberVector<?, ?> vec) {
      super();
      this.id = id;
      this.vec = vec;
    }

    @Override
    public int getDimensionality() {
      return vec.getDimensionality();
    }

    @Override
    public double getMin(int dimension) {
      return vec.getMin(dimension);
    }

    @Override
    public double getMax(int dimension) {
      return vec.getMax(dimension);
    }
  }
}