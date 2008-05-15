package de.lmu.ifi.dbs.utilities;

import java.util.ArrayList;
import java.util.BitSet;

public class Tree {

	private int placeToAdd;
	private BitSet node;
	private int rootEdge;
	private BitSet rootEdges;
	private ArrayList<Integer> edges;
	private ArrayList<Tree> children;
	private Tree parent;
//	private int size;

	public Tree() {
		node = new BitSet();
		rootEdge = -1;
		edges = new ArrayList<Integer>();
		children = new ArrayList<Tree>();
		rootEdges = new BitSet();
		// children.add(this);
		placeToAdd = 0;
		parent = this;
//		size = 1;
	}

	public ArrayList<Tree> getChildren(){
		return this.children;
	}
	
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

	public void setNode(BitSet newNode) {
		for (int i = newNode.nextSetBit(0); i >= 0; i = newNode.nextSetBit(i+1)) {
			node.set(i);
		}
	}

	public BitSet getEdgesToRoot() {
		return rootEdges;

	}

	public int getChildCount() {
		return this.children.size();
	}

	public BitSet getNode() {
		return node;
	}

	private void setRootEdge(int edge) {
		this.rootEdge = edge;
	}

	private Tree getChild(int root) {
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i).rootEdge == root) {
				return children.get(i);
			}
		}
		return null;
	}
	
	public Tree getParent(){
		return this.parent;
	}
	

	public void insertTree(BitSet edge, BitSet node, int initialEdge) {
		for (int i = edge.nextSetBit(0); i>=0; i = edge.nextSetBit(i+1)) {
			Tree child = this.getChild(i);
			if (child != null) {
				if ((child.rootEdges.cardinality()>=initialEdge)) {
					child.setNode(node);
				}
					edge.clear(i);
					child.insertTree(edge, node, initialEdge);
					return;
				
			}
				Tree newTree = new Tree();
				newTree.setRootEdge(i);
				this.addEdge(i);
				this.children.add(placeToAdd, newTree);
				newTree.parent = this;
				placeToAdd = this.edges.size();
				// Pfad zum newTree (aus columns)
				newTree.rootEdges = (BitSet)this.rootEdges.clone();
				newTree.rootEdges.set(i);
				if ((newTree.rootEdges.cardinality()>=initialEdge)) {
					newTree.setNode(node);
				}
				edge.clear(i);
				newTree.insertTree(edge, node, initialEdge);
				return;
			

		}
	}
}
