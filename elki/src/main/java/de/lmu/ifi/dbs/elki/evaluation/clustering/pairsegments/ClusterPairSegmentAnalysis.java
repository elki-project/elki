package de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Evaluate clustering results by building segments for their pairs: shared
 * pairs and differences.
 * 
 * Reference:
 * <p>
 * Evaluation of Clusterings – Metrics and Visual Support<br />
 * Elke Achtert, Sascha Goldhofer, Hans-Peter Kriegel, Erich Schubert, Arthur
 * Zimek<br />
 * In: Proc. 28th International Conference on Data Engineering (ICDE) 2012
 * </p>
 *
 * @author Sascha Goldhofer
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @apiviz.uses Clustering
 * @apiviz.uses Segments
 */
@Reference(title = "Evaluation of Clusterings – Metrics and Visual Support", //
authors = "Elke Achtert, Sascha Goldhofer, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
booktitle = "Proc. 28th International Conference on Data Engineering (ICDE) 2012", //
url = "http://dx.doi.org/10.1109/ICDE.2012.128")
public class ClusterPairSegmentAnalysis implements Evaluator {
  /**
   * Constructor.
   */
  public ClusterPairSegmentAnalysis() {
    super();
  }

  /**
   * Perform clusterings evaluation
   */
  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    // Get all new clusterings
    // TODO: handle clusterings added later, too. Can we update the result?

    List<Clustering<?>> clusterings = ResultUtil.getClusteringResults(result);
    // Abort if not enough clusterings to compare
    if(clusterings.size() < 2) {
      return;
    }

    // create segments
    Segments segments = new Segments(clusterings);
    hier.add(result, segments);
  }
}