package experimentalcode.students.goldhofa;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

/**
 * Evaluate a clustering result by comparing it to an existing cluster label.
 * 
 * @author Sascha Goldhofer
 * 
 * @param <O> Database
 */
public class ClusteringComparison implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging logger = Logging.getLogger(ClusteringComparison.class);

  /**
   * Constructor.
   */
  public ClusteringComparison() {
    super();
  }

  /**
   * Perform clusterings evaluation
   */
  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    // Get all new clusterings
    // TODO: handle clusterings added later, too.
    List<Clustering<?>> clusterings = ResultUtil.getClusteringResults(result);
    // Abort if not enough clusterings to compare
    if(clusterings.size() < 2) {
      return;
    }

    // result to save evaluations
    ClusteringComparisonResult thisResult = new ClusteringComparisonResult("ClusteringComparison", "cc", clusterings.get(0));

    // build clustering sets (for evaluation. obsolete...)
    ArrayList<ClusteringInfo> clrsExt = new ArrayList<ClusteringInfo>();

    int index = 1;
    for(Clustering<?> clustering : clusterings) {
      clrsExt.add(new ClusteringInfo(index, clustering));
      index++;
    }

    // create segments
    Segments segments = new Segments(clusterings, baseResult);

    // log segmentation info
    segments.print(logger);

    // add segmentation to result
    thisResult.add(segments);

    // store measure results (segments) in comparison result for visualization
    thisResult.add(clrsExt);

    // and add comparison result to result tree
    baseResult.getHierarchy().add(result, thisResult);
  }
}