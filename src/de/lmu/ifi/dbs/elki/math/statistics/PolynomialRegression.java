package de.lmu.ifi.dbs.elki.math.statistics;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * A polynomial fit is a specific type of multiple regression.
 * The simple regression model (a first-order polynomial) can be trivially extended
 * to higher orders.
 * <p/>
 * The regression model y = b0 + b1*x + b2*x^2 + ... + bp*x^p + e
 * is a system of polynomial equations of order p with
 * polynomial coefficients { b0 ... bp}.
 * The model can be expressed using data matrix x, target vector y and parameter vector ?.
 * The ith row of X and Y will contain the x and y value for the ith data sample.
 * <p/>
 * The variables will be transformed in the following way:
 * x => x1, ..., x^p => xp
 * Then the model can be written as a multiple
 * linear equation model: y = b0 + b1*x1 + b2*x2 + ... + bp*xp + e
 *
 * @author Elke Achtert
 */
public class PolynomialRegression extends MultipleLinearRegression {

    /**
     * The order of the polynom.
     */
    public final int p;

    /**
     * Provides a new polynomial regression model with the specified
     * parameters.
     *
     * @param y the (n x 1) - vector holding the response values (y1, ..., yn)^T.
     * @param x the (n x 1)-vector holding the x-values (x1, ..., xn)^T.
     * @param p the order of the polynom.
     */
    public PolynomialRegression(Vector y, Vector x, int p) {
        super(y, xMatrix(x, p));
        this.p = p;
    }


    private static Matrix xMatrix(Vector x, int p) {
        int n = x.getRowDimensionality();

        Matrix result = new Matrix(n, p + 1);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < p + 1; j++) {
                result.set(i, j, Math.pow(x.get(i), j));
            }
        }
        return result;
    }

    /**
     * Returns the adapted coefficient of determination
     *
     * @return the adapted coefficient of determination
     */
    public double adaptedCoefficientOfDetermination() {
        int n = getEstimatedResiduals().getRowDimensionality();
        return 1.0 - ((n - 1.0) / (n * 1.0 - p)) * (1 - coefficientOfDetermination());
    }

    /**
     * Performes an estimatation of y on the specified x value.
     *
     * @param x the x-value for which y is estimeated
     * @return the estimatation of y
     */
    public double estimateY(double x) {
        return super.estimateY(xMatrix(new Vector(new double[]{x}), p));
    }
}
