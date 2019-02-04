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
package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;

/**
 * Test the main hierarchy implementation.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class HashMapHierarchyTest {
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
    validate(hier.iterAncestors(a), new String[] {});
    validate(hier.iterAncestorsSelf(a), new String[] { a });
    validate(hier.iterDescendants(a), new String[] { b, c, d });
    validate(hier.iterDescendantsSelf(a), new String[] { a, b, c, d });
    validate(hier.iterParents(d), new String[] { b, c });
    validate(hier.iterAncestors(d), new String[] { a, b, c });
    validate(hier.iterAncestorsSelf(d), new String[] { a, b, c, d });
    validate(hier.iterDescendants(d), new String[] {});
    validate(hier.iterDescendantsSelf(d), new String[] { d });
  }

  private <O> void validate(It<O> iter, O[] all) {
    HashSet<O> seen = new HashSet<>(all.length);
    for(; iter.valid(); iter.advance()) {
      O cur = iter.get();
      boolean found = false;
      for(int i = 0; i < all.length; i++) {
        if(all[i].equals(cur)) {
          found = true;
          seen.add(cur);
          break;
        }
      }
      assertTrue("Object not found in master solution: " + cur, found);
    }
    assertEquals("Not all objects were seen.", all.length, seen.size());
  }

  @Test
  public void testRemovals() {
    final String a = "a", b = "b", c = "c";
    HashMapHierarchy<Object> hier = new HashMapHierarchy<>();
    hier.remove(a, b);
    hier.remove(a);
    hier.remove(b);
    hier.add(a, b);
    hier.remove(a, b);
    hier.remove(a);
    hier.add(a, b);
    hier.remove(b);
    hier.remove(a);
    assertFalse(hier.iterAll().valid());
    // Duplicate additions test:
    hier.add(a, b);
    hier.add(a, b);
    hier.add(a, c);
    assertEquals("Wrong number of children.", 2, hier.numChildren(a));
    assertEquals("Wrong number of parents.", 1, hier.numParents(b));
    hier.remove(a, b);
    hier.remove(a, c);
    // Clear
    hier.remove(a);
    hier.remove(b);
    hier.remove(c);
    assertFalse(hier.iterAll().valid());
    // Try removing 1...n children
    for(int i = 1; i < 10; i++) {
      for(int j = 0; j < i; j++) {
        hier.add(a, Integer.valueOf(j));
      }
      assertEquals("Wrong number of children.", i, hier.numChildren(a));
      for(int j = i - 1; j >= 0; --j) {
        hier.remove(a, Integer.valueOf(j));
        hier.remove(Integer.valueOf(j));
      }
      assertEquals("Wrong number of children.", 0, hier.numChildren(a));
      hier.remove(a);
      assertFalse(hier.iterAll().valid());
    }
  }
}
