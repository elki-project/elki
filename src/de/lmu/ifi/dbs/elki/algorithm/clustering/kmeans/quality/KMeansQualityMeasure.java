package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface for computing the quality of a K-Means clustering.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Input Object restriction type
 * @param <D> Distance restriction type
 */
public interface KMeansQualityMeasure<O extends NumberVector<?>, D extends Distance<?>> {
  /**
   * Calculates and returns the quality measure.
   * 
   * @param clustering Clustering to analyze
   * @param distanceFunction Distance function to use (usually Euclidean or
   *        squared Euclidean!)
   * @param relation Relation for accessing objects
   * @param <V> Actual vector type (could be a subtype of O!)
   * 
   * @return quality measure
   */
  <V extends O> double calculateCost(Clustering<? extends MeanModel<V>> clustering, PrimitiveDistanceFunction<? super V, ? extends D> distanceFunction, Relation<V> relation);
}
