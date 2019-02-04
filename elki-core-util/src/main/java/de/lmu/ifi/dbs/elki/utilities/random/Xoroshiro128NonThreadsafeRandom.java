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
package de.lmu.ifi.dbs.elki.utilities.random;

import java.util.Random;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Replacement for Java's {@link java.util.Random} class, using a different
 * random number generation strategy. Java's random generator is optimized for
 * speed, but may lack the randomness needed for more complex experiments.
 * <p>
 * This approach is based on the work on Xoroshiro128+ by Sebastiano Vigna,
 * with the original copyright statement:
 * <p>
 * Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)
 * <p>
 * To the extent possible under law, the author has dedicated all copyright and
 * related and neighboring rights to this software to the public domain
 * worldwide. This software is distributed without any warranty.
 * <p>
 * See http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Reference(authors = "D. Blackman, S. Vigna", //
    title = "xoroshiro+ / xorshift* / xorshift+ generators and the PRNG shootout", //
    booktitle = "Online", //
    url = "http://xoroshiro.di.unimi.it/", //
    bibkey = "web/BlackmanV16")
public class Xoroshiro128NonThreadsafeRandom extends Random {
  /**
   * Serial version number.
   */
  private static final long serialVersionUID = 1L;

  /**
   * State of random number generator.
   */
  private long s0, s1;

  /**
   * Constructor called only by localRandom.initialValue.
   */
  public Xoroshiro128NonThreadsafeRandom() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param seed Random generator seed.
   */
  public Xoroshiro128NonThreadsafeRandom(long seed) {
    super(seed);
  }

  @Override
  public void setSeed(long seed) {
    long xor64 = seed != 0 ? seed : 4101842887655102017L;
    // XorShift64* generator to seed:
    xor64 ^= xor64 >>> 12; // a
    xor64 ^= xor64 << 25; // b
    xor64 ^= xor64 >>> 27; // c
    s0 = xor64 * 2685821657736338717L;
    xor64 ^= xor64 >>> 12; // a
    xor64 ^= xor64 << 25; // b
    xor64 ^= xor64 >>> 27; // c
    s1 = xor64 * 2685821657736338717L;
  }

  @Override
  public long nextLong() {
    long t0 = s0, t1 = s1;
    final long result = t0 + t1;
    t1 ^= t0;
    s0 = Long.rotateLeft(t0, 55) ^ t1 ^ (t1 << 14); // a, b
    s1 = Long.rotateLeft(t1, 36); // c
    return result;
  }

  @Override
  protected int next(int bits) {
    return (int) (nextLong() >>> (64 - bits));
  }

  @Override
  public int nextInt() {
    return (int) (nextLong() >>> 32);
  }

  @Override
  public double nextDouble() {
    return ((long) nextLong() >>> 11) * 0x1.0p-53;
  }

  static final String BADBOUND = "bound must be positive";

  /**
   * Returns a pseudorandom, uniformly distributed {@code int} value between 0
   * (inclusive) and the specified value (exclusive), drawn from this random
   * number generator's sequence. The general contract of {@code nextInt} is
   * that one {@code int} value in the specified range is pseudorandomly
   * generated and returned. All {@code n} possible {@code int} values are
   * produced with (approximately) equal probability.
   * <p>
   * In contrast to the Java version, we use an approach that tries to avoid
   * divisions for performance discussed in:
   * <p>
   * D. Lemire<br>
   * Fast random shuffling<br>
   * http://lemire.me/blog/2016/06/30/fast-random-shuffling/
   */
  @Reference(authors = "D. Lemire", //
      title = "Fast random shuffling", //
      booktitle = "Daniel Lemire's blog", //
      url = "http://lemire.me/blog/2016/06/30/fast-random-shuffling/", //
      bibkey = "blog/Lemire16")
  @Override
  public int nextInt(int n) {
    if(n <= 0) {
      throw new IllegalArgumentException(BADBOUND);
    }
    long r = nextLong();
    if((n & -n) == n) { // power of two
      r &= n - 1;
    }
    else {
      r = ((r >>> 32) * n) >>> 32;
    }
    return (int) r;
  }
}
