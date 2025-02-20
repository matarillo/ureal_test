/*
 * Copyright (C) 2020 The Android Open Source Project
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

import com.android.calculator2.evaluation.UnifiedReal;
import com.hp.creals.CR;

final class HTest {
  public static UnifiedReal hTo(int n) {
    UnifiedReal sum = UnifiedReal.ZERO;
    for (int i = 1; i <= n; ++i) {
      sum = sum.add(UnifiedReal.valueOf(i).inverse());
    }
    return sum;
  }

  public static UnifiedReal recHTo(int from, int to, int step) {
    if (from + step > to) {
      return UnifiedReal.valueOf(from).inverse();
    }
    return recHTo(from, to, 2 * step).add(recHTo(from + step, to, 2 * step));
  }

  public static CR rrHTo(int n) {
    CR sum = CR.ZERO;
    for (int i = 1; i <= n; ++i) {
      sum = sum.add(CR.valueOf(i).inverse());
    }
    return sum;
  }

  public static CR recRrHTo(int from, int to, int step) {
    if (from + step > to) {
      return CR.valueOf(from).inverse();
    }
    return recRrHTo(from, to, 2 * step).add(recRrHTo(from + step, to, 2 * step));
  }

  private static long startTime;

  private static void initTiming() {
    startTime = System.currentTimeMillis();
  }

  private static void finishTiming(String label) {
    final long finishTime = System.currentTimeMillis();
    System.out.println (label + ": took " + (finishTime - startTime) + " msecs");
  }

  static void timeAt(int n) {
    if (n <= 5000) {
      initTiming();
      System.out.println("Answer:" + hTo(n).toStringTruncated(1000));
      finishTiming("real " + n);
    }
    if (n < 5000) {
      initTiming();
      System.out.println("Answer:" + rrHTo(n).toString(1000));
      finishTiming("rr " + n);
    }
    initTiming();
    System.out.println("Answer:" + recHTo(1, n, 1).toStringTruncated(1000));
    finishTiming("rec real " + n);
    initTiming();
    System.out.println("Answer:" + recRrHTo(1, n, 1).toString(1000));
    finishTiming("rec rr " + n);
  }

  public static void main(String[] args) {
    for (int i = 0; i < 6; ++i) {
      timeAt(1000);
      timeAt(5000);
      timeAt(10000);
    }
  }
}
