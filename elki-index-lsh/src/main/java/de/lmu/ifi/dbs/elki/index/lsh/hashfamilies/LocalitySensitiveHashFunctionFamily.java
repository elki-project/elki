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
package de.lmu.ifi.dbs.elki.index.lsh.hashfamilies;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.lsh.hashfunctions.LocalitySensitiveHashFunction;

/**
 * LSH family of hash functions.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <V> Object type
 */
public interface LocalitySensitiveHashFunctionFamily<V> {
  /**
   * Get the input type information.
   * 
   * @return Input type information.
   */
  TypeInformation getInputTypeRestriction();

  /**
   * Generate hash functions for the given relation.
   * 
   * @param relation Relation to index
   * @param l Number of hash tables to use
   * @return Family of hash functions
   */
  ArrayList<? extends LocalitySensitiveHashFunction<? super V>> generateHashFunctions(Relation<? extends V> relation, int l);

  /**
   * Check whether the given distance function can be accelerated using this
   * hash family.
   * 
   * @param df Distance function.
   * @return {@code true} when appropriate.
   */
  boolean isCompatible(DistanceFunction<?> df);
}
