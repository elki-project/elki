package ch.ethz.globis.pht.v8;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011-2015
Eidgenössische Technische Hochschule Zürich (ETH Zurich)
Institute for Information Systems
GlobIS Group

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

import java.util.NoSuchElementException;

import ch.ethz.globis.pht.PhEntry;
import ch.ethz.globis.pht.PhTree.PhQuery;
import ch.ethz.globis.pht.PhTreeHelper;

/**
 * This PhIterator uses a loop instead of recursion in findNextElement();. 
 * It also reuses PhEntry objects to avoid unnecessary creation of objects.
 * Calls to next() and nextKey() will result in creation of new PhEntry and long[] objects
 * respectively to maintain expected behaviour. However, the nextEntryUnstable() method
 * returns the internal PhEntry without creating any new objects. The returned PhEntry and long[]
 * are valid until the next call to nextXXX().
 * 
 * @author ztilmann
 *
 * @param <T>
 */
public final class PhIteratorNoGC<T> implements PhQuery<T> {

	private class PhIteratorStack {
		private final NodeIteratorNoGC<T>[] stack;
		private int size = 0;
		
		@SuppressWarnings("unchecked")
		public PhIteratorStack() {
			stack = new NodeIteratorNoGC[PhTree8.DEPTH_64];
		}

		public boolean isEmpty() {
			return size == 0;
		}

		public boolean prepare(Node<T> node) {
			if (!PhTree8.checkAndApplyInfix(node, valTemplate, rangeMin, rangeMax)) {
				STAT_NODES_PREFIX_FAILED++;
				return false;
			}

			NodeIteratorNoGC<T> ni = stack[size++];
			if (ni == null)  {
				ni = new NodeIteratorNoGC<>(DIM, valTemplate);
				stack[size-1] = ni;
			}
			
			ni.init(rangeMin, rangeMax, valTemplate, node);
			return true;
		}

		public NodeIteratorNoGC<T> peek() {
			return stack[size-1];
		}

		public NodeIteratorNoGC<T> pop() {
			return stack[--size];
		}
	}

	public static int STAT_NODES_CHECKED = 0;
	public static int STAT_NODES_IGNORED = 0;
	public static int STAT_NODES_PREFIX_FAILED = 0;
	public static int STAT_NODES_EARLY_IRE_CHECK = 0;
	public static int STAT_NODES_EARLY_IRE_ABORT_I = 0;
	public static int STAT_NODES_EARLY_IRE_ABORT_E = 0;
	public static long MBB_TIME = 0;
	
	private final int DIM;
	private final PhIteratorStack stack;
	private final long[] valTemplate;
	private long[] rangeMin;
	private long[] rangeMax;
	private final PhTree8<T> pht;
	
	private PhEntry<T> result;
	boolean isFinished = false;
	
	public PhIteratorNoGC(PhTree8<T> pht, int DIM) {
		this.DIM = DIM;
		this.stack = new PhIteratorStack();
		this.valTemplate = new long[DIM];
		this.pht = pht;
	}	
		
	public void reset(long[] rangeMin, long[] rangeMax) {	
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		this.stack.size = 0;
		this.isFinished = false;
		
		if (pht.getRoot() == null) {
			//empty index
			isFinished = true;
			return;
		}
		
		if (stack.prepare(pht.getRoot())) {
			findNextElement();
		} else {
			isFinished = true;
		}
	}

	private void findNextElement() {
		stackLoop:
		while (!stack.isEmpty()) {
			NodeIteratorNoGC<T> p = stack.peek();
			while (p.increment()) {
				if (p.isNextSub()) {
					//leave this here. We could move applyToArrayPos somewhere else, but we have to
					//take care that it is only applied AFTER the previous subNodes has been traversed,
					//otherwise we may mess up the valTemplate which is used in the previous Subnode.
					PhTreeHelper.applyHcPos(p.getCurrentPos(), p.node().getPostLen(), valTemplate);
					if (stack.prepare(p.getCurrentSubNode())) {
						continue stackLoop;
					} else {
						// infix comparison failed or node has no matching entries
						continue;
					}
				} else {
					result = p.getCurrentPost();
					return;
				}
			}
			// no matching (more) elements found
			stack.pop();
		}
		//finished
		isFinished = true;
	}
	
	@Override
	public long[] nextKey() {
		long[] key = nextEntryUnstable().getKey();
		long[] ret = new long[key.length];
		System.arraycopy(key, 0, ret, 0, key.length);
		return ret;
	}

	@Override
	public T nextValue() {
		return nextEntryUnstable().getValue();
	}

	@Override
	public boolean hasNext() {
		return !isFinished;
	}

	@Override
	public PhEntry<T> nextEntry() {
		PhEntry<T> internal = nextEntryUnstable();
		PhEntry<T> e = new PhEntry<T>(internal.getKey().clone(), internal.getValue());
		return e;
	}
	
	@Override
	public T next() {
		return nextEntryUnstable().getValue();
	}

	/**
	 * Special 'next' method that avoids creating new objects internally by reusing Entry objects.
	 * Advantage: Should completely avoid any GC effort.
	 * Disadvantage: Returned PhEntries are not stable and are only valid until the
	 * next call to next(). After that they may change state. Modifying returned entries may
	 * invalidate the backing tree.
	 * @return The next entry
	 */
	public PhEntry<T> nextEntryUnstable() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		PhEntry<T> ret = result;
		findNextElement();
		return ret;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
}