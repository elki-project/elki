package de.lmu.ifi.dbs.elki.database.ids;

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

import de.lmu.ifi.dbs.elki.utilities.pairs.PairInterface;

/**
 * Pair of a double value and a DBID
 * 
 * @author Erich Schubert
 */
public interface DoubleDBIDPair extends PairInterface<Double, DBID>, DBIDRef, Comparable<DoubleDBIDPair> {
  /**
   * Get the double value of the pair.
   * 
   * @return Double
   */
  public double doubleValue();

  /**
   * Get the first object - note: this may cause autoboxing, use pair.first for
   * native pairs!
   * 
   * @deprecated Avoid autoboxing. Use {@link #doubleValue}!
   * 
   * @return First object
   */
  @Override
  @Deprecated
  public Double getFirst();

  /**
   * Get the second object - note: this may cause autoboxing, use pair.second
   * for native pairs!
   * 
   * @deprecated Avoid autoboxing! Use {@link DBIDRef} interface!
   * 
   * @return Second object
   */
  @Override
  @Deprecated
  public DBID getSecond();
}
