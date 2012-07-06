/**
 * <p>Classes for building and storing the results of distance-based queries</p>
 * 
 * <p>The classes in this package essentially form three groups:
 * <ol>
 * <li>{@link KNNHeap} for <b>building kNN results</b>. It allows adding new
 * candidates (and loses old candidates automatically), but it is not iterable.<br />
 * To get an instance, use {@link KNNUtil#newHeap}!
 * </li>
 * <li>{@link KNNResult} is the <b>final kNN result</b> obtained via {@link KNNHeap#toKNNList}.
 * It is iterable and totally ordered, but can no longer be modified (unless you call
 * {@link KNNUtil.newHeap}!</li>
 * <li>{@link GenericDistanceDBIDList} and the optimized counterpart
 * {@link DoubleDistanceDBIDList}, are <b>modifiable, but not necessarily sorted</b> lists of
 * neighbors, useful for example for range queries.</li>
 * </ol>
 * </p>
 * 
 * <p>Try to choose the most appropriate one! Heaps are optimized for updates but bad for reading,
 * KNNResult is optimized for reading but unmodifiable, and the lists are easy to modify,
 * but less efficient than heaps.</p>
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