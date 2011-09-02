package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableRecordStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * Fast Outlier Detection Using the "Local Correlation Integral".
 * 
 * Exact implementation only, not aLOCI.
 * 
 * TODO: add aLOCI
 * 
 * Outlier detection using multiple epsilon neighborhoods.
 * 
 * Based on: S. Papadimitriou, H. Kitagawa, P. B. Gibbons and C. Faloutsos:
 * LOCI: Fast Outlier Detection Using the Local Correlation Integral. In: Proc.
 * 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has RangeQuery
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
@Title("LOCI: Fast Outlier Detection Using the Local Correlation Integral")
@Description("Algorithm to compute outliers based on the Local Correlation Integral")
@Reference(authors = "S. Papadimitriou, H. Kitagawa, P. B. Gibbons, C. Faloutsos", title = "LOCI: Fast Outlier Detection Using the Local Correlation Integral", booktitle = "Proc. 19th IEEE Int. Conf. on Data Engineering (ICDE '03), Bangalore, India, 2003", url = "http://dx.doi.org/10.1109/ICDE.2003.1260802")
public class LOCI<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(LOCI.class);

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   */
  public static final OptionID RMAX_ID = OptionID.getOrCreateOptionID("loci.rmax", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the minimum neighborhood size
   */
  public static final OptionID NMIN_ID = OptionID.getOrCreateOptionID("loci.nmin", "Minimum neighborhood size to be considered.");

  /**
   * Parameter to specify the averaging neighborhood scaling.
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("loci.alpha", "Scaling factor for averaging neighborhood");

  /**
   * Holds the value of {@link #RMAX_ID}.
   */
  private D rmax;

  /**
   * Holds the value of {@link #NMIN_ID}.
   */
  private int nmin;

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private double alpha;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param rmax Maximum radius
   * @param nmin Minimum neighborhood size
   * @param alpha Alpha value
   */
  public LOCI(DistanceFunction<? super O, D> distanceFunction, D rmax, int nmin, double alpha) {
    super(distanceFunction);
    this.rmax = rmax;
    this.nmin = nmin;
    this.alpha = alpha;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    Relation<O> relation = database.getRelation(getInputTypeRestriction()[0]);
    DistanceQuery<O, D> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    RangeQuery<O, D> rangeQuery = database.getRangeQuery(distFunc);

    FiniteProgress progressPreproc = logger.isVerbose() ? new FiniteProgress("LOCI preprocessing", relation.size(), logger) : null;
    // LOCI preprocessing step
    WritableDataStore<ArrayList<DoubleIntPair>> interestingDistances = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_SORTED, ArrayList.class);
    for(DBID id : relation.iterDBIDs()) {
      List<DistanceResultPair<D>> neighbors = rangeQuery.getRangeForDBID(id, rmax);
      // build list of critical distances
      ArrayList<DoubleIntPair> cdist = new ArrayList<DoubleIntPair>(neighbors.size() * 2);
      {
        for(int i = 0; i < neighbors.size(); i++) {
          DistanceResultPair<D> r = neighbors.get(i);
          if(i + 1 < neighbors.size() && r.getDistance().compareTo(neighbors.get(i + 1).getDistance()) == 0) {
            continue;
          }
          cdist.add(new DoubleIntPair(r.getDistance().doubleValue(), i));
          final double ri = r.getDistance().doubleValue() / alpha;
          if(ri <= rmax.doubleValue()) {
            cdist.add(new DoubleIntPair(ri, Integer.MIN_VALUE));
          }
        }
      }
      Collections.sort(cdist);
      // fill the gaps to have fast lookups of number of neighbors at a given
      // distance.
      int lastk = 0;
      for(DoubleIntPair c : cdist) {
        if(c.second == Integer.MIN_VALUE) {
          c.second = lastk;
        }
        else {
          lastk = c.second;
        }
      }

      interestingDistances.put(id, cdist);
      if(progressPreproc != null) {
        progressPreproc.incrementProcessed(logger);
      }
    }
    if(progressPreproc != null) {
      progressPreproc.ensureCompleted(logger);
    }
    // LOCI main step
    FiniteProgress progressLOCI = logger.isVerbose() ? new FiniteProgress("LOCI scores", relation.size(), logger) : null;
    WritableRecordStore store = DataStoreUtil.makeRecordStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class, Double.class);
    WritableDataStore<Double> mdef_norm = store.getStorage(0, Double.class);
    WritableDataStore<Double> mdef_radius = store.getStorage(1, Double.class);
    for(DBID id : relation.iterDBIDs()) {
      double maxmdefnorm = 0.0;
      double maxnormr = 0;
      List<DoubleIntPair> cdist = interestingDistances.get(id);
      double maxdist = cdist.get(cdist.size() - 1).first;
      int maxneig = cdist.get(cdist.size() - 1).second;
      if(maxneig >= nmin) {
        D range = distFunc.getDistanceFactory().fromDouble(maxdist);
        // Compute the largest neighborhood we will need.
        List<DistanceResultPair<D>> maxneighbors = rangeQuery.getRangeForDBID(id, range);
        for(DoubleIntPair c : cdist) {
          double alpha_r = alpha * c.first;
          // compute n(p_i, \alpha * r) from list
          int n_alphar = 0;
          for(DoubleIntPair c2 : cdist) {
            if(c2.first <= alpha_r) {
              n_alphar = c2.second;
            }
            else {
              break;
            }
          }
          // compute \hat{n}(p_i, r, \alpha)
          double nhat_r_alpha = 0.0;
          double sigma_nhat_r_alpha = 0.0;
          // Build the sublist from maxneighbors to match the radius c.first
          List<DistanceResultPair<D>> rneighbors = null;
          for(int i = nmin; i < maxneighbors.size(); i++) {
            DistanceResultPair<D> ne = maxneighbors.get(i);
            if(ne.getDistance().doubleValue() > c.first) {
              rneighbors = maxneighbors.subList(1, i);
              break;
            }
          }
          if(rneighbors == null) {
            continue;
          }
          for(DistanceResultPair<D> rn : rneighbors) {
            List<DoubleIntPair> rncdist = interestingDistances.get(rn.getDBID());
            int rn_alphar = 0;
            for(DoubleIntPair c2 : rncdist) {
              if(c2.first <= alpha_r) {
                rn_alphar = c2.second;
              }
              else {
                break;
              }
            }
            nhat_r_alpha = nhat_r_alpha + rn_alphar;
            sigma_nhat_r_alpha = sigma_nhat_r_alpha + (rn_alphar * rn_alphar);
          }
          // finalize average and deviation
          nhat_r_alpha = nhat_r_alpha / rneighbors.size();
          sigma_nhat_r_alpha = Math.sqrt(sigma_nhat_r_alpha / rneighbors.size() - nhat_r_alpha * nhat_r_alpha);
          double mdef = 1.0 - (n_alphar / nhat_r_alpha);
          double sigmamdef = sigma_nhat_r_alpha / nhat_r_alpha;
          double mdefnorm = mdef / sigmamdef;

          if(mdefnorm > maxmdefnorm) {
            maxmdefnorm = mdefnorm;
            maxnormr = c.first;
          }
        }
      }
      else {
        // FIXME: when nmin was never fulfilled - what is the proper value then?
        maxmdefnorm = 1.0;
        maxnormr = maxdist;
      }
      mdef_norm.put(id, maxmdefnorm);
      mdef_radius.put(id, maxnormr);
      if(progressLOCI != null) {
        progressLOCI.incrementProcessed(logger);
      }
    }
    if(progressLOCI != null) {
      progressLOCI.ensureCompleted(logger);
    }
    Relation<Double> scoreResult = new MaterializedRelation<Double>("LOCI normalized MDEF", "loci-mdef-outlier", TypeUtil.DOUBLE, mdef_norm, relation.getDBIDs());
    // TODO: actually provide min and max?
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(Double.NaN, Double.NaN, 0.0, Double.POSITIVE_INFINITY, 0.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    result.addChildResult(new MaterializedRelation<Double>("LOCI MDEF Radius", "loci-critical-radius", TypeUtil.DOUBLE, mdef_radius, relation.getDBIDs()));
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
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
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected D rmax = null;

    protected int nmin = 0;

    protected double alpha = 0.5;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final D distanceFactory = (distanceFunction != null) ? distanceFunction.getDistanceFactory() : null;
      final DistanceParameter<D> rmaxP = new DistanceParameter<D>(RMAX_ID, distanceFactory);
      if(config.grab(rmaxP)) {
        rmax = rmaxP.getValue();
      }

      final IntParameter nminP = new IntParameter(NMIN_ID, 20);
      if(config.grab(nminP)) {
        nmin = nminP.getValue();
      }

      final DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 0.5);
      if(config.grab(alphaP)) {
        alpha = alphaP.getValue();
      }
    }

    @Override
    protected LOCI<O, D> makeInstance() {
      return new LOCI<O, D>(distanceFunction, rmax, nmin, alpha);
    }
  }
}