/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2.evaluation;

import java.util.Random;

public class UnifiedRealVsFpTimer {

  private static final int WRONG = 3;
  private static final UnifiedReal THREE = UnifiedReal.valueOf(3);
  private static final UnifiedReal MINUS17 = UnifiedReal.valueOf(-17);
  private static Random rand;
  private static final int COMPARE_PREC = -2000;

  private static void checkComparable(UnifiedReal x, UnifiedReal y) {
    if (!x.isComparable(y)) {
      throw new AssertionError(x.toNiceString() + " not comparable to " + y.toNiceString());
    }
  }

  /**
   * Return the difference between fpVal and urVal in ulps. 0 ==> correctly rounded. fpVal is a/the
   * closest representable value fp value. 1 ==> within 1 ulp. fpVal is either the next higher or
   * next lower fp value. If the exact answer is representable, then fpVal is exactly urVal, and
   * hence we would have returned 0, not 1. 2 ==> within 2 ulps. fpVal is one removed from the next
   * higher or lower fpVal. WRONG ==> More than 2 ulps error. We optimistically assume that either
   * urVal is known to be rational, or urVal is irrational, and thus all of our comparisons will
   * converge. In a few cases below, we explicitly avoid empirically observed divergence resulting
   * from violation of this assumption.
   */
  private static int ulpError(double fpVal, UnifiedReal urVal) {
    final UnifiedReal fpAsUr = UnifiedReal.valueOf(fpVal);
    checkComparable(fpAsUr, urVal);
    final int errorSign = fpAsUr.compareTo(urVal);
    if (errorSign == 0) {
      return 0; // Exactly equal.
    }
    if (errorSign < 0) {
      return ulpError(-fpVal, urVal.negate());
    }
    // errorSign > 0
    final double prevFp = Math.nextAfter(fpVal, Double.NEGATIVE_INFINITY);
    if (Double.isInfinite(prevFp)) {
      // Most negative representable value was returned. True result is smaller.
      // That seems to qualify as "correctly rounded".
      return 0;
    }
    final UnifiedReal prev = UnifiedReal.valueOf(prevFp);
    checkComparable(prev, urVal);
    if (prev.compareTo(urVal) >= 0) {
      // prev is a better approximation.
      final double prevprevFp = Math.nextAfter(prevFp, Double.NEGATIVE_INFINITY);
      if (Double.isInfinite(prevprevFp)) {
        return 2; // Dubious, but seems to qualify.
      }
      final UnifiedReal prevprev = UnifiedReal.valueOf(prevprevFp);
      checkComparable(prevprev, urVal);
      if (prevprev.compareTo(urVal) >= 0) {
        // urVal <= prevprev < prev < fpVal. fpVal is neither one of the
        // bracketing values, nor one next to it.
        return WRONG;
      } else {
        return 2;
      }
    } else {
      UnifiedReal prevDiff = urVal.subtract(prev);
      UnifiedReal fpValDiff = fpAsUr.subtract(urVal);
      checkComparable(fpValDiff, prevDiff);
      if (fpValDiff.compareTo(prevDiff) <= 0) {
        return 0;
      } else {
        return 1;
      }
    }
  }

  /**
   * Return the difference between fpVal and urVal in ulps. Behaves like ulpError(),
   * but accommodates situations in which urVal is not known comparable with rationals.
   * In that case the answer could conceivably be wrong, though we evaluate to sufficiently
   * high precision to make that unlikely.
   */
  private static int approxUlpError(double fpVal, UnifiedReal urVal) {
    final UnifiedReal fpAsUr = UnifiedReal.valueOf(fpVal);
    final int errorSign = fpAsUr.compareTo(urVal, COMPARE_PREC);
    if (errorSign == 0) {
      return 0; // Exactly equal.
    }
    if (errorSign < 0) {
      return approxUlpError(-fpVal, urVal.negate());
    }
    // errorSign > 0
    final double prevFp = Math.nextAfter(fpVal, Double.NEGATIVE_INFINITY);
    if (Double.isInfinite(prevFp)) {
      // Most negative representable value was returned. True result is smaller.
      // That seems to qualify as "correctly rounded".
      return 0;
    }
    final UnifiedReal prev = UnifiedReal.valueOf(prevFp);
    if (prev.compareTo(urVal, COMPARE_PREC) >= 0) {
      // prev is a better approximation.
      final double prevprevFp = Math.nextAfter(prevFp, Double.NEGATIVE_INFINITY);
      if (Double.isInfinite(prevprevFp)) {
        return 2; // Dubious, but seems to qualify.
      }
      final UnifiedReal prevprev = UnifiedReal.valueOf(prevprevFp);
      if (prevprev.compareTo(urVal, COMPARE_PREC) >= 0) {
        // urVal <= prevprev < prev < fpVal. fpVal is neither one of the
        // bracketing values, nor one next to it.
        return WRONG;
      } else {
        return 2;
      }
    } else {
      UnifiedReal prevDiff = urVal.subtract(prev);
      UnifiedReal fpValDiff = fpAsUr.subtract(urVal);
      if (fpValDiff.compareTo(prevDiff, COMPARE_PREC) <= 0) {
        return 0;
      } else {
        return 1;
      }
    }
  }

