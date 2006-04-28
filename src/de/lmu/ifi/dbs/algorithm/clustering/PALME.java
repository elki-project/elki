package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.PALMEResult;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.RepresentationSelectingDistanceFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * TODO: comment and new name
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PALME<O extends DatabaseObject, D extends Distance<D>, M extends MultiRepresentedObject<O>> extends AbstractAlgorithm<M> {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings("unused")
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());


  /**
   * The distance function for the single representations.
   */
  private RepresentationSelectingDistanceFunction<O, M, D> mr_distanceFunction;

  /**
   * The result of this algorithm.
   */
  private Result<M> result;

  /**
   * Holds the number of distance objects having same classes.
   */
  private double sameClasses;

  /**
   * Holds the number of distance objects having different classes.
   */
  private double differentClasses;

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
      if (database.size() == 0) {
        result = new PALMEResult<O, D, M>(database);
        return;
      }
      System.out.println("database.size " + database.size());
      mr_distanceFunction.setDatabase(database, isVerbose(), isTime());

      int numberOfRepresentations = database.get(database.iterator().next()).getNumberOfRepresentations();
      Progress progress = new Progress("PALME", 2 * database.size() * numberOfRepresentations);

      for (int r = 0; r < numberOfRepresentations; r++) {
        if (isVerbose()) {
          System.out.println("\nRepresentation " + (r + 1));
        }
        mr_distanceFunction.setCurrentRepresentationIndex(r);

        List<DistanceObject> distances = determineDistances(database, progress, r);


        if (isVerbose()) {
          logger.info("\n...determine propability ranges");
        }
        D similarityRange = null;
        int similarityRangeIndex = 0;
        D dissimilarityRange = null;
        int dissimilarityRangeIndex = distances.size() - 1;

        double maxSimilarityPrecision = 0;
        double maxDissimilarityPrecision = 0;

        double foundAndDesired = 0;
        double inverseFoundAndDesired = 0;

        double threshold = 0.90;
        double ap_sim = sameClasses / (sameClasses + differentClasses);
        double ap_dissim = differentClasses / (sameClasses + differentClasses);

//        double similarityThreshold = ap_sim + ap_dissim * threshold;
//                double dissimilarityThreshold = ap_dissim + ap_sim * threshold;
        double similarityThreshold = 0.90;
        double dissimilarityThreshold = 0.95;

        if (DEBUG) {
          StringBuffer msg = new StringBuffer();
          msg.append("\nsimilarityThreshold " + similarityThreshold);
          msg.append("\ndissimilarityThreshold " + dissimilarityThreshold);
          logger.fine(msg.toString());
        }

        for (int i = 0; i < distances.size(); i++) {
          DistanceObject distanceObject = distances.get(i);

          if (distanceObject.sameClass) {
            foundAndDesired++;
          }

          distanceObject.similarityPrecision = foundAndDesired / (i + 1);
          maxSimilarityPrecision = Math.max(maxSimilarityPrecision, distanceObject.similarityPrecision);
          if (distanceObject.similarityPrecision > similarityThreshold) {
            similarityRange = distanceObject.distance;
            similarityRangeIndex = i + 1;
          }

          DistanceObject inverseDistanceObject = distances.get(distances.size() - 1 - i);
          if (! inverseDistanceObject.sameClass) {
            inverseFoundAndDesired++;
          }
          inverseDistanceObject.dissimilarityPrecision = inverseFoundAndDesired / (i + 1);
          maxDissimilarityPrecision = Math.max(maxSimilarityPrecision, distanceObject.dissimilarityPrecision);
          if (inverseDistanceObject.dissimilarityPrecision > dissimilarityThreshold) {
            dissimilarityRange = inverseDistanceObject.distance;
            dissimilarityRangeIndex = distances.size() - 1 - i;
          }
        }

        if (similarityRange == null) {
          similarityRange = mr_distanceFunction.nullDistance();
        }
        if (dissimilarityRange == null) {
          dissimilarityRange = mr_distanceFunction.infiniteDistance();
        }

        if (DEBUG) {
          StringBuffer msg = new StringBuffer();
          msg.append("\nsimilarityRange " + similarityRange + " (" + similarityRangeIndex + ")");
          msg.append("\ndissimilarityRange " + dissimilarityRange + " (" + dissimilarityRangeIndex + ")");
          logger.fine(msg.toString());
        }

        output(r, distances, similarityRange, dissimilarityRange,
               similarityThreshold, dissimilarityThreshold,
               similarityRangeIndex, dissimilarityRangeIndex,
               maxSimilarityPrecision, maxDissimilarityPrecision);

//        maxDistances.add(maxDist);
//        outputRanges(r, rangesList);
//        resultList.add(rangesList);
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
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    mr_distanceFunction = new RepresentationSelectingDistanceFunction<O, M, D>();

    remainingParameters = mr_distanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
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

  private List<DistanceObject> determineDistances(Database<M> database, Progress progress, int r) {
    sameClasses = 0;
    differentClasses = 0;

    int processed = r * database.size();
    if (isVerbose()) {
      logger.info("...determine distances");
    }

    List<DistanceObject> distances = new ArrayList<DistanceObject>((database.size() * database.size() - 1) / 2);

    for (Iterator<Integer> it_1 = database.iterator(); it_1.hasNext();) {
      Integer id_1 = it_1.next();
      ClassLabel classLabel_1 = (ClassLabel) database.getAssociation(AssociationID.CLASS, id_1);
      String externalID_1 = (String) database.getAssociation(AssociationID.EXTERNAL_ID, id_1);

      for (Iterator<Integer> it_2 = database.iterator(); it_2.hasNext();) {
        Integer id_2 = it_2.next();
        if (id_1 >= id_2) continue;
        ClassLabel classLabel_2 = (ClassLabel) database.getAssociation(AssociationID.CLASS, id_2);
        String externalID_2 = (String) database.getAssociation(AssociationID.EXTERNAL_ID, id_2);

        D distance = mr_distanceFunction.distance(id_1, id_2);
        DistanceObject object = new DistanceObject(externalID_1, externalID_2, classLabel_1, classLabel_2, distance);
        distances.add(object);

        if (object.sameClass) sameClasses++;
        else differentClasses++;
      }

      if (isVerbose()) {
        progress.setProcessed(++processed);
        logger.info("\r" + progress.toString());
      }
    }

//    if (DEBUG) {
//      StringBuffer msg = new StringBuffer();
//      msg.append("\nsame class ").append(sameClasses);
//      msg.append("\ndifferent class ").append(differentClasses);
//      logger.fine(msg.toString());
//    }

    Collections.sort(distances);
    return distances;
  }

  private void output(int r, List<DistanceObject> distanceObjects,
                      D similarityRange,
                      D dissimilarityRange,
                      double similarityThreshold,
                      double dissimilarityThreshold,
                      int similarityRangeIndex,
                      int dissimilarityRangeIndex,
                      double maxSimilarityPrecision,
                      double maxDissimilarityPrecision) throws UnableToComplyException {
    try {
      String fileName = "../Stock4b/palme_elki/ranges_rep_" + (r + 1) + ".txt";
      File file = new File(fileName);
      file.getParentFile().mkdirs();
      PrintStream outStream = new PrintStream(new FileOutputStream(file));

      outStream.println("similarity-range " + similarityRange + " (" + similarityRangeIndex + ")");
      outStream.println("similarity-threshhold " + similarityThreshold);
      outStream.println("maxSimilarityPrecision " + maxSimilarityPrecision);
      outStream.println("dissimilarity-range " + dissimilarityRange + " (" + dissimilarityRangeIndex + ")");
      outStream.println("dissimilarity-threshhold " + dissimilarityThreshold);
      outStream.println("maxDissimilarityPrecision " + maxDissimilarityPrecision);
      outStream.println(distanceObjects.get(0).getDescription());
      for (DistanceObject object : distanceObjects) {
        outStream.println(object);
      }
    }
    catch (FileNotFoundException e) {
      throw new UnableToComplyException(e);
    }
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

  public class DistanceObject implements Comparable<DistanceObject> {
    String id1;
    String id2;
    ClassLabel classLabel1;
    ClassLabel classLabel2;
    boolean sameClass;
    D distance;
    double similarityPrecision;
    double dissimilarityPrecision;

    public DistanceObject(String id1, String id2,
                          ClassLabel classLabel1, ClassLabel classLabel2,
                          D distance) {
      this.id1 = id1;
      this.id2 = id2;
      this.classLabel1 = classLabel1;
      this.classLabel2 = classLabel2;
      this.sameClass = classLabel1.equals(classLabel2);
      this.distance = distance;
    }

    public String getDescription() {
      return "id1 id2 class_label1 class_label2 same_class distance similarity-precision dissimilarity-precision";
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
      StringBuffer result = new StringBuffer();
      result.append(id1);
      result.append(" ");
      result.append(id2);
      result.append(" ");
      result.append(classLabel1);
      result.append(" ");
      result.append(classLabel2);
      result.append(" ");
      if (sameClass)
        result.append(0);
      else
        result.append(1);
      result.append(" ");
      result.append(distance);
      result.append(" ");
      result.append(similarityPrecision);
      result.append(" ");
      result.append(dissimilarityPrecision);
      result.append(" ");
      return result.toString();
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(DistanceObject o) {
      int comp = this.distance.compareTo(o.distance);
      if (comp != 0) return comp;

      comp = this.id1.compareTo(o.id2);
      if (comp != 0) return comp;

      return this.id2.compareTo(o.id2);
    }

  }

}
