/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.database.ids;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import elki.utilities.random.XorShift1024NonThreadsafeRandom;

/**
 * Unit tests for the DBIDUtil utility package.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class DBIDUtilTest {
  @Test
  public void testShuffling() {
    DBIDRange range = DBIDFactory.FACTORY.generateStaticDBIDRange(2);
    int first = 0;
    for(int i = 0; i < 100; i++) {
      ArrayModifiableDBIDs sample = DBIDUtil.newArray(range);
      DBIDUtil.randomShuffle(sample, new XorShift1024NonThreadsafeRandom(i), 1);
      if(DBIDUtil.equal(range.iter(), sample.iter())) {
        first++;
      }
    }
    assertNotEquals("Random broken", 0, first);
    assertNotEquals("Random broken", 100, first);
    assertTrue("Random broken", first < 60);
    assertTrue("Random broken", first > 40);
  }
}
