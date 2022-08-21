/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import elki.Algorithm;
import elki.clustering.optics.AbstractOPTICS;
import elki.clustering.optics.OPTICSTypeAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.QuotientOutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @has - - - KNNSearcher
 * @has - - - RangeSearcher
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
public class OPTICSOF<O> implements OutlierAlgorithm {
  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Parameter to specify the threshold MinPts.
   */
  protected int minpts;

  /**
   * Constructor with parameters.
   *
   * @param distance distance function
   * @param minpts minPts parameter
   */
  public OPTICSOF(Distance<? super O> distance, int minpts) {
    super();
    this.distance = distance;
    this.minpts = minpts;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Perform OPTICS-based outlier detection.
   *
   * @param relation Relation
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<O> relation) {
    KNNSearcher<DBIDRef> knnQuery = new QueryBuilder<>(relation, distance).kNNByDBID(minpts);
    DBIDs ids = relation.getDBIDs();

    // FIXME: implicit preprocessor.
    WritableDataStore<KNNList> nMinPts = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, KNNList.class);
    WritableDoubleDataStore coreDistance = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

    // Pass 1
    // N_minpts(id) and core-distance(id)

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNList minptsNeighbours = knnQuery.getKNN(iditer, minpts);
      double d = minptsNeighbours.getKNNDistance();
      nMinPts.put(iditer, minptsNeighbours);
      coreDistance.putDouble(iditer, d);
    }

    // Pass 2
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final KNNList neighbors = nMinPts.get(iditer);
      double lrd = 0;
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        double coreDist = coreDistance.doubleValue(neighbor);
        double rd = MathUtil.max(coreDist, neighbor.doubleValue());
        lrd += rd;
      }
      lrd = neighbors.size() / lrd;
      lrds.putDouble(iditer, lrd);
    }

    // Pass 3
    DoubleMinMax ofminmax = new DoubleMinMax();
    WritableDoubleDataStore ofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final KNNList neighbors = nMinPts.get(iditer);
      double lrd = lrds.doubleValue(iditer);
      double of = 0;
      if(lrd > 0) {
        for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          of += lrds.doubleValue(neighbor) / lrd;
        }
        of /= neighbors.size();
      } // else 0.
      ofs.putDouble(iditer, of);
      ofminmax.put(of);
    }
    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("OPTICS Outlier Scores", relation.getDBIDs(), ofs);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(ofminmax.getMin(), ofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Parameter to specify the threshold MinPts.
     */
    protected int minpts = 0;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(AbstractOPTICS.Par.MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> minpts = x);
    }

    @Override
    public OPTICSOF<O> make() {
      return new OPTICSOF<>(distance, minpts);
    }
  }
}
