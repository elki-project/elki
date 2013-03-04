package experimentalcode.erich.approxknn;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.AbstractSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.SpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * Compute the nearest neighbors approximatively using space filling curves.
 * 
 * @author Erich Schubert
 */
public class SpacefillingMaterializeKNNPreprocessor<O extends NumberVector<?>, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor<O, D, KNNList<D>> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SpacefillingMaterializeKNNPreprocessor.class);

  static {
    LoggingConfiguration.setLevelFor(SpacefillingMaterializeKNNPreprocessor.class.getName(), Level.INFO.getName());
  }

  /**
   * Spatial curve generators
   */
  final List<SpatialSorter> curvegen;

  /**
   * Curve window size
   */
  final double window;

  /**
   * Number of variants to generate for each curve
   */
  final int variants;

  /**
   * Constructor.
   * 
   * @param relation Relation to index.
   * @param distanceFunction Distance function
   * @param k k
   * @param curvegen Curve generators
   * @param window Window multiplicator
   * @param variants Number of curve variants to generate
   */
  public SpacefillingMaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k, List<SpatialSorter> curvegen, double window, int variants) {
    super(relation, distanceFunction, k);
    this.curvegen = curvegen;
    this.window = window;
    this.variants = variants;
  }

  @Override
  protected void preprocess() {
    // Prepare space filling curve:
    final long start = System.nanoTime();
    final int size = relation.size();
    final int wsize = (int) (window * (k - 1));

    final int numgen = curvegen.size();
    final int numcurves = numgen * variants;
    List<List<SpatialRef>> curves = new ArrayList<>(numcurves);
    for(int i = 0; i < numcurves; i++) {
      curves.add(new ArrayList<SpatialRef>(size));
    }

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBID id  = DBIDUtil.deref(iditer);
      final O v = relation.get(id);
      SpatialRef ref = new SpatialRef(id, v);
      for(List<SpatialRef> curve : curves) {
        curve.add(ref);
      }
    }

    // Sort spatially
    double[] mms = AbstractSpatialSorter.computeMinMax(curves.get(0));
    for(int j = 0; j < variants; j++) {
      final double[] mm = setupMinMax(mms, j);
      for(int i = 0; i < numgen; i++) {
        curvegen.get(i).sort(curves.get(i + numgen * j), 0, size, mm);
      }
    }

    // Build position index, DBID -> position in the three curves
    WritableDataStore<int[]> positions = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, int[].class);
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

    Mean mean = new Mean();

    // Convert to final storage
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNList.class);
    KNNHeap<D> heap = DBIDUtil.newHeap(distanceQuery.getDistanceFactory(), k);
    HashSetModifiableDBIDs cands = DBIDUtil.newHashSet(wsize * numcurves * 2);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final D n = distanceQuery.getDistanceFactory().nullDistance();
      heap.add(n, iditer);

      // Get candidates.
      cands.clear();
      int[] ps = positions.get(iditer);
      int distcomp = 0;
      for(int i = 0; i < numcurves; i++) {
        distcomp += scanCurve(curves.get(i), wsize, ps[i], cands, heap);
      }

      storage.put(iditer, heap.toKNNList());
      mean.put(distcomp);
    }

    final long end = System.nanoTime();
    if(LOG.isVerbose()) {
      LOG.verbose("SFC preprocessor took " + ((end - start) / 1.E6) + " milliseconds and " + mean.getMean() + " distance computations on average.");
    }
  }

  /**
   * Perform a window scan on the curve.
   * 
   * @param curve Curve to process
   * @param wsize Window size
   * @param heap Heap
   * @param tempstorage Temporary storage
   * @return number of distance computations
   */
  private int scanCurve(List<SpatialRef> curve, final int wsize, int p, HashSetModifiableDBIDs cands, KNNHeap<D> heap) {
    int distcomp = 0;
    final int win2size = wsize * 2 + 1;
    final int start, end;
    if(p < wsize) {
      start = 0;
      end = win2size;
    }
    else if(p + win2size > curve.size()) {
      start = curve.size() - win2size;
      end = curve.size();
    }
    else {
      start = p - wsize;
      end = p + wsize + 1;
    }

    O q = curve.get(p).vec;
    for(int pos = start; pos < end; pos++) {
      if(pos == p) {
        continue;
      }
      SpatialRef ref2 = curve.get(pos);
      if(cands.add(ref2.id)) {
        heap.add(distanceQuery.distance(q, ref2.vec), ref2.id);
        distcomp++;
      }
    }
    return distcomp;
  }

  protected double[] setupMinMax(final double[] mms, int variant) {
    final double[] mm;
    {
      if(variant == 0) {
        mm = mms;
      }
      else if(variant == 1) {
        // Hardcoded for publication CIKM12
        mm = new double[mms.length];
        for(int i = 0; i < mms.length; i += 2) {
          double len = mms[i + 1] - mms[i];
          mm[i] = mms[i] - len * .1234;
          mm[i + 1] = mms[i + 1] + len * .3784123;
        }
      }
      else if(variant == 2) {
        // Hardcoded for publication CIKM12
        mm = new double[mms.length];
        for(int i = 0; i < mms.length; i += 2) {
          double len = mms[i + 1] - mms[i];
          mm[i] = mms[i] - len * .321078;
          mm[i + 1] = mms[i + 1] + len * .51824172;
        }
      }
      else {
        throw new AbortException("Currently, only 1-3 variants may be used!");
      }
    }
    return mm;
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
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public void logStatistics() {
    // Nothing to do, yet.
  }

  /**
   * Object used in spatial sorting, combining the spatial object and the object
   * ID.
   * 
   * @author Erich Schubert
   */
  class SpatialRef implements SpatialComparable {
    protected DBID id;

    protected O vec;

    /**
     * Constructor.
     * 
     * @param id
     * @param vec
     */
    protected SpatialRef(DBID id, O vec) {
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

  /**
   * Index factory class
   * 
   * @author Erich Schubert
   * 
   * @param <V> Vector type
   * @param <D> Distance type
   */
  public static class Factory<V extends NumberVector<?>, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory<V, D, KNNList<D>> {
    /**
     * Spatial curve generators
     */
    List<SpatialSorter> curvegen;

    /**
     * Curve window size
     */
    double window;

    /**
     * Number of variants to generate for each curve
     */
    int variants;

    /**
     * Constructor.
     * 
     * @param curvegen Curve generators
     * @param window Window multiplicator
     * @param variants Number of curve variants to generate
     */
    public Factory(int k, DistanceFunction<? super V, D> distanceFunction, List<SpatialSorter> curvegen, double window, int variants) {
      super(k, distanceFunction);
      this.curvegen = curvegen;
      this.window = window;
      this.variants = variants;
    }

    @Override
    public SpacefillingMaterializeKNNPreprocessor<V, D> instantiate(Relation<V> relation) {
      return new SpacefillingMaterializeKNNPreprocessor<>(relation, distanceFunction, k, curvegen, window, variants);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     * 
     * @param <V> Vector type
     * @param <D> Distance type
     */
    public static class Parameterizer<V extends NumberVector<?>, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<V, D> {
      /**
       * Parameter for choosing the space filling curves to use.
       */
      public static final OptionID CURVES_ID = new OptionID("sfcknn.curves", "Space filling curve generators to use for kNN approximation.");

      /**
       * Parameter for setting the widows size multiplicator.
       */
      public static final OptionID WINDOW_ID = new OptionID("sfcknn.windowmult", "Window size multiplicator.");

      /**
       * Parameter for choosing the number of variants to use.
       */
      public static final OptionID VARIANTS_ID = new OptionID("sfcknn.variants", "Number of curve variants to generate.");

      /**
       * Spatial curve generators
       */
      List<SpatialSorter> curvegen;

      /**
       * Curve window size
       */
      double window;

      /**
       * Number of variants to generate for each curve
       */
      int variants;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        ObjectListParameter<SpatialSorter> curveP = new ObjectListParameter<>(CURVES_ID, SpatialSorter.class);
        if(config.grab(curveP)) {
          curvegen = curveP.instantiateClasses(config);
        }
        DoubleParameter windowP = new DoubleParameter(WINDOW_ID, 10.0);
        if(config.grab(windowP)) {
          window = windowP.getValue();
        }
        IntParameter variantsP = new IntParameter(VARIANTS_ID, 1);
        variantsP.addConstraint(new GreaterEqualConstraint(1));
        variantsP.addConstraint(new LessEqualConstraint(3));
        if(config.grab(variantsP)) {
          variants = variantsP.getValue();
        }
      }

      @Override
      protected Factory<V, D> makeInstance() {
        return new Factory<>(k, distanceFunction, curvegen, window, variants);
      }
    }
  }
}