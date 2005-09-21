package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.ClusterOrder;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.SpatialIndexDatabase;
import de.lmu.ifi.dbs.database.DeliRTreeDatabase;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.SpatialNode;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

/**
 * DeliClu provides the DeliClu algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeliClu extends DistanceBasedAlgorithm<DoubleVector> {
  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

  /**
   * Minimum points.
   */
  private int minpts;

  /**
   * Provides the result of the algorithm.
   */
  private ClusterOrder clusterOrder;

  /**
   * The priority queue for the algorithm.
   */
//  private Heap<Distance, COEntry> heap;


  /**
   * Sets minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since DeliClu is a non-abstract
   * class, finally optionHandler is initialized.
   */
  public DeliClu() {
    super();
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  public void run(Database<DoubleVector> database) throws IllegalStateException {
    long start = System.currentTimeMillis();
    try {
      if (!(database instanceof DeliRTreeDatabase))
        throw new IllegalArgumentException("Database must be an instance of " +
          DeliRTreeDatabase.class.getName());

      if (!(getDistanceFunction() instanceof SpatialDistanceFunction))
        throw new IllegalArgumentException("Distance Function must be an instance of " +
          SpatialDistanceFunction.class.getName());

      DeliRTreeDatabase db = (DeliRTreeDatabase) database;
      SpatialDistanceFunction<DoubleVector> distFunction = (SpatialDistanceFunction<DoubleVector>) getDistanceFunction();

      Progress progress = new Progress(database.size());
      int size = database.size();

      clusterOrder = new ClusterOrder<DoubleVector>(database, getDistanceFunction(), getParameterSettings());

      /*
      heap = new DefaultHeap<Distance, COEntry>();

      SpatialNode root = db.getRootNode();

      // add start object to cluster order and (root, root) to priority queue
      Integer o = getStartObject();
      clusterOrder.add(o, null, distFunction.infiniteDistance());
      int numHandled = 1;

      db.setHandled(o);
      pq.addNode(new PQNodePair(root, root, 0));

      while (numHandled != size) {
        PQNode node = (PQNode) pq.getMinNode();

        // Seitenpaar
        if (node.isExpandable()) {
//        System.out.println("expand " + node + "("+Util.format(node.getKey())+")");
          PQNodePair nodePair = (PQNodePair) node;
          expandNodes(nodePair);
          db.getTree().setExpanded(nodePair.getNode1(), nodePair.getNode2());
        }
        // Objektpaar
        else {
//          System.out.println("node " + node + "   #=" + numHandled);
          PQDataPair dataPair = (PQDataPair) node;
          RTreeDBObject x = (RTreeDBObject) dataPair.getUnhandledEntry();
          RTreeDBObject y = (RTreeDBObject) dataPair.getHandledEntry();

//          if (x.getObjectID() == 308) System.out.println("rech(308) = "+nodeAlt.getKey()+" --> "+y);
          TreeEntry[] path = db.setHandled(x);

          numHandled++;
//          if (x.getObjectID() == 1) System.out.println("y="+y+", key="+dataPair.getKey());
          co.add(x, y, dataPair.getKey());
//          System.out.println(x + ", " + y + ", distance = " + x.distance(y) + " handled = " + numHandled + ", path=" + path);
          if (numHandled % 1000 == 0) System.out.print(numHandled + " ");

          reinsertExpanded(x, (RTreeNode) path[path.length - 2]);
//          reinsertExpanded(root, root, path, 0);
        }
      }
      System.out.println("count = " + count);
      System.out.println();
      */
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    long end = System.currentTimeMillis();

    if (isTime()) {
      long elapsedTime = end - start;
      System.out.println(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
    }
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("DeliClu", "Density-Based Hierarchical Clustering",
      "Algorithm to find density-connected sets in a database based on the parameters " +
        "minimumPoints.", "Unpublished");
  }

  /**
   * Sets the parameters epsilon and minpts additionally to the parameters set
   * by the super-class' method. Both epsilon and minpts are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      minpts = Integer.parseInt(optionHandler.getOptionValue(MINPTS_P));
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public String[] getParameterSettings() {
    return new String[]{MINPTS_P + " = " + minpts};
  }

  /**
   * @see Algorithm#getResult()
   */
  public Result getResult() {
    return clusterOrder;
  }

}
