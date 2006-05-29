package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.index.btree.BTree;
import de.lmu.ifi.dbs.index.btree.DefaultKey;

import java.io.PrintStream;
import java.io.IOException;
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
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The BTree containing the nearest neighbors.
   */
  private BTree<DefaultKey, NeighborList> nn;

  /**
   * The BTree containing the reverse nearest neighbors.
   */
  private BTree<DefaultKey, NeighborList> rnn;

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
    int keySize = 4;
    int neighborSize = 4 + 4 + 4 + 8 + 8;
    int valueSize = minpts * neighborSize;

    this.nn = new BTree<DefaultKey, NeighborList>(keySize, valueSize, pageSize, cacheSize);
    this.rnn = new BTree<DefaultKey, NeighborList>(keySize, valueSize, pageSize, cacheSize);
    this.minpts = minpts;
  }

  /**
   * Creates a new NNTable with the specified parameters.
   *
   * @param fileName  the name of the file containing the entries
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param minpts    number of nearest neighbors of an object to be considered for computing its LOF
   */
  public NNTable(String fileName, int pageSize, int cacheSize, int minpts) throws IOException {
    this(pageSize, cacheSize, minpts);
    // todo
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
  public NeighborList getNeighbors(Integer id) {
    return nn.search(new DefaultKey(id));
  }

  /**
   * Returns the neighbors of the object with the specified id
   * for update (i.e. marks the node containing the neighbors as dirty).
   *
   * @param id the object id
   * @return the neighbors of the object with the specified id
   */
  public NeighborList getNeighborsForUpdate(Integer id) {
    return nn.search(new DefaultKey(id));
  }

  /**
   * Returns the reverse neighbors of the object with the specified id.
   *
   * @param id the object id
   * @return the reverse neighbors of the object with the specified id
   */
  public NeighborList getReverseNeighbors(Integer id) {
    return rnn.search(new DefaultKey(id));
  }

  /**
   * Returns the reverse neighbors of the object with the specified id
   * for update (i.e. marks the node containing the reverse neighbors as dirty).
   *
   * @param id the object id
   * @return the reverse neighbors of the object with the specified id
   */
  public NeighborList getReverseNeighborsForUpdate(Integer id) {
    return rnn.search(new DefaultKey(id));
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
    NeighborList neighbors = nn.searchForUpdate(new DefaultKey(id));

    // remove old neighbor from nn
    Neighbor oldNeighbor = neighbors.removeLast();
    int index = newNeighbor.getIndex();
    // add newNeighbor to nn
    neighbors.add(index, newNeighbor);

    for (int i = neighbors.size() - 1; i > index; i--) {
      Neighbor neighbor = neighbors.get(i);
      // change index
      neighbor.setIndex(i);

      // remove oldNeighbor from rnn
      if (i == neighbors.size() - 1) {
        NeighborList reverseNeighbors = rnn.searchForUpdate(new DefaultKey(neighbor.getNeighborID()));
        reverseNeighbors.remove(neighbor);
      }
      // update index in rnn
      else {
        NeighborList reverseNeighbors = rnn.searchForUpdate(new DefaultKey(neighbor.getNeighborID()));
        for (Neighbor reverseNeighbor : reverseNeighbors) {
          if (reverseNeighbor.getObjectID() == neighbor.getObjectID()) {
            reverseNeighbor.setIndex(i);
            break;
          }
        }
      }
    }

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
    NeighborList neighbors = nn.search(new DefaultKey(id));
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
    DefaultKey nn_id = new DefaultKey(neighbor.getObjectID());
    NeighborList neighbors = nn.searchForUpdate(nn_id);
    neighbors.get(neighbor.getIndex()).setReachabilityDistance(reachabilityDistance);

    // update rnn
    DefaultKey rnn_id = new DefaultKey(neighbor.getNeighborID());
    NeighborList reverseNeighbors = rnn.searchForUpdate(rnn_id);
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

    /**
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
     */
  }

  /**
   * Inserts the specified neighbor into the nnTable.
   *
   * @param neighbor the neighbor to be inserted
   */
  private void insertNN(Neighbor neighbor) {
    DefaultKey id = new DefaultKey(neighbor.getObjectID());
    int index = neighbor.getIndex();
    NeighborList neighbors = nn.searchForUpdate(id);

    // create and insert a new one
    if (neighbors == null) {
      neighbors = new NeighborList(minpts);
      neighbors.add(index, neighbor);
      nn.insert(id, neighbors);
    }
    // update
    else {
      neighbors.add(index, neighbor);
    }
  }

  /**
   * Inserts the specified neighbor into the rnnTable.
   *
   * @param neighbor the neighbor to be inserted
   */
  private void insertRNN(Neighbor neighbor) {
    DefaultKey id = new DefaultKey(neighbor.getNeighborID());
    NeighborList revereseNeighbors = rnn.searchForUpdate(id);

    // create and insert a new one
    if (revereseNeighbors == null) {
      revereseNeighbors = new NeighborList();
      revereseNeighbors.add(neighbor);
      rnn.insert(id, revereseNeighbors);
    }
    // update
    else {
      revereseNeighbors.add(neighbor);
    }
  }

  /**
   * Returns the physical read access of this table.
   */
  public long getPhysicalReadAccess() {
    return nn.getPhysicalReadAccess() + rnn.getPhysicalReadAccess();
  }

  /**
   * Returns the physical write access of this table.
   */
  public long getPhysicalWriteAccess() {
    return nn.getPhysicalWriteAccess() + rnn.getPhysicalWriteAccess();
  }

  /**
   * Returns the logical page access of this table.
   */
  public long getLogicalPageAccess() {
    return nn.getLogicalPageAccess() + rnn.getLogicalPageAccess();
  }

}
