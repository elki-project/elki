/**
 * Pairs utility classes
 * <p>
 * A number of commonly needed primitive pairs are the following:
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair IntIntPair} storing
 * two <code>int</code> values</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair DoubleIntPair}
 * storing one <code>double</code> and one <code>int</code> value.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.IntDoublePair IntDoublePair}
 * storing one <code>int</code> and one <code>double</code> value.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair
 * DoubleDoublePair} storing two <code>double</code> values</li>
 * </ul>
 * Why no more {@code Pair<A,B>}?
 * <ul>
 * <li>Because such pairs are expensive in Java when using primitive types.</li>
 * <li>Because domain-specific code can often be optimized better by the HotSpot
 * VM.</li>
 * </ul>
 * 
 * @opt hide java.lang.
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
package de.lmu.ifi.dbs.elki.utilities.pairs;
