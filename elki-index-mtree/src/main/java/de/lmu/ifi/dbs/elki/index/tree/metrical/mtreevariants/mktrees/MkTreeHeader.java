/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

import java.io.IOException;
import java.io.RandomAccessFile;

import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;

/**
 * Encapsulates the header information for subclasses of
 * {@link de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnified}.
 * This information is needed for persistent storage.
 *
 * @author Elke Achtert
 * @since 0.2
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
     */
    @Override
    public int size() {
        return super.size() + SIZE;
    }
}
