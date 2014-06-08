package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.mst.utils.uf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

public class UnionFindWeightedQuickUnionTest {

  @Test
  public void testTree() {
    DBIDRange range = DBIDUtil.generateStaticDBIDRange(8);
    UnionFindWeightedQuickUnion uf = new UnionFindWeightedQuickUnion(range);
    assertFalse(uf.isConnected(range.get(0), range.get(7)));
    uf.union(range.get(0), range.get(1));
    assertTrue(uf.isConnected(range.get(0),range.get( 1)));
    assertEquals(uf.maxTreeHeight(), 1);
    uf.union(range.get(2), range.get(3));
    assertFalse(uf.isConnected(range.get(0), range.get(2)));
    uf.union(range.get(0), range.get(2));
    assertTrue(uf.isConnected(range.get(3), range.get(1)));
    uf.union(range.get(4),range.get( 5));
    uf.union(range.get(6), range.get(7));
    uf.union(range.get(4),range.get( 6));
    uf.union(range.get(0),range.get( 4));
    assertEquals(3, uf.maxTreeHeight());
    for(int i = 0; i < 8; i++) {
      for(int j = 0; j < 8; j++) {
        assertTrue(uf.isConnected(range.get(i), range.get(j)));
      }
    }
  }
  @Test
  public void testRoots() {
    DBIDRange range = DBIDUtil.generateStaticDBIDRange(8);
    UnionFindWeightedQuickUnion uf = new UnionFindWeightedQuickUnion(range);
    uf.union(range.get(0), range.get(1));
    uf.union(range.get(2), range.get(3));
    assertEquals(uf.getRoots().size(),6);
    uf.union(range.get(0), range.get(2));
    assertEquals(uf.getRoots().size(),5);
    uf.union(range.get(4), range.get(5));
    uf.union(range.get(6), range.get(7));
    uf.union(range.get(4), range.get(6));
    uf.union(range.get(0), range.get(4));
    assertEquals(uf.getRoots().size(),1);
   
  }

}
