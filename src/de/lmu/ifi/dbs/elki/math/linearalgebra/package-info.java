/**
<p>Linear Algebra package provides classes and computational methods for operations on matrices.</p>
<p>
   The content of this package is adapted from the Jama package.
</p>

<p>
   Five fundamental matrix decompositions, which consist of pairs or triples
   of matrices, permutation vectors, and the like, produce results in five
   decomposition classes.  These decompositions are accessed by the Matrix
   class to compute solutions of simultaneous linear equations, determinants,
   inverses and other matrix functions.  The five decompositions are:
<ul>
   <li>Cholesky Decomposition of symmetric, positive definite matrices.</li>
   <li>LU Decomposition of rectangular matrices.</li>
   <li>QR Decomposition of rectangular matrices.</li>
   <li>Singular Value Decomposition of rectangular matrices.</li>
   <li>Eigenvalue Decomposition of both symmetric and nonsymmetric square matrices.</li>
</ul>
<dl>
<dt><b>Example of use:</b></dt>
<p>
<dd>Solve a linear system A x = b and compute the residual norm, ||b - A x||.
<p><pre>
      double[][] vals = {{1.,2.,3},{4.,5.,6.},{7.,8.,10.}};
      Matrix A = new Matrix(vals);
      Matrix b = Matrix.random(3,1);
      Matrix x = A.solve(b);
      Matrix r = A.times(x).minus(b);
      double rnorm = r.normInf();
</pre></dd>
</dl>

<p>The original Jama-package has been developed by
the <a href="http://www.mathworks.com/">MathWorks</a> and <a
href="http://www.nist.gov/">NIST</a> and
can be found at <a href="http://math.nist.gov/javanumerics/jama/">http://math.nist.gov/javanumerics/jama/</a>.

   Here, for the adaption some classes and methods convenient for data mining applications within ELKI were added.
   Furthermore some erroneous comments were corrected and the coding-style was subtly changed to a more Java-typical style.
*/
package de.lmu.ifi.dbs.elki.math.linearalgebra;