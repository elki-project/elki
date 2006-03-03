package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.PALMEResult;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.AbstractDistanceFunction;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.RepresentationSelectingDistanceFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * TODO: comment and new name
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PALME<O extends DatabaseObject, D extends Distance<D>, M extends MultiRepresentedObject<O>> extends AbstractAlgorithm<M> {

  /**
   * The distance function for the single representations.
   */
  private RepresentationSelectingDistanceFunction<O, M, D> mr_distanceFunction;

  /**
   * The result of this algorithm.
   */
  private Result<M> result;

  public PALME() {
    super();
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
   *                               setParameters(String[]) method has been failed to be called).
   */
  protected void runInTime(Database<M> database) throws IllegalStateException {
    try {
      System.out.println("database.size " + database.size());
      mr_distanceFunction.setDatabase(database, isVerbose());


      if (database.size() == 0) {
        // todo empty result
        return;
      }

      int numberOfRepresentations = database.get(database.iterator().next()).getNumberOfRepresentations();
      Map<ClassLabel, Set<Integer>> classMap = determineClassPartitions(database);

      Progress progress = new Progress(database.size() * numberOfRepresentations);

      List<D> maxDistances = new ArrayList<D>(numberOfRepresentations);
      List<List<Ranges>> resultList = new ArrayList<List<Ranges>>(numberOfRepresentations);
      for (int r = 0; r < numberOfRepresentations; r++) {
        int processed = 0;
        if (isVerbose()) {
          System.out.println("Representation " + (r+1));
        }
        mr_distanceFunction.setCurrentRepresentationIndex(r);
        List<Ranges> rangesList = new ArrayList<Ranges>();
        D maxDist = mr_distanceFunction.nullDistance();

        Iterator<Integer> it = database.iterator();
        while (it.hasNext()) {
          Integer id = it.next();
//          System.out.println("id " + id);
          ClassLabel classLabel = (ClassLabel) database.getAssociation(AssociationID.CLASS, id);
          Set<Integer> desired = classMap.get(classLabel);

          List<QueryResult<D>> neighbors = database.rangeQuery(id, AbstractDistanceFunction.INFINITY_PATTERN,
                                                               mr_distanceFunction);
          if (neighbors.size() != database.size()) {
            System.out.println("neighbors.size() " + neighbors.size());
            throw new IllegalArgumentException();
          }

          String externalID = (String) database.getAssociation(AssociationID.EXTERNAL_ID, id);
          Ranges ranges = getProbabilityRanges(externalID, classLabel, desired, neighbors);
          rangesList.add(ranges);

          maxDist = Util.max(maxDist, neighbors.get(neighbors.size() - 1).getDistance());

          if (isVerbose()) {
            progress.setProcessed(processed++);
            System.out.print("\r" + progress.toString());
          }
        }
        maxDistances.add(maxDist);
//        outputRanges(r, rangesList);
        resultList.add(rangesList);
      }


      this.result = new PALMEResult<O, D, M>(database, resultList, maxDistances);


    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainungParams = super.setParameters(args);
    mr_distanceFunction = new RepresentationSelectingDistanceFunction<O, M, D>();
    return mr_distanceFunction.setParameters(remainungParams);
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public Result<M> getResult() {
    return result;
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    // todo
    return new Description("PALME",
                           "algorithm without name ;-)",
                           "First it determines the Recall and Precision levels for each representation " +
                           "of a database containing multirepresented_old objects according to a training set. " +
                           "Second a multirepresented_old version of OPTICS will be applied.",
                           "unpublished");
  }

  private Map<ClassLabel, Set<Integer>> determineClassPartitions(Database<M> database) throws IllegalStateException {
    Map<ClassLabel, Set<Integer>> classMap = new HashMap<ClassLabel, Set<Integer>>();

    Iterator<Integer> it = database.iterator();
    while (it.hasNext()) {
      Integer id = it.next();
      ClassLabel classLabel = (ClassLabel) database.getAssociation(AssociationID.CLASS, id);

      Set<Integer> ids = classMap.get(classLabel);
      if (ids == null) {
        ids = new HashSet<Integer>();
        classMap.put(classLabel, ids);
      }
      ids.add(id);

    }

    return classMap;
  }

  private Ranges getProbabilityRanges(String id, ClassLabel classLabel, Set<Integer> desiredSet, List<QueryResult<D>> neighbors) {
    if (neighbors.isEmpty())
      throw new IllegalArgumentException("Empty neighbors!");

    D similarityPrecision = null;
    D dissimilaityPrecision = null;

    double foundAndDesired = 0;

    double inverseFoundAndDesired = 0;
    double found = 0;
    double inverseFound = 0;

    double ws = (1.0 * desiredSet.size()) / (1.0 * neighbors.size());
    double pp = ws + (1.0 - ws) * 0.9;
    double ipp = (1 - ws) + ws * 0.9;
//    System.out.println("ws = " + ws);
//    System.out.println("pp = " + pp);
//    System.out.println("ipp = " + ipp);

    int size = neighbors.size() - 1;
    for (int i = 0; i <= size; i++) {
      found++;
      QueryResult<D> neighbor = neighbors.get(i);
      if (desiredSet.contains(neighbor.getID())) {
        foundAndDesired++;
      }
      // precision
      double p = foundAndDesired / found;
      if (p >= pp) {
        similarityPrecision = neighbor.getDistance();
      }

      inverseFound++;
      QueryResult<D> i_neighbor = neighbors.get(size - i);
      if (! desiredSet.contains(i_neighbor.getID())) {
        inverseFoundAndDesired++;
      }

      // inverse precision
      double ip = inverseFoundAndDesired / inverseFound;

      if (ip >= ipp)
        dissimilaityPrecision = i_neighbor.getDistance();
    }
    if (dissimilaityPrecision == null) {
      dissimilaityPrecision = neighbors.get(neighbors.size() - 1).getDistance();
    }
    if (similarityPrecision == null) {
      similarityPrecision = neighbors.get(0).getDistance();
    }
    return new Ranges(id, classLabel, similarityPrecision, dissimilaityPrecision);
  }

  public class Ranges {
    String id;
    ClassLabel classLabel;
    D similarityPrecision;
    D dissimilarityPrecision;

    public Ranges(String id, ClassLabel classLabel, D similarityPrecision, D dissimilarityPrecision) {
      this.id = id;
      this.classLabel = classLabel;
      this.similarityPrecision = similarityPrecision;
      this.dissimilarityPrecision = dissimilarityPrecision;
    }

    public String toString() {
      return id + " " + classLabel + " " + similarityPrecision + " " + dissimilarityPrecision;
    }

    public String getDescription() {
      return "id class-label similarity-precision dissimilarity-precision";
    }
  }

}
