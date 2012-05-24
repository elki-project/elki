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
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.AbstractSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.SpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * Compute the nearest neighbors approximatively using space filling curves.
 * 
 * @author Erich Schubert
 */
public class SpacefillingMaterializeKNNPreprocessor<O extends NumberVector<?, ?>, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor<O, D, KNNResult<D>> {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(SpacefillingMaterializeKNNPreprocessor.class);

  static {
    logger.getWrappedLogger().setLevel(Level.INFO);
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
   * Mean number of distance computations
   */
  Mean mean = new Mean();

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
    final int wsize = (int) (window * k);

    // Setup temporary knn heap storage
    WritableDataStore<KNNHeap<D>> tempstorage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNHeap.class);

    // Curve storage:
    List<SpatialRef> curve = new ArrayList<SpatialRef>(size);

    for(DBID id : relation.iterDBIDs()) {
      final O v = relation.get(id);
      SpatialRef ref = new SpatialRef(id, v);
      curve.add(ref);
    }
    // Compute min and max
    final double[] mms = AbstractSpatialSorter.computeMinMax(curve);
    // For each curve and variant:
    for(SpatialSorter sorter : curvegen) {
      for(int variant = 0; variant < variants; variant++) {
        // Setup actual min and max:
        final double[] mm = setupMinMax(mms, variant);
        // sort
        sorter.sort(curve, 0, size, mm);
        // Window-scan
        scanCurve(curve, wsize, tempstorage);
      }
    }

    // Convert to final storage
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNResult.class);
    for(DBID id : relation.iterDBIDs()) {
      storage.put(id, tempstorage.get(id).toKNNList());
      tempstorage.delete(id);
    }
    tempstorage.destroy();

    final long end = System.nanoTime();
    if(logger.isVerbose()) {
      logger.verbose("SFC preprocessor took " + ((end - start) / 1.E6) + " milliseconds.");
    }
  }

  /**
   * Perform a window scan on the curve.
   * 
   * @param curve Curve to process
   * @param wsize Window size
   * @param tempstorage Temporary storage
   */
  private void scanCurve(List<SpatialRef> curve, int wsize, WritableDataStore<KNNHeap<D>> tempstorage) {
    for(int start = 0; start < curve.size(); start++) {
      final int end = Math.min(curve.size() - 1, start + wsize + 1);
      SpatialRef ref1 = curve.get(start);
      KNNHeap<D> heap1 = tempstorage.get(ref1.id);
      for(int pos = start; pos < end; pos++) {
        SpatialRef ref2 = curve.get(pos);
        KNNHeap<D> heap2 = tempstorage.get(ref2.id);
        D dist = distanceQuery.distance(ref1.vec, ref2.vec);
        heap1.add(dist, ref2.id);
        heap2.add(dist, ref1.id);
      }
    }
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
  public String toString() {
    return "Mean number of distance computations / k: " + mean.getMean();
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
    return logger;
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
  public static class Factory<V extends NumberVector<?, ?>, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory<V, D, KNNResult<D>> {
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
      return new SpacefillingMaterializeKNNPreprocessor<V, D>(relation, distanceFunction, k, curvegen, window, variants);
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
    public static class Parameterizer<V extends NumberVector<?, ?>, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<V, D> {
      /**
       * Parameter for choosing the space filling curves to use.
       */
      public static final OptionID CURVES_ID = OptionID.getOrCreateOptionID("sfcknn.curves", "Space filling curve generators to use for kNN approximation.");

      /**
       * Parameter for setting the widows size multiplicator.
       */
      public static final OptionID WINDOW_ID = OptionID.getOrCreateOptionID("sfcknn.windowmult", "Window size multiplicator.");

      /**
       * Parameter for choosing the number of variants to use.
       */
      public static final OptionID VARIANTS_ID = OptionID.getOrCreateOptionID("sfcknn.variants", "Number of curve variants to generate.");

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
        ObjectListParameter<SpatialSorter> curveP = new ObjectListParameter<SpatialSorter>(CURVES_ID, SpatialSorter.class);
        if(config.grab(curveP)) {
          curvegen = curveP.instantiateClasses(config);
        }
        DoubleParameter windowP = new DoubleParameter(WINDOW_ID, 10.0);
        if(config.grab(windowP)) {
          window = windowP.getValue();
        }
        IntParameter variantsP = new IntParameter(VARIANTS_ID, new IntervalConstraint(1, IntervalBoundary.CLOSE, 3, IntervalBoundary.CLOSE), 1);
        if(config.grab(variantsP)) {
          variants = variantsP.getValue();
        }
      }

      @Override
      protected Factory<V, D> makeInstance() {
        return new Factory<V, D>(k, distanceFunction, curvegen, window, variants);
      }
    }
  }
}