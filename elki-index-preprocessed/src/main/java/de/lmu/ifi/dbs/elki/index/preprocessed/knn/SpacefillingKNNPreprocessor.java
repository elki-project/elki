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
package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.projection.random.RandomProjectionFamily;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.SpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.*;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Compute the nearest neighbors approximatively using space filling curves.
 * <p>
 * This version computes the data projections and stores, then queries this data
 * on-demand. This usually needs less memory (except for very small neighborhood
 * sizes k) than {@link SpacefillingMaterializeKNNPreprocessor}, but will also
 * be slower.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br>
 * Fast and Scalable Outlier Detection with Approximate Nearest Neighbor
 * Ensembles<br>
 * Proc. 20th Int. Conf. Database Systems for Advanced Applications
 * (DASFAA 2015)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - SpaceFillingKNNQuery
 * @has - - - SpatialPair
 *
 * @param <O> Vector type indexed
 */
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
    title = "Fast and Scalable Outlier Detection with Approximate Nearest Neighbor Ensembles", //
    booktitle = "Proc. 20th Int. Conf. Database Systems for Advanced Applications (DASFAA 2015)", //
    url = "https://doi.org/10.1007/978-3-319-18123-3_2", //
    bibkey = "DBLP:conf/dasfaa/SchubertZK15")
