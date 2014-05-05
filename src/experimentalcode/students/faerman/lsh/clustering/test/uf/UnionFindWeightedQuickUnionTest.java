package experimentalcode.students.faerman.lsh.clustering.test.uf;

import static org.junit.Assert.*;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

import experimentalcode.students.faerman.lsh.clustering.uf.UnionFindWeightedQuickUnion;

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
    UnionFindWeightedQuickUnion<Integer> uf = new UnionFindWeightedQuickUnion<>();
    uf.init(8);
    assertFalse(uf.isConnected(0, 7));
    uf.union(0, 1);
    assertTrue(uf.isConnected(0, 1));
    assertEquals(uf.maxTreeHeight(), 1);
    uf.union(2, 3);
    assertFalse(uf.isConnected(0, 2));
    uf.union(0, 2);
    assertTrue(uf.isConnected(3, 1));
    uf.union(4, 5);
    uf.union(6, 7);
    uf.union(4, 6);
    uf.union(0, 4);
    assertEquals(3, uf.maxTreeHeight());
    for(int i = 0; i < 8; i++) {
      for(int j = 0; j < 8; j++) {
        assertTrue(uf.isConnected(i, j));
      }
    }
  }
  @Test
  public void testRoots() {
    UnionFindWeightedQuickUnion<Integer> uf = new UnionFindWeightedQuickUnion<>();
    uf.init(8);
    uf.union(0, 1);
    uf.union(2, 3);
    assertEquals(uf.getRoots().size(),2);
    uf.union(0, 2);
    assertEquals(uf.getRoots().size(),1);
    uf.union(4, 5);
    uf.union(6, 7);
    uf.union(4, 6);
    uf.union(0, 4);
    assertEquals(uf.getRoots().size(),1);
   
  }

}
