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
package de.lmu.ifi.dbs.elki.math.statistics.distribution;

import java.util.Random;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Gaussian distribution aka normal distribution
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Alias({ "GaussianDistribution", "normal", "gauss" })
public class NormalDistribution extends AbstractDistribution {
  /**
   * Treshold for switching nethods for erfinv approximation
   */
  static final double P_LOW = 0.02425D;

  /**
   * Treshold for switching nethods for erfinv approximation
   */
  static final double P_HIGH = 1.0D - P_LOW;

  /**
   * CDFINV(0.75)
   */
  public static final double PHIINV075 = 0.67448975019608171;

  /**
   * 1 / CDFINV(0.75)
   */
  public static final double ONEBYPHIINV075 = 1.48260221850560186054;

  /**
   * Mean value for the generator
   */
  private double mean;

  /**
   * Standard deviation
   */
  private double stddev;

  /**
   * Constructor for Gaussian distribution
   * 
   * @param mean Mean
   * @param stddev Standard Deviation
   * @param random Random generator
   */
  public NormalDistribution(double mean, double stddev, RandomFactory random) {
    super(random);
    this.mean = mean;
    this.stddev = stddev;
  }

  /**
   * Constructor for Gaussian distribution
   * 
   * @param mean Mean
   * @param stddev Standard Deviation
   * @param random Random generator
   */
  public NormalDistribution(double mean, double stddev, Random random) {
    super(random);
    this.mean = mean;
    this.stddev = stddev;
  }

  /**
   * Constructor for Gaussian distribution
   * 
   * @param mean Mean
   * @param stddev Standard Deviation
   */
  public NormalDistribution(double mean, double stddev) {
    this(mean, stddev, (Random) null);
  }

