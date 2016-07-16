package de.lmu.ifi.dbs.elki.math.statistics.dependence;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

/**
 * Validate jensen shannon dependence.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class AbstractDependenceMeasureTest {
  @Test
  public void testIndexing() {
    double[] data = { 1e-10, 1, 1e-5, 1, 2, 1 };
    int[] indexes = { 0, 2, 1, 3, 5, 4 };
    int[] idx = AbstractDependenceMeasure.sortedIndex(DoubleArrayAdapter.STATIC, data, data.length);
    for(int i = 0; i < indexes.length; i++) {
      assertEquals("Index " + i, indexes[i], idx[i]);
    }
  }

  @Test
  public void testRanks() {
    double[] data = { 1e-10, 1, 1e-5, 1, 2, 1 };
    double[] ranks = { 1., 4, 2., 4, 6, 4 };
    double[] r = AbstractDependenceMeasure.ranks(DoubleArrayAdapter.STATIC, data, data.length);
    for(int i = 0; i < ranks.length; i++) {
      assertEquals("Rank " + i, ranks[i], r[i], 1e-20);
    }
  }
}
