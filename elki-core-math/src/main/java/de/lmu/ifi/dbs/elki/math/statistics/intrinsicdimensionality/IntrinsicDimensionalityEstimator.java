package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;

/**
 * Estimate the intrinsic dimensionality from a distance list.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface IntrinsicDimensionalityEstimator {
  /**
   * Estimate from a distance list.
   * 
   * @param distances Distances
   * @return Estimated intrinsic dimensionality
   */
  double estimate(double[] distances);

  /**
   * Estimate from a distance list.
   * 
   * @param distances Distances
   * @param size Valid size
   * @return Estimated intrinsic dimensionality
   */
  double estimate(double[] distances, int size);

  /**
   * Estimate from a distance list.
   * 
   * @param data Data
   * @param adapter Array adapter
   * @param <A> array type
   * @return Estimated intrinsic dimensionality
   */
  <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter);

  /**
   * Estimate from a distance list.
   * 
   * @param data Data
   * @param adapter Array adapter
   * @param size Length
   * @param <A> array type
   * @return Estimated intrinsic dimensionality
   */
  <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, int size);

  /**
   * Estimate from a Reference Point, a KNNQuery and the neighborhood size k.
   * 
   * @param knnq KNNQuery
   * @param cur reference point
   * @param k neighborhood size
   * @return Estimated intrinsic dimensionality
   */
  double estimate(KNNQuery<?> knnq, DBIDRef cur, int k);

  /**
   * Estimate from a distance list.
   * 
   * @param rnq RangeQuery
   * @param cur reference point
   * @param range neighborhood radius
   * @return Estimated intrinsic dimensionality
   */
  double estimate(RangeQuery<?> rnq, DBIDRef cur, double range);
}
