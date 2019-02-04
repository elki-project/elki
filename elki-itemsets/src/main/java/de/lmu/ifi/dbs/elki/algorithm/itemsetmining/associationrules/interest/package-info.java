/**
 * Association rule interestingness measures.
 * <p>
 * Much of the confusion with these measures arises from the anti-monotonicity
 * of itemsets, which are omnipresent in the literature.
 * <p>
 * In the itemset notation, the itemset \(X\) denotes the set of matching
 * transactions \(\{T|X\subseteq T\}\) that contain the itemset \(X\).
 * If we enlarge \(Z=X\cup Y\), the resulting set shrinks:
 * \(\{T|Z\subseteq T\}=\{T|X\subseteq T\}\cap\{T|Y\subseteq T\}\).
 * <p>
 * Because of this: \(\text{support}(X\cup Y) = P(X \cap Y)\)
 * and \(\text{support}(X\cap Y) = P(X \cup Y)\). With "support" and
 * "confidence", it is common to see the reversed semantics (the union on the
 * constraints is the intersection on the matches, and conversely); with
 * probabilities it is common to use "events" as in frequentist inference.
 * <p>
 * To make things worse, the "support" is sometimes in absolute (integer)
 * counts, and sometimes used in a relative share.
 *
 * @opt include .*elki.algorithm.itemsetmining.associationrules.AssociationRuleGeneration
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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules.interest;
