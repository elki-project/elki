package de.lmu.ifi.dbs.elki.utilities.pairs;
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

/**
 * Pair interface.
 * 
 * Note: this currently is <em>empty by design</em>. You should always decide
 * explicitly whether to use boxing pairs {@link Pair} or primitive pairs such
 * as {@link IntIntPair}
 * 
 * @author Erich Schubert
 * 
 * @param FIRST first type
 * @param SECOND second type
 */
public interface PairInterface<FIRST, SECOND> {
  /**
   * Get the first object - note: this may cause autoboxing, use pair.first for native pairs!
   * 
   * @return First object
   */
  public FIRST getFirst();
  
  /**
   * Get the second object - note: this may cause autoboxing, use pair.second for native pairs!
   * 
   * @return Second object
   */
  public SECOND getSecond();
}