  @Override
  public double pdf(double val) {
    return pdf(val, mean, stddev);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, mean, stddev);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, mean, stddev);
  }

  @Override
  public double quantile(double q) {
    return quantile(q, mean, stddev);
  }

  @Override
  public double nextRandom() {
    return mean + random.nextGaussian() * stddev;
  }

  @Override
  public String toString() {
    return "NormalDistribution(mean=" + mean + ", stddev=" + stddev + ")";
  }

  /**
   * @return the mean
   */
  public double getMean() {
    return mean;
  }

  /**
   * @return the standard deviation
   */
  public double getStddev() {
    return stddev;
  }

  /**
   * Complementary error function for Gaussian distributions = Normal
   * distributions.
   * <p>
   * Based on:<br>
   * Takuya Ooura, http://www.kurims.kyoto-u.ac.jp/~ooura/gamerf.html<br>
   * Copyright (C) 1996 Takuya OOURA (email: ooura@mmm.t.u-tokyo.ac.jp).<br>
   * "You may use, copy, modify this code for any purpose and without fee."
   * 
   * @param x parameter value
   * @return erfc(x)
   */
  @Reference(authors = "T. Ooura", //
      title = "Gamma / Error Functions", booktitle = "", //
      url = "http://www.kurims.kyoto-u.ac.jp/~ooura/gamerf.html", //
      bibkey = "web/Ooura96")
  public static double erfc(double x) {
    if(Double.isNaN(x)) {
      return Double.NaN;
    }
    if(Double.isInfinite(x)) {
      return (x < 0.0) ? 2 : 0;
    }
    final double t = 3.97886080735226 / (Math.abs(x) + 3.97886080735226);
    final double u = t - 0.5;
    double y = (((//
    ((((((0.00127109764952614092 * u //
        + 1.19314022838340944e-4) * u //
        - 0.003963850973605135) * u //
        - 8.70779635317295828e-4) * u //
        + 0.00773672528313526668) * u //
        + 0.00383335126264887303) * u //
        - 0.0127223813782122755) * u //
        - 0.0133823644533460069) * u //
        + 0.0161315329733252248) * u //
        + 0.0390976845588484035) * u //
        + 0.00249367200053503304;
    y = ((((((((((((y * u //
        - 0.0838864557023001992) * u //
        - 0.119463959964325415) * u //
        + 0.0166207924969367356) * u //
        + 0.357524274449531043) * u //
        + 0.805276408752910567) * u //
        + 1.18902982909273333) * u //
        + 1.37040217682338167) * u //
        + 1.31314653831023098) * u //
        + 1.07925515155856677) * u //
        + 0.774368199119538609) * u //
        + 0.490165080585318424) * u //
        + 0.275374741597376782) //
        * t * FastMath.exp(-x * x);
    return x < 0 ? 2 - y : y;
  }

  /**
   * T. Ooura, http://www.kurims.kyoto-u.ac.jp/~ooura/gamerf.html
   */
  private static final double[] ERF_COEFF1 = { //
      5.958930743e-11, -1.13739022964e-9, //
      1.466005199839e-8, -1.635035446196e-7, //
      1.6461004480962e-6, -1.492559551950604e-5, //
      1.2055331122299265e-4, -8.548326981129666e-4, //
      0.00522397762482322257, -0.0268661706450773342, //
      0.11283791670954881569, -0.37612638903183748117, //
      1.12837916709551257377, //
      2.372510631e-11, -4.5493253732e-10, //
      5.90362766598e-9, -6.642090827576e-8, //
      6.7595634268133e-7, -6.21188515924e-6, //
      5.10388300970969e-5, -3.7015410692956173e-4, //
      0.00233307631218880978, -0.0125498847718219221, //
      0.05657061146827041994, -0.2137966477645600658, //
      0.84270079294971486929, //
      9.49905026e-12, -1.8310229805e-10, //
      2.39463074e-9, -2.721444369609e-8, //
      2.8045522331686e-7, -2.61830022482897e-6, //
      2.195455056768781e-5, -1.6358986921372656e-4, //
      0.00107052153564110318, -0.00608284718113590151, //
      0.02986978465246258244, -0.13055593046562267625, //
      0.67493323603965504676, //
      3.82722073e-12, -7.421598602e-11, //
      9.793057408e-10, -1.126008898854e-8, //
      1.1775134830784e-7, -1.1199275838265e-6, //
      9.62023443095201e-6, -7.404402135070773e-5, //
      5.0689993654144881e-4, -0.00307553051439272889, //
      0.01668977892553165586, -0.08548534594781312114, //
      0.56909076642393639985, //
      1.55296588e-12, -3.032205868e-11, //
      4.0424830707e-10, -4.71135111493e-9, //
      5.011915876293e-8, -4.8722516178974e-7, //
      4.30683284629395e-6, -3.445026145385764e-5, //
      2.4879276133931664e-4, -0.00162940941748079288, //
      0.00988786373932350462, -0.05962426839442303805, //
      0.49766113250947636708 };

  /**
   * T. Ooura, http://www.kurims.kyoto-u.ac.jp/~ooura/gamerf.html
   */
  private static final double[] ERF_COEFF2 = { //
      -2.9734388465e-10, 2.69776334046e-9, //
      -6.40788827665e-9, -1.6678201321e-8, //
      -2.1854388148686e-7, 2.66246030457984e-6, //
      1.612722157047886e-5, -2.5616361025506629e-4, //
      1.5380842432375365e-4, 0.00815533022524927908, //
      -0.01402283663896319337, -0.19746892495383021487, //
      0.71511720328842845913, //
      -1.951073787e-11, -3.2302692214e-10, //
      5.22461866919e-9, 3.42940918551e-9, //
      -3.5772874310272e-7, 1.9999935792654e-7, //
      2.687044575042908e-5, -1.1843240273775776e-4, //
      -8.0991728956032271e-4, 0.00661062970502241174, //
      0.00909530922354827295, -0.2016007277849101314, //
      0.51169696718727644908, //
      3.147682272e-11, -4.8465972408e-10, //
      6.3675740242e-10, 3.377623323271e-8, //
      -1.5451139637086e-7, -2.03340624738438e-6, //
      1.947204525295057e-5, 2.854147231653228e-5, //
      -0.00101565063152200272, 0.00271187003520095655, //
      0.02328095035422810727, -0.16725021123116877197, //
      0.32490054966649436974, //
      2.31936337e-11, -6.303206648e-11, //
      -2.64888267434e-9, 2.050708040581e-8, //
      1.1371857327578e-7, -2.11211337219663e-6, //
      3.68797328322935e-6, 9.823686253424796e-5, //
      -6.5860243990455368e-4, -7.5285814895230877e-4, //
      0.02585434424202960464, -0.11637092784486193258, //
      0.18267336775296612024, //
      -3.67789363e-12, 2.0876046746e-10, //
      -1.93319027226e-9, -4.35953392472e-9, //
      1.8006992266137e-7, -7.8441223763969e-7, //
      -6.75407647949153e-6, 8.428418334440096e-5, //
      -1.7604388937031815e-4, -0.0023972961143507161, //
      0.0206412902387602297, -0.06905562880005864105, //
      0.09084526782065478489 };

  /**
   * Error function for Gaussian distributions = Normal distributions.
   * 
   * @param x parameter value
   * @return erf(x)
   */
  public static double erf(double x) {
    final double w = x < 0 ? -x : x;
    double y;
    if(w < 2.2) {
      double t = w * w;
      int k = (int) t;
      t -= k;
      k *= 13;
      y = ((((((((((((ERF_COEFF1[k] * t + ERF_COEFF1[k + 1]) * t + //
          ERF_COEFF1[k + 2]) * t + ERF_COEFF1[k + 3]) * t + ERF_COEFF1[k + 4]) * t + //
          ERF_COEFF1[k + 5]) * t + ERF_COEFF1[k + 6]) * t + ERF_COEFF1[k + 7]) * t + //
          ERF_COEFF1[k + 8]) * t + ERF_COEFF1[k + 9]) * t + ERF_COEFF1[k + 10]) * t + //
          ERF_COEFF1[k + 11]) * t + ERF_COEFF1[k + 12]) * w;
    }
    else if(w < 6.9) {
      int k = (int) w;
      double t = w - k;
      k = 13 * (k - 2);
      y = (((((((((((ERF_COEFF2[k] * t + ERF_COEFF2[k + 1]) * t + //
          ERF_COEFF2[k + 2]) * t + ERF_COEFF2[k + 3]) * t + ERF_COEFF2[k + 4]) * t + //
          ERF_COEFF2[k + 5]) * t + ERF_COEFF2[k + 6]) * t + ERF_COEFF2[k + 7]) * t + //
          ERF_COEFF2[k + 8]) * t + ERF_COEFF2[k + 9]) * t + ERF_COEFF2[k + 10]) * t + //
          ERF_COEFF2[k + 11]) * t + ERF_COEFF2[k + 12];
      y *= y;
      y *= y;
      y *= y;
      y = 1 - y * y;
    }
    else if(w == w) {
      y = 1;
    }
    else {
      return Double.NaN;
    }
    return x < 0 ? -y : y;
  }

  /**
   * Inverse error function.
   * <p>
   * Based on:<br>
   * T. Ooura, http://www.kurims.kyoto-u.ac.jp/~ooura/gamerf.html<br>
   * Copyright (C) 1996 Takuya OOURA (email: ooura@mmm.t.u-tokyo.ac.jp).<br>
   * "You may use, copy, modify this code for any purpose and without fee."
   * 
   * @param y parameter value
   * @return erfcinv(y)
   */
  @Reference(authors = "T. Ooura", //
      title = "Gamma / Error Functions", booktitle = "", //
      url = "http://www.kurims.kyoto-u.ac.jp/~ooura/gamerf.html", //
      bibkey = "web/Ooura96")
  public static double erfcinv(double y) {
    final double z = (y > 1) ? 2 - y : y;
    final double w = 0.916461398268964 - FastMath.log(z);
    double u = FastMath.sqrt(w);
    double s = (FastMath.log(u) + 0.488826640273108) / w;
    double t = 1 / (u + 0.231729200323405);
    double x = u * (1 - s * (s * 0.124610454613712 + 0.5)) //
        - ((((-0.0728846765585675 * t //
            + 0.269999308670029) * t //
            + 0.150689047360223) * t //
            + 0.116065025341614) * t //
            + 0.499999303439796) * t;
    t = 3.97886080735226 / (x + 3.97886080735226);
    u = t - 0.5;
    s = (((((((((0.00112648096188977922 * u //
        + 1.05739299623423047e-4) * u //
        - 0.00351287146129100025) * u //
        - 7.71708358954120939e-4) * u //
        + 0.00685649426074558612) * u //
        + 0.00339721910367775861) * u //
        - 0.011274916933250487) * u //
        - 0.0118598117047771104) * u //
        + 0.0142961988697898018) * u //
        + 0.0346494207789099922) * u //
        + 0.00220995927012179067;
    s = ((((((((((((s * u //
        - 0.0743424357241784861) * u //
        - 0.105872177941595488) * u //
        + 0.0147297938331485121) * u //
        + 0.316847638520135944) * u //
        + 0.713657635868730364) * u //
        + 1.05375024970847138) * u //
        + 1.21448730779995237) * u //
        + 1.16374581931560831) * u //
        + 0.956464974744799006) * u //
        + 0.686265948274097816) * u //
        + 0.434397492331430115) * u //
        + 0.244044510593190935) //
        * t - z * FastMath.exp(x * x - 0.120782237635245222);
    x += s * (x * s + 1);
    return (y > 1) ? -x : x;
  }

  /**
   * Probability density function of the normal distribution.
   * <p>
   * \[ \frac{1}{\sqrt{2\pi\sigma^2}} \exp(-\frac{(x-\mu)^2}{2\sigma^2}) \]
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return PDF of the given normal distribution at x.
   */
  public static double pdf(double x, double mu, double sigma) {
    x = (x - mu) / sigma;
    return MathUtil.ONE_BY_SQRTTWOPI / sigma * FastMath.exp(-.5 * x * x);
  }

  /**
   * Log probability density function of the normal distribution.
   * <p>
   * \[\log\frac{1}{\sqrt{2\pi}} - \log\sigma - \tfrac{(x-\mu)^2}{2\sigma^2}\]
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return PDF of the given normal distribution at x.
   */
  public static double logpdf(double x, double mu, double sigma) {
    x = (x - mu) / sigma;
    return MathUtil.LOG_ONE_BY_SQRTTWOPI - FastMath.log(sigma) - .5 * x * x;
  }

  /**
   * Log probability density function of the standard normal distribution.
   * <p>
   * \[ \log\frac{1}{\sqrt{2\pi}} -\frac{x^2}{2}) \]
   * 
   * @param x The value.
   * @return PDF of the given normal distribution at x.
   */
  public static double standardNormalLogPDF(double x) {
    return -.5 * x * x + MathUtil.LOG_ONE_BY_SQRTTWOPI;
  }

  /**
   * Probability density function of the standard normal distribution.
   * <p>
   * \[ \frac{1}{\sqrt{2\pi}} \exp(-\frac{x^2}{2}) \]
   * 
   * @param x The value.
   * @return PDF of the given normal distribution at x.
   */
  public static double standardNormalPDF(double x) {
    return FastMath.exp(-.5 * x * x) * MathUtil.ONE_BY_SQRTTWOPI;
  }

  /**
   * Cumulative probability density function (CDF) of a normal distribution.
   * <p>
   * Reference:
   * <p>
   * G. Marsaglia<br>
   * Evaluating the Normal Distribution<br>
   * Journal of Statistical Software 11(4)
   *
   * @param x value to evaluate CDF at
   * @param mu Mean value
   * @param sigma Standard deviation.
   * @return The CDF of the given normal distribution at x.
   */
  @Reference(authors = "G. Marsaglia", //
      title = "Evaluating the Normal Distribution", //
      booktitle = "Journal of Statistical Software 11(4)", //
      url = "https://doi.org/10.18637/jss.v011.i04", //
      bibkey = "doi:10.18637/jss.v011.i04")
  public static double cdf(double x, double mu, double sigma) {
    x = (x - mu) / sigma;
    if(x >= 8.22) {
      return 1.;
    }
    if(x <= -8.22) {
      return 0.;
    }
    if(x != x) {
      return Double.NaN;
    }
    double s = x, t = 0, b = x, q = x * x, i = 1;
    while(s != t && i < 1000) {
      t = s;
      s += (b *= q / (i += 2));
    }
    // Constant is 0.5 * log(2*pi)
    return .5 + s * FastMath.exp(-.5 * q - .91893853320467274178);
  }

  /**
   * Cumulative probability density function (CDF) of a normal distribution.
   * <p>
   * Reference:
   * <p>
   * G. Marsaglia<br>
   * Evaluating the Normal Distribution<br>
   * Journal of Statistical Software 11(4)
   *
   * @param x value to evaluate CDF at
   * @return The CDF of the given normal distribution at x.
   */
  @Reference(authors = "G. Marsaglia", //
      title = "Evaluating the Normal Distribution", //
      booktitle = "Journal of Statistical Software 11(4)", //
      url = "https://doi.org/10.18637/jss.v011.i04", //
      bibkey = "doi:10.18637/jss.v011.i04")
  public static double standardNormalCDF(double x) {
    if(x >= 8.22) {
      return 1.;
    }
    if(x <= -8.22) {
      return 0.;
    }
    if(x != x) {
      return Double.NaN;
    }
    double s = x, t = 0, b = x, q = x * x, i = 1;
    while(s != t) {
      s = (t = s) + (b *= q / (i += 2));
    }
    return .5 + s * FastMath.exp(-.5 * q - .91893853320467274178);
  }

  /**
   * Inverse cumulative probability density function (probit) of a normal
   * distribution.
   * 
   * @param x value to evaluate probit function at
   * @param mu Mean value
   * @param sigma Standard deviation.
   * @return The probit of the given normal distribution at x.
   */
  public static double quantile(double x, double mu, double sigma) {
    return mu + sigma * standardNormalQuantile(x);
  }

  /**
   * Approximate the inverse error function for normal distributions.
   * 
   * @param d Quantile. Must be in [0:1], obviously.
   * @return Inverse erf.
   */
  public static double standardNormalQuantile(double d) {
    return (d == 0) ? Double.NEGATIVE_INFINITY : //
        (d == 1) ? Double.POSITIVE_INFINITY : //
            (Double.isNaN(d) || d < 0 || d > 1) ? Double.NaN //
                : MathUtil.SQRT2 * -erfcinv(2 * d);
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double mu, sigma;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter muP = new DoubleParameter(LOCATION_ID);
      if(config.grab(muP)) {
        mu = muP.doubleValue();
      }

      DoubleParameter sigmaP = new DoubleParameter(SCALE_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(sigmaP)) {
        sigma = sigmaP.doubleValue();
      }
    }

    @Override
    protected NormalDistribution makeInstance() {
      return new NormalDistribution(mu, sigma, rnd);
    }
  }
}
