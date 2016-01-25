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
 * This approach is based on the work on XorShift64* by Sebastiano Vigna, with
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
public class XorShift64NonThreadsafeRandom extends Random {
  /**
   * Serial version number.
   */
  private static final long serialVersionUID = 1L;

  /**
   * State of random number generator.
   */
  private long x = 4101842887655102017L;

  /**
   * Constructor called only by localRandom.initialValue.
   */
  public XorShift64NonThreadsafeRandom() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param seed Random generator seed.
   */
  public XorShift64NonThreadsafeRandom(long seed) {
    super(seed);
  }

  @Override
  public void setSeed(long seed) {
    x = seed != 0 ? seed : 4101842887655102017L;
  }

  @Override
  public long nextLong() {
    x ^= x >>> 12; // a
    x ^= x << 25; // b
    x ^= x >>> 27; // c
    return x * 2685821657736338717L;
  }

  @Override
  protected int next(int bits) {
    return (int) (nextLong() >>> (64 - bits));
  }
}
