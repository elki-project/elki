package de.lmu.ifi.dbs.utilities;

import java.util.ArrayList;
import java.util.BitSet;

public class RootTree {

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
	 * The edges of this RootTree.
	 */
	private ArrayList<Integer> edges;

	/**
	 * The children of this node.
	 */
	private ArrayList<RootTree> children;

	private RootTree parent;

	/**
	 * Constructor creating a new RootTree-Object.
	 */
	public RootTree() {
		node = new BitSet();
		rootEdge = -1;
		edges = new ArrayList<Integer>();
		children = new ArrayList<RootTree>();
		rootEdges = new BitSet();
		placeToAdd = 0;
		parent = this;
	}

	/**
	 * Getter for the children of this Object.
	 * 
	 * @return the childrentrees of this
	 */
	public ArrayList<RootTree> getChildren() {
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
	 * Getter
	 * 
	 * @return the node of this tree.
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
	 * @return the child with the rootnode which has the parameter root as
	 *         origin.
	 */
	private RootTree getChild(int root) {
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i).rootEdge == root) {
				return children.get(i);
			}
		}
		return null;
	}

	/**
	 * 
	 * @return the parent of this tree.
	 */
	public RootTree getParent() {
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
	 * @param node node to be added to the tree
	 * @param initialEdge rootEdge leading to the current node.
	 */
	public void insertTree(BitSet edge, BitSet node, int initialEdge) {
		for (int i = edge.nextSetBit(0); i >= 0; i = edge.nextSetBit(i + 1)) {
			RootTree child = this.getChild(i);
			if (child != null) {
				if ((child.rootEdges.cardinality() >= initialEdge)) {
					child.setNode(node);
				}
				edge.clear(i);
				child.insertTree(edge, node, initialEdge);
				return;

			}
			RootTree newTree = new RootTree();
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
