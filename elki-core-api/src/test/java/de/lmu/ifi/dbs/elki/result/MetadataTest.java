/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2018
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
package de.lmu.ifi.dbs.elki.result;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;

/**
 * Basic unit test of metadata functionality.
 *
 * @author Erich Schubert
 */
public class MetadataTest {
  @Test
  public void setLongNameTest() {
    Object a = new byte[] { 0 };
    Metadata.of(a).setLongName("long name");
    assertEquals("Name not stored", "long name", Metadata.of(a).getLongName());
  }

  @Test
  public void childrenTest() {
    Object a = new byte[] { 1 };
    assertEquals("Expected 0 children.", 0, Metadata.hierarchyOf(a).numChildren());
    String c = "child";
    Metadata.hierarchyOf(a).addChild(c);
    assertEquals("Expected 1 children.", 1, Metadata.hierarchyOf(a).numChildren());
    It<Object> it = Metadata.hierarchyOf(a).iterDescendantsSelf();
    assertTrue(it.valid());
    assertEquals(a, it.get());
    assertTrue(it.advance().valid());
    assertEquals(c, it.get());
    assertFalse(it.advance().valid());
    Metadata.hierarchyOf(a).removeChild(c);
    assertEquals("Expected 0 children again.", 0, Metadata.hierarchyOf(a).numChildren());
  }

  @Test
  public void descendantsTest() {
    Object a = new byte[] { 2 }, b = new byte[] { 3 }, c = new byte[] { 4 };
    Metadata.hierarchyOf(b).addChild(c);
    Metadata.hierarchyOf(a).addChild(b);
    It<Object> it = Metadata.hierarchyOf(a).iterDescendantsSelf();
    assertTrue(it.valid());
    assertEquals(a, it.get());
    assertTrue(it.advance().valid());
    assertEquals(b, it.get());
    assertTrue(it.advance().valid());
    assertEquals(c, it.get());
    assertFalse(it.advance().valid());

    it = Metadata.hierarchyOf(c).iterAncestorsSelf();
    assertTrue(it.valid());
    assertEquals(c, it.get());
    assertTrue(it.advance().valid());
    assertEquals(b, it.get());
    assertTrue(it.advance().valid());
    assertEquals(a, it.get());
    assertFalse(it.advance().valid());

    it = Metadata.hierarchyOf(c).iterAncestors();
    assertTrue(it.valid());
    assertEquals(b, it.get());
    assertTrue(it.advance().valid());
    assertEquals(a, it.get());
    assertFalse(it.advance().valid());
  }

  /**
   * O
   * Test soft reference handling. This may be fragile on some JVMs.
   */
  @Test
  public void softReferenceTest() {
    Object a = new byte[] { 42 };
    addWeakChild(a);
    assertTrue(Metadata.hierarchyOf(a).iterDescendants().valid());
    Metadata.hierarchyOf(a).addChild("strong");
    forceGarbageCollection();
    assertEquals(2, Metadata.hierarchyOf(a).numChildren());
    assertTrue(Metadata.hierarchyOf(a).iterDescendants().valid());
    assertEquals("strong", Metadata.hierarchyOf(a).iterDescendants().get());
    assertFalse(Metadata.hierarchyOf(a).iterDescendants().advance().valid());
    assertEquals(1, Metadata.hierarchyOf(a).numChildren());
  }

  private void addWeakChild(Object a) {
    byte[] buf = new byte[1_000_001];
    Arrays.fill(buf, (byte) 42);
    Metadata.hierarchyOf(a).addWeakChild(buf);
    buf = null;
    It<Object> it = Metadata.hierarchyOf(a).iterDescendants();
    assertTrue(it.valid());
    assertNotNull(it.get());
    assertTrue(it.get() instanceof byte[]);
    it.advance();
    assertFalse(it.valid());
    it = null;
  }

  private static void forceGarbageCollection() {
    try {
      ArrayList<byte[]> bufs = new ArrayList<>();
      long size;
      while((size = Runtime.getRuntime().freeMemory()) > 10) {
        final byte[] buf = new byte[(int) Math.min(size, Integer.MAX_VALUE) - 10];
        Arrays.fill(buf, (byte) size);
        bufs.add(buf);
      }
      for(int i = 0; i < 100; i++) {
        bufs.add(new byte[] { (byte) i });
      }
      fail("Did not run out of memory?!?");
    }
    catch(OutOfMemoryError e) {
      System.gc();
    }
  }
}
