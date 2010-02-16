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
 * <blockquote><pre>{@code  // Defining option IDs
 * public static final OptionID DISTANCE_FUNCTION_ID =
 *   OptionID.getOrCreateOptionID(
 *     "algorithm.distancefunction",
 *     "Distance function to determine the distance between database objects."
 *   ); 
 * }</pre></blockquote>
 * (This example is from {@link de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm DistanceBasedAlgorithm}.)
 * </li>
 * 
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
 * <blockquote><pre>{@code  // Defining Parameters
 * protected final ObjectParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM =
 *   new ObjectParameter<DistanceFunction<O, D>>(
 *     DISTANCE_FUNCTION_ID,
 *     DistanceFunction.class,
 *     EuclideanDistanceFunction.class
 *   ); 
 * }</pre></blockquote>
 * (This example is from {@link de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm DistanceBasedAlgorithm}.)
 * </li>
 * 
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
 * <blockquote><pre>{@code  // Getting parameters
 * protected DistanceBasedAlgorithm(Parameterization config) {
 *   super(config);
 *   if (config.grab(this, DISTANCE_FUNCTION_PARAM)) {
 *     distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
 *   }
 * }
 * }</pre></blockquote>
 * (This example is from {@link de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm DistanceBasedAlgorithm}.)
 * <p/>
 * 
 * The {@code if config.grab} statement ensures that the parameter was set. Note that the configuration
 * manager is passed on to the child instance.
 * </li>
 * 
 * <li><b>Compound conditionals</b>: Sometimes, more than one parameter value is required.
 * However, {@code config.grab(...) && config.grab(...)} is <em>not working</em> as intended, since
 * a negative results in the first config.grab statement will prevent the evaluation of the second.
 * <em>Instead, the following code should be used</em>:
 * <blockquote><pre>{@code // Compound parameter dependency
 * config.grab(this, FIRST_OPTION);
 * config.grab(this, SECOND_OPTION);
 * if (FIRST_OPTION.isDefined() && SECOND_OPTION.isDefined()) {
 *   // Now we have guaranteed value for both available.
 * }
 * }</pre></blockquote>
 * </li>
 * 
 * <li><b>Global Constraints:</b> additional global constraints can be added using
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization#checkConstraint checkConstraint}
 * <p />
 * 
 * Example code:
 * <blockquote><pre>{@code  // Global constraints
 * config.grab(NORMALIZATION_PARAM);
 * config.grab(NORMALIZATION_UNDO_FLAG);
 * GlobalParameterConstraint gpc =
 *   new ParameterFlagGlobalConstraint<Class<?>, Class<? extends Normalization<O>>>(
 *     NORMALIZATION_PARAM, null,
 *     NORMALIZATION_UNDO_FLAG, true);
 * if (config.checkConstraint(gpc)) {
 *   // Code that depends on the constraints being satisfied.
 * }
 * }</pre></blockquote>
 * (This example is from {@link de.lmu.ifi.dbs.elki.KDDTask KDDTask}.)
 * <p />
 * 
 * TODO: Much of the constraint functionality can be solved much easier by direct Java code and
 * {@code reportError}. Unless the constraints can be used by a GUI for input assistance, we should
 * consider replacing them with direct code.
 * </li>
 * 
 * <li><b>Error reporting</b>:
 * <blockquote><pre>{@code  // Proper dealing with errors
 * try {
 *   // code that might fail with an IO exception
 * } except(IOException e) {
 *   config.reportError(new WrongParameterValueException(...));
 * }
 * // process remaining parameters, to report additional errors. 
 * }</pre></blockquote>
 * </li>
 * 
 * <li><b>Command line parameterization</b>:
 * Command line parameters are handled by the class
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization SerializedParameterization}
 * which provided convenient constructors from String arrays:
 * <blockquote><pre>{@code  // Use command line parameters
 * SerializedParameterization params = new SerializedParameterization(args);
 * }</pre></blockquote>
 * (This example is from {@link de.lmu.ifi.dbs.elki.application.AbstractApplication AbstractApplication}.)
 * </li>
 * 
 * <li><b>Internal Parameterization</b>:
 * Often one algorithm will need to call another algorithm, with specific parameters.
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization ListParameterization}
 * offers convenience function for this that do not require String serialization.
 * <blockquote><pre>{@code  // Internal parameterization
 * ListParameterization parameters = new ListParameterization();
 *
 * parameters.addParameter(PCAFilteredRunner.PCA_EIGENPAIR_FILTER, FirstNEigenPairFilter.class);
 * parameters.addParameter(FirstNEigenPairFilter.EIGENPAIR_FILTER_N, correlationDimension);
 * }</pre></blockquote>
 * (This example is from {@link de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ERiC ERiC}.)
 * </li>
 * 
 * <li><b>Combined parameterization</b>:
 * Sometimes, an algorithm will pre-define some parameters, while additional parameters can be
 * supplied by the user. This can be done using a chained parameterization as provided by
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization ChainedParameterization}
 * <blockquote><pre>{@code  // predefine some parameters
 * ListParameterization opticsParameters = new ListParameterization();
 * opticsParameters.addParameter(OPTICS.DISTANCE_FUNCTION_ID, DiSHDistanceFunction.class);
 * // ... more parameters ...
 * ChainedParameterization chain = new ChainedParameterization(opticsParameters, config);
 * chain.errorsTo(opticsParameters);
 * optics = new OPTICS<V, PreferenceVectorBasedCorrelationDistance>(chain);
 * opticsParameters.failOnErrors();
 * }</pre></blockquote>
 * (This example code is from {@link de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.DiSH DiSH}.)
 * <p />
 * Note how error handling is performed by explicity specification of an error target and by
 * calling failOnErrors() at the end of parameterization.
 * <p />
 * 
 * (Note: the current implementation of this approach may be inadequate for XML or Tree based
 * parameterization, due to tree inconsistencies. This is an open TODO issue)
 * </li>
 * 
 * <li><b>Tracking parameterizations:</b>:
 * Sometimes (e.g. for help functions, re-running, configuration templates etc.) it is
 * required to track all parameters an (sub-) algorithm consumed. This can be done using
 * a {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters TrackParameters}
 * wrapper around the configuration. The wrapper does not have own configuration items or error
 * recording, instead everything is forwarded to the inner configuration. It does however keep track
 * of consumed values, that can then be used for re-parameterization of an Algorithm.
 * <blockquote><pre>{@code  // config is an existing parameterization
 * TrackParameters trackpar = new TrackParameters(config);
 * Database<V> tmpDB = PARTITION_DB_PARAM.instantiateClass(trackpar);
 * Collection<Pair<Object, Parameter<?, ?>>> dbpars = trackpar.getParameters();
 * }</pre></blockquote>
 * (This is an example from {@link de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC COPAC}.)
 * <p/>
 * 
 * Note: when using getParameters, you should filter the output to only keep references to the
 * data you actually need. Often, you only need the OptionID and given value of the Parameter, not the
 * owning object. For documentation and help output, the owning object is relevant, whereas the parameter
 * value will often not be set. A future API change may offer two separate get methods for these use cases.
 * </li>
 * </ol>
 */
package de.lmu.ifi.dbs.elki.utilities.optionhandling;
