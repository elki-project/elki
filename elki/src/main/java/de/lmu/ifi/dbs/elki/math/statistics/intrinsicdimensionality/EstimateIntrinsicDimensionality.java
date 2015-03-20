package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

// TODO: make random configurable
public class EstimateIntrinsicDimensionality<O> extends AbstractDistanceBasedAlgorithm<O, Result> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(EstimateIntrinsicDimensionality.class);

  /**
   * Number of neighbors to use.
   */
  private double krate;

  /**
   * Number of samples to draw.
   */
  private double samples;

  private IntrinsicDimensionalityEstimator estimator = HillEstimator.STATIC;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param krate kNN rate
   * @param samples Sample size
   */
  public EstimateIntrinsicDimensionality(DistanceFunction<? super O> distanceFunction, double krate, double samples) {
    super(distanceFunction);
    this.krate = krate;
    this.samples = samples;
  }

  public Result run(Database database, Relation<O> relation) {
    DBIDs allids = relation.getDBIDs();
    // Number of samples to draw.
    int ssize = (int) ((samples > 1.) ? samples : Math.ceil(samples * allids.size()));
    // Number of neighbors to fetch
    int kk = (int) ((krate > 1.) ? krate : Math.ceil(krate * allids.size()));

    DBIDs sampleids = DBIDUtil.randomSample(allids, ssize, 0L);

    DistanceQuery<O> dq = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnq = database.getKNNQuery(dq, kk + 1);

    double[] idim = new double[ssize], kdists = new double[ssize];
    int samples = 0;
    for(DBIDIter iter = sampleids.iter(); iter.valid(); iter.advance()) {
      KNNList knns = knnq.getKNNForDBID(iter, kk);
      DoubleDBIDListIter it = knns.iter();
      // Skip zeros.
      while(it.valid() && it.doubleValue() == 0.) {
        it.advance();
      }
      if(!it.valid()) {
        continue;
      }
      double[] vals = new double[knns.size() - it.getOffset()];
      for(int i = 0; it.valid(); it.advance(), ++i) {
        vals[i] = it.doubleValue();
      }
      kdists[samples] = vals[vals.length - 1];
      idim[samples] = estimator.estimate(vals);
      ++samples;
    }
    double id = (samples > 1) ? QuickSelect.median(idim, 0, samples) : -1;
    double kdist = (samples > 1) ? QuickSelect.median(kdists, 0, samples) : 0.;
    LOG.statistics(new DoubleStatistic(EstimateIntrinsicDimensionality.class.getName() + ".k-distance", kdist));
    LOG.statistics(new DoubleStatistic(EstimateIntrinsicDimensionality.class.getName() + ".intrinsic-dimensionality", id));
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    private static final OptionID KRATE_ID = new OptionID("idist.k", "Number of kNN (absolute or relative)");

    private static final OptionID SAMPLES_ID = new OptionID("idist.sampling", "Sample size (absolute or relative)");

    /**
     * Number of neighbors to use.
     */
    private double krate;

    /**
     * Number of samples to draw.
     */
    private double samples;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter krateP = new DoubleParameter(KRATE_ID, 50);
      if(config.grab(krateP)) {
        krate = krateP.doubleValue();
      }
      DoubleParameter samplesP = new DoubleParameter(SAMPLES_ID, .1);
      if(config.grab(samplesP)) {
        samples = samplesP.doubleValue();
      }
    }

    @Override
    protected EstimateIntrinsicDimensionality<O> makeInstance() {
      return new EstimateIntrinsicDimensionality<>(distanceFunction, krate, samples);
    }
  }
}
