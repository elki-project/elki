package de.lmu.ifi.dbs.elki.utilities.random;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.util.Random;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Drop-in replacement for {@link java.util.Random}, but not using atomic long
 * seeds. This implementation is <em>no longer thread-safe</em> (but faster)!
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class FastNonThreadsafeRandom extends Random {
  /**
   * Serial version number.
   */
  private static final long serialVersionUID = 1L;

  // These are the same constants as in {@link java.util.Random}
  // since we want to leave the random sequence unchanged.
  private static final long multiplier = 0x5DEECE66DL, addend = 0xBL,
      mask = (1L << 48) - 1;

  /**
   * The random seed. We can't use super.seed.
   */
  private long seed;

  /**
   * Constructor called only by localRandom.initialValue.
   */
  public FastNonThreadsafeRandom() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param seed Random generator seed.
   */
  public FastNonThreadsafeRandom(long seed) {
    this.seed = (seed ^ multiplier) & mask;
  }

  @Override
  public void setSeed(long seed) {
    this.seed = (seed ^ multiplier) & mask;
  }

  @Override
  protected int next(int bits) {
    // Linear Congruential Generator:
    seed = (seed * multiplier + addend) & mask;
    return (int) (seed >>> (48 - bits));
  }

  /**
   * Returns a pseudorandom, uniformly distributed {@code int} value between 0
   * (inclusive) and the specified value (exclusive), drawn from this random
   * number generator's sequence. The general contract of {@code nextInt} is
   * that one {@code int} value in the specified range is pseudorandomly
   * generated and returned. All {@code n} possible {@code int} values are
   * produced with (approximately) equal probability.
   * 
   * In contrast to the Java version, we use an approach that tries to avoid
   * divisions for performance. We will have a slightly worse distribution in
   * this fast version (see the XorShift generators for higher quality with
   * rejection sampling) discussed in:
   * <p>
   * D. Lemire<br />
   * Fast random shuffling<br />
   * http://lemire.me/blog/2016/06/30/fast-random-shuffling/
   * </p>
   */
  @Reference(authors = "D. Lemire", //
      title = "Fast random shuffling", //
      booktitle = "Daniel Lemire's blog", //
      url = "http://lemire.me/blog/2016/06/30/fast-random-shuffling/")
  @Override
  public int nextInt(int n) {
    if((n & -n) == n) { // i.e., n is a power of 2
      return (int) ((n * (long) next(31)) >>> 31);
    }
    seed = (seed * multiplier + addend) & mask;
    long ret = (seed >>> 16) * n;
    // Rejection sampling
    int leftover = (int) ret & 0x7FFFFFFF;
    if(leftover < n) {
      // With Java 8, we could use Integer.remainderUnsigned
      final long threshold = (-n & 0xFFFFFFFFL) % n;
      while(leftover < threshold) {
        seed = (seed * multiplier + addend) & mask;
        leftover = (int) (ret = (seed >>> 16) * n) & 0x7FFFFFFF;
      }
    }
    return (int) (ret >>> 32);
  }
}
