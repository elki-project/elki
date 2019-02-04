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
package de.lmu.ifi.dbs.elki.evaluation;

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelOrAllInOneClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.clustering.EvaluateClustering;
import de.lmu.ifi.dbs.elki.evaluation.outlier.*;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.scaling.LinearScaling;

/**
 * Evaluator that tries to auto-run a number of evaluation methods.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @assoc - - - OutlierResult
 * @assoc - - - Clustering
 * @composed - - - OutlierROCCurve
 * @composed - - - OutlierPrecisionAtKCurve
 * @composed - - - OutlierPrecisionRecallCurve
 * @composed - - - ComputeOutlierHistogram
 * @composed - - - EvaluateClustering
 */
public class AutomaticEvaluation implements Evaluator {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(AutomaticEvaluation.class);

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    autoEvaluateClusterings(hier, newResult);
    autoEvaluateOutliers(hier, newResult);
  }

  protected void autoEvaluateOutliers(ResultHierarchy hier, Result newResult) {
    Collection<OutlierResult> outliers = ResultUtil.filterResults(hier, newResult, OutlierResult.class);
    if(LOG.isDebugging()) {
      LOG.debug("Number of new outlier results: " + outliers.size());
    }
    if(!outliers.isEmpty()) {
      Database db = ResultUtil.findDatabase(hier);
      ensureClusteringResult(db, db);
      Collection<Clustering<?>> clusterings = ResultUtil.filterResults(hier, db, Clustering.class);
      if(clusterings.isEmpty()) {
        LOG.warning("Could not find a clustering result, even after running 'ensureClusteringResult'?!?");
        return;
      }
      Clustering<?> basec = clusterings.iterator().next();
      // Find minority class label
      int min = Integer.MAX_VALUE;
      int total = 0;
      String label = null;
      if(basec.getAllClusters().size() > 1) {
        for(Cluster<?> c : basec.getAllClusters()) {
          final int csize = c.getIDs().size();
          total += csize;
          if(csize < min) {
            min = csize;
            label = c.getName();
          }
        }
      }
      if(label == null) {
        LOG.warning("Could not evaluate outlier results, as I could not find a minority label.");
        return;
      }
      if(min == 1) {
        LOG.warning("The minority class label had a single object. Try using 'ClassLabelFilter' to identify the class label column.");
      }
      if(min > 0.05 * total) {
        LOG.warning("The minority class I discovered (labeled '" + label + "') has " + (min * 100. / total) + "% of objects. Outlier classes should be more rare!");
      }
      LOG.verbose("Evaluating using minority class: " + label);
      Pattern pat = Pattern.compile("^" + Pattern.quote(label) + "$");
      // Evaluate rankings.
      new OutlierRankingEvaluation(pat).processNewResult(hier, newResult);
      // Compute ROC curve
      new OutlierROCCurve(pat).processNewResult(hier, newResult);
      // Compute Precision at k
      new OutlierPrecisionAtKCurve(pat, min << 1).processNewResult(hier, newResult);
      // Compute ROC curve
      new OutlierPrecisionRecallCurve(pat).processNewResult(hier, newResult);
      // Compute outlier histogram
      new ComputeOutlierHistogram(pat, 50, new LinearScaling(), false).processNewResult(hier, newResult);
    }
  }

  protected void autoEvaluateClusterings(ResultHierarchy hier, Result newResult) {
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(hier, newResult, Clustering.class);
    if(LOG.isDebugging()) {
      LOG.warning("Number of new clustering results: " + clusterings.size());
    }
    for(Iterator<Clustering<?>> c = clusterings.iterator(); c.hasNext();) {
      Clustering<?> test = c.next();
      if("allinone-clustering".equals(test.getShortName())) {
        c.remove();
      }
      else if("allinnoise-clustering".equals(test.getShortName())) {
        c.remove();
      }
      else if("bylabel-clustering".equals(test.getShortName())) {
        c.remove();
      }
      else if("bymodel-clustering".equals(test.getShortName())) {
        c.remove();
      }
    }
    if(!clusterings.isEmpty()) {
      try {
        new EvaluateClustering(new ByLabelClustering(), false, true).processNewResult(hier, newResult);
      }
      catch(NoSupportedDataTypeException e) {
        // Pass - the data probably did not have labels.
      }
    }
  }

  /**
   * Ensure that the result contains at least one Clustering.
   *
   * @param db Database to process
   * @param result result
   */
  public static void ensureClusteringResult(final Database db, final Result result) {
    Collection<Clustering<?>> clusterings = ResultUtil.filterResults(db.getHierarchy(), result, Clustering.class);
    if(clusterings.isEmpty()) {
      ResultUtil.addChildResult(db, new ByLabelOrAllInOneClustering().run(db));
    }
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected AutomaticEvaluation makeInstance() {
      return new AutomaticEvaluation();
    }
  }
}
