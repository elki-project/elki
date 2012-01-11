package experimentalcode.students.goldhofa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.evaluation.outlier.JudgeOutlierScores;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;
import experimentalcode.students.goldhofa.evaluation.paircounting.MeasureIndex;

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
  protected static final Logging logger = Logging.getLogger(JudgeOutlierScores.class);

  /*
   * OptionID for {@link #REFERENCE_PARAM}
   */
  // public static final OptionID REFERENCE_ID =
  // OptionID.getOrCreateOptionID("similaritymeasures.reference",
  // "Reference clustering to compare with. Defaults to a by-label clustering.");

  /*
   * Parameter to obtain the reference clustering. Defaults to a flat label
   * clustering.
   */
  // private final ObjectParameter<Algorithm<O, ?>> REFERENCE_PARAM = new
  // ObjectParameter<Algorithm<O, ?>>(REFERENCE_ID, Algorithm.class,
  // ByLabelClustering.class);

  private ArrayList<MeasureIndex> measures;

  /**
   * Constructor.
   * 
   * @param config Parameters
   */
  public ClusteringComparison(Parameterization config) {
    super();

    config = config.descend(this);
    // if (config.grab(REFERENCE_PARAM)) {
    // referencealg = REFERENCE_PARAM.instantiateClass(config);
    // }

    getMeasures(config);
  }

  /**
   * Collect all measures
   * 
   * @param config Parameterization
   */
  private void getMeasures(Parameterization config) {

    measures = new ArrayList<MeasureIndex>();

    for (Class<?> c : InspectionUtil.cachedFindAllImplementations(MeasureIndex.class)) {
      try {
        MeasureIndex mi = ClassGenericsUtil.tryInstantiate(MeasureIndex.class, c, config);
        // if (mi.withSelfPairing()) withSelfPairing = true;
        measures.add(mi);
      }
      catch(Throwable e) {
        logger.exception("Error instantiating MeasureIndex " + c.getName(), e);
      }
    }
  }

  /**
   * Perform clusterings evaluation
   */
  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    final Database db = ResultUtil.findDatabase(baseResult);

    // get all clustering
    List<Clustering<?>> clusterings = ResultUtil.getClusteringResults(result);

    // Abort if no clusterings to compare
    if (clusterings.size() < 2)
      return;

    // result to save evaluations
    ClusteringComparisonResult thisResult = new ClusteringComparisonResult("ClusteringComparison", "cc", clusterings.get(0));
    
    
    //
    // building clustering sets
    //    
    ArrayList<ClusteringInfo> clrsExt = new ArrayList<ClusteringInfo>();
    
    int index = 1;
    for (Clustering<?> clustering : clusterings) {
      
      clrsExt.add(new ClusteringInfo(index, clustering));
      index++;
    }

    
    // 
    // Build Segments
    //
    // collects all Objects into Segments and
    // converts ObjectSegments into PairSegments
    //
    
    Segments segments = new Segments(clusterings);
    
    for (DBID id : db.getRelation(TypeUtil.DBID).iterDBIDs()) {

      // tag Object over SegmentID
      segments.addObject(id);
    }

    segments.convertToPairSegments();
    
    
    //
    // calculate measure values through cluster segmentation
    //
    
    int allPairs = segments.getPairCount(true);
    
    for (int first = 0; first < clrsExt.size() - 1; first++) {
      
      // get basic clustering for measure index
      ClusteringInfo firstClustering = clrsExt.get(first);
      
      // iterate over remaining clusterings for comparison clustering
      for (int second = first + 1; second < clusterings.size(); second++) {
        
        ClusteringInfo secondClustering = clrsExt.get(second);
        
        // get paircount
        Triple<Integer, Integer, Integer> pairs = segments.getPaircount(first, true, second, true);
        /*
        Triple<Integer, Integer, Integer> pairsFirstPaired = segments.getPaircount(first, false, second, true);
        Triple<Integer, Integer, Integer> pairsSecondPaired = segments.getPaircount(first, true, second, false);
        Triple<Integer, Integer, Integer> pairsAllPaired = segments.getPaircount(first, false, second, false);
        */
        
        for (MeasureIndex measureIndex : measures) {
          
          double first2Second;
          double second2First;
          
          // 
          // First Clustering (Noise as cluster) <=> Second Clustering (Noise as cluster)
          //
          first2Second = measureIndex.measure(pairs.first, pairs.second, pairs.third, allPairs);
          firstClustering.addMeasureResult(secondClustering, measureIndex, first2Second, true, true);
          // vice versa
          second2First = measureIndex.measure(pairs.first, pairs.third, pairs.second, allPairs);
          secondClustering.addMeasureResult(firstClustering, measureIndex, second2First, true, true);

          // 
          // First Clustering (Noise self paired) <=> Second Clustering (Noise as cluster)
          //
          
          /*
          
          if (firstClustering.hasNoise()) {

            first2Second = measureIndex.measure(pairsFirstPaired.first, pairsFirstPaired.second, pairsFirstPaired.third, allPairs);
            firstClustering.addMeasureResult(secondClustering, measureIndex, first2Second, false, true);
            // vice versa
            second2First = measureIndex.measure(pairsFirstPaired.first, pairsFirstPaired.third, pairsFirstPaired.second, allPairs);
            secondClustering.addMeasureResult(firstClustering, measureIndex, second2First, true, false);
          }
          
          // 
          // First Clustering (Noise as cluster) <=> Second Clustering (Noise self paired)
          //
          if (secondClustering.hasNoise()) {
            
            first2Second = measureIndex.measure(pairsSecondPaired.first, pairsSecondPaired.second, pairsSecondPaired.third, allPairs);
            firstClustering.addMeasureResult(secondClustering, measureIndex, first2Second, true, false);
            // vice versa
            second2First = measureIndex.measure(pairsSecondPaired.first, pairsSecondPaired.third, pairsSecondPaired.second, allPairs);
            secondClustering.addMeasureResult(firstClustering, measureIndex, second2First, false, true);
            
            // 
            // First Clustering (Noise self paired) <=> Second Clustering (Noise self paired)
            //
            if (firstClustering.hasNoise()) {
              
              first2Second = measureIndex.measure(pairsAllPaired.first, pairsAllPaired.second, pairsAllPaired.third, allPairs);
              firstClustering.addMeasureResult(secondClustering, measureIndex, first2Second, false, false);
              // vice versa
              second2First = measureIndex.measure(pairsAllPaired.first, pairsAllPaired.third, pairsAllPaired.second, allPairs);
              secondClustering.addMeasureResult(firstClustering, measureIndex, second2First, false, false);
            }
          }
          */
        }
      }
    }
    
    
    //
    // store measure results as table rows ordered by clusterings
    //
    
    ArrayList<Tablerow> resultTable = new ArrayList<Tablerow>();
    ArrayList<Tablerow> segRes = new ArrayList<Tablerow>();
    
    // store measure results as table rows ordered by clusterings
    for (ClusteringInfo clr : clrsExt) {
      
      clr.printMeasures(segRes);
      resultTable.add(new Tablerow("#"));
      resultTable.add(new Tablerow("#"));
    }
    
    db.getHierarchy().add(result, new ComparisonMeasureResult(segRes));
    
    // ---

    
    // log segmentation info
    segments.print(logger);
    
    // add segmentation to result
    thisResult.add(segments);
    
    // store measure results (segments) in comparison result for visualization
    thisResult.add(clrsExt);
    
    // and add comparison result to result tree
    db.getHierarchy().add(result, thisResult);
  }
  
  /**
   * Result object for outlier score judgements.
   * 
   * @author Erich Schubert
   */
  public static class ComparisonMeasureResult extends CollectionResult<Tablerow> {
    /**
     * Constructor.
     * 
     * @param col score result
     */
    public ComparisonMeasureResult(Collection<Tablerow> col) {
      super("Clustering Comparison: measure indices", "comparison_measures", col);
    }
    /**
     * Constructor.
     * 
     * @param col score result
     */
    public ComparisonMeasureResult(Collection<Tablerow> col, String filename) {
      super("Clustering Comparison: measure indices", filename, col);
    }
  }
}