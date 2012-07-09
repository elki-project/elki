package de.lmu.ifi.dbs.elki.utilities;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 * RandomFactory is responsible for creating {@link Random} generator objects.
 * It does not provide individual random numbers, but will create a random
 * generator; either using a fixed seed or random seeded (default).
 * 
 * TODO: allow global fixing of seed, to make whole experiments reproducible,
 * without having to set every single seed.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Random
 */
public class RandomFactory {
  /**
   * Global default factory
   */
  public static RandomFactory DEFAULT = new RandomFactory(null);

  /**
   * Seed
   */
  private Long seed = null;

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
    else {
      return new RandomFactory(seed);
    }
  }

  /**
   * Constructor.
   * 
   * @param seed Random seed
   */
  protected RandomFactory(Long seed) {
    super();
    this.seed = seed;
  }

  /**
   * Get a random generator.
   * 
   * @return Random generator
   */
  public Random getRandom() {
    if(seed != null) {
      return new Random(seed);
    }
    else {
      return new Random();
    }
  }
}