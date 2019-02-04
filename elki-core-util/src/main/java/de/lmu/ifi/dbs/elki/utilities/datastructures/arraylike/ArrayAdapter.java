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
 * Adapter for array-like things. For example, arrays and lists.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <T>
 *            Item type
 * @param <A>
 *            Array object type
 */
public interface ArrayAdapter<T, A> {
	/**
	 * Get the size of the array.
	 * 
	 * @param array
	 *            Array-like thing
	 * @return Size
	 */
	int size(A array);

	/**
	 * Get the off'th item from the array.
	 * 
	 * @param array
	 *            Array to get from
	 * @param off
	 *            Offset
	 * @return Item at offset off
	 * @throws IndexOutOfBoundsException
	 *             for an invalid index.
	 */
	T get(A array, int off) throws IndexOutOfBoundsException;
}