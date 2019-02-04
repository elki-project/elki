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
package de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike;

/**
 * Adapter for arrays of numbers, to avoid boxing.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <N>
 *            Number type
 * @param <A>
 *            Array type
 */
public interface NumberArrayAdapter<N extends Number, A> extends ArrayAdapter<N, A> {
	@Override
	int size(A array);

	@Override
	N get(A array, int off) throws IndexOutOfBoundsException;

	/**
	 * Get the off'th item from the array as double.
	 * 
	 * @param array
	 *            Array to get from
	 * @param off
	 *            Offset
	 * @return Item at offset off
	 * @throws IndexOutOfBoundsException
	 *             for an invalid index.
	 */
	double getDouble(A array, int off) throws IndexOutOfBoundsException;

	/**
	 * Get the off'th item from the array as float.
	 * 
	 * @param array
	 *            Array to get from
	 * @param off
	 *            Offset
	 * @return Item at offset off
	 * @throws IndexOutOfBoundsException
	 *             for an invalid index.
	 */
	default float getFloat(A array, int off) throws IndexOutOfBoundsException {
	  return (float) getDouble(array, off);
	}

	/**
	 * Get the off'th item from the array as integer.
	 * 
	 * @param array
	 *            Array to get from
	 * @param off
	 *            Offset
	 * @return Item at offset off
	 * @throws IndexOutOfBoundsException
	 *             for an invalid index.
	 */
	default int getInteger(A array, int off) throws IndexOutOfBoundsException {
    return (int) getLong(array, off);
	}

	/**
	 * Get the off'th item from the array as short.
	 * 
	 * @param array
	 *            Array to get from
	 * @param off
	 *            Offset
	 * @return Item at offset off
	 * @throws IndexOutOfBoundsException
	 *             for an invalid index.
	 */
	default short getShort(A array, int off) throws IndexOutOfBoundsException {
    return (short) getLong(array, off);
	}

	/**
	 * Get the off'th item from the array as long.
	 * 
	 * @param array
	 *            Array to get from
	 * @param off
	 *            Offset
	 * @return Item at offset off
	 * @throws IndexOutOfBoundsException
	 *             for an invalid index.
	 */
	long getLong(A array, int off) throws IndexOutOfBoundsException;

	/**
	 * Get the off'th item from the array as byte.
	 * 
	 * @param array
	 *            Array to get from
	 * @param off
	 *            Offset
	 * @return Item at offset off
	 * @throws IndexOutOfBoundsException
	 *             for an invalid index.
	 */
	default byte getByte(A array, int off) throws IndexOutOfBoundsException {
		return (byte) getLong(array, off);
	}
}