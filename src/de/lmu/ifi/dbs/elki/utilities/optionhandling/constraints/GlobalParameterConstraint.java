package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * <p>Interface for specifying global parameter constraints, i.e. constraints addressing
 * several parameters.
 * </p>
 * <p>Each class specifying a global parameter constraint should implement this interface.
 * The parameters to be tested should be defined as private attributes and should be
 * initialized in the respective constructor of the class, i.e. they are parameters of the constructor.
 * The proper constraint test should be implemented in the method {@link #test()}. 
 * </p>
 * @author Steffi Wanka
 *
 */
public interface GlobalParameterConstraint {
	
	/**
	 * Checks if the respective parameters satisfy the parameter constraint. If not,
	 * a parameter exception is thrown.
	 * 
	 * @throws ParameterException if the parameters don't satisfy the parameter constraint.
	 */
	public abstract void test() throws ParameterException;

    /**
     * Returns a description of this global constraint.
     *
     * @return a description of this global constraint
     */
    public abstract String getDescription();
	
}
