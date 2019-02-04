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
package de.lmu.ifi.dbs.elki.evaluation.scores;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;

/**
 * Validate evaluation based on F1 scores.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class MaximumF1EvaluationTest {
  /**
   * Test maximum F1 score computation
   */
  @Test
  public void testMaximumF1() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    distances.add(0.0, DBIDUtil.importInteger(1)); // P: 1.0 R: 0.2
    distances.add(1.0, DBIDUtil.importInteger(2)); // P: 1.0 R: 0.4
    distances.add(2.0, DBIDUtil.importInteger(6)); // P: 2/3 R: 0.4
    distances.add(3.0, DBIDUtil.importInteger(7)); // P: 0.5 R: 0.4
    distances.add(3.0, DBIDUtil.importInteger(3)); // P: 0.6 R: 0.6
    distances.add(4.0, DBIDUtil.importInteger(8)); // P: 0.5 R: 0.6
    distances.add(4.0, DBIDUtil.importInteger(4)); // P: 4/7 R: 0.8
    distances.add(5.0, DBIDUtil.importInteger(9)); // P: 0.5 R: 0.8
    distances.add(6.0, DBIDUtil.importInteger(5)); // P: 5/9 R: 1.0

    double maxf1 = new MaximumF1Evaluation().evaluate(positive, distances);
    assertEquals("Maximum F1 not correct.", 0.7142857142857143, maxf1, 1e-14);
  }
}
