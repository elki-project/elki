/**
 * <p>Parameter handling and option descriptions.</p> 
 * 
 * <ol>
 * <li><b>Option ID</b>: Any parameter <em>must</em> have a {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID OptionID}.<br />
 * These are Singleton objects to uniquely identify the option. They should be "public static". <br />
 * The OptionID specifies the parameter name and a generic description.
 * <p />
 * 
 * Example code:
 * <blockquote><pre>{@code // Defining option IDs
 * public static final OptionID DISTANCE_FUNCTION_ID =
 *   OptionID.getOrCreateOptionID(
 *     "algorithm.distancefunction",
 *     "Distance function to determine the distance between database objects."
 *   ); 
 * }</pre></blockquote>
 * </li>
 * <li><b>Parameter Object</b>: To obtain a value, you <em>must</em> use a
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter Parameter}
 * object. <br />
 * Parameter objects handle <em>parsing</em> of values into the desired type, and various
 * subclasses for common types are provided. It is not desirable to subclass these types
 * too much, since a UI should be able to offer content assistance for input.
 * <p />
 * 
 * Parameters often have types and constraints attached to them, and may be flagged optional
 * or have a default value. Note that a parameter with a default value is by definition optional,
 * so there is no constructor with both a default value and the optional flag.
 * <p />
 * 
 * Due to restrictions imposed by Java Generics,
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter ListParameter} based
 * types do not have the full set of constructors, since a List of Constraints and a List of
 * Default values produce the same signature. In such a signature conflict situation, you can use
 * a full constructor either by giving {@code null} as constraints (and a list of default values) or
 * by giving constraints and setting optional to {@code false}.
 * <p />
 * 
 * Notice the difference between an
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter ObjectParameter} and a
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter ClassParameter}. The first is
 * meant to carry a single object (which as fallback can be initialized with a class, resulting
 * in a new object of that class), the second is meant for a "object factory" use.
 * <p />
 * 
 * Example code:
 * <blockquote><pre>{@code // Defining Parameters
 * protected final ObjectParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM =
 *   new ObjectParameter<DistanceFunction<O, D>>(
 *     DISTANCE_FUNCTION_ID,
 *     DistanceFunction.class,
 *     EuclideanDistanceFunction.class
 *   ); 
 * }</pre></blockquote>
 * </li>
 * <li><b>Initialization</b>: Initialization happens in the constructor, which <em>must</em> have the
 * signature {@code Class(Parameterization config)}.<br />
 * The {@code config} object manages configuration data, whichever source it is coming from
 * (e.g. command line, XML, lists, ...)
 * <p />
 * 
 * The {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization Parameterization}
 * class offers the method
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization#grab grab}
 * , which returns {@code true} when the parameter value is defined <em>and satisfies the given constraints</em>.
 * Note that for {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag Flag} objects,
 * which default to {@code false}, this method will always return {@code true} - there is a valid
 * value available.
 * <p />
 * 
 * Initialization should happen in a delayed-fail way. Failure is managed by the Parameterization object,
 * and not failing immediately allows for reporting all configuration errors (and all options) in a single
 * run. As such, when reporting a configuration error, you should <em>not throw the error</em>, but
 * instead call {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization#reportError reportError}
 * and leave error handling to the Parameterization class. Note that this method <em>will return
 * eventually</em>, so you might need to use try-catch-report blocks.
 * <p />
 * 
 * When writing constructors, try to make error handling as local as possible, to report as many errors
 * at once as possible.
 * <p />
 * 
 * Example code:
 * <blockquote><pre>{@code // Getting parameters
 * protected DistanceBasedAlgorithm(Parameterization config) {
 *   super(config);
 *   if (config.grab(this, DISTANCE_FUNCTION_PARAM)) {
 *     distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
 *   }
 * }
 * }</pre></blockquote>
 * The {@code if config.grab} statement ensures that the parameter was set. Note that the configuration
 * manager is passed on to the child instance.
 * </li>
 * <li><b>Compound conditionals</b>: Sometimes, more than one parameter value is required.
 * However, {@code config.grab(...) && config.grab(...)} is <em>not working</em> as intended, since
 * a negative results in the first config.grab statement will prevent the evaluation of the second.
 * <em>Instead, the following code should be used</em>:
 * <blockquote><pre>{@code // Compound parameter dependency
 *   config.grab(this, FIRST_OPTION);
 *   config.grab(this, SECOND_OPTION);
 *   if (FIRST_OPTION.isDefined() && SECOND_OPTION.isDefined()) {
 *     // Now we have guaranteed value for both available.
 *   }
 * }</pre></blockquote>
 * </li>
 * <li><b>Global Constraints:</b> additional global constraints can be added using
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization#checkConstraint checkConstraint}
 * <p />
 * 
 * Example code:
 * <blockquote><pre>{@code // Global constraints
 *   config.grab(NORMALIZATION_PARAM);
 *   config.grab(NORMALIZATION_UNDO_FLAG);
 *   GlobalParameterConstraint gpc =
 *     new ParameterFlagGlobalConstraint<Class<?>, Class<? extends Normalization<O>>>(
 *       NORMALIZATION_PARAM, null,
 *       NORMALIZATION_UNDO_FLAG, true);
 *   if (config.checkConstraint(gpc)) {
 *     // Code that depends on the constraints being satisfied.
 *   }
 * }</pre></blockquote>
 * <p />
 * 
 * TODO: Much of the constraint functionality can be solved much easier by direct Java code and
 * {@code reportError}. Unless the constraints can be used by a GUI for input assistance, we should
 * consider replacing them with direct code.
 * </li>
 * <li><b>Error reporting</b>:
 * <blockquote><pre>{@code // Proper dealing with errors
 *   try {
 *     // code that might fail with an IO exception
 *   } except(IOException e) {
 *     config.reportError(new WrongParameterValueException(...));
 *   }
 *   // process remaining parameters, to report additional errors. 
 * }</pre></blockquote>
 * </li>
 * <li><b>TODO:</b>:
 * ... document more ... for example internal re-parameterization and parameter tracking.
 * <blockquote><pre>{@code
 * }</pre></blockquote>
 * </li>
 * </ol>
 */
package de.lmu.ifi.dbs.elki.utilities.optionhandling;
