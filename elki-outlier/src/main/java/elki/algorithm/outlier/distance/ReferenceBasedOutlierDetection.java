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
package elki.algorithm.outlier.distance;

import java.util.Collection;

import elki.algorithm.AbstractNumberVectorDistanceBasedAlgorithm;
import elki.algorithm.outlier.OutlierAlgorithm;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.distance.PrimitiveDistanceQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.distancefunction.NumberVectorDistanceFunction;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.result.Metadata;
import elki.result.ReferencePointsResult;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.referencepoints.GridBasedReferencePoints;
import elki.utilities.referencepoints.ReferencePointsHeuristic;

/**
 * Reference-Based Outlier Detection algorithm, an algorithm that computes kNN
 * distances approximately, using reference points.
 * <p>
 * kNN distances are approximated by the difference in distance from a reference
 * point. For this approximation to be of high quality, triangle inequality is
 * required; but the algorithm can also process non-metric distances.
 * <p>
 * Reference:
 * <p>
 * Y. Pei, O. R. Zaiane, Y. Gao<br>
 * An Efficient Reference-Based Approach to Outlier Detection in Large
 * Datasets<br>
 * Proc. IEEE Int. Conf. on Data Mining (ICDM'06)
 *
 * @author Lisa Reichert
 * @author Erich Schubert
 * @since 0.3
 *
 * @composed - - - ReferencePointsHeuristic
 */
@Title("An Efficient Reference-based Approach to Outlier Detection in Large Datasets")
@Description("Computes kNN distances approximately, using reference points with various reference point strategies.")
@Reference(authors = "Y. Pei, O. R. Zaiane, Y. Gao", //
    title = "An Efficient Reference-based Approach to Outlier Detection in Large Datasets", //
    booktitle = "Proc. 6th IEEE Int. Conf. on Data Mining (ICDM '06)", //
    url = "https://doi.org/10.1109/ICDM.2006.17", //
    bibkey = "DBLP:conf/icdm/PeiZG06")
