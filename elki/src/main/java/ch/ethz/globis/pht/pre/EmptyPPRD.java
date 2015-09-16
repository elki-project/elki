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

import ch.ethz.globis.pht.util.BitTools;

public class EmptyPPRD implements PreProcessorRangeD {

  @Override
  public void pre(double[] raw1, double[] raw2, long[] pre) {
    final int pDIM = raw1.length;
    for (int d=0; d<pDIM; d++) {
      pre[d] = BitTools.toSortableLong(raw1[d]);
      pre[d+pDIM] = BitTools.toSortableLong(raw2[d]);
    }
  }

  @Override
  public void post(long[] pre, double[] post1, double[] post2) {
    final int pDIM = post1.length;
    for (int d=0; d<pDIM; d++) {
      post1[d] = BitTools.toDouble(pre[d]);
      post2[d] = BitTools.toDouble(pre[d+pDIM]);
    }
  }

}
