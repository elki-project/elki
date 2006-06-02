package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.index.btree.BTree;
import de.lmu.ifi.dbs.index.btree.BTreeData;
import de.lmu.ifi.dbs.index.btree.DefaultKey;
import de.lmu.ifi.dbs.parser.AbstractParser;
import de.lmu.ifi.dbs.utilities.output.ObjectPrinter;

import java.io.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Allows efficient access to nearest and reverse nearest neighbors of an object.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class NNTable {
  /**
   * The printer for output.
   */
  public static ObjectPrinter<BTreeData<DefaultKey, NeighborList>> printer = new NeighborPrinter();

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

    InputStream in = new FileInputStream(fileName);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    for (String line; (line = reader.readLine()) != null;) {
      if (! line.startsWith(AbstractParser.COMMENT)) {
        BTreeData<DefaultKey, NeighborList> data = printer.restoreObject(line);
        NeighborList nnList = data.getValue();
        for (Neighbor neighbor : nnList) {
          insert(neighbor);
        }
      }
    }
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
    return nn.searchForUpdate(new DefaultKey(id));
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
    return rnn.searchForUpdate(new DefaultKey(id));
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

    // *** do changes in nn
    NeighborList neighbors = nn.searchForUpdate(new DefaultKey(id));
    // remove old neighbor from nn
    Neighbor oldNeighbor = neighbors.removeLast();
    int index = newNeighbor.getIndex();
    // add newNeighbor to nn
    neighbors.add(index, newNeighbor);

    // *** do changes in rnn
    // remove old neighbor from rnn
    NeighborList reverseNeighbors_old = getReverseNeighborsForUpdate(oldNeighbor.getNeighborID());
    if (reverseNeighbors_old != null) {
      for (int i = 0; i < reverseNeighbors_old.size(); i++) {
        Neighbor reverseNeighbor = reverseNeighbors_old.get(i);
        if (reverseNeighbor.getObjectID().equals(oldNeighbor.getObjectID())) {
          reverseNeighbors_old.remove(i);
          break;
        }
      }
    }
    // add newNeighbor to rnn
    insertRNN(newNeighbor.copy());

    // update indices in other neighbors
    for (int i = neighbors.size() - 1; i > index; i--) {
      Neighbor nn_old = neighbors.get(i);
      Neighbor nn_new = new Neighbor(nn_old.getObjectID(), i, nn_old.getNeighborID(), nn_old.getReachabilityDistance(), nn_old.getDistance());
      neighbors.remove(i);
      neighbors.add(i, nn_new);

      reverseNeighbors_old = rnn.searchForUpdate(new DefaultKey(nn_new.getNeighborID()));
      if (reverseNeighbors_old == null) continue;
      for (int j = 0; j < reverseNeighbors_old.size(); j++) {
        Neighbor rnn_old = reverseNeighbors_old.get(j);
        if (rnn_old.getObjectID().equals(nn_new.getObjectID())) {
          Neighbor rnn_new = new Neighbor(rnn_old.getObjectID(), i, rnn_old.getNeighborID(), rnn_old.getReachabilityDistance(), rnn_old.getDistance());
          reverseNeighbors_old.remove(j);
          reverseNeighbors_old.add(j, rnn_new);
        }
      }
    }

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
   * Computes the reachability distances of the neighbors
   * of the object with the specified id
   * and inserts them into the nnTable.
   *
   * @param id the object id
   */
  public void computeReachabilityDistances(Integer id) {
    NeighborList neighbors = getNeighborsForUpdate(id);
    for (int i = 0; i < neighbors.size(); i++) {
      Neighbor nn_old = neighbors.get(i);
      NeighborList neighbors_p = getNeighbors(nn_old.getNeighborID());
      double knnDist_p = neighbors_p.get(minpts - 1).getDistance();
      double dist = nn_old.getDistance();
      double reachabilityDistance = Math.max(knnDist_p, dist);

      Neighbor nn_new = new Neighbor(nn_old.getObjectID(), nn_old.getIndex(), nn_old.getNeighborID(), reachabilityDistance, nn_old.getDistance());
      neighbors.remove(i);
      neighbors.add(i, nn_new);

      NeighborList reverseNeighbors = getReverseNeighborsForUpdate(nn_old.getNeighborID());
      for (int j = 0; j < reverseNeighbors.size(); j++) {
        Neighbor rnn_old = reverseNeighbors.get(j);
        if (rnn_old.getObjectID().equals(nn_old.getObjectID())) {
          Neighbor rnn_new = new Neighbor(rnn_old.getObjectID(), rnn_old.getIndex(), rnn_old.getNeighborID(), reachabilityDistance, rnn_old.getDistance());
          reverseNeighbors.remove(j);
          reverseNeighbors.add(j, rnn_new);
          break;
        }
      }
    }
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
    Neighbor nn_new = new Neighbor(neighbor.getObjectID(), neighbor.getIndex(), neighbor.getNeighborID(), reachabilityDistance, neighbor.getDistance());
    neighbors.remove(neighbor.getIndex());
    neighbors.add(nn_new.getIndex(), nn_new);

    // update rnn
    DefaultKey rnn_id = new DefaultKey(neighbor.getNeighborID());
    NeighborList reverseNeighbors = rnn.searchForUpdate(rnn_id);
    for (int j = 0; j < reverseNeighbors.size(); j++) {
      Neighbor rnn_old = reverseNeighbors.get(j);
      if (rnn_old.getObjectID().equals(neighbor.getObjectID())) {
        Neighbor rnn_new = new Neighbor(rnn_old.getObjectID(), rnn_old.getIndex(), rnn_old.getNeighborID(), reachabilityDistance, rnn_old.getDistance());
        reverseNeighbors.remove(j);
        reverseNeighbors.add(j, rnn_new);
        break;
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

    nn.writeData(outStream, printer);
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
    NeighborList reverseNeighbors = rnn.searchForUpdate(id);

    // create and insert a new one
    if (reverseNeighbors == null) {
      reverseNeighbors = new NeighborList();
      reverseNeighbors.add(neighbor);
      rnn.insert(id, reverseNeighbors);
    }
    // update
    else {
      reverseNeighbors.add(neighbor);
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

  /**
   * Resets the counters for page access.
   */
  public void resetPageAccess() {
    nn.resetPageAccess();
    rnn.resetPageAccess();
  }

  private static class NeighborPrinter implements ObjectPrinter<BTreeData<DefaultKey, NeighborList>> {
    private Pattern split_blank = Pattern.compile(" ");
    private Pattern split_line = Pattern.compile("\n");

    /**
     * Get the object's print data.
     *
     * @param o the object to be printed
     * @return result  a string containing the ouput
     */
    public String getPrintData(BTreeData<DefaultKey, NeighborList> o) {
      // object-ID sum1 sum2_1 ... sum2_k
      StringBuffer result = new StringBuffer();
      NeighborList neighbors = o.getValue();
      for (int i = 0; i < neighbors.size(); i++) {
        Neighbor neighbor = neighbors.get(i);
        if (i != 0) {
          result.append("\n");
        }
        result.append(neighbor.getObjectID());
        result.append(" ");
        result.append(neighbor.getIndex());
        result.append(" ");
        result.append(neighbor.getNeighborID());
        result.append(" ");
        result.append(neighbor.getReachabilityDistance());
        result.append(" ");
        result.append(neighbor.getDistance());
      }
      return result.toString();
    }


    /**
     * Restores the object which is specified by the given String.
     *
     * @param s the string that specifies the object to be restored
     * @return the restored object
     */
    public BTreeData<DefaultKey, NeighborList> restoreObject(String s) {
      String[] parameters = split_line.split(s);
      DefaultKey key = null;

      NeighborList neighborList = new NeighborList();
      for (String parameter : parameters) {
        Neighbor neighbor = restoreNeighbor(parameter);
        neighborList.add(neighbor);
        if (key == null) key = new DefaultKey(neighbor.getObjectID());
      }

      return new BTreeData<DefaultKey, NeighborList>(key, neighborList);
    }

    /**
     * Restores a neighbor from the specified string
     *
     * @param s the string that specifies the neighbor to be restored
     * @return the restored neighbor
     */
    private Neighbor restoreNeighbor(String s) {
      String[] parameters = split_blank.split(s);

      int objectID = Integer.parseInt(parameters[0]);
      int index = Integer.parseInt(parameters[1]);
      int neighborID = Integer.parseInt(parameters[2]);
      double reachabilityDistance = Double.parseDouble(parameters[3]);
      double distance = Double.parseDouble(parameters[4]);
      return new Neighbor(objectID, index, neighborID, reachabilityDistance, distance);
    }
  }

  public boolean testNN_and_RNN(int id) {
    NeighborList neighbors = nn.search(new DefaultKey(id));
    for (Neighbor neighbor : neighbors) {
      NeighborList reverseNeighbors = rnn.search(new DefaultKey(neighbor.getNeighborID()));
      for (Neighbor reverseNeighbor : reverseNeighbors) {
        if (! reverseNeighbor.equals(neighbor)) {
          return false;
        }
      }
    }
    return true;
  }

}
