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
 * Implement the common functionality of caching the current result in a base class.
 * 
 * @author Erich Schubert
 *
 */
public abstract class PairSortedGenerator implements PairSortedGeneratorInterface {
  /**
   * Current pair
   */
  private IntIntPair cur = null;

  /**
   * Set the current pair.
   * 
   * @param cur new current pair.
   */
  protected final void setCurrent(IntIntPair cur) {
    this.cur = cur;
  }
  
  /**
   * Return current pair.
   * 
   * Marked as final to avoid bad implementations.
   * If you intend to override this, just implement the interface!
   */
  @Override
  public final IntIntPair current() {
    return cur;
  }

  /**
   * Return next pair.
   * 
   * Marked as final to avoid bad implementations.
   * If you intend to override this, just implement the interface!
   */
  @Override
  public final IntIntPair next() {
    setCurrent(advance());
    return current();
  }
  
  /**
   * Main advance method.
   */
  protected abstract IntIntPair advance();
}