package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Encapsulates the header information for subclasses of
 * {@link de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree}.
 * This information is needed for persistent storage.
 *
 * @author Elke Achtert
 */
public class MkTreeHeader extends TreeIndexHeader {

    /**
     * The size of this header in Bytes,
     * which is 4 Bytes (for {@link #k_max}).
     */
    private static int SIZE = 4;

    /**
     * The maximum number k of reverse kNN queries to be supported.
     */
    private int k_max;

    /**
     * Empty constructor for serialization.
     */
    public MkTreeHeader() {
        super();
    }

    /**
     * Creates a nerw header with the specified parameters.
     *
     * @param pageSize     the size of a page in bytes
     * @param dirCapacity  the capacity of a directory node
     * @param leafCapacity the capacity of a leaf node
     * @param k_max        the parameter k
     */
    public MkTreeHeader(int pageSize, int dirCapacity, int leafCapacity, int k_max) {
        super(pageSize, dirCapacity, leafCapacity, 0, 0);
        this.k_max = k_max;
    }

    /**
     * Initializes this header from the specified file.
     * Calls {@link de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader#readHeader(java.io.RandomAccessFile)
     * TreeIndexHeader#readHeader(file)} and reads additionally the integer value of
     * {@link #k_max}
     * from the file.
     *
     * @see de.lmu.ifi.dbs.elki.persistent.PageHeader#readHeader(java.io.RandomAccessFile)
     */
    @Override
    public void readHeader(RandomAccessFile file) throws IOException {
        super.readHeader(file);
        this.k_max = file.readInt();
    }

    /**
     * Writes this header to the specified file.
     * Calls {@link de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader#writeHeader(java.io.RandomAccessFile)}
     * and writes additionally the integer value of
     * {@link #k_max}
     * to the file.
     *
     * @see de.lmu.ifi.dbs.elki.persistent.PageHeader#writeHeader(java.io.RandomAccessFile)
     */
    @Override
    public void writeHeader(RandomAccessFile file) throws IOException {
        super.writeHeader(file);
        file.writeInt(this.k_max);
    }

    /**
     * Returns the parameter k.
     *
     * @return the parameter k
     */
    public int getK_max() {
        return k_max;
    }

    /**
     * Returns {@link de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader#size()}
     * plus the value of {@link #SIZE}).
     *
     * @see de.lmu.ifi.dbs.elki.persistent.PageHeader#size()
     */
    @Override
    public int size() {
        return super.size() + SIZE;
    }
}
