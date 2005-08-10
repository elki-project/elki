package de.lmu.ifi.dbs.utilities.heap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.ArrayList;

/**
 * This file stores the elements of a persistent heap.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class PersistentHeapFile {

  /**
   * The file storing the nodes.
   */
  private final RandomAccessFile file;

  private Deap[] list = new Deap[1000];

  public PersistentHeapFile(String fileName) {
    try {
      File fileTest = new File(fileName);
      file = new RandomAccessFile(fileTest, "rw");
    }
    catch (FileNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public void write(Deap deap) {
    list[deap.getIndex()] = deap;
  }

  public Deap read(int index) {
    return list[index];
  }

  public void delete(int index) {
    list[index] = null;
  }

  public void clear() {
    list = new Deap[1000];
  }
}