public class SpacefillingKNNPreprocessor<O extends NumberVector> implements KNNIndex<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SpacefillingKNNPreprocessor.class);

  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;

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
  List<List<SpatialPair<DBID, NumberVector>>> curves = null;

  /**
   * Curve position storage
   */
  WritableDataStore<int[]> positions = null;

  /**
   * Mean number of distance computations
   */
  Mean mean = new Mean();

  /**
   * Number of dimensions to use.
   */
  final int odim;

  /**
   * Random projection family to use.
   */
  RandomProjectionFamily proj;

  /**
   * Random number generator.
   */
  Random random;

  /**
   * Constructor.
   *
   * @param relation Relation to index.
   * @param curvegen Curve generators
   * @param window Window multiplicator
   * @param variants Number of curve variants to generate
   * @param odim Number of dimensions to use -1 == all.
   * @param proj Random projection to apply
   * @param random Random number generator
   */
  public SpacefillingKNNPreprocessor(Relation<O> relation, List<SpatialSorter> curvegen, double window, int variants, int odim, RandomProjectionFamily proj, Random random) {
    super();
    this.relation = relation;
    this.curvegen = curvegen;
    this.window = window;
    this.variants = variants;
    this.odim = odim;
    this.proj = proj;
    this.random = random;
  }

  @Override
  public void initialize() {
    if(curves != null) {
      throw new UnsupportedOperationException("Preprocessor already ran.");
    }
    if(relation.size() > 0) {
      preprocess();
    }
  }

  protected void preprocess() {
    final long starttime = System.currentTimeMillis();
    final int size = relation.size();

    final int numgen = curvegen.size();
    final int numcurves = variants; // numgen * variants;
    curves = new ArrayList<>(numcurves);
    for(int i = 0; i < numcurves; i++) {
      curves.add(new ArrayList<SpatialPair<DBID, NumberVector>>(size));
    }

    if(proj == null) {
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        final NumberVector v = relation.get(iditer);
        SpatialPair<DBID, NumberVector> ref = new SpatialPair<DBID, NumberVector>(DBIDUtil.deref(iditer), v);
        for(List<SpatialPair<DBID, NumberVector>> curve : curves) {
          curve.add(ref);
        }
      }

      // Sort spatially
      final double[] mms = SpatialSorter.computeMinMax(curves.get(0));
      // Find maximum extend.
      double extend = 0;
      for(int d2 = 0, e = mms.length - 1; d2 < e; d2 += 2) {
        extend = Math.max(extend, mms[d2 + 1] - mms[d2]);
      }
      final double[] mmscratch = new double[mms.length];
      final int idim = mms.length >>> 1;
      final int dim = (odim < 0) ? idim : Math.min(odim, idim);
      final int[] permutation = range(0, idim);
      final int[] apermutation = (dim != idim) ? new int[dim] : permutation;
      for(int j = 0; j < numcurves; j++) {
        final int ctype = numgen > 1 ? random.nextInt(numgen) : 0;
        // Scale all axes by the same factor:
        final double scale = 1. + random.nextDouble();
        for(int d2 = 0, e = mms.length - 1; d2 < e; d2 += 2) {
          // Note: use global extend, to be unbiased against different scales.
          mmscratch[d2] = mms[d2] - extend * random.nextDouble();
          mmscratch[d2 + 1] = mmscratch[d2] + extend * scale;
        }
        // Generate permutation:
        randomPermutation(permutation, random);
        System.arraycopy(permutation, 0, apermutation, 0, dim);
        curvegen.get(ctype).sort(curves.get(j), 0, size, mmscratch, apermutation);
      }
    }
    else {
      // With projections, min/max management gets more tricky and expensive.
      final int idim = RelationUtil.dimensionality(relation);
      final int dim = (odim < 0) ? idim : odim;
      final int[] permutation = range(0, dim);
      NumberVector.Factory<O> factory = RelationUtil.getNumberVectorFactory(relation);
      final double[] mms = new double[odim << 1];

      for(int j = 0; j < numcurves; j++) {
        final List<SpatialPair<DBID, NumberVector>> curve = curves.get(j);
        final RandomProjectionFamily.Projection mat = proj.generateProjection(idim, dim);
        final int ctype = numgen > 1 ? random.nextInt(numgen) : 0;

        // Initialize min/max:
        for(int d2 = 0; d2 < mms.length; d2 += 2) {
          mms[d2] = Double.POSITIVE_INFINITY;
          mms[d2 + 1] = Double.NEGATIVE_INFINITY;
        }
        // Project data set:
        for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
          double[] proj = mat.project(relation.get(iditer));
          curve.add(new SpatialPair<DBID, NumberVector>(DBIDUtil.deref(iditer), factory.newNumberVector(proj)));
          for(int d2 = 0, d = 0; d2 < mms.length; d2 += 2, d++) {
            mms[d2] = Math.min(mms[d2], proj[d]);
            mms[d2 + 1] = Math.max(mms[d2 + 1], proj[d]);
          }
        }
        // Find maximum extend.
        double extend = 0.;
        for(int d2 = 0; d2 < mms.length; d2 += 2) {
          extend = Math.max(extend, mms[d2 + 1] - mms[d2]);
        }
        // Scale all axes by the same factor:
        final double scale = 1. + random.nextDouble();
        for(int d2 = 0; d2 < mms.length; d2 += 2) {
          // Note: use global extend, to be unbiased against different scales.
          mms[d2] -= extend * random.nextDouble();
          mms[d2 + 1] = mms[d2] + extend * scale;
        }
        // Generate permutation:
        randomPermutation(permutation, random);
        // Sort spatially.
        curvegen.get(ctype).sort(curve, 0, size, mms, permutation);
      }
    }

    // Build position index, DBID -> position in the three curves
    positions = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, int[].class);
    for(int cnum = 0; cnum < numcurves; cnum++) {
      Iterator<SpatialPair<DBID, NumberVector>> it = curves.get(cnum).iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialPair<DBID, NumberVector> r = it.next();
        final int[] data;
        if(cnum == 0) {
          data = new int[numcurves];
          positions.put(r.first, data);
        }
        else {
          data = positions.get(r.first);
        }
        data[cnum] = i;
      }
    }
    final long end = System.currentTimeMillis();
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(this.getClass().getCanonicalName() + ".construction-time.ms", end - starttime));
    }
  }

  /**
   * Initialize an integer value range.
   *
   * @param start Starting value
   * @param end End value (exclusive)
   * @return Array of integers start..end, excluding end.
   */
  public static int[] range(int start, int end) {
    int[] out = new int[end - start];
    for(int i = 0, j = start; j < end; i++, j++) {
      out[i] = j;
    }
    return out;
  }

  /**
   * Perform a random permutation of the array, in-place.
   *
   * Knuth / Fisher-Yates style shuffle
   *
   * @param out Prefilled output array.
   * @param random Random generator.
   * @return Same array.
   */
  public static int[] randomPermutation(final int[] out, Random random) {
    for(int i = out.length - 1; i > 0; i--) {
      // Swap with random preceeding element.
      int ri = random.nextInt(i + 1);
      int tmp = out[ri];
      out[ri] = out[i];
      out[i] = tmp;
    }
    return out;
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
  public void logStatistics() {
    LOG.statistics(new DoubleStatistic(this.getClass().getCanonicalName() + ".distance-computations-per-k", mean.getMean()));
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    for(Object hint : hints) {
      if(DatabaseQuery.HINT_EXACT.equals(hint)) {
        return null;
      }
    }
    return new SpaceFillingKNNQuery(distanceQuery);
  }

  /**
   * KNN Query processor for space filling curves
   *
   * @author Erich Schubert
   */
  protected class SpaceFillingKNNQuery implements KNNQuery<O> {
    /**
     * Distance query to use for refinement
     */
    DistanceQuery<O> distq;

    /**
     * Constructor.
     *
     * @param distanceQuery Distance query to use for refinement
     */
    public SpaceFillingKNNQuery(DistanceQuery<O> distanceQuery) {
      super();
      this.distq = distanceQuery;
    }

    @Override
    public KNNList getKNNForDBID(DBIDRef id, int k) {
      final int wsize = (int) Math.ceil(window * k);
      // Build candidates
      ModifiableDBIDs cands = DBIDUtil.newHashSet(2 * wsize * curves.size());
      final int[] posi = positions.get(id);
      for(int i = 0; i < posi.length; i++) {
        List<SpatialPair<DBID, NumberVector>> curve = curves.get(i);
        final int start = Math.max(0, posi[i] - wsize);
        final int end = Math.min(posi[i] + wsize + 1, curve.size());
        for(int j = start; j < end; j++) {
          cands.add(curve.get(j).first);
        }
      }
      // Refine:
      int distc = 0;
      KNNHeap heap = DBIDUtil.newHeap(k);
      final O vec = relation.get(id);
      for(DBIDIter iter = cands.iter(); iter.valid(); iter.advance()) {
        heap.insert(distq.distance(vec, iter), iter);
        distc++;
      }
      mean.put(distc / (double) k);
      return heap.toKNNList();
    }

    @Override
    public List<KNNList> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
      throw new AbortException("Not yet implemented");
    }

    @Override
    public KNNList getKNNForObject(O obj, int k) {
      throw new AbortException("Not yet implemented");
    }
  }

  /**
   * Index factory class
   *
   * @author Erich Schubert
   *
   * @param <V> Vector type
   *
   * @has - - - SpacefillingKNNPreprocessor
   */
  public static class Factory<V extends NumberVector> implements IndexFactory<V> {
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
     * Number of dimensions to use.
     */
    int odim;

    /**
     * Random projection family to use.
     */
    RandomProjectionFamily proj;

    /**
     * Random number generator.
     */
    RandomFactory random;

    /**
     * Constructor.
     *
     * @param curvegen Curve generators
     * @param window Window multiplicator
     * @param variants Number of curve variants to generate
     * @param odim Number of dimensions to use -1 == all.
     * @param proj Random projection family
     * @param random Random number generator
     */
    public Factory(List<SpatialSorter> curvegen, double window, int variants, int odim, RandomProjectionFamily proj, RandomFactory random) {
      super();
      this.curvegen = curvegen;
      this.window = window;
      this.variants = variants;
      this.odim = odim;
      this.proj = proj;
      this.random = random;
    }

    @Override
    public SpacefillingKNNPreprocessor<V> instantiate(Relation<V> relation) {
      return new SpacefillingKNNPreprocessor<>(relation, curvegen, window, variants, odim, proj, random.getRandom());
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }

    /**
     * Parameterization class.
     *
     * @author Erich Schubert
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
       * Parameter for choosing the number of dimensions to use for each curve.
       */
      public static final OptionID DIM_ID = new OptionID("sfcknn.dim", "Number of dimensions to use for each curve.");

      /**
       * Parameter for choosing the random projections.
       */
      public static final OptionID PROJECTION_ID = new OptionID("sfcknn.proj", "Random projection to use.");

      /**
       * Parameter for choosing the number of variants to use.
       */
      public static final OptionID RANDOM_ID = new OptionID("sfcknn.seed", "Random generator.");

      /**
       * Spatial curve generators.
       */
      List<SpatialSorter> curvegen;

      /**
       * Curve window size.
       */
      double window;

      /**
       * Number of variants to generate for each curve.
       */
      int variants;

      /**
       * Number of dimensions to use.
       */
      int odim = -1;

      /**
       * Random projection family to use.
       */
      RandomProjectionFamily proj;

      /**
       * Random number generator.
       */
      RandomFactory random;

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
        IntParameter variantsP = new IntParameter(VARIANTS_ID, 1) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(variantsP)) {
          variants = variantsP.getValue();
        }
        IntParameter dimP = new IntParameter(DIM_ID) //
            .setOptional(true) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(dimP)) {
          odim = dimP.intValue();
        }

        ObjectParameter<RandomProjectionFamily> projP = new ObjectParameter<>(PROJECTION_ID, RandomProjectionFamily.class);
        projP.setOptional(true);
        if(config.grab(projP)) {
          proj = projP.instantiateClass(config);
        }
        RandomParameter randomP = new RandomParameter(RANDOM_ID);
        if(config.grab(randomP)) {
          random = randomP.getValue();
        }
      }

      @Override
      protected Factory<?> makeInstance() {
        return new Factory<DoubleVector>(curvegen, window, variants, odim, proj, random);
      }
    }
  }
}
