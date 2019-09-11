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
package elki.distance;

import java.util.Random;

import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBID;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.query.distance.DBIDDistanceQuery;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.utilities.Priority;
import elki.utilities.Util;
import elki.utilities.optionhandling.Parameterizer;

/**
 * This is a dummy distance providing random values (obviously not metrical),
 * useful mostly for unit tests and baseline evaluations: obviously this
 * distance provides no benefit whatsoever.
 * 
 * This distance is based on the combined hash codes of the two objects queried,
 * if they are different. Extra caution is done to ensure symmetry and objects
 * with the same ID will have a distance of 0. Obviously this distance is not
 * metrical.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Priority(Priority.SUPPLEMENTARY)
public class RandomStableDistance extends AbstractDatabaseDistance<DBID> implements DBIDDistance {
  /**
   * Static instance
   */
  public static final RandomStableDistance STATIC = new RandomStableDistance((new Random()).nextLong());

  /**
   * Seed for reproducible random.
   */
  private long seed;

  /**
   * Constructor. Usually it is preferred to use the static instance!
   */
  public RandomStableDistance(long seed) {
    super();
    this.seed = seed;
  }

  @Override
  public double distance(DBIDRef o1, DBIDRef o2) {
    final int c = DBIDUtil.compare(o1, o2);
    if(c == 0) {
      return 0.;
    }
    // Symmetry
    if(c > 0) {
      return distance(o2, o1);
    }
    return pseudoRandom(seed, Util.mixHashCodes(DBIDUtil.asInteger(o1), DBIDUtil.asInteger(o2), (int) seed));
  }

  /**
   * Pseudo random number generator, adaption of the common rand48 generator
   * which can be found in C (man drand48), Java and attributed to Donald Knuth.
   * 
   * @param seed Seed value
   * @param input Input code
   * 
   * @return Pseudo random double value
   */
  private double pseudoRandom(final long seed, int input) {
    // Default constants from "man drand48"
    final long mult = 0x5DEECE66DL;
    final long add = 0xBL;
    final long mask = (1L << 48) - 1; // 48 bit
    // Produce an initial seed each
    final long i1 = (input ^ seed ^ mult) & mask;
    final long i2 = (input ^ (seed >>> 16) ^ mult) & mask;
    // Compute the first random each
    final long l1 = (i1 * mult + add) & mask;
    final long l2 = (i2 * mult + add) & mask;
    // Use 53 bit total:
    final int r1 = (int) (l1 >>> 22); // 48 - 22 = 26
    final int r2 = (int) (l2 >>> 21); // 48 - 21 = 27
    double random = ((((long) r1) << 27) + r2) / (double) (1L << 53);
    return random;
  }

  @Override
  public String toString() {
    return "RandomDistance";
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(!this.getClass().equals(obj.getClass())) {
      return false;
    }
    return this.seed == ((RandomStableDistance) obj).seed;
  }

  @Override
  public int hashCode() {
    return (int) seed;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.DBID;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends DBID> DistanceQuery<T> instantiate(Relation<T> relation) {
    return (DistanceQuery<T>) new DBIDDistanceQuery((Relation<DBID>) relation, this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public RandomStableDistance make() {
      return RandomStableDistance.STATIC;
    }
  }
}
