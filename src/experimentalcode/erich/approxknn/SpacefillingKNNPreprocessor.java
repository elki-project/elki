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

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.AbstractSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.SpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
public class SpacefillingKNNPreprocessor<O extends NumberVector<?>> extends AbstractIndex<O> implements KNNIndex<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SpacefillingKNNPreprocessor.class);

  // Enable logging for debugging.
  static {
    LoggingConfiguration.setLevelFor(SpacefillingKNNPreprocessor.class.getName(), Level.INFO.getName());
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
   * Curve storage
   */
  List<List<SpatialRef>> curves = null;

  /**
   * Curve position storage
   */
  WritableDataStore<int[]> positions = null;

  /**
   * Mean number of distance computations
   */
  Mean mean = new Mean();

  /**
   * Constructor.
   * 
   * @param relation Relation to index.
   * @param curvegen Curve generators
   * @param window Window multiplicator
   * @param variants Number of curve variants to generate
   */
  public SpacefillingKNNPreprocessor(Relation<O> relation, List<SpatialSorter> curvegen, double window, int variants) {
    super(relation);
    this.curvegen = curvegen;
    this.window = window;
    this.variants = variants;
  }

  @Override
  public void initialize() {
    if (curves == null) {
      if (relation.size() > 0) {
        preprocess();
      }
    } else {
      throw new UnsupportedOperationException("Preprocessor already ran.");
    }
  }

  protected void preprocess() {
    final long start = System.nanoTime();
    final int size = relation.size();

    final int numgen = curvegen.size();
    final int numcurves = numgen * variants;
    curves = new ArrayList<>(numcurves);
    for (int i = 0; i < numcurves; i++) {
      curves.add(new ArrayList<SpatialRef>(size));
    }

    for (DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final NumberVector<?> v = relation.get(iditer);
      SpatialRef ref = new SpatialRef(DBIDUtil.deref(iditer), v);
      for (List<SpatialRef> curve : curves) {
        curve.add(ref);
      }
    }

    // Sort spatially
    double[] mms = AbstractSpatialSorter.computeMinMax(curves.get(0));
    for (int j = 0; j < variants; j++) {
      final double[] mm;
      if (j == 0) {
        mm = mms;
      } else if (j == 1) {
        // Hardcoded for publication CIKM12
        mm = new double[mms.length];
        for (int i = 0; i < mms.length; i += 2) {
          double len = mms[i + 1] - mms[i];
          mm[i] = mms[i] - len * .1234;
          mm[i + 1] = mms[i + 1] + len * .3784123;
        }
      } else if (j == 2) {
        // Hardcoded for publication CIKM12
        mm = new double[mms.length];
        for (int i = 0; i < mms.length; i += 2) {
          double len = mms[i + 1] - mms[i];
          mm[i] = mms[i] - len * .321078;
          mm[i + 1] = mms[i + 1] + len * .51824172;
        }
      } else {
        throw new AbortException("Currently, only 1-3 variants may be used!");
      }
      for (int i = 0; i < numgen; i++) {
        curvegen.get(i).sort(curves.get(i + numgen * j), 0, size, mm);
      }
    }

    // Build position index, DBID -> position in the three curves
    positions = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, int[].class);
    for (int cnum = 0; cnum < numcurves; cnum++) {
      Iterator<SpatialRef> it = curves.get(cnum).iterator();
      for (int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        final int[] data;
        if (cnum == 0) {
          data = new int[numcurves];
          positions.put(r.id, data);
        } else {
          data = positions.get(r.id);
        }
        data[cnum] = i;
      }
    }
    final long end = System.nanoTime();
    if (LOG.isVerbose()) {
      LOG.verbose("SFC preprocessor took " + ((end - start) / 1.E6) + " milliseconds.");
    }
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
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    for (Object hint : hints) {
      if (DatabaseQuery.HINT_EXACT.equals(hint)) {
        return null;
      }
    }
    return new SpaceFillingKNNQuery<>(distanceQuery);
  }

  @Override
  public void logStatistics() {
    // Nothing to do, yet.
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
    public KNNList<D> getKNNForDBID(DBIDRef id, int k) {
      final int wsize = (int) (window * k);
      // Build candidates
      ModifiableDBIDs cands = DBIDUtil.newHashSet(wsize * curves.size());
      final int[] posi = positions.get(id);
      for (int i = 0; i < posi.length; i++) {
        List<SpatialRef> curve = curves.get(i);
        final int start = Math.max(0, posi[i] - wsize);
        final int end = Math.min(posi[i] + wsize + 1, curve.size());
        for (int j = start; j < end; j++) {
          cands.add(curve.get(j).id);
        }
      }
      // Refine:
      int distc = 0;
      KNNHeap<D> heap = DBIDUtil.newHeap(distq.getDistanceFactory(), k);
      final O vec = relation.get(id);
      for (DBIDIter iter = cands.iter(); iter.valid(); iter.advance()) {
        heap.add(distq.distance(vec, iter), iter);
        distc++;
      }
      mean.put(distc / (double) k);
      return heap.toKNNList();
    }

    @Override
    public List<KNNList<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
      throw new AbortException("Not yet implemented");
    }

    @Override
    public KNNList<D> getKNNForObject(O obj, int k) {
      throw new AbortException("Not yet implemented");
    }
  }

  /**
   * Object used in spatial sorting, combining the spatial object and the object
   * ID.
   * 
   * @author Erich Schubert
   */
  protected static class SpatialRef implements SpatialComparable {
    /**
     * Object reference.
     */
    protected DBID id;

    /**
     * Spatial vector.
     */
    protected NumberVector<?> vec;

    /**
     * Constructor.
     * 
     * @param id
     * @param vec
     */
    protected SpatialRef(DBID id, NumberVector<?> vec) {
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
   */
  public static class Factory<V extends NumberVector<?>> implements IndexFactory<V, SpacefillingKNNPreprocessor<V>> {
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
    public Factory(List<SpatialSorter> curvegen, double window, int variants) {
      super();
      this.curvegen = curvegen;
      this.window = window;
      this.variants = variants;
    }

    @Override
    public SpacefillingKNNPreprocessor<V> instantiate(Relation<V> relation) {
      return new SpacefillingKNNPreprocessor<>(relation, curvegen, window, variants);
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
     */
    public static class Parameterizer extends AbstractParameterizer {
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
        if (config.grab(curveP)) {
          curvegen = curveP.instantiateClasses(config);
        }
        DoubleParameter windowP = new DoubleParameter(WINDOW_ID, 10.0);
        if (config.grab(windowP)) {
          window = windowP.getValue();
        }
        IntParameter variantsP = new IntParameter(VARIANTS_ID, 1);
        variantsP.addConstraint(new GreaterEqualConstraint(1));
        variantsP.addConstraint(new LessEqualConstraint(3));
        if (config.grab(variantsP)) {
          variants = variantsP.getValue();
        }
      }

      @Override
      protected Factory<?> makeInstance() {
        return new Factory<DoubleVector>(curvegen, window, variants);
      }
    }
  }
}
