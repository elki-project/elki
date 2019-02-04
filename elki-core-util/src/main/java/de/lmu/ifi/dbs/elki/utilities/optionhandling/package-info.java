/**
 * Parameter handling and option descriptions.
 * <p>
 * <ol>
 * <li><b>Option ID</b>: Any parameter <em>must</em> have an
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID OptionID}.<br>
 * These are Singleton objects to uniquely identify the option. They should be
 * "public static".<br>
 * The OptionID specifies the parameter name and a generic description.
 * <p>
 * Example code:
 * 
 * <pre>
 * // Defining option IDs
 * public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("algorithm.distancefunction", "Distance function to determine the distance between database objects.");
 * </pre>
 * 
 * (This example is from
 * {@link de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm
 * AbstractDistanceBasedAlgorithm}.)
 * </li>
 * <li><b>Parameter Object</b>: To obtain a value, you <em>must</em> use a
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.AbstractParameter
 * Parameter} object.<br>
 * Parameter objects handle <em>parsing</em> of values into the desired type,
 * and various subclasses for common types are provided. It is not desirable to
 * subclass these types too much, since a UI should be able to offer content
 * assistance for input.
 * <p>
 * Parameters often have types and constraints attached to them, and may be
 * flagged optional or have a default value. Note that a parameter with a
 * default value is by definition optional, so there is no constructor with both
 * a default value and the optional flag.
 * <p>
 * Due to restrictions imposed by Java Generics,
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter
 * ListParameter} based types do not have the full set of constructors, since a
 * lList of constraints and a list of default values produce the same signature.
 * In such a signature conflict situation, you can use a full constructor either
 * by giving {@code null} as constraints (and a list of default values) or by
 * giving constraints and setting optional to {@code false}.
 * <p>
 * Notice the difference between an
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter
 * ObjectParameter} and a
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter
 * ClassParameter}. The first is meant to carry a single object (which as
 * fallback can be initialized with a class, resulting in a new object of that
 * class), the second is meant for a "object factory" use.
 * <p>
 * Example code:
 * 
 * <pre>
 * // Defining Parameters
 * final ObjectParameter&lt;DistanceFunction&lt;O, D&gt;&gt; DISTANCE_FUNCTION_PARAM = new ObjectParameter&lt;DistanceFunction&lt;O, D&gt;&gt;(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
 * </pre>
 * 
 * (This example is from
 * {@link de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm
 * DistanceBasedAlgorithm}.)
 * </li>
 * <li><b>Initialization</b>: Initialization happens in the constructor, which
 * <em>must</em> have the signature {@code Class(Parameterization config)} or
 * using a <em>static method</em>
 * {@code parameterize(Parameterization config)}.<br>
 * The {@code config} object manages configuration data, whichever source it is
 * coming from (e.g., command line, XML, lists, ...)
 * <p>
 * The
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization
 * Parameterization} class offers the method
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization#grab
 * grab}, which returns {@code true} when the parameter value is defined <em>and
 * satisfies the given constraints</em>.
 * <p>
 * Initialization should happen in a delayed-fail way. Failure is managed by the
 * Parameterization object, and not failing immediately allows for reporting all
 * configuration errors (and all options) in a single run. As such, when
 * reporting a configuration error, you should <em>not throw the error</em>, but
 * instead call
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization#reportError
 * reportError} and leave error handling to the Parameterization class. Note
 * that this method <em>will return eventually</em>, so you might need to use
 * try-catch-report blocks.
 * <p>
 * The static {@code parameterize(Parameterization config)} factory method
 * <em>may</em> return {@code null} when Parameterization failed. Otherwise, it
 * <em>must</em> return an instance of the given class or a subclass. Example:
 * LPNormDistanceFunction returns an instance of EuclideanDistance for p=2.
 * <p>
 * When writing constructors, try to make error handling as local as possible,
 * to report as many errors at once as possible.
 * <p>
 * Example code:
 * 
 * <pre>
 * // Getting parameters
 * protected DistanceBasedAlgorithm(Parameterization config) {
 *   super(config);
 *   if(config.grab(DISTANCE_FUNCTION_PARAM)) {
 *     distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
 *   }
 * }
 * </pre>
 * 
 * (This example is from
 * {@link de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm
 * DistanceBasedAlgorithm}.)
 * 
 * <pre>
 * // Using flags
 * protected AbstractApplication(Parameterization config) {
 *   super(config);
 *   if(config.grab(VERBOSE_FLAG)) {
 *     verbose = VERBOSE_FLAG.getValue();
 *   }
 * }
 * </pre>
 * 
 * (This example is from
 * {@link de.lmu.ifi.dbs.elki.application.AbstractApplication
 * AbstractApplication}.)
 * <br>
 * The {@code if config.grab} statement ensures that the parameter was set. Note
 * that the configuration manager is passed on to the child instance.
 * </li>
 * <li><b>Compound conditionals</b>: Sometimes, more than one parameter value is
 * required. However, {@code config.grab(...) && config.grab(...)} is <em>not
 * working</em> as intended, since a negative results in the first config.grab
 * statement will prevent the evaluation of the second. <em>Instead, the
 * following code should be used</em>:
 * 
 * <pre>
 * // Compound parameter dependency
 * config.grab(FIRST_OPTION);
 * config.grab(SECOND_OPTION);
 * if(FIRST_OPTION.isDefined() &amp;&amp; SECOND_OPTION.isDefined()) {
 *   // Now we have validated values for both available.
 * }
 * </pre>
 * 
 * </li>
 * <li><b>Error reporting</b>:
 * 
 * <pre>
 * // Proper dealing with errors
 * try {
 *   // code that might fail with an IO exception
 * } except(IOException e) {
 *   config.reportError(new WrongParameterValueException(...));
 * }
 * // process remaining parameters, to report additional errors.
 * </pre>
 * 
 * </li>
 * <li><b>Command line parameterization</b>:
 * Command line parameters are handled by the class
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization
 * SerializedParameterization}
 * which provided convenient constructors from String arrays:
 * 
 * <pre>
 * // Use command line parameters
 * SerializedParameterization params = new SerializedParameterization(args);
 * </pre>
 * 
 * (This example is from
 * {@link de.lmu.ifi.dbs.elki.application.AbstractApplication
 * AbstractApplication}.)
 * </li>
 * <li><b>Internal Parameterization</b>:
 * Often one algorithm will need to call another algorithm, with specific
 * parameters.
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization
 * ListParameterization} offers convenience function for this that do not
 * require String serialization.
 * 
 * <pre>
 * // Internal parameterization
 * ListParameterization parameters = new ListParameterization();
 *
 * parameters.addParameter(PCAFilteredRunner.PCA_EIGENPAIR_FILTER, FirstNEigenPairFilter.class);
 * parameters.addParameter(FirstNEigenPairFilter.EIGENPAIR_FILTER_N, correlationDimension);
 * </pre>
 * 
 * (This example is from
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ERiC ERiC}.)
 * </li>
 * <li><b>Combined parameterization</b>:
 * Sometimes, an algorithm will pre-define some parameters, while additional
 * parameters can be supplied by the user. This can be done using a chained
 * parameterization as provided by
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization
 * ChainedParameterization}.
 * 
 * <pre>
 * // predefine some parameters
 * ListParameterization opticsParameters = new ListParameterization();
 * opticsParameters.addParameter(OPTICS.DISTANCE_FUNCTION_ID, DiSHDistanceFunction.class);
 * // ... more parameters ...
 * ChainedParameterization chain = new ChainedParameterization(opticsParameters, config);
 * chain.errorsTo(opticsParameters);
 * optics = new OPTICS&lt;V, PreferenceVectorBasedCorrelationDistance&gt;(chain);
 * opticsParameters.failOnErrors();
 * </pre>
 * 
 * (This example code is from
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.DiSH DiSH}.)
 * <p>
 * Note how error handling is performed by explicity specification of an error
 * target and by calling failOnErrors() at the end of parameterization.
 * <p>
 * (Note: the current implementation of this approach may be inadequate for XML
 * or Tree based parameterization, due to tree inconsistencies. This is an open
 * TODO issue)
 * </li>
 * <li><b>Tracking parameterizations:</b>:
 * Sometimes (e.g. for help functions, re-running, configuration templates etc.)
 * it is required to track all parameters an (sub-) algorithm consumed. This can
 * be done using a
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters
 * TrackParameters} wrapper around the configuration. The wrapper does not have
 * own configuration items or error recording, instead everything is forwarded
 * to the inner configuration. It does however keep track of consumed values,
 * that can then be used for re-parameterization of an Algorithm.
 * 
 * <pre>
 * // config is an existing parameterization
 * TrackParameters trackpar = new TrackParameters(config);
 * 
 * Database&lt;V&gt; tmpDB = PARTITION_DB_PARAM.instantiateClass(trackpar);
 * 
 * Collection&lt;Pair&lt;OptionID, Object&gt;&gt; dbpars = trackpar.getGivenParameters();
 * </pre>
 * 
 * (This is an example from
 * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.COPAC COPAC}.)
 * </li>
 * <li><b>Advanced tracking</b>:
 * When parameterizing a sub-algorithm, it can be useful to provide some
 * parameters that should not be tracked (because the actual values will only be
 * available afterwards). This is possible by using a ChainedParameterization of
 * untracked and tracked values.
 * <p>
 * Example:
 * 
 * <pre>
 * // config is an existing parameterization
 * ListParameterization myconfig = new ListParameterization();
 * // dummy values for input and output
 * myconfig.addParameter(INPUT_ID, "/dev/null");
 * myconfig.addParameter(OUTPUT_ID, "/dev/null");
 * TrackParameters track = new TrackParameters(config);
 * ChainedParameterization chain = new ChainedParameterization(myconfig, track);
 * wrapper = WRAPPER_PARAM.instantiateClass(chain);
 * </pre>
 * 
 * </li>
 * </ol>
 * For <em>documentation</em>, the classes should also be annotated with
 * {@link de.lmu.ifi.dbs.elki.utilities.documentation.Title}
 * {@link de.lmu.ifi.dbs.elki.utilities.documentation.Description} and
 * {@link de.lmu.ifi.dbs.elki.utilities.documentation.Reference} (where
 * possible).
 */
/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.optionhandling;