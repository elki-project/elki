package de.lmu.ifi.dbs.elki.algorithm.statistics;

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
import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Evaluate a distance functions performance by computing the average precision
 * at k, when ranking the objects by distance.
 * 
 * @author Erich Schubert
 * @param <V> Vector type
 * @param <D> Distance type
 */
public class AveragePrecisionAtK<V extends Object, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, D, CollectionResult<DoubleVector>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(AveragePrecisionAtK.class);

  /**
   * The parameter k - the number of neighbors to retrieve.
   */
  private int k;
  
  /**
   * Relative number of object to use in sampling.
   */
  private double sampling = 1.0;
  
  /**
   * Random sampling seed.
   */
  private Long seed = null;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param sampling Sampling rate
   * @param seed Random sampling seed (may be null)
   */
  public AveragePrecisionAtK(DistanceFunction<? super V, D> distanceFunction, int k, double sampling, Long seed) {
    super(distanceFunction);
    this.k = k;
    this.sampling = sampling;
    this.seed = seed;
  }

  @Override
  public HistogramResult<DoubleVector> run(Database database) {
    final Relation<V> relation = database.getRelation(getInputTypeRestriction()[0]);
    final Relation<Object> lrelation = database.getRelation(getInputTypeRestriction()[1]);
    final DistanceQuery<V, D> distQuery = database.getDistanceQuery(relation, getDistanceFunction());
    final KNNQuery<V, D> knnQuery = database.getKNNQuery(distQuery, k);

    MeanVariance[] mvs = MeanVariance.newArray(k);

    final DBIDs ids;
    if (sampling < 1.0) {
      int size = Math.max(1, (int) (sampling * relation.size()));
      ids = DBIDUtil.randomSample(relation.getDBIDs(), size, seed);
    } else {
      ids = relation.getDBIDs();
    }
    
    if(logger.isVerbose()) {
      logger.verbose("Processing points...");
    }
    FiniteProgress objloop = logger.isVerbose() ? new FiniteProgress("Computing nearest neighbors", ids.size(), logger) : null;
    // sort neighbors
    for(DBID id : ids) {
      KNNResult<D> knn = knnQuery.getKNNForDBID(id, k);
      Object label = lrelation.get(id);

      int positive = 0;
      Iterator<DistanceResultPair<D>> ri = knn.iterator();
      for(int i = 0; i < k && ri.hasNext(); i++) {
        DBID nid = ri.next().getDBID();
        Object olabel = lrelation.get(nid);
        if(label == null) {
          if(olabel == null) {
            positive += 1;
          }
        }
        else {
          if(label.equals(olabel)) {
            positive += 1;
          }
        }
        final double precision = positive / (double) (i + 1);
        mvs[i].put(precision);
      }
      if(objloop != null) {
        objloop.incrementProcessed(logger);
      }
    }
    if(objloop != null) {
      objloop.ensureCompleted(logger);
    }
    // Collections.sort(results);

    // Transform Histogram into a Double Vector array.
    Collection<DoubleVector> res = new ArrayList<DoubleVector>(k);
    for(int i = 0; i < k; i++) {
      DoubleVector row = new DoubleVector(new double[] { mvs[i].getMean(), mvs[i].getSampleStddev() });
      res.add(row);
    }
    return new HistogramResult<DoubleVector>("Average Precision", "average-precision", res);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction(), TypeUtil.GUESSED_LABEL);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<V, D> {
    /**
     * Parameter k to compute the average precision at.
     */
    private static final OptionID K_ID = OptionID.getOrCreateOptionID("avep.k", "K to compute the average precision at.");

    /**
     * Parameter to enable sampling
     */
    public static final OptionID SAMPLING_ID = OptionID.getOrCreateOptionID("avep.sampling", "Relative amount of object to sample.");

    /**
     * Parameter to control the sampling random seed
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("avep.sampling-seed", "Random seed for deterministic sampling.");

    /**
     * Neighborhood size
     */
    protected int k = 20;
    
    /**
     * Relative amount of data to sample
     */
    protected double sampling = 1.0;
    
    /**
     * Random sampling seed.
     */
    protected Long seed = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID, new GreaterEqualConstraint(2));
      if(config.grab(kP)) {
        k = kP.getValue();
      }
      final DoubleParameter samplingP = new DoubleParameter(SAMPLING_ID, new IntervalConstraint(0.0, IntervalBoundary.OPEN, 1.0, IntervalBoundary.CLOSE), true);
      if (config.grab(samplingP)) {
        sampling = samplingP.getValue();
      }
      final LongParameter seedP = new LongParameter(SEED_ID, true);
      if (config.grab(seedP)) {
        seed = seedP.getValue();
      }
    }

    @Override
    protected AveragePrecisionAtK<V, D> makeInstance() {
      return new AveragePrecisionAtK<V, D>(distanceFunction, k, sampling, seed);
    }
  }
}