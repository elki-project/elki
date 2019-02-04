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
package de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Evaluate clustering results by building segments for their pairs: shared
 * pairs and differences.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Sascha Goldhofer, Hans-Peter Kriegel, Erich Schubert,
 * Arthur Zimek<br>
 * Evaluation of Clusterings – Metrics and Visual Support<br>
 * Proc. 28th International Conference on Data Engineering (ICDE 2012)
 *
 * @author Sascha Goldhofer
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @assoc - - - Clustering
 * @assoc - - - Segments
 */
@Reference(authors = "Elke Achtert, Sascha Goldhofer, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", //
    title = "Evaluation of Clusterings - Metrics and Visual Support", //
    booktitle = "Proc. 28th International Conference on Data Engineering (ICDE 2012)", //
    url = "https://doi.org/10.1109/ICDE.2012.128", //
    bibkey = "DBLP:conf/icde/AchtertGKSZ12")
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

    List<Clustering<?>> clusterings = Clustering.getClusteringResults(result);
    // Abort if not enough clusterings to compare
    if(clusterings.size() < 2) {
      return;
    }

    // create segments
    Segments segments = new Segments(clusterings);
    hier.add(result, segments);
  }
}
