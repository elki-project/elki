package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.index.btree.BTree;

import java.io.IOException;
import java.util.ArrayList;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class NNTable {
  private BTree<Integer, NNEntry> nn;
  private BTree<Integer, NNEntry> rnn;

  /**
   * Creates a new NNTable with the specified parameters.
   * The underlying BTrees will be saved persistently
   * in the specified files.
   *
   * @param m         the order of the BTree
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Bytes
   * @param nnFileName  the name of the file storing the BTree for nearest neighbors.
   * @param rnnFileName  the name of the file storing the BTree for reverse nearest neighbors.
   */
  public NNTable(int m, int pageSize, int cacheSize, String nnFileName, String rnnFileName) {
    this.nn = new BTree<Integer, NNEntry>(m, pageSize, cacheSize, nnFileName);
    this.rnn = new BTree<Integer, NNEntry>(m, pageSize, cacheSize, rnnFileName);

  }
  /*

  public NNTableEntry[] insert(NNTableEntry entry) throws IOException {
    NNTableEntry[] result = insertNN(entry);
    insertRNN(entry);
    return result;
  }

  public NNTableEntry insertAndMove(NNTableEntry entry) throws IOException {
    Integer nn_id = entry.getObjectID();
    NNTableEntry[] in = (NNTableEntry[]) nn.find(nn_id);
    NNTableEntry last = in[in.length - 1];

    // remove last from rnn
    Integer old_id = last.getNNObjectID();
    ArrayList old = (ArrayList) rnn.find(old_id);
    old.remove(last);

    int k = entry.getK();
    for (int i = in.length - 1; i > k; i--) {
      NNTableEntry nnEntry = in[i - 1];

      // delete in rnn
      Integer rnn_id = nnEntry.getNNObjectID();
      ArrayList rnns = (ArrayList) rnn.find(rnn_id);
      rnns.remove(nnEntry);

      // change in nn
      NNTableEntry newEntry = new NNTableEntry(nnEntry.getObject(), i, nnEntry.getNNObject(),
                                               nnEntry.getReachDist(), nnEntry.getDistance());
      in[i] = newEntry;

      // add again to rnn
      rnns.add(nnEntry);
      rnn.insert(rnn_id, rnns, true);
    }

    // add new entry to rnn
    insertRNN(entry);

    // add new entry to nn
    in[k] = entry;

    nn.insert(nn_id, in, true);
    return last;
  }

  public void setReachDist(NNTableEntry entry, double reachDist) throws IOException {
    NNTableEntry newEntry = new NNTableEntry(entry.getObject(), entry.getK(), entry.getNNObject(),
                                             reachDist, entry.getDistance());

    Integer nn_id = entry.getObjectID();
    NNTableEntry[] in = (NNTableEntry[]) nn.find(nn_id);
    in[entry.getK()] = newEntry;
    nn.insert(nn_id, in, true);

    Integer rnn_id = entry.getNNObjectID();
    ArrayList rnns = (ArrayList) rnn.find(rnn_id);
    for (int i = 0; i < rnns.size(); i++) {
      NNTableEntry rnn = (NNTableEntry) rnns.get(i);
      if (rnn.getObjectID().equals(nn_id)) {
        rnns.remove(i);
        rnns.add(i, newEntry);
      }
    }
    rnn.insert(rnn_id, rnns, true);
  }

  public NNTableEntry[] getKNNs(LofDBObject key) throws IOException {
    Integer id = new Integer(key.getObjectID());
    return (NNTableEntry[]) nn.find(id);
  }

  public NNTableEntry[] getKNNs(Integer key) throws IOException {
    return (NNTableEntry[]) nn.find(key);
  }

  public NNTableEntry[] getRNNs(LofDBObject key) throws IOException {
    Integer id = new Integer(key.getObjectID());
    ArrayList rnns = (ArrayList) rnn.find(id);

    NNTableEntry[] result = new NNTableEntry[rnns.size()];
    return (NNTableEntry[]) rnns.toArray(result);
  }

  public double getSumReachDists(LofDBObject key) throws IOException {
    Integer id = new Integer(key.getObjectID());
    NNTableEntry[] kNNs = (NNTableEntry[]) nn.find(id);
    double sum = 0;
    for (int i = 0; i < kNNs.length; i++) {
      sum += kNNs[i].getReachDist();
    }
    return sum;
  }

  public TupleBrowser getNNBrowser() throws IOException {
    return nn.browse();
  }

  public TupleBrowser getRNNBrowser() throws IOException {
    return rnn.browse();
  }

  private NNTableEntry[] insertNN(NNTableEntry entry) throws IOException {
    Integer id = entry.getObjectID();
    NNTableEntry[] in = (NNTableEntry[]) nn.find(id);

    if (in == null) {
      in = new NNTableEntry[minPts];
      int k = entry.getK();
      in[k] = entry;
      nn.insert(id, in, true);
      return in;
    }

    in[entry.getK()] = entry;
    nn.insert(id, in, true);
    return in;
  }

  private void insertRNN(NNTableEntry entry) throws IOException {
    Integer id = entry.getNNObjectID();
    ArrayList in = (ArrayList) rnn.find(id);

    if (in == null) {
      in = new ArrayList();
      in.add(entry);
      rnn.insert(id, in, true);
      return;
    }

    in.add(entry);
    rnn.insert(id, in, true);
  }
  */
}
