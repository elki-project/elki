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

import static org.junit.Assert.*;

import org.junit.Test;

import elki.database.ids.DBIDUtil;
import elki.database.ids.HashSetModifiableDBIDs;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.evaluation.scores.PRGCEvaluation.PRGCurve;
import elki.evaluation.scores.adapter.DistanceResultAdapter;
import elki.math.geometry.XYCurve;

/**
 * Unit test for Precision-Recall-Gain-Curves.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class PRGCEvaluationTest {
  @Test
  public void testPRGCurve() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    // negative x values are ignored at calculation, hence 3 is our starting
    // point
    // Starting point: ................................ 0.0,1.0 ++
    distances.add(0.0, DBIDUtil.importInteger(1)); // . -0.4,1.0 --
    distances.add(1.0, DBIDUtil.importInteger(2)); // . -0.875,1.0 ++
    distances.add(2.0, DBIDUtil.importInteger(6)); // . -0.875,.375 ++
    distances.add(3.0, DBIDUtil.importInteger(7)); // . -0.875,-.25 ++ tied...
    distances.add(3.0, DBIDUtil.importInteger(3)); // . 0.1666,.1666 ++ tied...
    distances.add(4.0, DBIDUtil.importInteger(8)); // . 0.1666,.-.25 ++ tied...
    distances.add(4.0, DBIDUtil.importInteger(4)); // . 0.6875,.0625 ++ tied...
    distances.add(5.0, DBIDUtil.importInteger(9)); // . 0.6875,-.25 ++
    distances.add(6.0, DBIDUtil.importInteger(5)); // . 1.0,.0 ++

    PRGCurve curve = PRGCEvaluation.materializePRGC(new DistanceResultAdapter(positive, distances.iter(), distances.size()));
    // As this does not print all points, we check only for
    assertTrue("PRG curve too complex", 9 > curve.size());
    assertEquals("AUPRGC (cached) not correct.", 0.051171875, curve.getAUC(), 1e-10);
    assertEquals("AUPRGC (direct) not correct.", 0.051171875, new PRGCEvaluation().evaluate(positive, distances), 1e-10);
    // This is only an approximation:
    assertEquals("AUPRC (curve) not correct.", 0.051171875, XYCurve.areaUnderCurve(curve), 1e-10);
  }
}
