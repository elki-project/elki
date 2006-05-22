package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.index.btree.BTree;
import de.lmu.ifi.dbs.utilities.output.ObjectPrinter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Allows efficient access to nearest and reverse nearest neighbors of an object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class NNTable {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The BTree containing the nearest neighbors.
   */
  private BTree<Integer, Neighbor[]> nn;

  /**
   * The BTree containing the reverse nearest neighbors.
   */
  private BTree<Integer, ArrayList<Neighbor>> rnn;

  /**
   * Number of nearest neighbors of an object to be considered for computing its LOF.
   */
  private int minpts;

  /**
   * Creates a new NNTable with the specified parameters.
   *
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param minpts    number of nearest neighbors of an object to be considered for computing its LOF
   */
  public NNTable(int pageSize, int cacheSize, int minpts) {
    double m = ((double) pageSize - 16) / (4 + minpts * 28);
    if (DEBUG) {
      logger.fine("m = " + m);
    }

    this.nn = new BTree<Integer, Neighbor[]>((int) m, pageSize, cacheSize);
    this.rnn = new BTree<Integer, ArrayList<Neighbor>>((int) m, pageSize, cacheSize);
    this.minpts = minpts;
  }

  /**
   * Inserts the given neighbor into this table.
   *
   * @param neighbor the neighbor to be inserted
   */
  public void insert(Neighbor neighbor) {
    insertNN(neighbor);
    insertRNN(neighbor.copy());
  }

  /**
   * Returns the neighbors of the object with the specified id.
   *
   * @param id the object id
   * @return the neighbors of the object with the specified id
   */
  public Neighbor[] getNeighbors(Integer id) {
    return nn.search(id);
  }

  /**
   * Returns the neighbors of the object with the specified id
   * for update (i.e. marks the node containing the neighbors as dirty).
   *
   * @param id the object id
   * @return the neighbors of the object with the specified id
   */
  public Neighbor[] getNeighborsForUpdate(Integer id) {
    return nn.search(id);
  }

  /**
   * Returns the reverse neighbors of the object with the specified id.
   *
   * @param id the object id
   * @return the reverse neighbors of the object with the specified id
   */
  public ArrayList<Neighbor> getReverseNeighbors(Integer id) {
    return rnn.search(id);
  }

  /**
   * Returns the reverse neighbors of the object with the specified id
   * for update (i.e. marks the node containing the reverse neighbors as dirty).
   *
   * @param id the object id
   * @return the reverse neighbors of the object with the specified id
   */
  public ArrayList<Neighbor> getReverseNeighborsForUpdate(Integer id) {
    return rnn.search(id);
  }

  /**
   * Inserts the specified new neighbor into the nnTable, old neighbors will
   * be moved, the last (old) neigbor will be removed and returned. The rnnTable will be
   * updated accordingly.
   *
   * @param newNeighbor the neigbohr object
   * @return the old neighbor of the specified object
   */
  public Neighbor insertAndMove(Neighbor newNeighbor) {
    Integer id = newNeighbor.getObjectID();

    // do changes in nn and rnn
    Neighbor[] neighbors = nn.searchForUpdate(id);
    Neighbor oldNeighbor = neighbors[neighbors.length - 1];
    int index = newNeighbor.getIndex();
    for (int i = neighbors.length - 1; i > index; i--) {
      // shift old entries in nn
      neighbors[i] = neighbors[i - 1];
      neighbors[i].setIndex(i);

      // remove oldNeighbor from rnn
      if (i == neighbors.length - 1) {
        ArrayList<Neighbor> reverseNeighbors = rnn.searchForUpdate(neighbors[i].getNeighborID());
        reverseNeighbors.remove(neighbors[i]);
      }
      // update index in rnn
      else {
        ArrayList<Neighbor> reverseNeighbors = rnn.searchForUpdate(neighbors[i].getNeighborID());
        for (Neighbor reverseNeighbor : reverseNeighbors) {
          if (reverseNeighbor.getObjectID() == neighbors[i].getObjectID()) {
            reverseNeighbor.setIndex(i);
            break;
          }
        }
      }
    }

    // add newNeighbor to nn
    neighbors[index] = newNeighbor;

    // add newNeighbor to rnn
    insertRNN(newNeighbor.copy());

    return oldNeighbor;
  }

  /**
   * Returns the sum of the reachability distances of all neighbors of
   * the object with the specified id.
   *
   * @param id the id of the object
   * @return the sum of the reachability distances of all neighbors of
   *         the object with the specified id
   */
  public double getSumOfReachabiltyDistances(Integer id) {
    Neighbor[] neighbors = nn.search(id);
    double sum = 0;
    for (Neighbor neighbor : neighbors) {
      sum += neighbor.getReachabilityDistance();
    }
    return sum;
  }

  /**
   * Sets the specified reachabilty distance of the given neighbor in the
   * nnTable and rnnTable.
   *
   * @param neighbor             the neighbor of which the reachabilty distance is to be set
   * @param reachabilityDistance the reachabilty distance to set
   */
  public void setReachabilityDistance(Neighbor neighbor, double reachabilityDistance) {
    // update nn
    Integer nn_id = neighbor.getObjectID();
    Neighbor[] neighbors = nn.searchForUpdate(nn_id);
    neighbors[neighbor.getIndex()].setReachabilityDistance(reachabilityDistance);

    // update rnn
    Integer rnn_id = neighbor.getNeighborID();
    ArrayList<Neighbor> reverseNeighbors = rnn.searchForUpdate(rnn_id);
    for (Neighbor rnn : reverseNeighbors) {
      if (rnn.getObjectID().equals(nn_id)) {
        rnn.setReachabilityDistance(reachabilityDistance);
      }
    }
  }

  /**
   * Writes the nnTable to the specified stream.
   *
   * @param outStream the stream to write into
   */
  public void write(PrintStream outStream) {
    outStream.println("################################################################################");
    outStream.println("### object_o  k kNN_p reachDist(o,p) dist(o,p)");
    outStream.println("################################################################################");

    ObjectPrinter printer = new ObjectPrinter() {
      public String getPrintData(Object o) {
        if (o instanceof Neighbor[]) {
          StringBuffer result = new StringBuffer();
          Neighbor[] neighbors = (Neighbor[]) o;
          for (int i = 0; i < neighbors.length; i++) {
            if (i != 0) {
              result.append("\n");
              result.append(neighbors[i].getObjectID());
              result.append(" ");
            }
            result.append(neighbors[i].getIndex());
            result.append(" ");
            result.append(neighbors[i].getNeighborID());
            result.append(" ");
            result.append(neighbors[i].getReachabilityDistance());
            result.append(" ");
            result.append(neighbors[i].getDistance());
          }
          return result.toString();
        }

        return o.toString();
      }
    };

    nn.writeData(outStream, printer);
  }

  /**
   * Inserts the specified neighbor into the nnTable.
   *
   * @param neighbor the neighbor to be inserted
   */
  private void insertNN(Neighbor neighbor) {
    Integer id = neighbor.getObjectID();
    int index = neighbor.getIndex();
    Neighbor[] neighbors = nn.searchForUpdate(id);

    // create and insert a new one
    if (neighbors == null) {
      neighbors = new Neighbor[minpts];
      neighbors[index] = neighbor;
      nn.insert(id, neighbors);
    }
    // update
    else {
      neighbors[index] = neighbor;
    }
  }

  /**
   * Inserts the specified neighbor into the rnnTable.
   *
   * @param neighbor the neighbor to be inserted
   */
  private void insertRNN(Neighbor neighbor) {
    Integer id = neighbor.getNeighborID();
    ArrayList<Neighbor> revereseNeighbors = rnn.searchForUpdate(id);

    // create and insert a new one
    if (revereseNeighbors == null) {
      revereseNeighbors = new ArrayList<Neighbor>();
      revereseNeighbors.add(neighbor);
      rnn.insert(id, revereseNeighbors);
    }
    // update
    else {
      revereseNeighbors.add(neighbor);
    }
  }
}
