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

import elki.logging.LoggingUtil;

/**
 * RandomFactory is responsible for creating {@link Random} generator objects.
 * It does not provide individual random numbers, but will create a random
 * generator; either using a fixed seed or random seeded (default).
 * <p>
 * The seed can be globally predefined using {@code -Delki.seed=123}.
 * <p>
 * These classes are not optimized for non-predictability, but for speed, as
 * scientific experiments are not likely to be adversarial.
 *
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @has - - - Random
 * @has - - - FastNonThreadsafeRandom
 */
public class RandomFactory {
  /**
   * Global default factory
   */
  public static final RandomFactory DEFAULT = new RandomFactory(getGlobalSeed()) {
    @Override
    public String toString() {
      return "GlobalRandom[" + Long.toString(this.seed) + "]";
    }
  };

  /**
   * Initialize the default random.
   * 
   * @return seed for the random generator factory.
   */
  private static long getGlobalSeed() {
    String sseed = System.getProperty("elki.seed");
    try {
      return (sseed != null && sseed.length() > 0) ? Long.parseLong(sseed) : System.nanoTime();
    }
    catch(NumberFormatException e) {
      LoggingUtil.warning("Failed to parse elki.seed environment variable containing '" + sseed + "', falling back to system time for seeding.");
      return System.nanoTime();
    }
  }

  /**
   * Seed.
   */
  protected long seed;

  /**
   * Factory method: Get a random factory for the given seed.
   * 
   * @param seed Seed
   * @return Instance
   */
  public static RandomFactory get(Long seed) {
    return seed == null ? DEFAULT : new RandomFactory(seed);
  }

  /**
   * Constructor.
   * 
   * @param seed Random seed
   */
  public RandomFactory(long seed) {
    super();
    this.seed = seed;
  }

  /**
   * Get a random generator.
   * 
   * @return Random generator
   */
  public Random getRandom() {
    return new Random(murmurMix64(seed++));
  }

  /**
   * Get a <em>non-threadsafe</em> random generator.
   * 
   * @return Random generator
   */
  public Random getSingleThreadedRandom() {
    return new Xoroshiro128NonThreadsafeRandom(murmurMix64(seed++));
  }

  /**
   * MurmurHash3 mixing function.
   * 
   * @param h input
   * @return Output
   */
  public static int murmurMix32(int h) {
    h ^= h >>> 16;
    h *= 0x85ebca6b;
    h ^= h >>> 13;
    h *= 0xc2b2ae35;
    h ^= h >>> 16;
    return h;
  }

  /**
   * MurmurHash3 mixing function.
   * 
   * @param k input
   * @return Output
   */
  public static long murmurMix64(long k) {
    k ^= k >>> 33;
    k *= 0xff51afd7ed558ccdL;
    k ^= k >>> 33;
    k *= 0xc4ceb9fe1a85ec53L;
    k ^= k >>> 33;
    return k;
  }

  @Override
  public String toString() {
    return "Random[" + Long.toString(seed) + "]";
  }
}
