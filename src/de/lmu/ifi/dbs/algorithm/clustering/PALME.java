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

      Progress progress = new Progress(database.size());

      if (database.size() == 0) {
        // todo empty result
        return;
      }

      int numberOfRepresentations = database.get(database.iterator().next()).getNumberOfRepresentations();

      System.out.println("distances");
      for (int r = 0; r < numberOfRepresentations; r++) {
        System.out.println("r " + r);
        mr_distanceFunction.setCurrentRepresentationIndex(r);
        List<DistanceObject> distanceObjects = new ArrayList<DistanceObject>((database.size() * (database.size() - 1)) / 2);

        Iterator<Integer> it1 = database.iterator();
        while (it1.hasNext()) {
          Integer id1 = it1.next();
          ClassLabel class1 = (ClassLabel) database.getAssociation(AssociationID.CLASS, id1);
          String extId1 = (String) database.getAssociation(AssociationID.EXTERNAL_ID, id1);
          Iterator<Integer> it2 = database.iterator();
          while (it2.hasNext()) {
            Integer id2 = it2.next();
            if (id1 >= id2) continue;

            D distance = mr_distanceFunction.distance(id1, id2);
            ClassLabel class2 = (ClassLabel) database.getAssociation(AssociationID.CLASS, id2);
            String extId2 = (String) database.getAssociation(AssociationID.EXTERNAL_ID, id2);

            distanceObjects.add(new DistanceObject(distance, extId1, extId2, class1, class2));
          }
        }

        Collections.sort(distanceObjects);

        double found = 0;
        double foundAndDesired = 0;
        for (DistanceObject distanceObject : distanceObjects) {
          found++;
          if (distanceObject.clazz) foundAndDesired++;
          distanceObject.precision = foundAndDesired / found;
        }
        output(r, distanceObjects);
      }

      System.out.println("precision and ip");
      Map<ClassLabel, Set<Integer>> classMap = determineClassPartitions(database);
