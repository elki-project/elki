package de.lmu.ifi.dbs.elki.math.random;

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
 * Replacement for Java's {@link java.util.Random} class, using a different
 * random number generation strategy. Java's random generator is optimized for
 * speed, but may lack the randomness needed for more complex experiments.
 * 
 * This approach is based on the work on XorShift1024* by Sebastiano Vigna, with
 * the original copyright statement:
 * <p>
 * Written in 2014 by Sebastiano Vigna (vigna@acm.org)
 * 
 * To the extent possible under law, the author has dedicated all copyright and
 * related and neighboring rights to this software to the public domain
 * worldwide. This software is distributed without any warranty.
 * 
 * See http://creativecommons.org/publicdomain/zero/1.0/
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "S. Vigna", //
title = "An experimental exploration of Marsaglia's xorshift generators, scrambled", //
booktitle = "", url = "http://vigna.di.unimi.it/ftp/papers/xorshift.pdf")
public class XorShift1024NonThreadsafeRandom extends Random {
  /**
   * Serial version number.
   */
  private static final long serialVersionUID = 1L;

  /**
   * State of random number generator.
   */
  private long[] x;

  /**
   * For rotating amongst states
   */
  int p = 0;

  /**
   * Constructor called only by localRandom.initialValue.
   */
  public XorShift1024NonThreadsafeRandom() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param seed Random generator seed.
   */
  public XorShift1024NonThreadsafeRandom(long seed) {
    super(seed);
  }

  @Override
  public void setSeed(long seed) {
    this.x = new long[16];
    long xor64 = seed != 0 ? seed : 4101842887655102017L;
    // XorShift64* generator to seed:
    for(int i = 0; i < 16; i++) {
      xor64 ^= xor64 >>> 12; // a
      xor64 ^= xor64 << 25; // b
      xor64 ^= xor64 >>> 27; // c
      x[i] = xor64 * 2685821657736338717L;
    }
  }

  @Override
  public long nextLong() {
    long s0 = x[p];
    long s1 = x[p = (p + 1) & 15];
    s1 ^= s1 << 31; // a
    s1 ^= s1 >>> 11; // b
    s0 ^= s0 >>> 30; // c
    return (x[p] = s0 ^ s1) * 1181783497276652981L;
  }

  @Override
  protected int next(int bits) {
    return (int) (nextLong() >>> (64 - bits));
  }
}
