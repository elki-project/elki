package de.lmu.ifi.dbs.elki.evaluation.paircounting.generator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Merge the output of multiple generators.
 * 
 * @author Erich Schubert
 */
public class PairGeneratorMerge extends PairSortedGenerator {
  /**
   * Generators to merge
   */
  private PairSortedGeneratorInterface[] generators;
  
  /**
   * Set up merging generator.
   * param generators will not be copied!
   * 
   * @param generators array of generators.
   */
  public PairGeneratorMerge(PairSortedGeneratorInterface[] generators) {
    this.generators = generators;
    setCurrent(advance());
  }
  
  /**
   * Advance iterator and return next pair.
   * 
   * This will return the smallest of all the "merged" generator results. 
   */
  @Override
  protected IntIntPair advance() {
    IntIntPair min = null;
    PairSortedGeneratorInterface best = null;
    for (PairSortedGeneratorInterface gen : this.generators) {
      IntIntPair n = gen.current();
      if (n != null && (min == null || n.compareTo(min) < 0)) {
        min = n;
        best = gen;
      }
    }
    // advance best generator
    if (best != null) {
      best.next();
    }
    return min;
  }
}