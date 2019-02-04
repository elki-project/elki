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
 * Test to validate ROC curve computation.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class AveragePrecisionEvaluationTest {
  /**
   * Test Average Precision score computation.
   */
  @Test
  public void testAveragePrecision() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    distances.add(0.0, DBIDUtil.importInteger(1)); // Precision: 1.0
    distances.add(1.0, DBIDUtil.importInteger(2)); // Precision: 1.0
    distances.add(2.0, DBIDUtil.importInteger(6)); //
    distances.add(3.0, DBIDUtil.importInteger(7)); //
    distances.add(3.0, DBIDUtil.importInteger(3)); // Precision: 0.6
    distances.add(4.0, DBIDUtil.importInteger(8)); //
    distances.add(4.0, DBIDUtil.importInteger(4)); // Precision: 4/7.
    distances.add(5.0, DBIDUtil.importInteger(9)); //
    distances.add(6.0, DBIDUtil.importInteger(5)); // Precision: 5/9.
    // (1+1+.6+4/7.+5/9.)/5 = 0.7453968253968254

    double ap = new AveragePrecisionEvaluation().evaluate(positive, distances);
    assertEquals("Average precision not correct.", 0.7453968253968254, ap, 1e-14);
  }
}
