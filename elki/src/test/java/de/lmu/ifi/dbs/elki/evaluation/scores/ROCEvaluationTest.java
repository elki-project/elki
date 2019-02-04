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
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DBIDsTest;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DistanceResultAdapter;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;

/**
 * Test to validate ROC curve computation.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ROCEvaluationTest {
  /**
   * Test ROC curve generation, including curve simplification
   */
  @Test
  public void testROCCurve() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    // Starting point: ................................ 0.0,0. ++
    distances.add(0.0, DBIDUtil.importInteger(1)); // + 0.0,.2 -- redundant
    distances.add(1.0, DBIDUtil.importInteger(2)); // + 0.0,.4 ++
    distances.add(2.0, DBIDUtil.importInteger(6)); // - .25,.4 ++
    distances.add(3.0, DBIDUtil.importInteger(7)); // -
    distances.add(3.0, DBIDUtil.importInteger(3)); // + .50,.6 -- redundant
    distances.add(4.0, DBIDUtil.importInteger(8)); // -
    distances.add(4.0, DBIDUtil.importInteger(4)); // + .75,.8 ++
    distances.add(5.0, DBIDUtil.importInteger(9)); // - 1.0,.8 ++
    distances.add(6.0, DBIDUtil.importInteger(5)); // + 1.0,1. ++

    XYCurve roccurve = ROCEvaluation.materializeROC(new DBIDsTest(positive), new DistanceResultAdapter(distances.iter()));
    // System.err.println(roccurve);
    assertEquals("ROC curve too complex", 6, roccurve.size());

    double auc = XYCurve.areaUnderCurve(roccurve);
    assertEquals("ROC AUC (curve) not correct.", 0.6, auc, 1e-14);
    double auc2 = new ROCEvaluation().evaluate(positive, distances);
    assertEquals("ROC AUC (direct) not correct.", 0.6, auc2, 1e-14);
  }
}
