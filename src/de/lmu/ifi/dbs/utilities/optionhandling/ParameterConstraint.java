package de.lmu.ifi.dbs.utilities.optionhandling;

public interface ParameterConstraint<T> {

	void test(T t) throws ParameterException;
}
