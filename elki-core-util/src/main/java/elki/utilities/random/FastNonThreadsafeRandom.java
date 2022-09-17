/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.utilities.random;

import java.util.Random;

import elki.utilities.documentation.Reference;

/**
 * Drop-in replacement for {@link java.util.Random}, but not using atomic long
 * seeds. This implementation is <em>no longer thread-safe</em> (but faster)!
 * <p>
 * It is still the same Linear Congruential Generator (LCG), with a cycle length
 * of 2<sup>48</sup>, of which we only use 32 bits at a time. Given the same
 * seed, it is expected to produce the exact same random sequence as Java's
 * {@link java.util.Random}, given the same seed. Which implies that you <b>need
 * to randomize your seeds</b> with a better random source.
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
  public synchronized void setSeed(long seed) {
    this.seed = (seed ^ multiplier) & mask;
  }

  @Override
  protected int next(int bits) {
    // Linear Congruential Generator:
    return (int) ((seed = (seed * multiplier + addend) & mask) >>> (48 - bits));
  }

  @Override
  public int nextInt() {
    // Linear Congruential Generator:
    return (int) ((seed = (seed * multiplier + addend) & mask) >>> 16);
  }

  @Override
  public double nextDouble() {
    return (((long) (next(26)) << 27) + next(27)) * 0x1.0p-53;
  }

  /** Exception message for non-positive bounds */
  protected static final String BADBOUND = "bound must be positive";

  /**
   * Returns a pseudorandom, uniformly distributed {@code int} value between 0
   * (inclusive) and the specified value (exclusive), drawn from this random
   * number generator's sequence. The general contract of {@code nextInt} is
   * that one {@code int} value in the specified range is pseudorandomly
   * generated and returned. All {@code n} possible {@code int} values are
   * produced with (approximately) equal probability.
   * <p>
   * This version mostly does this - except for very large n. In such cases, it
   * may be better to use {@link #nextIntRefined(int)} instead, which uses a
   * rejection sampling technique to further reduce the bias.
   * <p>
   * In contrast to the Java version, we use an approach that tries to avoid
   * divisions for performance. We will have a slightly worse distribution in
   * this fast version (see the XorShift generators for higher quality with
   * rejection sampling) discussed in:
   * <p>
   * D. Lemire<br>
   * Fast random shuffling<br>
   * http://lemire.me/blog/2016/06/30/fast-random-shuffling/
   * <p>
   * D. Lemire<br>
   * Fast Random Integer Generation in an Interval<br>
   * ACM Trans. Model. Comput. Simul. 29(1)
   */
  @Reference(authors = "D. Lemire", //
      title = "Fast random shuffling", //
      booktitle = "Daniel Lemire's blog", //
      url = "http://lemire.me/blog/2016/06/30/fast-random-shuffling/", //
      bibkey = "blog/Lemire16")
  @Reference(authors = "D. Lemire", //
      title = "Fast Random Integer Generation in an Interval", //
      booktitle = "ACM Trans. Model. Comput. Simul. 29(1)", //
      url = "https://doi.org/10.1145/3230636", //
      bibkey = "DBLP:journals/tomacs/Lemire19")
  @Override
  public int nextInt(int n) {
    if(n <= 0) {
      throw new IllegalArgumentException(BADBOUND);
    }
    return (int) ((((seed = (seed * multiplier + addend) & mask) >>> 16) * n) >>> 32);
  }

  /**
   * Returns a pseudorandom, uniformly distributed {@code int} value between 0
   * (inclusive) and the specified value (exclusive), drawn from this random
   * number generator's sequence. The general contract of {@code nextInt} is
   * that one {@code int} value in the specified range is pseudorandomly
   * generated and returned. All {@code n} possible {@code int} values are
   * produced with (approximately) equal probability.
   * <p>
   * In contrast to the Java version, we use an approach that tries to avoid
   * divisions for performance. In this method, we also employ rejection
   * sampling (for marginal improvement) as discussed in:
   * <p>
   * D. Lemire<br>
   * Fast random shuffling<br>
   * http://lemire.me/blog/2016/06/30/fast-random-shuffling/
   * <p>
   * D. Lemire<br>
   * Fast Random Integer Generation in an Interval<br>
   * ACM Trans. Model. Comput. Simul. 29(1)
   * <p>
   * In our experiments, the difference was negligible, as the rejections are
   * quite rare events at least for our use case.
   */
  @Reference(authors = "D. Lemire", //
      title = "Fast random shuffling", //
      booktitle = "Daniel Lemire's blog", //
      url = "http://lemire.me/blog/2016/06/30/fast-random-shuffling/", //
      bibkey = "blog/Lemire16")
  @Reference(authors = "D. Lemire", //
      title = "Fast Random Integer Generation in an Interval", //
      booktitle = "ACM Trans. Model. Comput. Simul. 29(1)", //
      url = "https://doi.org/10.1145/3230636", //
      bibkey = "DBLP:journals/tomacs/Lemire19")
  public int nextIntRefined(int n) {
    if(n <= 0) {
      throw new IllegalArgumentException(BADBOUND);
    }
    long ret = ((seed = (seed * multiplier + addend) & mask) >>> 16) * n;
    // Rejection sampling; but the if is only rarely required so this is fast.
    int leftover = (int) (ret & 0x7FFFFFFFL);
    if(leftover < n) {
      while(leftover < Integer.remainderUnsigned(-n, n)) {
        // Inlined for best performance.
        // Written in three lines it would be more readable, obviously.
        leftover = (int) ((ret = ((seed = (seed * multiplier + addend) & mask) >>> 16) * n) & 0x7FFFFFFFL);
      }
    }
    return (int) (ret >>> 32);
  }
}
