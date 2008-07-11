package de.lmu.ifi.dbs.elki.algorithm.clustering.biclustering;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * Provides a treeStructure with BitSets as nodes and Integers as edges. Every
 * node of the tree has a path to the root, defined trough the edges leading
 * from the node to the root. This class is used by PClustering as a
 * clusterTree, the edges representing columns and the nodes representing rows.
 * Every path from the root to some node of predefined depth, forms a bicluster
 * with that node.
 * 
 * @author Noemi Andor
 * 
 */
public class BiclusteringTree {

	/**
	 * Specifies the position in which a child should be added.
	 */
	private int placeToAdd;

	/**
	 * The treeNode currently worked at.
	 */
	private BitSet node;

	/**
	 * The edge leading from the previous node to the current node.
	 */
	private int rootEdge;

	/**
	 * The edges leading from the current node to the root of the tree.
	 */
	private BitSet rootEdges;

	/**
	 * The edges of this node.
	 */
	private ArrayList<Integer> edges;

	/**
	 * The children of this node.
	 */
	private ArrayList<BiclusteringTree> children;

	/**
	 * The parent node of this node.
	 */
	private BiclusteringTree parent;

	/**
	 * Constructor creating a new RootTree-Object. A node contains of a BitSet
	 * clustered to the edges leading to that node. Every edge is represented by
	 * an Integer-value and the edges belonging to same node are inserted in
	 * ascending order. All edges belonging to the same node are pairwise diverse.
	 * Each node has a single rootEdge, which is one of the edges of the parent-node.
	 * The rootEdge of the root has the value -1.
	 */
	public BiclusteringTree() {
		node = new BitSet();
		rootEdge = -1;
		edges = new ArrayList<Integer>();
		children = new ArrayList<BiclusteringTree>();
		rootEdges = new BitSet();
		placeToAdd = 0;
		parent = this;
	}

	/**
	 * Getter for the children of this Object.
	 * 
	 * @return the childrenTrees of this object.
	 */
	public ArrayList<BiclusteringTree> getChildren() {
		return this.children;
	}

	/**
	 * Adds an edge dependent on its value in ascending order to the List of
	 * edges, if it does not already contain this edge. If the list is empty,
	 * the edge is added on position 0 of the list.
	 * 
	 * @param edge
	 *            new edge to be added to the list of edges.
	 */
	private void addEdge(int edge) {
		if (this.edges.contains(edge)) {
			return;
		}
		for (int i = 0; i < edges.size(); i++) {
			if (edges.get(i).compareTo(edge) >= 0) {
				edges.add(i, edge);
				placeToAdd = i;
				return;
			}
		}
		edges.add(edge);
	}

	/**
	 * Expands this node with the entries of newNode.
	 * 
	 * @param newNode
	 *            new node to be added to this node.
	 */
	public void setNode(BitSet newNode) {
		// node.or(newNode);
		for (int i = newNode.nextSetBit(0); i >= 0; i = newNode
				.nextSetBit(i + 1)) {
			node.set(i);
		}
	}

	/**
	 * Getter for the edges leading from this node to the root of the tree.
	 * 
	 * @return BitSet of edges leading to the root.
	 */
	public BitSet getEdgesToRoot() {
		return rootEdges;

	}

	/**
	 * Getter for the number of children of this tree.
	 * 
	 * @return the number of children of this tree.
	 */
	public int getChildCount() {
		return this.children.size();
	}

	/**
	 * A node contains a subset of rows belonging to a potential bicluster, such
	 * that the edges (representing columns) leading to that node may form a
	 * bicluster with some of the rows in the node. A node is always empty if
	 * the number of rootedges is less then the minimal number of columns a
	 * bicluster must contain.
	 * 
	 * @return the node of this tree, representing potential rows of a potential
	 *         bicluster.
	 */
	public BitSet getNode() {
		return node;
	}

	/**
	 * Adds edge to the list of rootEdges.
	 * 
	 * @param edge
	 *            new edge to be added to the rootEdges.
	 */
	private void setRootEdge(int edge) {
		this.rootEdge = edge;
	}

	/**
	 * Getter for the child which has the node as root, whose rootEdge is
	 * parameter root.
	 * 
	 * @param root
	 *            edge which may lead to the rootNode of some child of this
	 *            tree.
	 * @return the child with the rootNode which has the parameter root as
	 *         origin.
	 */
	private BiclusteringTree getChild(int root) {
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i).rootEdge == root) {
				return children.get(i);
			}
		}
		return null;
	}

	/**
	 * Returns the tree with the parent-node as root. The parent-node has the
	 * actual rootEdge of this tree as one of its edges.
	 * 
	 * @return the tree containing the parent-node of the current root as root.
	 */
	public BiclusteringTree getParent() {
		return this.parent;
	}

	/**
	 * Inserts a new node to the tree by following the edges of the tree given
	 * by parameter edge. If the tree does not contain the edgePath given by
	 * edge, this path is created. The node is inserted at the and of the path
	 * given by edge.
	 * 
	 * @param edge
	 *            a BitSet of edges indicating the path on which end the node
	 *            should be inserted
	 * @param node
	 *            node to be added to the tree
	 * @param initialEdge
	 *            rootEdge leading to the current node.
	 */
	public void insertTree(BitSet edge, BitSet node, int initialEdge) {
		for (int i = edge.nextSetBit(0); i >= 0; i = edge.nextSetBit(i + 1)) {
			BiclusteringTree child = this.getChild(i);
			if (child != null) {
				if ((child.rootEdges.cardinality() >= initialEdge)) {
					child.setNode(node);
				}
				edge.clear(i);
				child.insertTree(edge, node, initialEdge);
				return;

			}
			BiclusteringTree newTree = new BiclusteringTree();
			newTree.setRootEdge(i);
			this.addEdge(i);
			this.children.add(placeToAdd, newTree);
			newTree.parent = this;
			placeToAdd = this.edges.size();
			// Path to the newTree (consisting columns)
			newTree.rootEdges = (BitSet) this.rootEdges.clone();
			newTree.rootEdges.set(i);
			if ((newTree.rootEdges.cardinality() >= initialEdge)) {
				newTree.setNode(node);
			}
			edge.clear(i);
			newTree.insertTree(edge, node, initialEdge);
			return;

		}
	}
}