  private static void assertTrue(boolean b) {
    if (!b) {
      throw new AssertionError();
    }
  }

  private static UnifiedReal hypot(UnifiedReal x, UnifiedReal y) {
    return x.multiply(x).add(y.multiply(y)).sqrt();
  }

  /**
   * Generate a random double such that all bit patterns representing a finite value are equally
   * likely. Do not generate a NaN or Infinite result.
   */
  private static double getRandomDouble() {
    double result;
    do {
      result = Double.longBitsToDouble(rand.nextLong());
    } while (Double.isNaN(result) || Double.isInfinite(result));
    return result;
  }

  private static int ops;
  /**
   * Check that basic Math functions obey stated error bounds on argument x. x is assumed to be
   * finite. We assume that the UnifiedReal functions produce known rational results when the
   * results are rational.
   */
  private static void checkDivAt(double x, double other) {
    if (!Double.isFinite(x)) {
      return;
    }
    if (x != 0.0) {
      final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
      if (!Double.isInfinite(other / x)) {
        final UnifiedReal otherAsUr = UnifiedReal.valueOf(other);
        ++ops;
        assertTrue(ulpError(other / x, otherAsUr.divide(xAsUr)) == 0);
      }
    }
  }

  private static void checkExpAt(double x) {
    if (!Double.isFinite(x)) {
      return;
    }
    double result = Math.exp(x);
    if (result != 0 && !Double.isInfinite(result)) {
      ++ops;
      final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
      assertTrue(ulpError(result, xAsUr.exp()) <= 1);
    } // Otherwise the UnifiedReal computation may be intractible.
  }

  private static void checkLnAt(double x) {
    if (!Double.isFinite(x) || x <= 0.0) {
      return;
    }
    double result = Math.log(x);
    ++ops;
    final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
    assertTrue(ulpError(result, xAsUr.ln()) <= 1);
  }

  private static void checkLogAt(double x) {
    if (!Double.isFinite(x) || x <= 0.0) {
      return;
    }
    double result = Math.log10(x);
    ++ops;
    final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
    assertTrue(ulpError(result, xAsUr.log()) <= 1);
  }

  private static void checkSqrtAt(double x) {
    if (!Double.isFinite(x)) {
      return;
    }
    if (x >= 0) {
      ++ops;
      double rt = Math.sqrt(x);
      final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
      UnifiedReal urRt = xAsUr.sqrt();
      assertTrue(ulpError(rt, urRt) == 0);
    }
  }

  private static void checkSinAt(double x) {
    if (!Double.isFinite(x)) {
      return;
    }
    ++ops;
    final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
    assertTrue(ulpError(Math.sin(x), xAsUr.sin()) <= 1);
  }

  private static void checkCosAt(double x) {
    if (!Double.isFinite(x)) {
      return;
    }
    ++ops;
    final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
    assertTrue(ulpError(Math.cos(x), xAsUr.cos()) <= 1);
  }

  private static void checkTanAt(double x) {
    if (!Double.isFinite(x)) {
      return;
    }
    ++ops;
    final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
    assertTrue(ulpError(Math.tan(x), xAsUr.tan()) <= 1);
  }

  private static void checkAtanAt(double x) {
    if (!Double.isFinite(x)) {
      return;
    }
    ++ops;
    final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
    assertTrue(ulpError(Math.atan(x), xAsUr.atan()) <= 1);
  }

