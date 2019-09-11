#!/bin/python
import scipy.stats, numpy, subprocess, math, sys, gzip, re

#
# R requirements:
# install.packages(c("statmod", "emg", "chi", "evir", "lmomco", "gumbel", "bda"))
#

# Random variate samples
samples = 100

# Quantile for probabilities
pqs = map(lambda x: x/10., range(1, 21, 1)) + [ 1e-5, 1e-10, .1234567, math.pi, math.e, math.pi/10, math.e/10 ]
pqs.sort()

# Quantiles for quantile function
qqs = [ 0.0001, 0.001, 0.01, 0.10, 0.25, 0.5, 0.75, 0.90, 0.99, 0.999, 0.9999 ]

dfs = [
	("gamma", "1_1", scipy.stats.gamma(1, scale=1./1), "gamma(x, 1, rate=1 %s)", 1),
	("gamma", "2_1", scipy.stats.gamma(2, scale=1./1), "gamma(x, 2, rate=1 %s)", 2),
	("gamma", "4_1", scipy.stats.gamma(4, scale=1./1), "gamma(x, 4, rate=1 %s)", 3),
	("gamma", "4_10", scipy.stats.gamma(4, scale=1./10), "gamma(x, 4, rate=10 %s)", 4),
	("gamma", "01_10", scipy.stats.gamma(.1, scale=1./10), "gamma(x, .1, rate=10 %s)", 5),
	("gamma", "01_20", scipy.stats.gamma(.1, scale=1./20), "gamma(x, .1, rate=20 %s)", 6),
	("gamma", "01_4", scipy.stats.gamma(.1, scale=1./4), "gamma(x, .1, rate=4 %s)", 7),
	("gamma", "01_1", scipy.stats.gamma(.1, scale=1./1), "gamma(x, .1, rate=1 %s)", 8),
	("beta", "01_01", scipy.stats.beta(.1, .1), "beta(x, .1, .1 %s)", 9),
	("beta", "01_05", scipy.stats.beta(.1, .5), "beta(x, .1, .5 %s)", 10),
	("beta", "01_1", scipy.stats.beta(.1, 1), "beta(x, .1, 1 %s)", 11),
	("beta", "01_2", scipy.stats.beta(.1, 2), "beta(x, .1, 2 %s)", 12),
	("beta", "01_4", scipy.stats.beta(.1, 4), "beta(x, .1, 4 %s)", 13),
	("beta", "05_01", scipy.stats.beta(.5, .1), "beta(x, .5, .1 %s)", 14),
	("beta", "05_05", scipy.stats.beta(.5, .5), "beta(x, .5, .5 %s)", 15),
	("beta", "05_1", scipy.stats.beta(.5, 1), "beta(x, .5, 1 %s)", 16),
	("beta", "05_2", scipy.stats.beta(.5, 2), "beta(x, .5, 2 %s)", 17),
	("beta", "05_4", scipy.stats.beta(.5, 4), "beta(x, .5, 4 %s)", 18),
	("beta", "1_01", scipy.stats.beta(1, .1), "beta(x, 1, .1 %s)", 19),
	("beta", "1_05", scipy.stats.beta(1, .5), "beta(x, 1, .5 %s)", 20),
	("beta", "1_1", scipy.stats.beta(1, 1), "beta(x, 1, 1 %s)", 21),
	("beta", "1_2", scipy.stats.beta(1, 2), "beta(x, 1, 2 %s)", 22),
	("beta", "1_4", scipy.stats.beta(1, 4), "beta(x, 1, 4 %s)", 23),
	("beta", "2_01", scipy.stats.beta(2, .1), "beta(x, 2, .1 %s)", 24),
	("beta", "2_05", scipy.stats.beta(2, .5), "beta(x, 2, .5 %s)", 25),
	("beta", "2_1", scipy.stats.beta(2, 1), "beta(x, 2, 1 %s)", 26),
	("beta", "2_2", scipy.stats.beta(2, 2), "beta(x, 2, 2 %s)", 27),
	("beta", "2_4", scipy.stats.beta(2, 4), "beta(x, 2, 4 %s)", 28),
	("beta", "4_01", scipy.stats.beta(4, .1), "beta(x, 4, .1 %s)", 29),
	("beta", "4_05", scipy.stats.beta(4, .5), "beta(x, 4, .5 %s)", 30),
	("beta", "4_1", scipy.stats.beta(4, 1), "beta(x, 4, 1 %s)", 31),
	("beta", "4_2", scipy.stats.beta(4, 2), "beta(x, 4, 2 %s)", 32),
	("beta", "4_4", scipy.stats.beta(4, 4), "beta(x, 4, 4 %s)", 33),
	("beta", "5000_10000", scipy.stats.beta(5000, 10000), "beta(x, 5000, 10000 %s)", 34),
	("chisq", "01", scipy.stats.chi2(.1), "chisq(x, .1 %s)", 35),
	("chisq", "1", scipy.stats.chi2(1), "chisq(x, 1 %s)", 36),
	("chisq", "2", scipy.stats.chi2(2), "chisq(x, 2 %s)", 37),
	("chisq", "4", scipy.stats.chi2(4), "chisq(x, 4 %s)", 38),
	("chisq", "10", scipy.stats.chi2(10), "chisq(x, 10 %s)", 39),
	("norm", "0_1", scipy.stats.norm(0, 1), "norm(x, 0, 1 %s)", 40),
	("norm", "1_3", scipy.stats.norm(1, 3), "norm(x, 1, 3 %s)", 41),
	("norm", "01_01", scipy.stats.norm(.1, .1), "norm(x, .1, .1 %s)", 42),
	("lognorm", "0_1", scipy.stats.lognorm(1, 0, math.exp(0)), "lnorm(x, 0, 1 %s)", 43),
	("lognorm", "1_3", scipy.stats.lognorm(3, 0, math.exp(1)), "lnorm(x, 1, 3 %s)", 44),
	("lognorm", "01_01", scipy.stats.lognorm(.1, 0, math.exp(.1)), "lnorm(x, .1, .1 %s)", 45),
	("unif", "0_1", scipy.stats.uniform(0, 1), "unif(x, 0, 1 %s)", 46),
	("unif", "M1_2", scipy.stats.uniform(-1, 3), "unif(x, -1, 2 %s)", 47),
	("exp", "01", scipy.stats.expon(scale=1/.1), "exp(x, .1 %s)", 48),
	("exp", "05", scipy.stats.expon(scale=1/.5), "exp(x, .5 %s)", 49),
	("exp", "1", scipy.stats.expon(scale=1/1.), "exp(x, 1. %s)", 50),
	("exp", "2", scipy.stats.expon(scale=1/2.), "exp(x, 2. %s)", 51),
	("exp", "4", scipy.stats.expon(scale=1/4.), "exp(x, 4. %s)", 52),
	("weibull", "1_1", scipy.stats.exponweib(1,1,scale=1), "weibull(x, 1, 1 %s)", 53),
	("weibull", "2_1", scipy.stats.exponweib(1,2,scale=1), "weibull(x, 2, 1 %s)", 54),
	("weibull", "4_1", scipy.stats.exponweib(1,4,scale=1), "weibull(x, 4, 1 %s)", 55),
	("weibull", "4_10", scipy.stats.exponweib(1,4,scale=10), "weibull(x, 4, 10 %s)", 56),
	("weibull", "01_10", scipy.stats.exponweib(1,.1,scale=10), "weibull(x, .1, 10 %s)", 57),
	("weibull", "01_20", scipy.stats.exponweib(1,.1,scale=20), "weibull(x, .1, 20 %s)", 58),
	("weibull", "01_4", scipy.stats.exponweib(1,.1,scale=4), "weibull(x, .1, 4 %s)", 59),
	("weibull", "01_1", scipy.stats.exponweib(1,.1,scale=1), "weibull(x, .1, 1 %s)", 60),
	("gumbel", "1_1", scipy.stats.gumbel_r(1,scale=1), "lmomco(x, vec2par(c(1, 1, 0), type=\"gev\") %s)", 61),
	("gumbel", "2_1", scipy.stats.gumbel_r(2,scale=1), "lmomco(x, vec2par(c(2, 1, 0), type=\"gev\") %s)", 62),
	("gumbel", "4_1", scipy.stats.gumbel_r(4,scale=1), "lmomco(x, vec2par(c(4, 1, 0), type=\"gev\") %s)", 63),
	("gumbel", "4_10", scipy.stats.gumbel_r(4,scale=10), "lmomco(x, vec2par(c(4, 10, 0), type=\"gev\") %s)", 64),
	("gumbel", "01_10", scipy.stats.gumbel_r(.1,scale=10), "lmomco(x, vec2par(c(.1, 10, 0), type=\"gev\") %s)", 65),
	("gumbel", "01_20", scipy.stats.gumbel_r(.1,scale=20), "lmomco(x, vec2par(c(.1, 20, 0), type=\"gev\") %s)", 66),
	("gumbel", "01_4", scipy.stats.gumbel_r(.1,scale=4), "lmomco(x, vec2par(c(.1, 4, 0), type=\"gev\") %s)", 67),
	("gumbel", "01_1", scipy.stats.gumbel_r(.1,scale=1), "lmomco(x, vec2par(c(.1, 1, 0), type=\"gev\") %s)", 68),
	("gev", "08_02_1", scipy.stats.genextreme(.8,loc=.2,scale=1), "lmomco(x, vec2par(c(.2, 1, .8), type=\"gev\") %s)", 69),
	("gev", "1_05_1", scipy.stats.genextreme(1,loc=.5,scale=1), "lmomco(x, vec2par(c(.5, 1, 1), type=\"gev\") %s)", 70),
	("gev", "1_05_05", scipy.stats.genextreme(1,loc=.5,scale=.5), "lmomco(x, vec2par(c(.5, .5, 1), type=\"gev\") %s)", 71),
	("gev", "2_05_05", scipy.stats.genextreme(2,loc=.5,scale=.5), "lmomco(x, vec2par(c(.5, .5, 2), type=\"gev\") %s)", 72),
	("gev", "4_05_05", scipy.stats.genextreme(4,loc=.5,scale=.5), "lmomco(x, vec2par(c(.5, .5, 4), type=\"gev\") %s)", 73),
	("gev", "M1_05_1", scipy.stats.genextreme(-1,loc=.5,scale=1), None, 74),
	("gev", "M1_05_05", scipy.stats.genextreme(-1,loc=.5,scale=.5), None, 75),
	("gev", "M2_05_05", scipy.stats.genextreme(-2,loc=.5,scale=.5), None, 76),
	("gev", "M4_05_05", scipy.stats.genextreme(-4,loc=.5,scale=.5), None, 77),
	("logistic", "05", scipy.stats.logistic(loc=.5), "logis(x, location=.5 %s)", 78),
	("logistic", "01", scipy.stats.logistic(loc=.1), "logis(x, location=.1 %s)", 79),
	("glogistic", "1_1", scipy.stats.genlogistic(1, loc=1.), None, 80),
	("glogistic", "2_05", scipy.stats.genlogistic(2, loc=.5), None, 81),
	("glogistic", "05_05", scipy.stats.genlogistic(.5, loc=.5), None, 82),
	("expgamma", "1_1", scipy.stats.loggamma(1, scale=1./1), None, 83),
	("expgamma", "2_1", scipy.stats.loggamma(2, scale=1./1), None, 84),
	("expgamma", "4_1", scipy.stats.loggamma(4, scale=1./1), None, 85),
	("expgamma", "4_10", scipy.stats.loggamma(4, scale=1./10), None, 86),
	("expgamma", "01_10", scipy.stats.loggamma(.1, scale=1./10), None, 87),
	("expgamma", "01_20", scipy.stats.loggamma(.1, scale=1./20), None, 88),
	("expgamma", "01_4", scipy.stats.loggamma(.1, scale=1./4), None, 89),
	("expgamma", "01_1", scipy.stats.loggamma(.1, scale=1./1), None, 90),
	("gpd", "01_05_01", scipy.stats.genpareto(.1,loc=.1,scale=.5), "lmomco(x, vec2par(c(.1, .5, .1), type=\"gpa\") %s)", 91),
	("invgauss", "1_1", scipy.stats.invgauss(1.,scale=1.), "invgauss(x, 1, 1 %s)", 92),
	("invgauss", "05_1", scipy.stats.invgauss(.5,scale=1.), "invgauss(x, .5, 1 %s)", 93),
	("invgauss", "1_05", scipy.stats.invgauss(2.,scale=.5), "invgauss(x, 1., .5 %s)", 94),
	("cauchy", "05_1", scipy.stats.cauchy(loc=.5,scale=1), "cauchy(x, 0.5, 1 %s)", 95),
	("cauchy", "1_05", scipy.stats.cauchy(loc=1,scale=.5), "cauchy(x, 1, 0.5 %s)", 96),
	("chi", "01", scipy.stats.chi(.1), "chi(x, 0.1 %s)", 97),
	("chi", "1", scipy.stats.chi(1), "chi(x, 1 %s)", 98),
	("chi", "2", scipy.stats.chi(2), "chi(x, 2 %s)", 99),
	("chi", "4", scipy.stats.chi(4), "chi(x, 4 %s)", 100),
	("chi", "10", scipy.stats.chi(10), "chi(x, 10 %s)", 101),
	("emg", "1_3_05", scipy.stats.exponnorm(1./(0.5*3), loc=1, scale=3), "emg(x, 1, 3, 0.5 %s)", 102),
	("emg", "1_3_01", scipy.stats.exponnorm(1./(0.1*3), loc=1, scale=3), "emg(x, 1, 3, 0.1 %s)", 102),
	("lap", "1_3", scipy.stats.laplace(3, 1), "lap(x, 3, 1 %s)", 103),
	("lap", "4_05", scipy.stats.laplace(.5, 1./4), "lap(x, 0.5, 4 %s)", 104),
	("ray", "1", scipy.stats.rayleigh(scale=1), "weibull(x, 2, 1*sqrt(2) %s)", 105),
	("ray", "2", scipy.stats.rayleigh(scale=2), "weibull(x, 2, 2*sqrt(2) %s)", 106),
	("kappa", "01_02_03_04", scipy.stats.kappa4(.4, .3, loc=.1, scale=.2), "kap(x, vec2par(c(.1,.2,.3,.4), type='kap') %s)", 107),
	("loglogistic", "1_1", scipy.stats.fisk(1, scale=1), None, 108),
	("loglogistic", "2_05", scipy.stats.fisk(2, scale=.5), None, 109),
	("loglogistic", "05_05", scipy.stats.fisk(.5, scale=.5), None, 110),
	("skewnorm", "01_02_03", None, "gno(x, vec2par(c(.1,.2,.3), type='gno') %s)", 111),
	("skewnorm", "1_2_3", None, "gno(x, vec2par(c(1,2,3), type='gno') %s)", 112),
]

