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
package elki.outlier.meta;

import org.junit.Test;

import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.outlier.distance.KNNOutlier;
import elki.outlier.lof.LOF;
import elki.database.Database;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;
import elki.utilities.ensemble.*;

/**
 * Tests the basic outlier ensemble and regression test different votings.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SimpleEnsembleTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testEnsembleMean() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(SimpleOutlierEnsemble.class) //
        .with(SimpleOutlierEnsemble.ALGORITHM_ID, LOF.class.getName() + "," + KNNOutlier.class.getName()) //
        .with(SimpleOutlierEnsemble.Parameterizer.VOTING_ID, EnsembleVotingMean.class) //
        .with(KNNOutlier.Parameterizer.K_ID, 1).with(LOF.Parameterizer.K_ID, 10).build().run(db);
    testAUC(db, "Noise", result, 0.98631);
  }

  @Test
  public void testEnsembleMin() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(SimpleOutlierEnsemble.class) //
        .with(SimpleOutlierEnsemble.ALGORITHM_ID, LOF.class.getName() + "," + KNNOutlier.class.getName()) //
        .with(SimpleOutlierEnsemble.Parameterizer.VOTING_ID, EnsembleVotingMin.class) //
        .with(KNNOutlier.Parameterizer.K_ID, 1).with(LOF.Parameterizer.K_ID, 10).build().run(db);
    testAUC(db, "Noise", result, 0.99134);
  }

  @Test
  public void testEnsembleMax() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(SimpleOutlierEnsemble.class) //
        .with(SimpleOutlierEnsemble.ALGORITHM_ID, LOF.class.getName() + "," + KNNOutlier.class.getName()) //
        .with(SimpleOutlierEnsemble.Parameterizer.VOTING_ID, EnsembleVotingMax.class) //
        .with(KNNOutlier.Parameterizer.K_ID, 1).with(LOF.Parameterizer.K_ID, 10).build().run(db);
    testAUC(db, "Noise", result, 0.97117);
  }

  @Test
  public void testEnsembleMedian() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(SimpleOutlierEnsemble.class) //
        .with(SimpleOutlierEnsemble.ALGORITHM_ID, LOF.class.getName() + "," + KNNOutlier.class.getName()) //
        .with(SimpleOutlierEnsemble.Parameterizer.VOTING_ID, EnsembleVotingMedian.class) //
        .with(KNNOutlier.Parameterizer.K_ID, 1).with(LOF.Parameterizer.K_ID, 10).build().run(db);
    testAUC(db, "Noise", result, 0.98631);
  }

  @Test
  public void testEnsembleMult() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(SimpleOutlierEnsemble.class) //
        .with(SimpleOutlierEnsemble.ALGORITHM_ID, LOF.class.getName() + "," + KNNOutlier.class.getName()) //
        .with(SimpleOutlierEnsemble.Parameterizer.VOTING_ID, EnsembleVotingMultiplicative.class) //
        .with(KNNOutlier.Parameterizer.K_ID, 1).with(LOF.Parameterizer.K_ID, 10).build().run(db);
    testAUC(db, "Noise", result, 0.98959);
  }

  @Test
  public void testEnsembleVotingInvM() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);
    OutlierResult result = new ELKIBuilder<>(SimpleOutlierEnsemble.class) //
        .with(SimpleOutlierEnsemble.ALGORITHM_ID, LOF.class.getName() + "," + KNNOutlier.class.getName()) //
        .with(SimpleOutlierEnsemble.Parameterizer.VOTING_ID, EnsembleVotingInverseMultiplicative.class) //
        .with(KNNOutlier.Parameterizer.K_ID, 1).with(LOF.Parameterizer.K_ID, 10).build().run(db);
    testAUC(db, "Noise", result, 0.1182);
  }
}
