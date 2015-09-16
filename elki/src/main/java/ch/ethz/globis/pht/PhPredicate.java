package ch.ethz.globis.pht;

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

import java.io.Serializable;

/**
 * A predicate class that can for example be used to filter query results before they are returned.
 *
 * This interface needs to be serializable because in the distributed version of the PhTree, it is send
 * from the client machine to the server machine.
 *
 * @author ztilmann
 */
public interface PhPredicate extends Serializable {

	PhPredicate ACCEPT_ALL = new PhPredicate() {
    
    /**  */
    private static final long serialVersionUID = 1L;

    @Override
    public boolean test(long[] point) {
      return true;
    }
  };

	boolean test(long[] point);
	
}