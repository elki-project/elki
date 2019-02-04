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

import de.lmu.ifi.dbs.elki.data.NumberVector;
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.SpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Compute the nearest neighbors approximatively using space filling curves.
 * <p>
 * This version does the bulk kNN-join operation, i.e. precomputes the k nearest
 * neighbors for every object, then discards the curves. This is usually more
 * memory intensive but faster than {@link SpacefillingKNNPreprocessor}.
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
 * @has - - - SpatialPair
 *
 * @param <O> Vector type indexed
 */
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
    title = "Fast and Scalable Outlier Detection with Approximate Nearest Neighbor Ensembles", //
    booktitle = "Proc. 20th Int. Conf. Database Systems for Advanced Applications (DASFAA 2015)", //
    url = "https://doi.org/10.1007/978-3-319-18123-3_2", //
    bibkey = "DBLP:conf/dasfaa/SchubertZK15")
public class SpacefillingMaterializeKNNPreprocessor<O extends NumberVector> extends AbstractMaterializeKNNPreprocessor<O> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SpacefillingMaterializeKNNPreprocessor.class);

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
   * Random number generator.
   */
  Random random;

  /**
   * Constructor.
   *
   * @param relation Relation to index.
   * @param distanceFunction Distance function
   * @param k k
   * @param curvegen Curve generators
   * @param window Window multiplicator
   * @param variants Number of curve variants to generate
   * @param random Random number generator
   */
  public SpacefillingMaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O> distanceFunction, int k, List<SpatialSorter> curvegen, double window, int variants, Random random) {
    super(relation, distanceFunction, k);
    this.curvegen = curvegen;
    this.window = window;
    this.variants = variants;
    this.random = random;
  }

  @Override
  protected void preprocess() {
    // Prepare space filling curve:
    final long starttime = System.currentTimeMillis();
    final int size = relation.size();

    final int numgen = curvegen.size();
    final int numcurves = numgen * variants;
    List<List<SpatialPair<DBID, NumberVector>>> curves = new ArrayList<>(numcurves);
    for(int i = 0; i < numcurves; i++) {
      curves.add(new ArrayList<SpatialPair<DBID, NumberVector>>(size));
    }

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final NumberVector v = relation.get(iditer);
      SpatialPair<DBID, NumberVector> ref = new SpatialPair<DBID, NumberVector>(DBIDUtil.deref(iditer), v);
      for(List<SpatialPair<DBID, NumberVector>> curve : curves) {
        curve.add(ref);
      }
    }

    // Sort spatially
    final double[] mms = SpatialSorter.computeMinMax(curves.get(0));
    final double[] mmscratch = new double[mms.length];
    final int numdim = mms.length >>> 1;
    final int[] permutation = new int[numdim];
    for(int j = 0; j < variants; j++) {
      for(int i = 0, e = mms.length - 1; i < e; i += 2) {
        double len = mms[i + 1] - mms[i];
        mmscratch[i] = mms[i] - len * random.nextDouble();
        mmscratch[i + 1] = mms[i + 1] + len * random.nextDouble();
      }
      // Generate permutation:
      for(int i = 0; i < numdim; i++) {
        permutation[i] = i;
      }
      // Knuth / Fisher-Yates style shuffle
      for(int i = numdim - 1; i > 0; i--) {
        // Swap with random preceeding element.
        int ri = random.nextInt(i + 1);
        int tmp = permutation[ri];
        permutation[ri] = permutation[i];
        permutation[i] = tmp;
      }
      for(int i = 0; i < numgen; i++) {
        curvegen.get(i).sort(curves.get(i + numgen * j), 0, size, mmscratch, permutation);
      }
    }

    // Build position index, DBID -> position in the three curves
    WritableDataStore<int[]> positions = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, int[].class);
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

    // Convert to final storage
    final int wsize = (int) Math.ceil(window * k);
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, KNNList.class);
    HashSetModifiableDBIDs cands = DBIDUtil.newHashSet(2 * wsize * numcurves);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      // Get candidates.
      cands.clear();
      int[] posi = positions.get(iditer);
      for(int i = 0; i < posi.length; i++) {
        List<SpatialPair<DBID, NumberVector>> curve = curves.get(i);
        final int start = Math.max(0, posi[i] - wsize);
        final int end = Math.min(posi[i] + wsize + 1, curve.size());
        for(int pos = start; pos < end; pos++) {
          cands.add(curve.get(pos).first);
        }
      }

      int distc = 0;
      KNNHeap heap = DBIDUtil.newHeap(k);
      O vec = relation.get(iditer);
      for(DBIDIter iter = cands.iter(); iter.valid(); iter.advance()) {
        heap.insert(distanceQuery.distance(vec, iter), iter);
        distc++;
      }

      storage.put(iditer, heap.toKNNList());
      mean.put(distc / (double) k);
    }

    final long end = System.currentTimeMillis();
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(this.getClass().getCanonicalName() + ".construction-time.ms", end - starttime));
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
  public void logStatistics() {
    LOG.statistics(new DoubleStatistic(this.getClass().getCanonicalName() + ".distance-computations-per-k", mean.getMean()));
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distQ, Object... hints) {
    for(Object hint : hints) {
      if(DatabaseQuery.HINT_EXACT.equals(hint)) {
        return null;
      }
    }
    return super.getKNNQuery(distQ, hints);
  }

  /**
   * Index factory class
   *
   * @author Erich Schubert
   *
   * @param <V> Vector type
   */
  public static class Factory<V extends NumberVector> extends AbstractMaterializeKNNPreprocessor.Factory<V> {
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
     * Random number generator.
     */
    RandomFactory random;

    /**
     * Constructor.
     *
     * @param curvegen Curve generators
     * @param window Window multiplicator
     * @param variants Number of curve variants to generate
     * @param random Random number generator
     */
    public Factory(int k, DistanceFunction<? super V> distanceFunction, List<SpatialSorter> curvegen, double window, int variants, RandomFactory random) {
      super(k, distanceFunction);
      this.curvegen = curvegen;
      this.window = window;
      this.variants = variants;
      this.random = random;
    }

    @Override
    public SpacefillingMaterializeKNNPreprocessor<V> instantiate(Relation<V> relation) {
      return new SpacefillingMaterializeKNNPreprocessor<>(relation, distanceFunction, k, curvegen, window, variants, random.getRandom());
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
     * @hidden
     *
     * @param <V> Vector type
     */
    public static class Parameterizer<V extends NumberVector> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<V> {
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
       * Parameter for choosing the number of variants to use.
       */
      public static final OptionID RANDOM_ID = new OptionID("sfcknn.seed", "Random generator.");

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
        RandomParameter randomP = new RandomParameter(RANDOM_ID);
        if(config.grab(randomP)) {
          random = randomP.getValue();
        }
      }

      @Override
      protected Factory<V> makeInstance() {
        return new Factory<>(k, distanceFunction, curvegen, window, variants, random);
      }
    }
  }
}
