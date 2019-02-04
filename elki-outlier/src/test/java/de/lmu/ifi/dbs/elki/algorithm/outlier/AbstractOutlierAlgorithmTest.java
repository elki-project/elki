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
package de.lmu.ifi.dbs.elki.algorithm.outlier;

import static org.junit.Assert.*;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.evaluation.outlier.OutlierROCCurve;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Abstract test for outlier algorithms.
 *
 * Includes convenience functions for evaluation.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public abstract class AbstractOutlierAlgorithmTest extends AbstractSimpleAlgorithmTest {
  /**
   * Test the AUC value for an outlier result.
   *
   * @param db Database
   * @param positive Positive class name
   * @param result Outlier result to process
   * @param expected Expected AUC value
   */
  protected void testAUC(Database db, String positive, OutlierResult result, double expected) {
    OutlierROCCurve rocCurve = new ELKIBuilder<>(OutlierROCCurve.class)//
        .with(OutlierROCCurve.Parameterizer.POSITIVE_CLASS_NAME_ID, positive).build();

    // Ensure the result has been added to the hierarchy:
    ResultHierarchy hier = db.getHierarchy();
    if(hier.numParents(result) < 1) {
      hier.add(db, result);
    }

    // Compute ROC and AUC:
    rocCurve.processNewResult(hier, result);
    // Find the ROC results
    Collection<OutlierROCCurve.ROCResult> rocs = ResultUtil.filterResults(hier, result, OutlierROCCurve.ROCResult.class);
    assertTrue("No ROC result found.", !rocs.isEmpty());
    double auc = rocs.iterator().next().getAUC();
    assertFalse("More than one ROC result found.", rocs.size() > 1);
    assertEquals("ROC value does not match.", expected, auc, 0.0001);
  }

  /**
   * Test the outlier score of a single object.
   *
   * @param result Result object to use
   * @param id Object ID
   * @param expected expected value
   */
  protected void testSingleScore(OutlierResult result, int id, double expected) {
    assertNotNull("No outlier result", result);
    assertNotNull("No score result.", result.getScores());
    DBIDs ids = result.getScores().getDBIDs();
    assertTrue("IDs must be array-based", ids instanceof ArrayDBIDs);
    // Translate offset. We used to use 1-indexed
    DBIDRef dbid = ((ArrayDBIDs) ids).iter().seek(id - 1);
    assertNotNull("No result for ID " + id, result.getScores().doubleValue(dbid));
    double actual = result.getScores().doubleValue(dbid);
    assertEquals("Outlier score of object " + id + " doesn't match.", expected, actual, 0.0001);
  }
}
