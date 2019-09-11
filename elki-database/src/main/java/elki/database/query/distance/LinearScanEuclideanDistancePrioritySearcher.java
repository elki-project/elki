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
package elki.database.query.distance;

import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.query.LinearScanQuery;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;

import net.jafama.FastMath;

/**
 * Default linear scan search class, for Euclidean distance.
 * <p>
 * This is a fallback option - results are not returned in order.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - DistanceQuery
 *
 * @param <O> Database object type
 */
public class LinearScanEuclideanDistancePrioritySearcher<O extends NumberVector> implements DistancePrioritySearcher<O>, LinearScanQuery {
  /**
   * Distance to use.
   */
  private DistanceQuery<O> distanceQuery;

  /**
   * Iterator.
   */
  private DBIDIter iter;

  /**
   * Current query object.
   */
  private O query;

  /**
   * Cutoff threshold.
   */
  private double thresholdsq;

  /**
   * Current distance
   */
  private double curdist;

  /**
   * Squared distance
   */
  private final static SquaredEuclideanDistance SQUARED = SquaredEuclideanDistance.STATIC;

  /**
   * Constructor.
   *
   * @param distanceQuery Distance function to use
   */
  public LinearScanEuclideanDistancePrioritySearcher(DistanceQuery<O> distanceQuery) {
    super();
    this.distanceQuery = distanceQuery;
    assert (EuclideanDistance.STATIC.equals(distanceQuery.getDistance()));
  }

  @Override
  public DistancePrioritySearcher<O> search(DBIDRef query) {
    return search(distanceQuery.getRelation().get(query));
  }

  @Override
  public DistancePrioritySearcher<O> search(O query) {
    this.query = query;
    this.iter = null;
    this.thresholdsq = Double.POSITIVE_INFINITY;
    return this;
  }

  @Override
  public boolean valid() {
    if(iter == null) {
      iter = distanceQuery.getRelation().iterDBIDs();
      scan();
    }
    return iter.valid();
  }

  @Override
  public DBIDIter advance() {
    iter.advance();
    scan();
    return this;
  }

  /**
   * Scan for the next object.
   */
  private void scan() {
    while(iter.valid()) {
      double curdistsq = SQUARED.distance(query, distanceQuery.getRelation().get(iter));
      if(curdistsq <= thresholdsq) {
        curdist = FastMath.sqrt(curdistsq);
        return;
      }
      iter.advance();
    }
    curdist = Double.NaN;
  }

  @Override
  public int internalGetIndex() {
    return iter.internalGetIndex();
  }

  @Override
  public DistancePrioritySearcher<O> decreaseCutoff(double threshold) {
    this.thresholdsq = threshold * threshold;
    return this; // Ignored
  }

  @Override
  public double computeExactDistance() {
    return curdist;
  }

  @Override
  public double getApproximateAccuracy() {
    return 0;
  }

  @Override
  public double getApproximateDistance() {
    return curdist;
  }

  @Override
  public double getLowerBound() {
    return curdist;
  }

  @Override
  public double getUpperBound() {
    return curdist;
  }

  @Override
  public O getCandidate() {
    return distanceQuery.getRelation().get(iter);
  }
}
