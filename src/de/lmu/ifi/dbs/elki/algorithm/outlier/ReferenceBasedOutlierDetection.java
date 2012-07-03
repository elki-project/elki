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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.result.ReferencePointsResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.GridBasedReferencePoints;
import de.lmu.ifi.dbs.elki.utilities.referencepoints.ReferencePointsHeuristic;

/**
 * <p>
 * provides the Reference-Based Outlier Detection algorithm, an algorithm that
 * computes kNN distances approximately, using reference points.
 * </p>
 * <p>
 * Reference:<br>
 * Y. Pei, O. R. Zaiane, Y. Gao: An Efficient Reference-Based Approach to
 * Outlier Detection in Large Datasets.</br> In: Proc. IEEE Int. Conf. on Data
 * Mining (ICDM'06), Hong Kong, China, 2006.
 * </p>
 * 
 * @author Lisa Reichert
 * @author Erich Schubert
 * 
 * @apiviz.composedOf ReferencePointsHeuristic
 * 
 * @param <V> a type of {@link NumberVector} as a suitable data object for this
 *        algorithm
 * @param <D> the distance type processed
 */
@Title("An Efficient Reference-based Approach to Outlier Detection in Large Datasets")
@Description("Computes kNN distances approximately, using reference points with various reference point strategies.")
@Reference(authors = "Y. Pei, O.R. Zaiane, Y. Gao", title = "An Efficient Reference-based Approach to Outlier Detection in Large Datasets", booktitle = "Proc. 6th IEEE Int. Conf. on Data Mining (ICDM '06), Hong Kong, China, 2006", url = "http://dx.doi.org/10.1109/ICDM.2006.17")
public class ReferenceBasedOutlierDetection<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ReferenceBasedOutlierDetection.class);

  /**
   * Parameter for the reference points heuristic.
   */
  public static final OptionID REFP_ID = OptionID.getOrCreateOptionID("refod.refp", "The heuristic for finding reference points.");

  /**
   * Parameter to specify the number of nearest neighbors of an object, to be
   * considered for computing its REFOD_SCORE, must be an integer greater than
   * 1.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("refod.k", "The number of nearest neighbors");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;

  /**
   * Stores the reference point strategy
   */
  private ReferencePointsHeuristic<V> refp;

  /**
   * Distance function to use.
   */
  private DistanceFunction<V, D> distanceFunction;

  /**
   * Constructor with parameters.
   * 
   * @param k k Parameter
   * @param distanceFunction distance function
   * @param refp Reference points heuristic
   */
  public ReferenceBasedOutlierDetection(int k, DistanceFunction<V, D> distanceFunction, ReferencePointsHeuristic<V> refp) {
    super();
    this.k = k;
    this.distanceFunction = distanceFunction;
    this.refp = refp;
  }

  /**
   * Run the algorithm on the given relation.
   * 
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Relation<V> relation) {
    DistanceQuery<V, D> distFunc = relation.getDatabase().getDistanceQuery(relation, distanceFunction);
    Collection<V> refPoints = refp.getReferencePoints(relation);

    DBIDs ids = relation.getDBIDs();
    // storage of distance/score values.
    WritableDoubleDataStore rbod_score = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_HOT);

    // Compute density estimation:
    {
      // compute density for one reference point, to initialize the first
      // density
      // value for each object, then update
      final Iterator<V> iter = refPoints.iterator();
      if(!iter.hasNext()) {
        throw new AbortException("Cannot compute ROS without reference points!");
      }
      V firstRef = iter.next();
      // compute distance vector for the first reference point
      List<DistanceResultPair<D>> firstReferenceDists = computeDistanceVector(firstRef, relation, distFunc);
      for(int l = 0; l < firstReferenceDists.size(); l++) {
        double density = computeDensity(firstReferenceDists, l);
        // Initial value
        rbod_score.putDouble(firstReferenceDists.get(l), density);
      }
      // compute density values for all remaining reference points
      while(iter.hasNext()) {
        V refPoint = iter.next();
        List<DistanceResultPair<D>> referenceDists = computeDistanceVector(refPoint, relation, distFunc);
        // compute density value for each object
        for(int l = 0; l < referenceDists.size(); l++) {
          double density = computeDensity(referenceDists, l);
          // Update minimum
          if(density < rbod_score.doubleValue(referenceDists.get(l))) {
            rbod_score.putDouble(referenceDists.get(l), density);
          }
        }
      }
    }
    // compute maximum density
    double maxDensity = 0.0;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double dens = rbod_score.doubleValue(iditer);
      if(dens > maxDensity) {
        maxDensity = dens;
      }
    }
    // compute ROS
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double score = 1 - (rbod_score.doubleValue(iditer) / maxDensity);
      rbod_score.putDouble(iditer, score);
    }

    // adds reference points to the result. header information for the
    // visualizer to find the reference points in the result
    ReferencePointsResult<V> refp = new ReferencePointsResult<V>("Reference points", "reference-points", refPoints);

    Relation<Double> scoreResult = new MaterializedRelation<Double>("Reference-points Outlier Scores", "reference-outlier", TypeUtil.DOUBLE, rbod_score, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(0.0, 1.0, 0.0, 1.0, 0.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    result.addChildResult(refp);
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
  protected DistanceDBIDResult<D> computeDistanceVector(V refPoint, Relation<V> database, DistanceQuery<V, D> distFunc) {
    // TODO: optimize for double distances?
    GenericDistanceDBIDList<D> referenceDists = new GenericDistanceDBIDList<D>(database.size());
    for(DBIDIter iditer = database.iterDBIDs(); iditer.valid(); iditer.advance()) {
      referenceDists.add(distFunc.distance(iditer, refPoint), iditer);
    }
    Collections.sort(referenceDists);
    return referenceDists;
  }

  /**
   * Computes the density of an object. The density of an object is the
   * distances to the k nearest neighbors. Neighbors and distances are computed
   * approximately. (approximation for kNN distance: instead of a normal NN
   * search the NN of an object are those objects that have a similar distance
   * to a reference point. The k- nearest neighbors of an object are those
   * objects that lay close to the object in the reference distance vector)
   * 
   * @param referenceDists vector of the reference distances,
   * @param index index of the current object
   * @return density for one object and reference point
   */
  protected double computeDensity(List<DistanceResultPair<D>> referenceDists, int index) {
    final DistanceResultPair<D> x = referenceDists.get(index);
    final double xDist = x.getDistance().doubleValue();

    int lef = index - 1;
    int rig = index + 1;
    Mean mean = new Mean();
    double lef_d = (lef >= 0) ? referenceDists.get(lef).getDistance().doubleValue() : Double.NEGATIVE_INFINITY;
    double rig_d = (rig < referenceDists.size()) ? referenceDists.get(rig).getDistance().doubleValue() : Double.NEGATIVE_INFINITY;
    while(mean.getCount() < k) {
      if(lef >= 0 && rig < referenceDists.size()) {
        // Prefer n or m?
        if(Math.abs(lef_d - xDist) < Math.abs(rig_d - xDist)) {
          mean.put(Math.abs(lef_d - xDist));
          // Update n
          lef--;
          lef_d = (lef >= 0) ? referenceDists.get(lef).getDistance().doubleValue() : Double.NEGATIVE_INFINITY;
        }
        else {
          mean.put(Math.abs(rig_d - xDist));
          // Update right
          rig++;
          rig_d = (rig < referenceDists.size()) ? referenceDists.get(rig).getDistance().doubleValue() : Double.NEGATIVE_INFINITY;
        }
      }
      else {
        if(lef >= 0) {
          // Choose left, since right is not available.
          mean.put(Math.abs(lef_d - xDist));
          // update left
          lef--;
          lef_d = (lef >= 0) ? referenceDists.get(lef).getDistance().doubleValue() : Double.NEGATIVE_INFINITY;
        }
        else if(rig < referenceDists.size()) {
          // Choose right, since left is not available
          mean.put(Math.abs(rig_d - xDist));
          // Update right
          rig++;
          rig_d = (rig < referenceDists.size()) ? referenceDists.get(rig).getDistance().doubleValue() : Double.NEGATIVE_INFINITY;
        }
        else {
          // Not enough objects in database?
          throw new IndexOutOfBoundsException();
        }
      }
    }

    return 1.0 / mean.getMean();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
  public static class Parameterizer<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<V, D> {
    /**
     * Holds the value of {@link #K_ID}.
     */
    private int k;

    /**
     * Stores the reference point strategy
     */
    private ReferencePointsHeuristic<V> refp;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter pK = new IntParameter(K_ID, new GreaterConstraint(1));
      if(config.grab(pK)) {
        k = pK.getValue();
      }
      final ObjectParameter<ReferencePointsHeuristic<V>> refpP = new ObjectParameter<ReferencePointsHeuristic<V>>(REFP_ID, ReferencePointsHeuristic.class, GridBasedReferencePoints.class);
      if(config.grab(refpP)) {
        refp = refpP.instantiateClass(config);
      }
    }

    @Override
    protected ReferenceBasedOutlierDetection<V, D> makeInstance() {
      return new ReferenceBasedOutlierDetection<V, D>(k, distanceFunction, refp);
    }
  }
}