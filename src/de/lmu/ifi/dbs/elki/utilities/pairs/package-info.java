/**
 * <p>Pairs and triples utility classes.</p>
 * <p>Pairs and Triples are frequently used classes and reimplemented too often.</p>
 * 
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.Pair Pair} is the generic non-comparable pair.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.FCPair FCPair} is the generic pair comparable in the first component only.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.CPair FCPair} is the pair comparable in both components.</li>
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
 */
package de.lmu.ifi.dbs.elki.utilities.pairs;