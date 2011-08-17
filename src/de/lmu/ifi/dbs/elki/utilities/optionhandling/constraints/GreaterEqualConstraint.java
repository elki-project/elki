package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.NumberParameter;

/**
 * Represents a Greater-Equal-Than-Number parameter constraint. The
 * value of the number parameter ({@link NumberParameter})
 * tested has to be greater equal than the specified
 * constraint value.
 *
 * @author Steffi Wanka
 */
public class GreaterEqualConstraint extends AbstractNumberConstraint<Number> {
    /**
     * Creates a Greater-Equal parameter constraint.
     * <p/>
     * That is, the value of the number
     * parameter given has to be greater equal than the constraint value given.
     *
     * @param constraintValue the constraint value
     */
    public GreaterEqualConstraint(Number constraintValue) {
        super(constraintValue);
    }

    /**
     * Checks if the number value given by the number parameter is
     * greater equal than the constraint
     * value. If not, a parameter exception is thrown.
     *
     */
    @Override
    public void test(Number t) throws ParameterException {
        if (t.doubleValue() < constraintValue.doubleValue()) {
            throw new WrongParameterValueException("Parameter Constraint Error: \n"
                + "The parameter value specified has to be greater equal than "
                + constraintValue.toString() +
                ". (current value: " + t.doubleValue() + ")\n");
        }
    }

    @Override
    public String getDescription(String parameterName) {
        return parameterName + " >= " + constraintValue;
    }

}
