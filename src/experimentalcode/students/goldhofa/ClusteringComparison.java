package experimentalcode.students.goldhofa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.evaluation.outlier.JudgeOutlierScores;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;
import experimentalcode.students.goldhofa.evaluation.paircounting.MeasureIndex;
import experimentalcode.students.goldhofa.evaluation.paircounting.MeasureResult;
import experimentalcode.students.goldhofa.evaluation.paircounting.PairCounting;

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

    for(Class<?> c : InspectionUtil.cachedFindAllImplementations(MeasureIndex.class)) {
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
  public void processResult(Database db, Result result) {

    // get all clustering
    List<Clustering<?>> clusterings = ResultUtil.getClusteringResults(result);

    // Abort if no clusterings to compare
    if (clusterings.size() < 2)
      return;

    // result to save evaluations
    ClusteringComparisonResult thisResult = new ClusteringComparisonResult("ClusteringComparison", "cc");

    //
    // calculate all measure values
    //

    SortedSet<MeasureResult> measureResults;
    if(measures.size() >= 1) {

      measureResults = new TreeSet<MeasureResult>();

      int totalObjects = db.getDBIDs().size();
      int allPairs = (totalObjects * (totalObjects - 1)) / 2;
      int allPairsSelfPaired = (totalObjects + 1) * totalObjects / 2;

      // map hashcode to clustering name
      Map<Integer, String> hashToClustering = new HashMap<Integer, String>(clusterings.size());
      for(Clustering<?> c : clusterings) {
        hashToClustering.put(c.hashCode(), c.getLongName());
      }

      // measure calculations for all clusterings and every direction
      for(int first = 0; first < clusterings.size() - 1; first++) {

        Clustering<?> firstClustering = clusterings.get(first);

        for(int second = first + 1; second < clusterings.size(); second++) {

          Clustering<?> secondClustering = clusterings.get(second);

          Triple<Integer, Integer, Integer> countedPairs = PairCounting.countPairs(PairCounting.getPairGenerator(firstClustering), PairCounting.getPairGenerator(secondClustering));
          Triple<Integer, Integer, Integer> countedPairsSelfPaired = PairCounting.countPairs(PairCounting.getPairGenerator(firstClustering, true), PairCounting.getPairGenerator(secondClustering, true));

          // for all measures calculate index and average
          for(MeasureIndex measureIndex : measures) {

            double first2Second;
            double second2First;

            if(measureIndex.withSelfPairing()) {

              first2Second = measureIndex.measure(countedPairsSelfPaired.first, countedPairsSelfPaired.second, countedPairsSelfPaired.third, allPairsSelfPaired);
              second2First = measureIndex.measure(countedPairsSelfPaired.first, countedPairsSelfPaired.third, countedPairsSelfPaired.second, allPairsSelfPaired);

            }
            else {

              first2Second = measureIndex.measure(countedPairs.first, countedPairs.second, countedPairs.third, allPairs);
              second2First = measureIndex.measure(countedPairs.first, countedPairs.third, countedPairs.second, allPairs);
            }

            // Add results to list
            measureResults.add(new MeasureResult(firstClustering.hashCode(), secondClustering.hashCode(), measureIndex.getName(), first2Second, measureIndex.inAverage()));
            measureResults.add(new MeasureResult(secondClustering.hashCode(), firstClustering.hashCode(), measureIndex.getName(), second2First, measureIndex.inAverage()));
          }
        }
      }

      // calculate average values
      Map<Integer, Map<Integer, Double>> average = new HashMap<Integer, Map<Integer, Double>>(clusterings.size());
      for(MeasureResult measure : measureResults) {

        if(measure.inAverage) {

          double valueToAdd = measure.measureValue / measures.size();

          Map<Integer, Double> firstClustering;

          if(!average.containsKey(measure.firstClustering)) {

            firstClustering = new HashMap<Integer, Double>(clusterings.size());
            average.put(measure.firstClustering, firstClustering);

          }
          else {

            firstClustering = average.get(measure.firstClustering);
          }

          if(!firstClustering.containsKey(measure.secondClustering)) {

            firstClustering.put(measure.secondClustering, valueToAdd);

          }
          else {

            double value = firstClustering.get(measure.secondClustering);
            firstClustering.put(measure.secondClustering, value + valueToAdd);
          }
        }
      }

      // add averages to measure results
      for(Integer first : average.keySet()) {

        for(Integer second : average.get(first).keySet()) {

          measureResults.add(new MeasureResult(first, second, "average", average.get(first).get(second), false));
        }
      }

      // verbose
      if (logger.isVerbose()) {

        for (MeasureResult currentResult : measureResults) {

          currentResult.print(logger, hashToClustering);
        }
      }
    }

    //
    // Compare Clusterings
    //
    // TODO cluster nach größe sortieren? homogene Ausgabe...

    // List of clusterings to compare
    // TODO sortby measure index
    // List<Clustering<?>> clusterings =
    // ResultUtil.getClusteringResults(result);
    // clusterings.add(refcrs.get(0));

    //
    // collects all Objects into Segments and
    // converts ObjectSegments into PairSegments
    //
    
    Segments segments = new Segments(clusterings);
    
    for (DBID id : db.getDBIDs()) {

      // tag Object over SegmentID
      segments.addObject(id);
    }

    segments.convertToPairSegments();

    segments.print(logger);

    thisResult.add(segments);

    db.getHierarchy().add(result, thisResult);
  }
}