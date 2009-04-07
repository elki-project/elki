/**
<p>Logging facility for controlling logging behavior of the complete framework.</p>
<h3>Logging in ELKI</h3>

<p>Logging in ELKI is closely following the {@link java.util.logging} approach.</p>

<p>However, system-wide configuration of logging does not seem appropriate, therefore
ELKI uses a configuration file named <pre>logging-cli.properties</pre> living in the package
{@link de.lmu.ifi.dbs.elki.logging} (or an appropriately named directory) for command line
interface based operation.</p>

<p>Logging levels can be configured on a per-class or per-package level using e.g.:</p>
<pre>de.lmu.ifi.dbs.elki.index.level = FINE</pre>
<p>to set the logging level for the index structure package to FINE.</p>

<h3>Logging for Developers:</h3>

<p>Developers working in ELKI are encouraged to use the following setup to make configurable
logging:</p>

<ol>
<li><p>Introduce one or multiple static final debug flags in their classes:</p>
<code>protected static final boolean debug = true || {@link de.lmu.ifi.dbs.elki.logging.LoggingConfiguration#DEBUG LoggingConfiguration.DEBUG};</code>
<p>After development, it should be changed to <code>false || {@link de.lmu.ifi.dbs.elki.logging.LoggingConfiguration#DEBUG LoggingConfiguration.DEBUG}</code>.</p>
</li>
<li><p>If the class contains 'frequent' logging code, acquire a static Logger reference:</p>
<code>protected static final {@link de.lmu.ifi.dbs.elki.logging.Logging Logging} logger = {@link de.lmu.ifi.dbs.elki.logging.Logging#getLogger Logging.getLogger}(Example.class);
</li>
<li><p>Wrap logging statements in appropriate level checks:</p>
<code>
if ({@link de.lmu.ifi.dbs.elki.logging.Logging#isVerbose logger.isVerbose()}) {
  // compute logging message
  {@link de.lmu.ifi.dbs.elki.logging.Logging#verbose logger.verbose}(expensive + message + construction);
}
</code>
</li>
<li><p>For infrequent logging, the following static convenience function is appropriate:</p>
<code>
  {@link de.lmu.ifi.dbs.elki.logging.LoggingUtil#exception LoggingUtil.exception}("Out of memory in algorithm.", exception);
</code>
<p>This function is expensive (it acquires a stack trace to obtain class and method references,
retrieves a logger reference etc.) and thus should only be used for 'rare' logging events.</p>
</li>
<li><p>In cases where many tests would occur, also consider using:</p>
<pre>
final boolean verbose = {@link de.lmu.ifi.dbs.elki.logging.Logging#isVerbose logger.isVerbose}();
// ... for, while, anything expensive
if (verbose) {
  {@link de.lmu.ifi.dbs.elki.logging.Logging#verbose logger.verbose}(...);
}
</pre>
</li>
</ol>
*/
package de.lmu.ifi.dbs.elki.logging;