//      System.out.println(classMap);

      for (int r = 0; r < numberOfRepresentations; r++) {
        System.out.println("r " + r);
        mr_distanceFunction.setCurrentRepresentationIndex(r);
        List<Ranges> rangesList = new ArrayList<Ranges>();

        Iterator<Integer> it = database.iterator();
        while (it.hasNext()) {
          Integer id = it.next();
          ClassLabel classLabel = (ClassLabel) database.getAssociation(AssociationID.CLASS, id);
          Set<Integer> desired = classMap.get(classLabel);

          List<QueryResult<D>> neighbors = database.rangeQuery(id, AbstractDistanceFunction.INFINITY_PATTERN, mr_distanceFunction);

          String externalID = (String) database.getAssociation(AssociationID.EXTERNAL_ID, id);
          Ranges ranges = getProbabilityRanges(externalID, desired, neighbors);
          rangesList.add(ranges);
        }
        outputRanges(r, rangesList);
      }


      this.result = new PALMEResult<O, D, M>(database);


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

  private List<D> getPrecisionAndRecallRange(Set<Integer> desiredSet, List<QueryResult<D>> neighbors, Database<M> db) {
    System.out.println("neighbors " + neighbors);

    if (neighbors.isEmpty())
      throw new IllegalArgumentException("Empty neighbors!");

    D precisionRange = mr_distanceFunction.nullDistance();
    D recallRange = mr_distanceFunction.infiniteDistance();
    D currentRange = mr_distanceFunction.nullDistance();

    double found = 0;
    double desired = desiredSet.size();
    double foundAndDesired = 0;

    System.out.println("desiredSet " + desiredSet);

    for (QueryResult<D> neighbor : neighbors) {
      found ++;
      if (desiredSet.contains(neighbor.getID())) {
        foundAndDesired++;
      }

      double p = foundAndDesired / found;
      double r = foundAndDesired / desired;

      System.out.println("");

      M object = db.get(neighbor.getID());
      System.out.println("  r_" + 0 + ": " + neighbor.getID() + " " + object.getRepresentation(0));


      System.out.println(" " + neighbor);

      System.out.println(" " + neighbor.getID() + " " + db.getAssociation(AssociationID.CLASS, neighbor.getID()));
      System.out.println(" foundAndDesired " + foundAndDesired);
      System.out.println(" found " + found);
      System.out.println(" desired " + desired);
      System.out.println(" p = " + p);
      System.out.println(" r = " + r);

      if (p > 0.90) {
//        System.out.println("");
//        System.out.println("foundAndDesired " + foundAndDesired);
//        System.out.println("found " + found);
        precisionRange = Util.max(precisionRange, currentRange);
      }

      if (r > 0.90) {
//        System.out.println("");
//        System.out.println("foundAndDesired " + foundAndDesired);
//        System.out.println("desired " + desired);
        recallRange = Util.min(recallRange, neighbor.getDistance());
      }

      currentRange = neighbor.getDistance();
    }

    List<D> result = new ArrayList<D>();
    result.add(precisionRange);
    result.add(recallRange);
    return result;
  }

  private Ranges getProbabilityRanges(String id, Set<Integer> desiredSet, List<QueryResult<D>> neighbors) {
    if (neighbors.isEmpty())
      throw new IllegalArgumentException("Empty neighbors!");

    D recall = null, inverseRecall = null;
    D precision = null, inversePrecision = null;

    double desired = desiredSet.size();
    double foundAndDesired = 0;
    double notDesired = neighbors.size() - desiredSet.size();
    double notFoundAndNotDesired = 0;
    double found = 0;
    double notFound = neighbors.size();

    int size = neighbors.size() - 1;
    for (int i = 0; i <= size; i++) {
      found++;
      QueryResult<D> neighbor = neighbors.get(i);
      if (desiredSet.contains(neighbor.getID())) {
        foundAndDesired++;
      }
      // precision
      double p = foundAndDesired / found;
      // recall
      double r = foundAndDesired / desired;

      if (recall == null && r >= 0.9) {
        recall = neighbor.getDistance();
      }
      if (p >= 0.9) {
        precision = neighbor.getDistance();
      }

      notFound--;
      QueryResult<D> i_neighbor = neighbors.get(size - i);
      if (! desiredSet.contains(i_neighbor.getID())) {
        notFoundAndNotDesired++;
      }

      // inverse precision
      double ip = notFoundAndNotDesired / notFound;
      // inverse recall
      double ir = notFoundAndNotDesired / notDesired;

      if (inverseRecall == null && ir >= 0.9) {
        inverseRecall = i_neighbor.getDistance();
      }
      if (ip >= 0.9)
        inversePrecision = i_neighbor.getDistance();
    }
    return new Ranges(id, precision, inversePrecision, recall, inverseRecall);
  }

  private D getRecallRange(Set<Integer> desiredSet, List<QueryResult<D>> neighbors) {
    if (neighbors.isEmpty())
      throw new IllegalArgumentException("Empty neighbors!");

    double desired = desiredSet.size();
    double foundAndDesired = 0;

    for (QueryResult<D> neighbor : neighbors) {
      if (desiredSet.contains(neighbor.getID())) {
        foundAndDesired++;
      }
      double r = foundAndDesired / desired;

      if (r >= 0.9) {
        return neighbor.getDistance();
      }
    }
    throw new IllegalStateException("Recall > 0.90  not found!");
  }

  private D getInverseRecallRange(Set<Integer> desiredSet, List<QueryResult<D>> neighbors) {
    if (neighbors.isEmpty())
      throw new IllegalArgumentException("Empty neighbors!");

    double notDesired = neighbors.size() - desiredSet.size();
    double notFoundAndNotDesired = 0;

    for (int i = neighbors.size() - 1; i >= 0; i--) {
      QueryResult<D> neighbor = neighbors.get(i);
      if (! desiredSet.contains(neighbor.getID())) {
        notFoundAndNotDesired++;
      }
      double ir = notFoundAndNotDesired / notDesired;

      if (ir >= 0.9) {
        return neighbor.getDistance();
      }
    }
    throw new IllegalStateException("Inverse Recall > 0.90  not found!");
  }

  private void output(int r, List<DistanceObject> distanceObjects) throws FileNotFoundException {
    String outPath = "H:/Stock4b/palme";
    String marker = "distances_rep_" + (r) + ".txt";
    File file = new File(outPath + File.separator + marker);
    file.getParentFile().mkdirs();
    PrintStream outStream = new PrintStream(new FileOutputStream(file));

    for (DistanceObject object : distanceObjects) {
      outStream.println(object);
    }
  }

  private void outputRanges(int r, List<Ranges> ranges) throws FileNotFoundException {
    String outPath = "H:/Stock4b/palme";
    String marker = "ranges_rep_" + (r) + ".txt";
    File file = new File(outPath + File.separator + marker);
    file.getParentFile().mkdirs();
    PrintStream outStream = new PrintStream(new FileOutputStream(file));

    for (Ranges object : ranges) {
      outStream.println(object);
    }
  }

  private class Ranges {
    String id;
    D recall;
    D inverseRecall;
    D precision;
    D inversePrecision;

    public Ranges(String id, D precision, D inveresePrecision, D recall, D inverseRecall) {
      this.precision = precision;
      this.inversePrecision = inveresePrecision;
      this.recall = recall;
      this.inverseRecall = inverseRecall;
    }

    public String toString() {
      return id + " " + precision + " " + inversePrecision + " " + recall + " " + inverseRecall;
    }
  }

  public class DistanceObject implements Comparable<DistanceObject> {
    D distance;
    String id1;
    String id2;
    ClassLabel class1;
    ClassLabel class2;
    boolean clazz;
    double precision;

    public DistanceObject(D distance, String id1, String id2, ClassLabel class1, ClassLabel class2) {
      this.distance = distance;
      this.id1 = id1;
      this.id2 = id2;
      this.class1 = class1;
      this.class2 = class2;
      clazz = class1.equals(class2);
    }

    public int compareTo(DistanceObject o) {
      int comp = distance.compareTo(o.distance);
      if (comp != 0) return comp;
      comp = id1.compareTo(o.id1);
      if (comp != 0) return comp;
      return id2.compareTo(o.id2);
    }

    public String toString() {
      return distance.toString() + " " + id1 + " " + id2 + " " + class1 + " " + class2 + " " + clazz + " " + precision;
    }
  }

}
