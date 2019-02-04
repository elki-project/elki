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

/**
 * RandomFactory is responsible for creating {@link Random} generator objects.
 * It does not provide individual random numbers, but will create a random
 * generator; either using a fixed seed or random seeded (default).
 * 
 * The seed can be globally predefined using {@code -Delki.seed=123}.
 * 
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
  public static RandomFactory DEFAULT = new RandomFactory(getGlobalSeed()) {
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
    return (sseed != null) ? Long.parseLong(sseed) : System.nanoTime();
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
    if(seed == null) {
      return DEFAULT;
    }
    return new RandomFactory(seed);
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
    return new Random(seed++);
  }

  /**
   * Get a <em>non-threadsafe</em> random generator.
   * 
   * @return Random generator
   */
  public Random getSingleThreadedRandom() {
    return new Xoroshiro128NonThreadsafeRandom(seed++);
  }

  @Override
  public String toString() {
    return "Random[" + Long.toString(seed) + "]";
  }
}