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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;

/**
 * Generator for noise points.
 * 
 * This generator will generate pairs (a,a) for all elements a in the given
 * list.
 * 
 * @author Erich Schubert
 */
public class PairGeneratorNoise extends PairSortedGenerator {
  /**
   * Ids to use
   */
  private int[] ids;

  /**
   * Current position.
   */
  private int pos;

  /**
   * Crate new generator for a base cluster object.
   * 
   * @param cluster object
   */
  public PairGeneratorNoise(Cluster<?> cluster) {
    // build int array for the cluster
    // TODO: copy less.
    DBIDs dbids = cluster.getIDs();
    ids = new int[dbids.size()];
    int j = 0;
    for (DBID id : dbids) {
      ids[j] = id.getIntegerID();
      j++;
    }
    Arrays.sort(ids);

    pos = 0;
    if(ids.length > 0) {
      setCurrent(new IntIntPair(ids[pos], ids[pos]));
    }
  }

  /**
   * Advance iterator and return new pair.
   */
  @Override
  protected IntIntPair advance() {
    if(current() == null) {
      return null;
    }
    pos++;
    if(pos >= ids.length) {
      return null;
    }
    else {
      return new IntIntPair(ids[pos], ids[pos]);
    }
  }
}