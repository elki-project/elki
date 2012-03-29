package de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

/**
 * Evaluate clustering results by building segments for their pairs: shared
 * pairs and differences.
 * 
 * @author Sascha Goldhofer
 * @author Erich Schubert
 * 
 * @apiviz.uses Clustering
 * @apiviz.uses Segments
 */
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
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    // Get all new clusterings
    // TODO: handle clusterings added later, too. Can we update the result?
    
    List<Clustering<?>> clusterings = ResultUtil.getClusteringResults(result);
    // Abort if not enough clusterings to compare
    if(clusterings.size() < 2) {
      return;
    }

    // create segments
    Segments segments = new Segments(clusterings, baseResult);
    baseResult.getHierarchy().add(result, segments);
  }
}