X-Tree addon for ELKI
=====================

This addon was written in 2009, for an old version of ELKI.

The code has received some basic API updating to the point where it compiles,
and even passes some basic unit test, but it may contain errors. In particular,
the unit test likely does not yet cover superpage functionality, the key feature
of the xtree.

Because of this, the code is currently **not enabled by default**, but you need
to edit `settings.gradle`.

It may be *less optimized* than the regular indexes in ELKI, and may thus
be slower, and we suggest to not use this for benchmarking without careful
profiling and optimization.

Please submit Github pull requests to restore full functionality, 
and update it to the latest ELKI APIs. Thank you!
