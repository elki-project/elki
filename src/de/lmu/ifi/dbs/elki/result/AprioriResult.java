package de.lmu.ifi.dbs.elki.result;
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

import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Result class for Apriori Algorithm.
 * 
 * @author Erich Schubert
 *
 */
public class AprioriResult extends BasicResult {
  /**
   * The frequent itemsets.
   */
  private List<BitSet> solution;

  /**
   * The supports of all itemsets.
   */
  private Map<BitSet, Integer> supports;

  /**
   * Constructor.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param solution Frequent itemsets
   * @param supports Supports for the itemsets
   */
  public AprioriResult(String name, String shortname, List<BitSet> solution, Map<BitSet, Integer> supports) {
    super(name, shortname);
    this.solution = solution;
    this.supports = supports;
  }

  /**
   * Returns the frequent item sets.
   *
   * @return the frequent item sets.
   */
  public List<BitSet> getSolution() {
      return solution;
  }

  /**
   * Returns the frequencies of the frequent item sets.
   *
   * @return the frequencies of the frequent item sets
   */
  public Map<BitSet, Integer> getSupports() {
      return supports;
  }
  
  // TODO: text writer for AprioriResult!
}
