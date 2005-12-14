package de.lmu.ifi.dbs.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Random;

/**
 * RationalNumber represents rational numbers in arbitrary precision.
 * Note that the best possible precision is the primary objective
 * of this class. Since numerator and denominator of the RationalNumber
 * are represented as BigIntegers, the required space can grow unlimited.
 * Also arithmetic operations are considerably less efficient compared to
 * the operations with doubles.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class RationalNumber extends Number implements Arithmetic<RationalNumber>
{
    /**
     * Generated serial version UID. 
     */
    private static final long serialVersionUID = 7347098153261459646L;

    /**
     * Holding the numerator of the RationalNumber.
     */
    private BigInteger numerator;
    
    /**
     * Holding the denominator of the RationalNumber.
     */
    private BigInteger denominator;

    /**
     * Constructs a RationalNumber for a given numerator and denominator.
     * The denominator must not be 0.
     * 
     * @param numerator the numerator of the RationalNumber
     * @param denominator the denominator of the RationalNumber
     * @throws IllegalArgumentException if <code>denominator.equalts(BigInteger.ZERO)</code>
     */
    public RationalNumber(final BigInteger numerator, final BigInteger denominator) throws IllegalArgumentException
    {
        if(denominator.equals(BigInteger.ZERO))
        {
            throw new IllegalArgumentException("denominator is 0");
        }
        this.numerator = new BigInteger(numerator.toByteArray());
        this.denominator = new BigInteger(denominator.toByteArray());
        normalize();
    }
    
    
    /**
     * Constructs a RationalNumber for a given numerator and denominator.
     * The denominator must not be 0.
     * 
     * @param numerator the numerator of the RationalNumber
     * @param denominator the denominator of the RationalNumber
     * @throws IllegalArgumentException if <code>denominator==0</code>
     */
    public RationalNumber(final long numerator, final long denominator) throws IllegalArgumentException
    {
        if(denominator==0)
        {
            throw new IllegalArgumentException("denominator is 0");
        }
        this.numerator = BigInteger.valueOf(numerator);
        this.denominator = BigInteger.valueOf(denominator);
        normalize();
    }
    
    /**
     * Constructs a RationalNumber out of the given double number.
     * 
     * @param number a double number to be represented as a RationalNumber
     * @throws IllegalArgumentException if the given String represents a doubel number that is infinit or not a number
     */
    public RationalNumber(final Double number) throws IllegalArgumentException
    {
        this(number.toString());
    }
    
    /**
     * Constructs a RationalNumber for a given String
     * representing a double.
     * 
     * @param doubleString a String representing a double number
     * @throws IllegalArgumentException if the given String represents a doubel number that is infinit or not a number
     */
    public RationalNumber(final String doubleString) throws IllegalArgumentException
    {
        try
        {
            Double number = Double.parseDouble(doubleString);
            if(number.isInfinite())
            {
                throw new IllegalArgumentException("given number is infinite");
            }
            if(number.isNaN())
            {
                throw new IllegalArgumentException("given number is NotANumber");
            }
            // ensure standard encoding of the double argument
            String standardDoubleString = number.toString();
            // index of decimal point '.'
            int pointIndex = standardDoubleString.indexOf('\u002E');
            // read integer part
            String integerPart = pointIndex == -1 ? standardDoubleString : standardDoubleString.substring(0,pointIndex);
            // index of power 'E'
            int powerIndex = standardDoubleString.indexOf('\u0045');
            // read fractional part
            String fractionalPart = powerIndex == -1 ? standardDoubleString.substring(pointIndex+1) : standardDoubleString.substring(pointIndex+1, powerIndex);
            // read power
            int power = powerIndex == -1 ? 0 : Integer.parseInt(standardDoubleString.substring(powerIndex+1));
            // concatenate integer part and fractional part to numerator
            numerator = new BigInteger(integerPart+fractionalPart);
            
            // reduce power accordingly to the shift of the fraction point
            power -= fractionalPart.length();
            denominator = BigInteger.ONE;
            // translate power notation
            StringBuffer multiplicandString = new StringBuffer("1");
            for(int i = 0; i < Math.abs(power); i++)
            {
                multiplicandString.append('0');
            }
            BigInteger multiplicand = new BigInteger(multiplicandString.toString());
            if(power < 0)
            {
                denominator = denominator.multiply(multiplicand);
            }
            else if (power > 0)
            {                
                numerator = numerator.multiply(multiplicand);
            }                    
            normalize();
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("Illegal format of given number: "+doubleString);
        }
    }
    
    /**
     * Normalizes the RationalNumber
     * by normalizing the signum
     * and canceling both, numerator and denominator,
     * by the greatest common divisor.
     * 
     * If the numerator is zero, the denominator is always one.
     */
    protected void normalize()
    {
        if(numerator.equals(BigInteger.ZERO))
        {
            denominator = BigInteger.ONE;
        }
        else
        {
            //normalize signum
            normalizeSignum();
            //greatest common divisor
            BigInteger gcd = numerator.gcd(denominator);
            // cancel
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        }
    }
    
    /**
     * Normalizes the signum such that
     * if the RationalNumber is negative,
     * the numerator will be negative, the denominator positive.
     * If the RationalNumber is positive,
     * both, the numerator and the denominator will be positive.
     * 
     *
     */
    protected void normalizeSignum()
    {
        int numeratorSignum = numerator.signum();
        int denominatorSignum = denominator.signum();
        if(numeratorSignum == denominatorSignum)
        {
            if(numeratorSignum < 0)
            {
                numerator = numerator.abs();
                denominator = denominator.abs();
            }
        }
        else
        {
            if(denominatorSignum < 0)
            {
                numerator = numerator.negate();
                denominator = denominator.negate();
            }
        }
    }

    /**
     * Returns the intValue of {@link #doubleValue() this.doubleValue()}.
     * @see java.lang.Double#intValue()
     */
    @Override
    public int intValue()
    {
        return (int) doubleValue();
    }

    /**
     * Returns the longValue of {@link #doubleValue() this.doubleValue()}.
     * @see java.lang.Double#longValue()
     */
    @Override
    public long longValue()
    {
        return (long) doubleValue();
    }

    /**
     * Returns the floatValue of {@link #doubleValue() this.doubleValue()}.
     * @see java.lang.Double#floatValue()
     */
    @Override
    public float floatValue()
    {
        return (float) doubleValue();
    }

    /**
     * Returns the byteValue of {@link #doubleValue() this.doubleValue()}.
     * 
     * @see java.lang.Double#byteValue()
     */
    @Override
    public byte byteValue()
    {
        return ((Double) doubleValue()).byteValue();
    }

    /**
     * Returns the shortValue of {@link #doubleValue() this.doubleValue()}.
     * 
     * @see java.lang.Double#shortValue()
     */
    @Override
    public short shortValue()
    {
        return ((Double) doubleValue()).shortValue();
    }

    /**
     * Returns the double value representation
     * of this RationalNumber.
     * 
     * The result is given by double division as
     * <code>numerator.doubleValue() / denominator.doubleValue()</code>.
     * Note that the result may not be exact.
     * Thus after <code>RationalNumber a = new RationalNumber(b.doubleValue())</code>,
     * <code>a.equals(b)</code> is not necessarily true.
     * 
     * 
     * @see java.lang.Number#doubleValue()
     */
    @Override
    public double doubleValue()
    {
        return numerator.doubleValue() / denominator.doubleValue();
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.Arithmetic#plus(N)
     */
    public void plus(final RationalNumber number)
    {
        numerator = numerator.multiply(number.denominator);
        BigInteger newDenominator = denominator.multiply(number.denominator);
        numerator = numerator.add(number.numerator.multiply(denominator));
        denominator = newDenominator;
        normalize();
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.Arithmetic#times(N)
     */
    public void times(final RationalNumber number)
    {
        numerator = numerator.multiply(number.numerator);
        denominator = denominator.multiply(number.denominator);
        normalize();
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.Arithmetic#minus(N)
     */
    public void minus(final RationalNumber number)
    {
        plus(number.additiveInverse());
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.Arithmetic#divided(N)
     * @throws ArithmeticException if the given divisor is 0
     */
    public void divided(final RationalNumber number) throws ArithmeticException
    {
        times(number.multiplicativeInverse());
    }
    
    /**
     * Returns the multiplicative inverse of this RationalNumber
     * if it exists.
     * 
     * 
     * @return the multiplicative inverse of this rational number
     * @throws ArithmeticException if numerator is 0
     * and hence the multiplicative inverse of this
     * rational number does not exist
     */
    public RationalNumber multiplicativeInverse() throws ArithmeticException
    {
        try
        {
            return new RationalNumber(denominator, numerator);
        }
        catch(IllegalArgumentException e)
        {
            throw new ArithmeticException("construction of inverse not possible for "+this);
        }
    }
    
    /**
     * Returns the additive inverse of this RationalNumber.
     * 
     * 
     * @return the additive inverse of this RationalNumber
     */
    public RationalNumber additiveInverse()
    {
        return new RationalNumber(numerator.negate(), denominator);
    }

    /**
     * Compares two RationalNumbers a/b and c/d.
     * Result is the same as
     * <code>ad.compareTo(cb)</code>.
     * 
     * @see java.lang.Comparable#compareTo(T)
     */
    public int compareTo(final RationalNumber o)
    {
        BigInteger left = numerator.multiply(o.denominator);
        BigInteger right = o.numerator.multiply(denominator);
        
        return left.compareTo(right);
    }
    
    
    /**
     * Two RationalNumbers are considered to be equal
     * if both denominators and numerators are equal, respectively.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        RationalNumber r = (RationalNumber) obj;
        
        return denominator.equals(r.denominator) && numerator.equals(r.numerator);
    }

    /**
     * Returns a String representation of this RationalNumber.
     * 
     * The representation consists of the numerator, a separating &quot; / &quot;,
     * and the denominator of the RationalNumber.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return numerator.toString()+" / "+denominator.toString();
    }
    
    /**
     * Provides a deep copy of this RationalNumber.
     * 
     * 
     * @return a deep copy of this RationalNumber
     */
    public RationalNumber copy()
    {
        return new RationalNumber(numerator, denominator);
    }
    
    /**
     * Compares doubles and RationalNumbers wrt efficiency and accuracy.
     * 
     * 
     * @param n the number of random numbers
     * @param out target to print results to
     */
    public static void test(final int n, final PrintStream out)
    {
        Random rnd = new Random();
        int[] testNumbers1 = new int[n];
        int[] testNumbers2 = new int[n];
        RationalNumber[] rationalNumbers = new RationalNumber[n];
        double[] doubles = new double[n];
                
        for(int i = 0; i < n; i++)
        {
            testNumbers1[i] = rnd.nextInt();
            int second = rnd.nextInt();
            while(second==0)
            {
                second = rnd.nextInt();
            }
            testNumbers2[i] = second; 
            rationalNumbers[i] = new RationalNumber(testNumbers1[i],testNumbers2[i]);
            doubles[i] = (double) testNumbers1[i] / (double) testNumbers2[i];
        }
        
        long doubleStart = System.currentTimeMillis();
        for(int i = 0; i < n; i++)
        {
            doubles[i] = doubles[i] * 7.0 / 5.0 * 5.0 / 7.0 * testNumbers2[i];
        }
        long doubleTime = System.currentTimeMillis()-doubleStart;
        long rnStart = System.currentTimeMillis();
        for(int i = 0; i < n; i++)
        {
            rationalNumbers[i].times(new RationalNumber(7,1));
            rationalNumbers[i].divided(new RationalNumber(5,1));
            rationalNumbers[i].times(new RationalNumber(5,1));
            rationalNumbers[i].divided(new RationalNumber(7,1));
            rationalNumbers[i].times(new RationalNumber(testNumbers2[i],1));
        }
        long rnTime = System.currentTimeMillis()-rnStart;
        out.println("Efficiency: ");
        out.println("  time required for a predefined sequence of operations on "+n+" random numbers:");
        out.println("    double:         "+doubleTime);
        out.println("    RationalNumber: "+rnTime);
        int accuracyDouble = n;
        double deviationDouble = 0.0;
        int accuracyRN = n;
        double deviationRN = 0.0;
        for(int i = 0; i < n; i++)
        {
            if((int) doubles[i] != testNumbers1[i])
            {
                accuracyDouble--;
                deviationDouble += ((double) testNumbers1[i] - doubles[i]);
            }
            if(rationalNumbers[i].intValue() != testNumbers1[i])
            {
                accuracyRN--;
                deviationRN += ((double) testNumbers1[i] - rationalNumbers[i].doubleValue());
            }
        }
        out.println("\nAccuracy: ");
        out.println("  percentage of correctly recomputed "+n+" random numbers for a sequence of predefined operations:");
        out.println("    double:         "+(double) accuracyDouble/ (double) n);
        out.println("    RationalNumber: "+(double) accuracyRN/ (double) n);
        out.println("  average deviation:");
        out.println("    double:         "+deviationDouble/n);
        out.println("    RationalNumber: "+deviationRN/n);
        
        out.print("Overview:\n--------------------------------------------\n");
        for(int i = 0; i < n ; i++)
        {
            out.print("target:     ");
            out.print((double) testNumbers1[i]);
            out.print("\ndouble:     ");
            out.print(doubles[i]);
            out.print("\nrationalnr: ");
            out.print(rationalNumbers[i].doubleValue());
            out.print("\n--------------------------------------------\n");
        }
    }

    /**
     * Calls test for a given number of numbers and a optionally given target file.
     * 
     * 
     * @param args &lt;int&gt; [&lt;filename&gt;]
     */
    public static void main(String[] args)
    {
        try
        {
            int numbers = Integer.parseInt(args[0]);
            PrintStream out;
            try
            {
                out = new PrintStream(new File(args[1]));
            }
            catch(FileNotFoundException e)
            {
                System.err.println(e.getMessage());
                System.err.println("printing output to STDOUT");
                out = System.out;
            }
            catch(ArrayIndexOutOfBoundsException e)
            {
                out = System.out;
            }
            test(numbers, out);
        }
        catch(Exception e)
        {
            System.err.print("Usage:\njava ");
            System.err.print(RationalNumber.class.getName());
            System.err.println(" <numberOfTestnumbers> [<filename>]");
            System.err.println("If <filename> is ommitted, output will be printed to STDOUT.");
        }
    }
}
