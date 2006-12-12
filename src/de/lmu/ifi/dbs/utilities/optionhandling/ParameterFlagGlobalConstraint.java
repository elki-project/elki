package de.lmu.ifi.dbs.utilities.optionhandling;

import java.util.List;

public class ParameterFlagGlobalConstraint implements GlobalParameterConstraint {

	private Parameter param;

	private Flag flag;

	private boolean flagConstraint;

	private List<ParameterConstraint> cons;

	public ParameterFlagGlobalConstraint(Parameter p, List<ParameterConstraint> c, Flag f, boolean flagConstraint) {
		param = p;
		flag = f;
		this.flagConstraint = flagConstraint;
		cons = c;
	}

	public void test() throws ParameterException {

		if (flagConstraint) {
			// only check constraints of param if flag is set
			if (flag.isSet()) {

				for (ParameterConstraint c : cons) {
					param.checkConstraint(c);
				}
			}
		} else {
			if (!flag.isSet()) {
				for (ParameterConstraint c : cons) {
					param.checkConstraint(c);
				}
			}
		}
	}

}
