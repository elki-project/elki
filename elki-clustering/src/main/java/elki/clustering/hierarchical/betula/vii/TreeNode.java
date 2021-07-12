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
package elki.clustering.hierarchical.betula.vii;

import elki.clustering.hierarchical.betula.CFNode;

public final class TreeNode extends ClusteringFeature implements CFNode<ClusteringFeature> {
    /**
     * Children of the TreeNode
     */
    ClusteringFeature[] children;

    /**
     * Constructor
     * 
     * @param dim Dimensionality
     * @param capacity Fanout of the Tree
     */
    public TreeNode(int dim, int capacity) {
        super(dim);
        children = new ClusteringFeature[capacity];
    }

    @Override
    public ClusteringFeature[] getChilds() {
        return children;
    }

    @Override
    public void setChild(int i, ClusteringFeature cf) {
        children[i] = cf;

    }

    @Override
    public ClusteringFeature getChild(int i) {
        return children[i];

    }

    @Override
    public void addToStatistics(int i, ClusteringFeature other) {
        addToStatistics(other);
        children[i] = other;
    }

    @Override
    public void addToStatistics(int i, CFNode<ClusteringFeature> other) {
        addToStatistics(other);
        children[i] = (TreeNode) other;
    }

    @Override
    public boolean addCF(ClusteringFeature other) {
        for(int i = 0; i < children.length; i++) {
            if(children[i] == null) {
                children[i] = other;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addCF(CFNode<ClusteringFeature> other) {
        for(int i = 0; i < children.length; i++) {
            if(children[i] == null) {
                children[i] = (TreeNode) other;
                return true;
            }
        }
        return false;
    }
}
