package de.lmu.ifi.dbs.elki.utilities;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
 * Drop-in replacement for {@link java.util.Random}, but not using atomic long
 * seeds. This implementation is <em>no longer thread-safe</em> (but faster)!
 * 
 * @author Erich Schubert
 */
public class UnsafeRandom extends Random {
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
  public UnsafeRandom() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param seed Random generator seed.
   */
  public UnsafeRandom(long seed) {
    this.seed = (seed ^ multiplier) & mask;
  }

  /**
   * Throws {@code UnsupportedOperationException}. Setting seeds in this
   * generator is not supported.
   * 
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setSeed(long seed) {
    this.seed = (seed ^ multiplier) & mask;
  }

  @Override
  protected int next(int bits) {
    seed = (seed * multiplier + addend) & mask;
    return (int) (seed >>> (48 - bits));
  }
}
