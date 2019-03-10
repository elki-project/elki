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
package elki.outlier;

import java.util.ArrayList;
import java.util.List;

import elki.AbstractDistanceBasedAlgorithm;
import elki.clustering.optics.AbstractOPTICS;
import elki.clustering.optics.OPTICSTypeAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.QuotientOutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * OPTICS-OF outlier detection algorithm, an algorithm to find Local Outliers in
 * a database based on ideas from {@link OPTICSTypeAlgorithm} clustering.
 * <p>
 * Reference:
 * <p>
 * Markus M. Breunig, Hans-Peter Kriegel, Raymond Ng, Jörg Sander<br>
 * OPTICS-OF: Identifying Local Outliers<br>
 * Proc. 3rd European Conf. on Principles of Knowledge Discovery and Data Mining
 * (PKDD'99)
 *
 * @author Ahmed Hettab
 * @since 0.3
 *
 * @has - - - KNNQuery
 * @has - - - RangeQuery
 *
 * @param <O> DatabaseObject
 */
@Title("OPTICS-OF: Identifying Local Outliers")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'minpts'")
@Reference(authors = "Markus M. Breunig, Hans-Peter Kriegel, Raymond Ng, Jörg Sander", //
    title = "OPTICS-OF: Identifying Local Outliers", //
    booktitle = "Proc. 3rd European Conf. on Principles of Knowledge Discovery and Data Mining (PKDD'99)", //
    url = "https://doi.org/10.1007/978-3-540-48247-5_28", //
    bibkey = "DBLP:conf/pkdd/BreunigKNS99")
public class OPTICSOF<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OPTICSOF.class);

  /**
   * Parameter to specify the threshold MinPts.
   */
  private int minpts;

  /**
   * Constructor with parameters.
   *
   * @param distanceFunction distance function
   * @param minpts minPts parameter
   */
  public OPTICSOF(Distance<? super O> distanceFunction, int minpts) {
    super(distanceFunction);
    this.minpts = minpts;
  }

  /**
   * Perform OPTICS-based outlier detection.
   *
   * @param database Database
   * @param relation Relation
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    DistanceQuery<O> distQuery = database.getDistanceQuery(relation, getDistance());
    KNNQuery<O> knnQuery = database.getKNNQuery(distQuery, minpts);
    RangeQuery<O> rangeQuery = database.getRangeQuery(distQuery);
    DBIDs ids = relation.getDBIDs();

    // FIXME: implicit preprocessor.
    WritableDataStore<KNNList> nMinPts = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, KNNList.class);
    WritableDoubleDataStore coreDistance = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    WritableIntegerDataStore minPtsNeighborhoodSize = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);

    // Pass 1
    // N_minpts(id) and core-distance(id)

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNList minptsNeighbours = knnQuery.getKNNForDBID(iditer, minpts);
      double d = minptsNeighbours.getKNNDistance();
      nMinPts.put(iditer, minptsNeighbours);
      coreDistance.putDouble(iditer, d);
      minPtsNeighborhoodSize.put(iditer, rangeQuery.getRangeForDBID(iditer, d).size());
    }

    // Pass 2
    WritableDataStore<List<Double>> reachDistance = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, List.class);
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      List<Double> core = new ArrayList<>();
      double lrd = 0;
      // TODO: optimize for double distances
      for(DoubleDBIDListIter neighbor = nMinPts.get(iditer).iter(); neighbor.valid(); neighbor.advance()) {
        double coreDist = coreDistance.doubleValue(neighbor);
        double dist = distQuery.distance(iditer, neighbor);
        double rd = MathUtil.max(coreDist, dist);
        lrd = rd + lrd;
        core.add(rd);
      }
      lrd = minPtsNeighborhoodSize.intValue(iditer) / lrd;
      reachDistance.put(iditer, core);
      lrds.putDouble(iditer, lrd);
    }

    // Pass 3
    DoubleMinMax ofminmax = new DoubleMinMax();
    WritableDoubleDataStore ofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double of = 0;
      for(DBIDIter neighbor = nMinPts.get(iditer).iter(); neighbor.valid(); neighbor.advance()) {
        double lrd = lrds.doubleValue(iditer);
        double lrdN = lrds.doubleValue(neighbor);
        of = of + lrdN / lrd;
      }
      of = of / minPtsNeighborhoodSize.intValue(iditer);
      ofs.putDouble(iditer, of);
      // update minimum and maximum
      ofminmax.put(of);
    }
    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("OPTICS Outlier Scores", relation.getDBIDs(), ofs);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(ofminmax.getMin(), ofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<Distance<? super O>> {
    /**
     * Parameter to specify the threshold MinPts.
     */
    protected int minpts = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter param = new IntParameter(AbstractOPTICS.Parameterizer.MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(param)) {
        minpts = param.getValue();
      }
    }

    @Override
    protected OPTICSOF<O> makeInstance() {
      return new OPTICSOF<>(distanceFunction, minpts);
    }
  }
}