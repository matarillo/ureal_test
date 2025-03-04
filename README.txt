Getting started
---------------
Two artifacts described in the paper are currently available, but only the first
is archived here:

1) The code to check Java arithmetic and math function accuracy. This includes
the publicly available source code for the arithmetic package itself.

The Java source code is under the com directory. To build and run the
test on nIters = 10000 random double-precision floating-point values:

a) A machine capable of running Java 8 or later is required. I've used a
Linux machine with OpenJDK 8, 9, or 11 installed. This is pure Java code
with no dependencies and a trivial build process. Any other Java implementation
is also likely to work. (java.lang.BigInteger performance matters.)

b) Look at the 2-line cmd and cmd2 files in this directory. Cmd
compiles and runs a test of java.lang.Math functions on 10,000 random doubles.
Cmd2 runs the variation of this test that was used to generate the timing
results in the paper. It takes longer, since the whole test is run repeatedly.

c) Type e.g. ./cmd in the ureal_test directory or paste the contents of that
file into a shell.  (Note that this might appropriately fail since OpenJDK does
have an obscure bug in this area. See
https://bugs.openjdk.java.net/browse/JDK-8229259.) On a reasonably fast machine,
this should finish in a minute or so.

d) To produce repeatable executions, the second command in either cmd file can
take a seed value for Random as an argument. Otherwise a different one seed
is chosen for each run.

e) If you want to try writing code against the UnifiedReal package,
HTest.java in the main directory is a simple example of such code. Run
with javac HTest.java; java HTest. It generates the harmonic series
timings from the paper.

Noteworthy files:

UnifiedRealVsFpTest.java: Precision test. The critical function is
ulpError().

UnifiedRealVsFpTimer.java: A version of the precision test that times each
function separately, to produce the timing results in the paper. It produces
results for each run, so some post-processing (delete first set of runs,
sort, average) is required.

UnifiedReal.java: The actual reals package. Tries to support comparisons, etc.,
as described. Essentially what's used by the calculator below.

BoundedRational.java: The rational arithmetic package used by the above.

CR.java: The underlying constructive/recursive reals package used by
UnifiedReal.java. Comparisons of equal numbers diverge. (From earlier work,
though with some bug fixes, particularly for performance bugs.)

HTest.java: Timing code for the simple harmonic series computation.
hTo() serves as a very simple example of code that uses the reals
package directly.

2) The calculator package itself is currently available on the Google Play
store, and is currently preinstalled on many, though far from all, Android
devices. Please check for Version 7.8 or later.

To download and install see

https://play.google.com/store/apps/details?id=com.google.android.calculator

or search for "google calculator" on Google Play.

Start the app as you would any other. The overflow menu (upper right corner,
three dots) points to a help page with basic instructions.

Full source code for an up-to-date version of the calculator is not available.
(Much older versions can be found in AOSP.) The arithmetic code is the same
as that described above, and is included here.


Evaluation instructions
-----------------------

1) Possible things to check with the precision test:

The claims about line counts and limited complexity of the package are accurate.

Performance should be roughly as reported in the paper. In particular, it should not
diverge.

On most Java implementations, it should not report errors, except possibly for the
previously mentioned OpenJDK hypot() bug.

Any programmatic use of UnifiedReal the reader may want to try.

2) Possible things to check with calculator:

We display exact (no trailing zeros, result not scrollable) results for e.g.
the expressions claimed in the paper: ln(e^2), (in degree mode) sin(65) -
sin(65), sin(65) - sin(115), sqrt(17)^2, and others the reviewers may wish to
try. (As discussed in the paper, this is not infallible, but is intended to work
for commonly occurring expressions.)

Note that results saved in the calculator's "memory" or in the calculation
history, or copied-and-immediately-pasted are still saved exactly, not as
the displayed approximation.

We generate scrollable results for non-terminating decimals. At any stage of
scrolling, these should be correctly truncated for values that can be easily
determined to be rational, or that can be easily determined to be irrational.

(1 + 10^-1000)^10^1000 produces an accurate approximation to Euler's number e,
as claimed. Try computing the difference between the result and e and
multiplying by 2.

