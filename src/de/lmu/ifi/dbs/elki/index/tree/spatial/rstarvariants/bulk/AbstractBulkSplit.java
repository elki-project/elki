package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk;

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
 * Encapsulates the required parameters for a bulk split of a spatial index.
 * 
 * @author Elke Achtert
 */
public abstract class AbstractBulkSplit implements BulkSplit {
  /**
   * Constructor
   */
  public AbstractBulkSplit() {
    // Nothing to do
  }

  /**
   * Computes and returns the best split point.
   * 
   * @param numEntries the number of entries to be split
   * @param minEntries the number of minimum entries in the node to be split
   * @param maxEntries number of maximum entries in the node to be split
   * @return the best split point
   */
  protected int chooseBulkSplitPoint(int numEntries, int minEntries, int maxEntries) {
    if(numEntries < minEntries) {
      throw new IllegalArgumentException("numEntries < minEntries!");
    }

    if(numEntries <= maxEntries) {
      return numEntries;
    }
    else if(numEntries < maxEntries + minEntries) {
      return (numEntries - minEntries);
    }
    else {
      return maxEntries;
    }
  }
}