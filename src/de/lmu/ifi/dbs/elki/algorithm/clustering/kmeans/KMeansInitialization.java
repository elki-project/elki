package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

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
import java.util.List;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;

/**
 * Interface for initializing K-Means
 * 
 * @author Erich Schubert
 *
 * @apiviz.landmark
 * 
 * @param <V> Object type
 */
public interface KMeansInitialization<V> {
  /**
   * Choose initial means
   * 
   * @param relation Relation
   * @param k Parameter k
   * @param distanceFunction Distance function 
   * @return List of chosen means for k-means
   */
  public abstract List<V> chooseInitialMeans(Relation<V> relation, int k, PrimitiveDistanceFunction<? super V, ?> distanceFunction);
}
