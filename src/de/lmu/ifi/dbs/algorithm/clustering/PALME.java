package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.RepresentationSelectingDistanceFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;

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

      Iterator<Integer> it = database.iterator();
      if (! it.hasNext()) {
        // todo empty result
        return;
      }

      Map<ClassLabel, Set<Integer>> classMap = determineClassPartitions(database);
      int numberOfRepresentations = database.get(it.next()).getNumberOfRepresentations();

      for (int r = 0; r < numberOfRepresentations; r++) {
        mr_distanceFunction.setCurrentRepresentationIndex(r);

        D recallRange = mr_distanceFunction.infiniteDistance();
        D precisionRange = mr_distanceFunction.nullDistance();

        int i = 0;
        for (it = database.iterator(); it.hasNext();) {
          Integer id = it.next();
          ClassLabel classLabel = (ClassLabel) database.getAssociation(AssociationID.CLASS, id);
          Set<Integer> desired = classMap.get(classLabel);


          Set<Integer> current_found = new HashSet<Integer>();


          List<QueryResult<D>> neighbors = database.rangeQuery(id, RepresentationSelectingDistanceFunction.INFINITY_PATTERN, mr_distanceFunction);


          System.out.println("desired " + desired);
          System.out.println("desired.size " + desired.size());
          System.out.println("neighbors " + neighbors);

          for (QueryResult<D> neighbor : neighbors) {
            current_found.add(neighbor.getID());

            if (desired.contains(neighbor.getID())) {
              precisionRange = neighbor.getDistance();
              System.out.println("    p " + precisionRange + " i " + i);
            }

            if (current_found.equals(desired)) {
              recallRange = neighbor.getDistance();
              System.out.println("    r " + recallRange + " i " + i);
              break;
            }
            i++;
          }


        }

        System.out.println("");
        System.out.println("Representation " + r);
        System.out.println("  precisionRange " + precisionRange);
        System.out.println("  recallRange " + recallRange);

      }


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
    // todo
    return null;  //To change body of implemented methods use File | Settings | File Templates.
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
      System.out.println("classLabel " + classLabel);

      Set<Integer> ids = classMap.get(classLabel);
      if (ids == null) {
        ids = new HashSet<Integer>();
        classMap.put(classLabel, ids);
      }
      ids.add(id);
      System.out.println("ids " + ids);

    }
    System.out.println("classMap " + classMap);

    return classMap;

  }
}
