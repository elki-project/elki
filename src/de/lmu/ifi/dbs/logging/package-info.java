/**
<p>Logging facility for controlling logging behaviour of the complete framework.</p>
<h3>Logging</h3>
Associating a logger to a specific class, use something like the following code:
<pre>
private static final boolean DEBUG = LoggingConfiguration.DEBUG;
private Logger logger = Logger.getLogger(this.getClass().getName());
</pre>
<h4>Level specific logging</h4>
<p><ul>
    <li>Debugging: for debugging messages use levels below <code>INFO</code>,
        such as <code>FINE</code>, <code>FINER</code>, or <code>FINEST</code>.
    </li>
    <li>Verbose messages for regular user information: for &quot;verbose&quot; messages
        use the level <code>INFO</code>.
    </li>
    <li>Warning messages for user information: For warning messages use the level <code>WARNING</code>.
    </li>
    <li>Exception messages: For Exception messages use the level <code>SEVERE</code>.
        <ul>
            <li>Regular user information:</li>
            <li>Detailed information: get throwable, cause...</li>
        </ul>
    </li>
</ul></p>
<h3>Handling</h3>
<h4>Level specific handling</h4>
<p><ul>
    <li>Debugging</li>
    <li>Verbose messages for regular user information</li>
    <li>Warning messages for user information</li>
    <li>Exception messages
        <ul>
            <li>Regular user information:</li>
            <li>Detailed information: get throwable, cause...</li>
        </ul>
    </li>
</ul></p>
<h4>Handling for Command Line Interface</h4>
<p><ul>
    <li>Debugging</li>
    <li>Verbose messages for regular user information</li>
    <li>Warning messages for user information</li>
    <li>Exception messages
        <ul>
            <li>Regular user information:</li>
            <li>Detailed information: get throwable, cause...</li>
        </ul>
    </li>
</ul></p>
<h4>Handling for Graphical User Interface</h4>
TODO
<p><ul>
    <li>Debugging</li>
    <li>Verbose messages for regular user information</li>
    <li>Warning messages for user information</li>
    <li>Exception messages
        <ul>
            <li>Regular user information:</li>
            <li>Detailed information: get throwable, cause...</li>
        </ul>
    </li>
</ul></p>
<h3>Efficiency considerations (for development)</h3>
<p>Coding the logging of debugging messages conditional by a final boolean variable
    can result in a considerable speedup if the final boolean is false.
    This is the purpose of the recommendation given above,
    to specify a variable in each class
    that owns a logger:
<pre>
private static final boolean DEBUG = LoggingConfiguration.DEBUG;
</pre>
    A debugging message can then get coded as follows:
<pre>
if(DEBUG)
{
    logger.fine("message\n"); // or: finer, finest
}
</pre>
    For class specific debugging, the developer will code the attribute as
<pre>
private static final boolean DEBUG = true;
</pre>
    while for general debugging the developer could also set the central attribute
    {@link LoggingConfiguration.DEBUG LoggingConfiguration.DEBUG} to <code>true</code>.
    </p>
    <p>For reasons similar to debugging, the verbosity of an algorithm is restricted by
        a boolean variable rather than by choosing the granularity of loggers. Any verbose message
        for information of users is of the level INFO. One could switch off the handler
        for verbose messages, but the logging of those messages will still be time consuming.
        Thus, the developer should log verbose messages also conditionally, like:
<pre>
if(verbose)
{
    logger.info("message\n");
}
</pre>
        </p>
*/
package de.lmu.ifi.dbs.logging;