public class ReferenceBasedOutlierDetection extends AbstractNumberVectorDistanceBasedAlgorithm<NumberVector, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ReferenceBasedOutlierDetection.class);

  /**
   * Holds the number of neighbors to use for density estimation.
   */
  private int k;

  /**
   * Stores the reference point strategy.
   */
  private ReferencePointsHeuristic refp;

  /**
   * Constructor with parameters.
   *
   * @param k k Parameter
   * @param distanceFunction distance function
   * @param refp Reference points heuristic
   */
  public ReferenceBasedOutlierDetection(int k, NumberVectorDistanceFunction<? super NumberVector> distanceFunction, ReferencePointsHeuristic refp) {
    super(distanceFunction);
    this.k = k;
    this.refp = refp;
  }

  /**
   * Run the algorithm on the given relation.
   *
   * @param database Database
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<? extends NumberVector> relation) {
    @SuppressWarnings("unchecked")
    PrimitiveDistanceQuery<? super NumberVector> distq = (PrimitiveDistanceQuery<? super NumberVector>) database.getDistanceQuery(relation, distanceFunction);
    Collection<? extends NumberVector> refPoints = refp.getReferencePoints(relation);
    if(refPoints.isEmpty()) {
      throw new AbortException("Cannot compute ROS without reference points!");
    }

    DBIDs ids = relation.getDBIDs();
    if(k >= ids.size()) {
      throw new AbortException("k must not be chosen larger than the database size!");
    }
    // storage of distance/score values.
    WritableDoubleDataStore rbod_score = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_HOT, Double.NaN);

    // Compute density estimation:
    for(NumberVector refPoint : refPoints) {
      DoubleDBIDList referenceDists = computeDistanceVector(refPoint, relation, distq);
      updateDensities(rbod_score, referenceDists);
    }
    // compute maximum density
    DoubleMinMax mm = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      mm.put(rbod_score.doubleValue(iditer));
    }
    // compute ROS
    double scale = mm.getMax() > 0. ? 1. / mm.getMax() : 1.;
    mm.reset(); // Reuse
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double score = 1 - (rbod_score.doubleValue(iditer) * scale);
      mm.put(score);
      rbod_score.putDouble(iditer, score);
    }

    DoubleRelation scoreResult = new MaterializedDoubleRelation("Reference-points Outlier Scores", relation.getDBIDs(), rbod_score);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), 0., 1., 0.);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    // adds reference points to the result. header information for the
    // visualizer to find the reference points in the result
    ReferencePointsResult<? extends NumberVector> refresult = new ReferencePointsResult<>(refPoints);
    Metadata.of(refresult).setLongName("Reference points");
    Metadata.hierarchyOf(result).addChild(refresult);
    return result;
  }

  /**
   * Computes for each object the distance to one reference point. (one
   * dimensional representation of the data set)
   *
   * @param refPoint Reference Point Feature Vector
   * @param database database to work on
   * @param distFunc Distance function to use
   * @return array containing the distance to one reference point for each
   *         database object and the object id
   */
  protected DoubleDBIDList computeDistanceVector(NumberVector refPoint, Relation<? extends NumberVector> database, PrimitiveDistanceQuery<? super NumberVector> distFunc) {
    ModifiableDoubleDBIDList referenceDists = DBIDUtil.newDistanceDBIDList(database.size());
    for(DBIDIter iditer = database.iterDBIDs(); iditer.valid(); iditer.advance()) {
      referenceDists.add(distFunc.distance(iditer, refPoint), iditer);
    }
    referenceDists.sort();
    return referenceDists;
  }

  /**
   * Update the density estimates for each object.
   *
   * @param rbod_score Density storage
   * @param referenceDists Distances from current reference point
   */
  protected void updateDensities(WritableDoubleDataStore rbod_score, DoubleDBIDList referenceDists) {
    DoubleDBIDListIter it = referenceDists.iter();
    for(int l = 0; l < referenceDists.size(); l++) {
      double density = computeDensity(referenceDists, it, l);
      // computeDensity modified the iterator, reset:
      it.seek(l);
      // NaN indicates the first run.
      if(!(density > rbod_score.doubleValue(it))) {
        rbod_score.putDouble(it, density);
      }
    }
  }

  /**
   * Computes the density of an object. The density of an object is the
   * distances to the k nearest neighbors. Neighbors and distances are computed
   * approximately. (approximation for kNN distance: instead of a normal NN
   * search the NN of an object are those objects that have a similar distance
   * to a reference point. The k- nearest neighbors of an object are those
   * objects that lay close to the object in the reference distance vector)
   *
   * @param referenceDists vector of the reference distances
   * @param iter Iterator to this list (will be reused)
   * @param index index of the current object
   * @return density for one object and reference point
   */
  protected double computeDensity(DoubleDBIDList referenceDists, DoubleDBIDListIter iter, int index) {
    final int size = referenceDists.size();
    final double xDist = iter.seek(index).doubleValue();

    int lef = index, rig = index;
    double sum = 0.;
    double lef_d = (--lef >= 0) ? xDist - iter.seek(lef).doubleValue() : Double.POSITIVE_INFINITY;
    double rig_d = (++rig < size) ? iter.seek(rig).doubleValue() - xDist : Double.POSITIVE_INFINITY;
    for(int i = 0; i < k; ++i) {
      if(lef >= 0 && rig < size) {
        // Prefer n or m?
        if(lef_d < rig_d) {
          sum += lef_d;
          // Update left
          lef_d = (--lef >= 0) ? xDist - iter.seek(lef).doubleValue() : Double.POSITIVE_INFINITY;
        }
        else {
          sum += rig_d;
          // Update right
          rig_d = (++rig < size) ? iter.seek(rig).doubleValue() - xDist : Double.POSITIVE_INFINITY;
        }
      }
      else if(lef >= 0) {
        // Choose left, since right is not available.
        sum += lef_d;
        // update left
        lef_d = (--lef >= 0) ? xDist - iter.seek(lef).doubleValue() : Double.POSITIVE_INFINITY;
      }
      else if(rig < size) {
        // Choose right, since left is not available
        sum += rig_d;
        // Update right
        rig_d = (++rig < size) ? iter.seek(rig).doubleValue() - xDist : Double.POSITIVE_INFINITY;
      }
      else {
        // Not enough objects in database?
        throw new IndexOutOfBoundsException("Less than k objects?");
      }
    }
    return k / sum;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distanceFunction.getInputTypeRestriction());
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
  public static class Parameterizer extends AbstractNumberVectorDistanceBasedAlgorithm.Parameterizer<NumberVector> {
    /**
     * Parameter for the reference points heuristic.
     */
    public static final OptionID REFP_ID = new OptionID("refod.refp", "The heuristic for finding reference points.");

    /**
     * Parameter to specify the number of nearest neighbors of an object, to be
     * considered for computing its REFOD_SCORE, must be an integer greater than
     * 1.
     */
    public static final OptionID K_ID = new OptionID("refod.k", "The number of nearest neighbors");

    /**
     * Holds the value of {@link #K_ID}.
     */
    private int k;

    /**
     * Stores the reference point strategy
     */
    private ReferencePointsHeuristic refp;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter pK = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(pK)) {
        k = pK.getValue();
      }
      final ObjectParameter<ReferencePointsHeuristic> refpP = new ObjectParameter<>(REFP_ID, ReferencePointsHeuristic.class, GridBasedReferencePoints.class);
      if(config.grab(refpP)) {
        refp = refpP.instantiateClass(config);
      }
    }

    @Override
    protected ReferenceBasedOutlierDetection makeInstance() {
      return new ReferenceBasedOutlierDetection(k, distanceFunction, refp);
    }
  }
}