  private static void checkAsinAt(double x) {
    if (!Double.isFinite(x) || Math.abs(x) > 1) {
      return;
    }
    ++ops;
    final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
    assertTrue(ulpError(Math.asin(x), xAsUr.asin()) <= 1);
  }

  private static void checkAcosAt(double x) {
    if (!Double.isFinite(x) || Math.abs(x) > 1) {
      return;
    }
    ++ops;
    final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
    assertTrue(ulpError(Math.acos(x), xAsUr.acos()) <= 1);
  }


  private static void checkHypotAt(double x, double other) {
    if (!Double.isFinite(x)) {
      return;
    }
    if (Double.isNaN(other) || Double.isInfinite(other)) {
      return;
    }
    ops++;
    double h = Math.hypot(x, other);
    final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
    final UnifiedReal otherAsUr = UnifiedReal.valueOf(other);
    UnifiedReal hUr = hypot(xAsUr, otherAsUr);
    if (Double.isInfinite(h)) {
      double h2 = hUr.doubleValue();
      assertTrue(
          Double.isInfinite(h2)
              || Double.isInfinite(Math.nextAfter(h2, Double.POSITIVE_INFINITY)));
      // TODO: Since h2 is not yet correctly rounded, this could conceivably still fail
      // spuriously. But that's extremely unlikely.
    } else {
      assertTrue(ulpError(h, hUr) <= 1);
    }
  }

  private static void checkPowAt(double x, double other) {
    if (!Double.isFinite(x) || !Double.isFinite(other)) {
      return;
    }
    if (x >= 0.0 || other == Math.rint(other)) {
      double p = Math.pow(x, other);
      if (!Double.isInfinite(p)) {
        ++ops;
        final UnifiedReal xAsUr = UnifiedReal.valueOf(x);
        final UnifiedReal otherAsUr = UnifiedReal.valueOf(other);
        UnifiedReal urP = xAsUr.pow(otherAsUr);
        if (urP.compareTo(UnifiedReal.valueOf(p), COMPARE_PREC) != 0) {
          assertTrue(approxUlpError(p, urP) <= 1);
        }
      }
    }
  }

  private static long startTime;

  private static void initTiming() {
    ops = 0;
    startTime = System.currentTimeMillis();
  }

  private static void finishTiming(String label) {
    final long finishTime = System.currentTimeMillis();
    System.out.println (label + ": " + ops + " checks took " + (finishTime - startTime)
        + " msecs or " + 1000.0 * (double)(finishTime - startTime) / (double)ops + " usecs/check");
  }

  public static void manyRandomDoubleChecks() {
    final int nIters = 10000;
    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkDivAt(getRandomDouble(), getRandomDouble());
    }
    finishTiming("div");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkExpAt(getRandomDouble());
    }
    finishTiming("exp");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkLnAt(getRandomDouble());
    }
    finishTiming("ln");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkLogAt(getRandomDouble());
    }
    finishTiming("log");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkSqrtAt(getRandomDouble());
    }
    finishTiming("sqrt");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkSinAt(getRandomDouble());
    }
    finishTiming("sin");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkCosAt(getRandomDouble());
    }
    finishTiming("cos");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkTanAt(getRandomDouble());
    }
    finishTiming("tan");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkAsinAt(getRandomDouble());
    }
    finishTiming("asin");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkAcosAt(getRandomDouble());
    }
    finishTiming("acos");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkAtanAt(getRandomDouble());
    }
    finishTiming("atan");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkHypotAt(getRandomDouble(), getRandomDouble());
    }
    finishTiming("hypot");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      checkPowAt(getRandomDouble(), getRandomDouble());
    }
    finishTiming("pow");

    initTiming();
    for (int i = 0; i < nIters; i++) {
      if (getRandomDouble() == getRandomDouble()) {
        System.err.println("jackpot!");
      }
    }
    finishTiming("empty");
  }

  public static void main(String[] args) {
    for (String s: args) {
      if (Character.isDigit(s.charAt(0))) {
        rand = new Random(Long.valueOf(s));
        break;
      }
    }
    if (rand == null) {
      rand = new Random();
    }
    for (int i = 0; i < 6; ++i) {
      manyRandomDoubleChecks();
    }
  }
}
