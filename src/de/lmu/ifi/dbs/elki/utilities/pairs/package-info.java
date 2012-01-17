/**
 * <p>Pairs and triples utility classes.</p>
 * <p>Pairs and triples are frequently used classes and reimplemented too often.</p>
 * 
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.Pair Pair} is the generic <em>non-comparable</em> pair (you can use external comparators!).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.FCPair FCPair} is the generic pair comparable in the <em>first</em> component only.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.CPair CPair} is the pair comparable in <em>both</em> components.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.SCPair SCPair} is the generic pair comparable in the <em>second</em> component only.</li>
 * </ul>
 * 
 * <p>Due to limitations in object subclassing, {@link de.lmu.ifi.dbs.elki.utilities.pairs.CPair CPair} cannot be
 * a subclass of {@link de.lmu.ifi.dbs.elki.utilities.pairs.FCPair FCPair}, since a class cannot implement
 * the Comparable interface twice.</p>
 * 
 * <p>Also primitive types cannot be used in Generics, resulting in the following classes for primitive types:</p>
 * 
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair IntIntPair} storing two <code>int</code> values</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair DoubleIntPair} storing one <code>double</code> and one <code>int</code> value.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.IntDoublePair IntDoublePair} storing one <code>int</code> and one <code>double</code> value.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair DoubleDoublePair} storing two <code>double</code> values</li>
 * </ul>
 * 
 * <p>Triples can be used via:</p>
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.Triple Triple} is the generic non-comparable triple.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.CTriple CTriple} is the triple comparable in <em>all</em> components.</li>
 * </ul>
 * 
 * <p>If you need a triple comparable in just particular components, either define a comparator for sorting
 * or subclass Triple appropriately.</p>
 * 
 * @apiviz.exclude java.lang.
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
package de.lmu.ifi.dbs.elki.utilities.pairs;