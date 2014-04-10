package de.lmu.ifi.dbs.elki.math.random;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

/**
 * Replacement for Java's {@link java.util.Random} class, using a different
 * random number generation strategy. Java's random generator is optimized for
 * speed, but may lack the randomness needed for more complex experiments.
 * 
 * This approach is based on
 * "Numerical Recipes in C: The Art of Scientific Computing" as well as the
 * discussion of this at
 * http://www.javamex.com/tutorials/random_numbers/numerical_recipes.shtml
 * 
 * Due to the license of "Numerical Recipes", we could use their code, but we
 * have to implement the mathematics ourselves. The resulting code was virtually
 * identical, as the mathematics of the random generator translate literally
 * into computer code.
 * 
 * This code is slower than {@link FastNonThreadsafeRandom} and
 * {@link java.util.Random}, but should offer higher quality random numbers. It
 * is, however, <b>not safe to use for cryptography</b>!
 * 
 * @author Erich Schubert
 */
public class NumericalRecipesNonThreadsafeRandom extends Random {
  /**
   * Serial version number.
   */
  private static final long serialVersionUID = 1L;

  /**
   * State of random number generator.
   */
  private long u, v = 4101842887655102017L, w = 1;

  /**
   * Constructor called only by localRandom.initialValue.
   */
  public NumericalRecipesNonThreadsafeRandom() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param seed Random generator seed.
   */
  public NumericalRecipesNonThreadsafeRandom(long seed) {
    super(seed);
  }

  @Override
  public void setSeed(long seed) {
    v = 4101842887655102017L;
    w = 1;
    u = (seed != v) ? seed ^ v : v;
    nextLong();
    v = u;
    nextLong();
    w = v;
    nextLong();
  }

  // Constants in this function are from "Numerical Recipes in C"
  @Override
  public long nextLong() {
    // Linear Congruential Generator, as used in regular java.util.Random
    u = u * 2862933555777941757L + 7046029254386353087L;
    // First XOR-Shift generator
    v ^= v >>> 17;
    v ^= v << 31;
    v ^= v >>> 8;
    // Multiply with carry generator:
    w = 4294957665L * (w & 0xffffffff) + (w >>> 32);
    // Second XOR-Shift generator
    long x = u ^ (u << 21);
    x ^= x >>> 35;
    x ^= x << 4;
    return (x + v) ^ w;
  }

  @Override
  protected int next(int bits) {
    return (int) (nextLong() >>> (64 - bits));
  }
}