def fmt(x):
	if isinstance(x, float): x = "%.17e" % x
	x = re.sub("\\.?0+e", "e", x)
	x = re.sub("e\\+00$", "", x)
	return x

pqstr = ",".join(map(fmt, pqs))
qqstr = ",".join(map(fmt, qqs))

pk, of = None, None
for n1, n2, df, rfunc, seed in sorted(dfs):
	if len(sys.argv) > 1 and not n1 in sys.argv[1:]: continue
	if pk != n1:
		if of: of.close()
		pk = n1
		of = gzip.open("%s.ascii.gz" % n1, "w")
	if df:
		print >>of, "random_%s" % (n2,),
		for x in df.rvs(size=samples, random_state=seed): print >>of, fmt(x),
		print >>of
		print >>of, "cdf_scipy_%s" % (n2,),
		for q in pqs: print >>of, fmt(q), fmt(df.cdf(q)),
		print >>of
		print >>of, "pdf_scipy_%s" % (n2,),
		for q in pqs: print >>of, fmt(q), fmt(df.pdf(q)),
		print >>of
		print >>of, "logpdf_scipy_%s" % (n2,),
		for q in pqs: print >>of, fmt(q), fmt(df.logpdf(q)),
		print >>of
		print >>of, "quant_scipy_%s" % (n2,),
		for q in qqs: print >>of, fmt(q), fmt(df.ppf(q)),
		print >>of

	if rfunc:
		c = subprocess.Popen(["R", "--quiet", "--no-save", "--slave"],
			stdin=subprocess.PIPE, stdout=subprocess.PIPE)
		rin="""
library("stats4")
library("statmod")
library("emg")
library("chi")
library("gumbel")
library("evir")
library("lmomco")
library("bda")

pkap <- cdfkap
dkap <- pdfkap
qkap <- quakap
pgno <- cdfgno
dgno <- pdfgno
qgno <- quagno

x <- c( %s )
y <- p%s
cat(formatC(y, digits=50, format="e"))
cat(" ")
y <- d%s
cat(formatC(y, digits=50, format="e"))
cat(" ")
y <- d%s
cat(formatC(y, digits=50, format="e"))
q()
""" % (pqstr, rfunc % "", rfunc % "", rfunc % ", log=TRUE")
		rout, rerr = c.communicate(rin)
		vals = map(float, rout.split())
		assert(len(vals) == len(pqs) * 3 or len(vals) == len(pqs) * 2)

		print >>of, "cdf_gnur_%s" % (n2,),
		for i in range(0, len(pqs)):
			print >>of, fmt(pqs[i]), fmt(vals[i]),
		print >>of
		print >>of, "pdf_gnur_%s" % (n2,),
		for i in range(0, len(pqs)):
			j = i + len(pqs)
			print >>of, fmt(pqs[i]), fmt(vals[j]),
		print >>of
		if len(vals) == len(pqs) * 3: # GEV, GPD do not have a log mode in R
			print >>of, "logpdf_gnur_%s" % (n2,),
			for i in range(0, len(pqs)):
				j = i + 2 * len(pqs)
				print >>of, fmt(pqs[i]), fmt(vals[j]),
			print >>of
		c = subprocess.Popen(["R", "--quiet", "--no-save", "--slave"],
			stdin=subprocess.PIPE, stdout=subprocess.PIPE)
		rin="""
library("stats4")
library("statmod")
library("emg")
library("chi")
library("gumbel")
library("lmomco")
library("bda")

pkap <- cdfkap
dkap <- pdfkap
qkap <- quakap
pgno <- cdfgno
dgno <- pdfgno
qgno <- quagno

x <- c( %s )
y <- q%s
cat(formatC(y, digits=50, format="e"))
q()
""" % (qqstr, rfunc % "")
		rout, rerr = c.communicate(rin)
		vals = map(float, rout.split())
		if len(vals) == len(qqs): # Gumbel does not have qgumbel
			print >>of, "quant_gnur_%s" % (n2,),
			for i in range(0, len(qqs)):
				print >>of, fmt(qqs[i]), fmt(vals[i]),
			print >>of
