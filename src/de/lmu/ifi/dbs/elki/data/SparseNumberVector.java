package de.lmu.ifi.dbs.elki.data;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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


/**
 * Combines the SparseFeatureVector and NumberVector
 * 
 * @author Erich Schubert
 *
 * @param <V>
 * @param <N>
 */
public interface SparseNumberVector<V extends SparseNumberVector<V, N>, N extends Number> extends NumberVector<V, N>, SparseFeatureVector<V, N> {
  // Empty combination interface
}
