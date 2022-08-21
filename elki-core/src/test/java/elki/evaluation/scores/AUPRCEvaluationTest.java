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
package elki.evaluation.scores;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.database.ids.DBIDUtil;
import elki.database.ids.HashSetModifiableDBIDs;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.evaluation.scores.AUPRCEvaluation.PRCurve;
import elki.evaluation.scores.adapter.DistanceResultAdapter;
import elki.math.geometry.XYCurve;

/**
 * Test to validate PR curve computation.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class AUPRCEvaluationTest {
  @Test
  public void testPRCurve() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    // Starting point: ................................ 0.0,1.0 ++
    distances.add(0.0, DBIDUtil.importInteger(1)); // + 0.2,1.0 -- redundant
    distances.add(1.0, DBIDUtil.importInteger(2)); // + 0.4,1.0 ++
    distances.add(2.0, DBIDUtil.importInteger(6)); // - 0.4,.66 ++
    distances.add(3.0, DBIDUtil.importInteger(7)); // - 0.5,.63 ++ tied...
    distances.add(3.0, DBIDUtil.importInteger(3)); // + 0.6,.60 ++ tied...
    distances.add(4.0, DBIDUtil.importInteger(8)); // - 0.7,.58 ++ tied...
    distances.add(4.0, DBIDUtil.importInteger(4)); // + 0.8,.57 ++ tied...
    distances.add(5.0, DBIDUtil.importInteger(9)); // - 0.8,.50 ++
    distances.add(6.0, DBIDUtil.importInteger(5)); // + 1.0,.56 ++

    PRCurve curve = AUPRCEvaluation.materializePRC(new DistanceResultAdapter(positive, distances.iter(), distances.size()));
    assertEquals("PR curve too complex", 9, curve.size());
    assertEquals("AUPRC (cached) not correct.", 0.7481384, curve.getAUC(), 1e-7);
    assertEquals("AUPRC (direct) not correct.", 0.7481384, new AUPRCEvaluation().evaluate(positive, distances), 1e-7);
    // This is only an approximation:
    assertEquals("AUPRC (curve) not correct.", 0.7482936, XYCurve.areaUnderCurve(curve), 1e-7);
  }
}
