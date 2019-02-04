/**
 * ELKI Iterator API
 * <p>
 * ELKI uses a custom iterator API instead of the usual
 * {@link java.util.Iterator} classes (the "Java Collections API").
 * The reason for this is largely efficiency. Benchmarking showed that the Java
 * Iterator API can be quite expensive when dealing with primitive types, as
 * {@link java.util.Iterator#next} is meant to always return an object.
 * <p>
 * However, the benefits become more apparent when considering multi-valued
 * iterators. For example an iterator over a k nearest neighbor set in ELKI both
 * represents an object by its DBID, and a distance value. For double-valued
 * distances, it can be retrieved using a primitive value getter (saving an
 * extra object copy), and since the iterator can be used as a DBIDRef, it can
 * also represent the current object without creating additional objects.
 * <p>
 * While it may seem odd to depart from Java conventions such as the collections
 * API, note that these iterators are very close to the standard C++
 * conventions, so nothing entirely unusual.
 */
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
package de.lmu.ifi.dbs.elki.utilities.datastructures.iterator;
