/**
 * <p>Classes for building and storing the results of distance-based queries</p>
 * 
 * <p>The classes in this package essentially form three groups:
 * <ol>
 * <li>{@link de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNHeap} for <b>building kNN results</b>.
 * It allows adding new candidates (and loses old candidates automatically), but it is not iterable.<br />
 * To get an instance, use {@link de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNUtil#newHeap}!
 * </li>
 * <li>{@link de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult} is the <b>final kNN result</b>
 * obtained by serializing a heap via {@link de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNHeap#toKNNList}.
 * It is iterable and totally ordered, but can no longer be modified (unless you call
 * {@link de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNUtil#newHeap}!</li>
 * <li>{@link de.lmu.ifi.dbs.elki.distance.distanceresultlist.GenericDistanceDBIDList} and the optimized
 * counterpart {@link de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceDBIDList}, are
 * <b>modifiable, but not necessarily sorted</b> lists of neighbors, useful for example for range queries.</li>
 * </ol>
 * </p>
 * 
 * <p>Try to choose the most appropriate one! Heaps are optimized for updates but bad for reading,
 * KNNResult is optimized for reading but unmodifiable, and the lists are easy to modify,
 * but less efficient than heaps.</p>
 * 
 * @apiviz.exclude java.util.*
 * @apiviz.exclude elki.database.query.*
 * @apiviz.exclude elki.database.ids.DBIDIter
 * @apiviz.exclude elki.database.ids.DBIDs
 * @apiviz.exclude KNNUtil.DistanceItr
 */
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.distance.distanceresultlist;