package ch.ethz.globis.pht.pre;

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

public class IntegerPP implements PreProcessorPoint {

	private final double preMult;
	private final double postMult;
	
	public IntegerPP(double multiplyer) {
		preMult = multiplyer;
		postMult = 1./multiplyer;
	}
	
	@Override
	public void pre(double[] raw, long[] pre) {
		for (int d=0; d<raw.length; d++) {
			pre[d] = (long) (raw[d] * preMult);
		}
	}

	@Override
	public void post(long[] pre, double[] post) {
		for (int d=0; d<pre.length; d++) {
			post[d] = pre[d] * postMult;
		}
	}

}
