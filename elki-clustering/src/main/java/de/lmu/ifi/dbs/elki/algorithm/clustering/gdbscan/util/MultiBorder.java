/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.util;

import java.util.Arrays;

/**
 * Multiple border point assignment.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MultiBorder implements Assignment {
  /**
   * Cluster numbers
   */
  public Border[] cs;

  /**
   * Constructor.
   *
   * @param i First cluster border
   * @param j Second cluster border
   */
  public MultiBorder(Border i, Border j) {
    assert (i.core.num != j.core.num);
    this.cs = new Border[] { i, j };
  }

  /**
   * Add a new border to the existing borders.
   *
   * @param border New border.
   */
  public Assignment update(Border border) {
    Arrays.sort(cs);
    int j = 1;
    boolean found = (cs[0].core == border.core);
    for(int i = 1; i < cs.length; i++) {
      if(cs[i].core != cs[i - 1].core) {
        cs[j++] = cs[i];
      }
      found |= (cs[i].core == border.core);
    }
    if(found) {
      if(j == 1) {
        Border r = cs[0];
        cs = null; // Prevent further use
        return r;
      }
      if(j < cs.length) {
        cs = Arrays.copyOf(cs, j);
      }
      return this;
    }
    if(j + 1 != cs.length) {
      cs = Arrays.copyOf(cs, j + 1);
    }
    cs[j] = border;
    return this;
  }

  /**
   * Get the core this is assigned to.
   *
   * @return Core
   */
  public Core getCore() {
    Core a = cs[0].core;
    for(int i = 1; i < cs.length; i++) {
      Core v = cs[i].core;
      a = a.num > v.num ? a : v; // max, of negative values
    }
    return a;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("MultiBorder[");
    for(Border b : cs) {
      buf.append(b.core.num).append(',');
    }
    buf.append(']');
    return buf.toString();
  }
}
