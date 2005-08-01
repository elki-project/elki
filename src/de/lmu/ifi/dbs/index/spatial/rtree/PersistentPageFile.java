package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.caching.Page;
import de.lmu.ifi.dbs.index.spatial.MBR;

import java.io.*;

/**
 * A persistent implementation of a CachedPageFile implemented based on
 * a RandomAccesFile.
 * <p/>
 * Structure of the File
 * <p/>
 * -- Header (17 Byte) --
 * int pageFileVersion </br>
 * int dimension </br>
 * int capacity = maxLoad + 1 for Overflow </br>
 * int minimum </br>
 * boolean flatDirectory </br>
 * <p/>
 * -- Body --
 * a sequence of page one after another with:
 * int typ - 1 LeafNode 2 DirectoryNode </br>
 * int index - index of the node in parent node </br>
 * int numEntries - number of entries in the node </br>
 * int parentID - page id of parent node </br>
 * int pageID - page id of the node </br>
 * - for(i = 0; i < capacity; i++) </br>
 * - int entryID - id of Entry i </br>
 * --- for(d = 0; i < dimensionality; d++) </br>
 * --- double min[d] - min[d] of MBR for Entry i </br>
 * --- for(d = 0; i < dimensionality; d++) </br>
 * --- double max[d] - max[d] of MBR for Entry i </br>
 * <p/>
 * pageSize = 20 + (4 + 16 * dimension) * capacity;
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class PersistentPageFile extends PageFile {

  /**
   * magic number
   */
  private static final int PAGEFILE_VERSION = 060676002;

  /**
   * Indicates a empty page.
   */
  private static final int EMPTY_PAGE = 0;

  /**
   * Indicates a leaf node.
   */
  private static final int LEAF_NODE = 1;

  /**
   * Indicates a directory node
   */
  private static final int DIR_NODE = 2;

  /**
   * The name of the file storing the nodes.
   */
  private String fileName;

  /**
   * The file storing the nodes.
   */
  private RandomAccessFile file;

  /**
   * A Boolean indicating if the file is closed.
   */
  private boolean closed;

  /**
   * The size of a page in bytes.
   */
  private int pageSize;

  /**
   * The size in bytes of the header of this PageFile.
   */
  private int headerSize;

  /**
   * The buffer for reading the bytes of the underlying file.
   */
  private byte[] buffer;

  /**
   * Creates a new PersistentPageFile with the specified file name.
   *
   * @param cacheSize the size of the cache of this PageFile.
   * @param cacheType the type of the cache
   * @param fileName  the name of the file
   */
  public PersistentPageFile(int cacheSize, String cacheType, String fileName) {
    super(cacheSize, cacheType);
    this.fileName = fileName;
    this.closed = false;
    this.headerSize = 17;
  }

  /**
   * Initializes the PageFile.
   *
   * @param dimensionality the dimension ofthe stored data
   * @param pageSize       the size of a page in bytes
   * @param flatDirectory  a boolean that indicates a flat directory
   */
  protected void initialize(int dimensionality, int pageSize, boolean flatDirectory) {
    super.initialize(dimensionality, pageSize, flatDirectory);

    File fileTest = new File(fileName);

    try {
      // Initialize from existing file
      if (dimensionality == -999) {
        if (!fileTest.exists())
          throw new RuntimeException("File does not exist");

        file = new RandomAccessFile(fileTest, "rw");
        //	Test if it is a PersistentPageFile
        file.seek(0);
        if (file.readInt() != PAGEFILE_VERSION)
          throw new RuntimeException("Not a PersistentPageFile or wrong version");

        // Reading header - Initializing PageFile
        this.dimensionality = file.readInt();
        this.capacity = file.readInt();
        this.minimum = file.readInt();

        this.pageSize = 20 + (4 + 16 * getDimensionality()) * getCapacity();
        this.buffer = new byte[this.pageSize];

        // reading empty pages in Stack
        int i = 0;
        try {
          while (true) {
            file.seek(headerSize + (i * pageSize));
            if (EMPTY_PAGE == file.readInt())
              emptyPages.push(new Integer(i));
            i++;
          }
        }
        catch (EOFException eof) {
          // not an exception - wanted
        }
      }

      // new file
      else {
        file = new RandomAccessFile(fileTest, "rw");
        file.setLength(0);
        this.pageSize = 20 + (4 + 16 * dimensionality) * getCapacity();
        this.buffer = new byte[this.pageSize];

        // writing header
        file.seek(0);
        file.writeInt(PAGEFILE_VERSION);
        file.writeInt(this.getDimensionality());
        file.writeInt(this.getCapacity());
        file.writeInt(this.getMinimum());
      }
    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException("IOException occured: \n " + e.getMessage());
    }
  }

  /**
   * @see de.lmu.ifi.dbs.caching.CachedPageFile#write(de.lmu.ifi.dbs.caching.Page)
   */
  public synchronized void write(Page page) {
    ioAccess++;
    Node node = (Node) page;

    try {
      ByteArrayOutputStream bs = new ByteArrayOutputStream(pageSize);
      DataOutputStream ds = new DataOutputStream(bs);

      int type = node.isLeaf() ? LEAF_NODE : DIR_NODE;

      ds.writeInt(type);
      ds.writeInt(node.index);
      ds.writeInt(node.numEntries);
      ds.writeInt(node.parentID);
      ds.writeInt(node.pageID);

      // write children
      for (int i = 0; i < node.getNumEntries(); i++) {
        Entry entry = node.entries[i];
        ds.writeInt(entry.getID());
        MBR mbr = entry.getMBR();
        for (int d = 1; d <= this.getDimensionality(); d++)
          ds.writeDouble(mbr.getMin(d));
        for (int d = 1; d <= this.getDimensionality(); d++)
          ds.writeDouble(mbr.getMax(d));
      }
      for (int i = 0; i < (this.getCapacity() - node.getNumEntries()); i++) {
        ds.writeInt(-1);
        for (int d = 1; d <= this.getDimensionality() * 2; d++) {
          ds.writeDouble(-1);
        }
      }

      ds.flush();
      bs.flush();

      file.seek(headerSize + (pageSize * node.getPageID()));
      file.write(bs.toByteArray());

      ds.close();
    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException("IOException occured ! \n " + e.getMessage());
    }
  }

  /**
   * @see PageFile#readNode(int)
   */
  protected synchronized Node readNode(int pageID) {
    // try to get from cache
    Node node = super.readNode(pageID);
    if (node != null) {
      return node;
    }

    // get from file
    ioAccess++;
    StringBuffer msg = new StringBuffer();

    try {
      int index = headerSize + pageID * pageSize;
      msg.append("\n seek " + index);
      file.seek(index);

      int read = file.read(buffer);
      if (pageSize == read) {
        DataInputStream ds = new DataInputStream(new ByteArrayInputStream(buffer));

        int type = ds.readInt();
        if (type == LEAF_NODE)
          node = new LeafNode(this);
        else if (type == DIR_NODE)
          node = new DirectoryNode(this);
        else if (type == EMPTY_PAGE)
          return null;
        else
          throw new RuntimeException("Unknown Node Type");
        msg.append("\n type " + type);

        node.index = ds.readInt();
        node.numEntries = ds.readInt();
        node.parentID = ds.readInt();
        node.pageID = ds.readInt();

        msg.append("\n index " + node.index);
        msg.append("\n numEntries " + node.numEntries);
        msg.append("\n parentID " + node.parentID);
        msg.append("\n pageID " + node.pageID);

        // set children
        for (int i = 0; i < this.getCapacity(); i++) {
          msg.append("\n child " + i);

          int id = ds.readInt();
          msg.append("\n id " + id);

          MBR mbr = readNextMBR(ds);
          msg.append("\n mbr " + mbr);

          if (id != -1) {
            Entry entry = new Entry(id, mbr);
            node.entries[i] = entry;
          }
        }
        ds.close();
      }

      else {
        return null;
      }

      return node;
    }
    catch (IOException e) {
      e.printStackTrace();
      logger.warning(msg.toString());
      e.fillInStackTrace();
      throw new RuntimeException("Exception during read operation of " + pageID +
                                 "\n" + e.getMessage());
    }
  }

  /**
   * @see PageFile#deleteNode(int)
   */
  protected void deleteNode(int pageID) {
    // put id to empty pages and delete from cache
    super.deleteNode(pageID);

    // delete from file
    ioAccess++;
    try {
      file.seek(headerSize + (pageSize * pageID));
      file.writeInt(EMPTY_PAGE);
    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException("IOException occured ! \n " + e.getMessage());
    }
  }


  /**
   * @see PageFile#increaseRootNode()
   * TODO muss noch implementiert werden
   */
  protected int increaseRootNode() {
    throw new UnsupportedOperationException();
  }

  /**
   * @see PageFile#close()
   */
  protected void close() {
    try {
      file.close();
    }
    catch (IOException e) {
      e.fillInStackTrace();
      throw new RuntimeException("IOException during close()");
    }
    closed = true;
  }

  /**
   * @see Object#finalize()
   */
  protected void finalize() throws Throwable {
    if (!closed)
      file.close();

    super.finalize();
  }

  /**
   * Reads the next MBR from the given DataInputStream ds
   *
   * @param ds the DataInputStream to be read from
   * @return the next MBR
   * @throws IOException
   */
  private MBR readNextMBR(DataInputStream ds) throws IOException {
    double[] point1, point2;
    point1 = new double[this.getDimensionality()];
    point2 = new double[this.getDimensionality()];

    for (int i = 0; i < this.getDimensionality(); i++) {
      point1[i] = ds.readDouble();
    }

    for (int i = 0; i < this.getDimensionality(); i++) {
      point2[i] = ds.readDouble();
    }

    return new MBR(point1, point2);
  }
}
