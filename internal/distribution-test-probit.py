#!/bin/python
import scipy.stats, numpy, subprocess

qs = [ 0.0001, 0.001, 0.01, 0.10, 0.25, 0.5, 0.75, 0.90, 0.99, 0.999, 0.9999 ]

dfs = [
	("GAMMA", "1_1", scipy.stats.gamma(1, scale=1./1), "qgamma(x, 1, rate=1)"),
	("GAMMA", "2_1", scipy.stats.gamma(2, scale=1./1), "qgamma(x, 2, rate=1)"),
	("GAMMA", "4_1", scipy.stats.gamma(4, scale=1./1), "qgamma(x, 4, rate=1)"),
	("GAMMA", "4_10", scipy.stats.gamma(4, scale=1./10), "qgamma(x, 4, rate=10)"),
	("GAMMA", "01_10", scipy.stats.gamma(.1, scale=1./10), "qgamma(x, .1, rate=10)"),
	("GAMMA", "01_20", scipy.stats.gamma(.1, scale=1./20), "qgamma(x, .1, rate=20)"),
	("GAMMA", "01_4", scipy.stats.gamma(.1, scale=1./4), "qgamma(x, .1, rate=4)"),
	("GAMMA", "01_1", scipy.stats.gamma(.1, scale=1./1), "qgamma(x, .1, rate=1)"),
	("BETA", "01_01", scipy.stats.beta(.1, .1), "qbeta(x, .1, .1)"),
	("BETA", "01_05", scipy.stats.beta(.1, .5), "qbeta(x, .1, .5)"),
	("BETA", "01_1", scipy.stats.beta(.1, 1), "qbeta(x, .1, 1)"),
	("BETA", "01_2", scipy.stats.beta(.1, 2), "qbeta(x, .1, 2)"),
	("BETA", "01_4", scipy.stats.beta(.1, .1), "qbeta(x, .1, 4)"),
	("BETA", "05_01", scipy.stats.beta(.5, .1), "qbeta(x, .5, .1)"),
	("BETA", "05_05", scipy.stats.beta(.5, .5), "qbeta(x, .5, .5)"),
	("BETA", "05_1", scipy.stats.beta(.5, 1), "qbeta(x, .5, 1)"),
	("BETA", "05_2", scipy.stats.beta(.5, 2), "qbeta(x, .5, 2)"),
	("BETA", "05_4", scipy.stats.beta(.5, 5), "qbeta(x, .5, 4)"),
	("BETA", "1_01", scipy.stats.beta(1, .1), "qbeta(x, 1, .1)"),
	("BETA", "1_05", scipy.stats.beta(1, .5), "qbeta(x, 1, .5)"),
	("BETA", "1_1", scipy.stats.beta(1, 1), "qbeta(x, 1, 1)"),
	("BETA", "1_2", scipy.stats.beta(1, 2), "qbeta(x, 1, 2)"),
	("BETA", "1_4", scipy.stats.beta(1, 5), "qbeta(x, 1, 4)"),
	("BETA", "2_01", scipy.stats.beta(2, .1), "qbeta(x, 2, .1)"),
	("BETA", "2_05", scipy.stats.beta(2, .5), "qbeta(x, 2, .5)"),
	("BETA", "2_1", scipy.stats.beta(2, 1), "qbeta(x, 2, 1)"),
	("BETA", "2_2", scipy.stats.beta(2, 2), "qbeta(x, 2, 2)"),
	("BETA", "2_4", scipy.stats.beta(2, 5), "qbeta(x, 2, 4)"),
	("BETA", "4_01", scipy.stats.beta(4, .1), "qbeta(x, 4, .1)"),
	("BETA", "4_05", scipy.stats.beta(4, .5), "qbeta(x, 4, .5)"),
	("BETA", "4_1", scipy.stats.beta(4, 1), "qbeta(x, 4, 1)"),
	("BETA", "4_2", scipy.stats.beta(4, 2), "qbeta(x, 4, 2)"),
	("BETA", "4_4", scipy.stats.beta(4, 5), "qbeta(x, 4, 4)"),
	("CHISQ", "01", scipy.stats.chi2(.1), "qchisq(x, .1)"),
	("CHISQ", "1", scipy.stats.chi2(1), "qchisq(x, 1)"),
	("CHISQ", "2", scipy.stats.chi2(2), "qchisq(x, 2)"),
	("CHISQ", "4", scipy.stats.chi2(4), "qchisq(x, 4)"),
	("CHISQ", "10", scipy.stats.chi2(10), "qchisq(x, 10)"),
	("NORM", "0_1", scipy.stats.norm(0, 1), "qnorm(x, 0, 1)"),
	("NORM", "1_3", scipy.stats.norm(1, 3), "qnorm(x, 1, 3)"),
	("NORM", "01_01", scipy.stats.norm(.1, .1), "qnorm(x, .1, .1)"),
	("UNIF", "0_1", scipy.stats.uniform(0, 1), "qunif(x, 0, 1)"),
	("UNIF", "M1_2", scipy.stats.uniform(-1, 2), "qunif(x, -1, 2)"),
]

qstr = ",".join(map(str, qs))

print "public static final double[] P_PROBIT = { //"
print qstr, "//"
print "};"

for n1, n2, df, rfunc in dfs:
	print "public static final double[] SCIPY_%s_PROBIT_%s = { //" % (n1, n2)
	for q in qs:
		v = df.ppf(q)
		print "%.50e, // %f" % (v, q)
	print "};"

	c = subprocess.Popen(["R", "--quiet", "--no-save", "--slave"],
		stdin=subprocess.PIPE, stdout=subprocess.PIPE)
	rin="""
x <- c( %s )
y <- %s
cat(formatC(y, digits=50, format="e"))
q()
""" % (qstr, rfunc)
	rout, rerr = c.communicate(rin)
	vals = rout.split()
	assert(len(vals) == len(qs))

	print "public static final double[] GNUR_%s_PROBIT_%s = { //" % (n1, n2)
	for i in range(0, len(qs)):
		print "%s, // %f" % (vals[i], qs[i])
	print "};"
