package de.lmu.ifi.dbs.elki.utilities.datastructures;

import gnu.trove.strategy.HashingStrategy;

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
 * Hashing strategy for GNU Trove using Java interfaces
 * 
 * @author Erich Schubert
 */
public class TroveJavaHashingStrategy implements HashingStrategy<Object> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1197595436947438715L;
  
  /**
   * Static instance
   */
  public static final TroveJavaHashingStrategy STATIC = new TroveJavaHashingStrategy();
  
  /**
   * Constructor. Limited visibility, use the static instance.
   */
  TroveJavaHashingStrategy() {
    // Nothing to do here
  }

  @Override
  public int computeHashCode(Object object) {
    return object.hashCode();
  }

  @Override
  public boolean equals(Object o1, Object o2) {
    return o1.equals(o2);
  }
}