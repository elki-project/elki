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
package de.lmu.ifi.dbs.elki.index.lsh.hashfunctions;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.projection.random.RandomProjectionFamily;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * LSH hash function for vector space data. Depending on the choice of random
 * vectors, it can be appropriate for Manhattan and Euclidean distances.
 * <p>
 * Reference:
 * <p>
 * M. Datar, N. Immorlica, P. Indyk, V. S. Mirrokni<br>
 * Locality-sensitive hashing scheme based on p-stable distributions<br>
 * Proc. 20th Annual Symposium on Computational Geometry<br>
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "M. Datar, N. Immorlica, P. Indyk, V. S. Mirrokni", //
    title = "Locality-sensitive hashing scheme based on p-stable distributions", //
    booktitle = "Proc. 20th Annual Symposium on Computational Geometry", //
    url = "https://doi.org/10.1145/997817.997857", //
    bibkey = "DBLP:conf/compgeom/DatarIIM04")
public class MultipleProjectionsLocalitySensitiveHashFunction implements LocalitySensitiveHashFunction<NumberVector> {
  /**
   * Projection matrix.
   */
  RandomProjectionFamily.Projection projection;

  /**
   * Shift offset.
   */
  double[] shift;

  /**
   * Scaling factor: inverse of width.
   */
  double iwidth;

  /**
   * Random numbers for mixing the hash codes of the individual functions
   */
  int[] randoms1;

  /**
   * Constructor.
   *
   * @param projection Projection vectors
   * @param width Width of bins
   * @param rnd Random number generator
   */
  public MultipleProjectionsLocalitySensitiveHashFunction(RandomProjectionFamily.Projection projection, double width, Random rnd) {
    super();
    this.projection = projection;
    this.iwidth = 1. / width;
    // Generate random shifts:
    final int num = projection.getOutputDimensionality();
    this.shift = new double[num];
    this.randoms1 = new int[num];
    for(int i = 0; i < num; i++) {
      shift[i] = rnd.nextDouble() * width;
      // Produce a large random number; although 7FFFFFFF would likely be large
      // enough, we try to stick to the suggested approach (which assumes
      // unsigned integers).
      randoms1[i] = (rnd.nextInt(0x10000D) << 16) + rnd.nextInt(0xFFFFD) + 1;
    }
  }

  /**
   * Bit mask for signed int to unsigned long conversion.
   */
  private final static long MASK32 = 0xFFFFFFFFL;

  @Override
  public int hashObject(NumberVector vec) {
    // Project the vector:
    final double[] proj = projection.project(vec);
    long t1sum = 0L;
    for(int i = 0; i < shift.length; i++) {
      int ai = (int) Math.floor((proj[i] + shift[i]) * iwidth);
      t1sum += (randoms1[i] & MASK32) * ai; // unsigned math!
    }
    return fastModPrime(t1sum);
  }

  @Override
  public int hashObject(NumberVector vec, double[] buf) {
    // Project the vector:
    projection.project(vec, buf);
    long t1sum = 0L;
    for(int i = 0; i < shift.length; i++) {
      int ai = (int) Math.floor((buf[i] + shift[i]) * iwidth);
      t1sum += (randoms1[i] & MASK32) * ai; // unsigned math!
    }
    return fastModPrime(t1sum);
  }

  /**
   * Fast modulo operation for the largest unsigned integer prime.
   *
   * @param data Long input
   * @return {@code data % (2^32 - 5)}.
   */
  public static int fastModPrime(long data) {
    // Mix high and low 32 bit:
    int high = (int) (data >>> 32);
    // Use fast multiplication with 5 for high:
    int alpha = ((int) data) + (high << 2 + high);
    // Note that in Java, PRIME will be negative.
    if(alpha < 0 && alpha > -5) {
      alpha = alpha + 5;
    }
    return alpha;
  }

  @Override
  public int getNumberOfProjections() {
    return this.projection.getOutputDimensionality();
  }
}
