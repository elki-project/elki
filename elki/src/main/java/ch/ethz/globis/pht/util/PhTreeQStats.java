/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.util;

import java.util.Arrays;

/**
 * Quality stats related to data characteristics and tree quality.
 */
public final class PhTreeQStats {
	final int DEPTH;
	public int nNodes;
	public int nHCP;
	public int nHCS;
	public int nNI;
	public int q_totalDepth;
	public int[] q_nPostFixN;  //filled with  x[currentDepth] = nPost;
	public int[] infixHist = new int[64];  //prefix len
	public int[] nodeDepthHist = new int[64];  //prefix len
	public int[] nodeSizeLogHist = new int[32];  //log (nEntries)
	public PhTreeQStats(int DEPTH) {
		this.DEPTH = DEPTH;
		this.q_nPostFixN = new int[DEPTH];
	}
	@Override
	public String toString() {
		StringBuilderLn r = new StringBuilderLn();
		r.appendLn("  nNodes = " + nNodes);
		r.appendLn("  avgNodeDepth = " + (double)q_totalDepth/(double)nNodes); 
		//            "  noPostChildren=" + q_nPostFix1 + "\n" +
		r.appendLn("  postHC=" + nHCP + "  subHC=" + nHCS);
		double apl = getAvgPostlen(r);
		r.appendLn("  avgPostLen = " + apl + " (" + (DEPTH-apl) + ")");

		return r.toString();
	}
	public String toStringHist() {
		StringBuilderLn r = new StringBuilderLn();
		r.appendLn("  infixLen      = " + Arrays.toString(infixHist));
		r.appendLn("  nodeSizeLog   = " + Arrays.toString(nodeSizeLogHist));
		r.appendLn("  nodeDepthHist = " + Arrays.toString(nodeDepthHist));
		r.appendLn("  depthHist     = " + Arrays.toString(q_nPostFixN));
		return r.toString();
	}
	/**
	 * 
	 * @param r
	 * @return average postLen, including the HC/LHC bit.
	 */
	public double getAvgPostlen(StringBuilderLn r) {
		long total = 0;
		int nEntry = 0;
		for (int i = 0; i < DEPTH; i++) {
			if (r!=null) {
				//r.appendLn("  depth= " + i + "  n= " + q_nPostFixN[i]);
			}
			total += (DEPTH-i)*(long)q_nPostFixN[i];
			nEntry += q_nPostFixN[i];
		}
		return total/(double)nEntry;
	}
	public int getNodeCount() {
		return nNodes;
	}
	public int getPostHcCount() {
		return nHCP;
	}
	public int getSubHcCount() {
		return nHCS;
	}
	public int getSubPostNiCount() {
		return nNI;
	}

}