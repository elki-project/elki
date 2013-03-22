package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;

/**
 * Test the main hierarchy implementation.
 * 
 * @author Erich Schubert
 */
public class TestHashMapHierarchy implements JUnit4Test {
  @Test
  public void testEmpty() {
    HashMapHierarchy<Object> hier = new HashMapHierarchy<>();
    assertFalse("Iterator valid?", hier.iterAll().valid());
    assertFalse("Iterator valid?", hier.iterParents(hier).valid());
    assertFalse("Iterator valid?", hier.iterAncestors(hier).valid());
    assertFalse("Iterator valid?", hier.iterChildren(hier).valid());
    assertFalse("Iterator valid?", hier.iterDescendants(hier).valid());
  }

  @Test
  public void testSimple() {
    final String a = "a", b = "b", c = "c", d = "d";
    HashMapHierarchy<Object> hier = new HashMapHierarchy<>();
    // A diamond hierarchy.
    hier.add(a, b);
    hier.add(a, c);
    hier.add(b, d);
    hier.add(c, d);
    validate(hier.iterAll(), new String[] { a, b, c, d });
    validate(hier.iterChildren(a), new String[] { b, c });
    validate(hier.iterDescendants(a), new String[] { b, c, d });
    validate(hier.iterParents(d), new String[] { b, c });
    validate(hier.iterAncestors(d), new String[] { a, b, c });
  }

  private <O> void validate(Iter<O> iter, O[] all) {
    HashSet<O> seen = new HashSet<>(all.length);
    for (; iter.valid(); iter.advance()) {
      O cur = iter.get();
      boolean found = false;
      for (int i = 0; i < all.length; i++) {
        if (all[i].equals(cur)) {
          found = true;
          seen.add(cur);
          break;
        }
      }
      assertTrue("Object not found in master solution: " + cur, found);
    }
    assertEquals("Not all objects were seen.", all.length, seen.size());
  }
}
