/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
package elki.clustering.hierarchical.betula;

/**
 * Interface for CFModel
 * 
 * @author Andreas Lang
 * 
 * @param <L> CF Model
 * @param <N> TreeNode Model
 */
public interface CFModel<L extends CFInterface> {

    /**
     * Creates a CF Feature of the corresponding Model.
     * 
     * @param d Dimensionality
     * @return
     */
    public L make(int d);

    /**
     * Creates a TreeNode of the corresponding Model.
     * 
     * @param d Dimensionality
     * @param capacity Fanout
     * @return
     */
    public CFNode<L> treeNode(int d, int capacity);

    /**
     * The selected distance Function
     * 
     * @return distance Function
     */
    public CFDistance<L> distance();

    /**
     * The selected absorption Criteria
     * 
     * @return distance Function
     */
    public CFDistance<L> absorption();

}
