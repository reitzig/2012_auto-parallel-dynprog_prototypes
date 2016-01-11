/*
 * This file is part of Parallel Dynamic Programming Implementation Prototype (PDPIP).
 *
 * PDPIP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PDPIP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PDPIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unikl.reitzig.paralleldynprog.prototypes.solvers;

import de.unikl.reitzig.paralleldynprog.prototypes.DynProgProblem;
import de.unikl.reitzig.paralleldynprog.prototypes.DynProgSolver;
import de.unikl.reitzig.paralleldynprog.prototypes.Util;
import vanilla.java.affinity.AffinityThreadFactory;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static vanilla.java.affinity.AffinityStrategies.ANY;
import static vanilla.java.affinity.AffinityStrategies.DIFFERENT_CORE;

/**
 * Implementation of RS scheme for type 2 DP
 *
 * @author Raphael Reitzig, 02.2012
 */
public class RowSplit implements DynProgSolver {
  private final int p;
  private final int blockSize;

  /**
   * Creates a new instance
   *
   * @param p The number of workers this solver will use. Has to be positive.
   * @param k Length of the blocks this solver will fill at a time. Passing a value less or equal to
   *          {@code 0} will result in {@code n/p + 1} being used, {@code n} the problem's
   *          second dimension. (i.e. number of columns).
   */
  public RowSplit(final int p, final int k) {
    this.p = p;
    this.blockSize = k;
  }

  /**
   * Creates a new instance that uses {@code p} blocks per row.
   *
   * @param p The number of workers this solver will use. Has to be positive.
   */
  public RowSplit(final int p) {
    this(p, -1);
  }

 /**
   * Creates a new instance that uses all processors and {@code p} blocks per row.
   */
  public RowSplit() {
    this(Runtime.getRuntime().availableProcessors(), -1);
  }

  @Override
  public void solve(final DynProgProblem<?> problem) {
    assert problem != null : "null parameter";

    if ( !problem.isSolved() && problem.getDimension().length == 2 ) {
      final CyclicBarrier rowGate = new CyclicBarrier(p);
      final CountDownLatch endGate = new CountDownLatch(p);

      final AffinityThreadFactory factory = new Util.AffinityFactory(this + ".Worker", Util.SAME_SOCKET_DIFFERENT_CORE, DIFFERENT_CORE, ANY);

      for ( int i=0; i<p; i++ ) {
        final int fi = i;

        factory.newThread(new Runnable() {
          @Override
          public void run() {
            final Worker w = new Worker(fi, problem);
            w.run(rowGate, endGate);
          }
        }).start();
      }

      try {
        endGate.await();
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private class Worker {
    private final int nr;
    private final DynProgProblem<?> prob;
    final int[] dim;
    private int[] param = new int[] { 0, 0 };

    Worker(final int nr, final DynProgProblem<?> prob) {
      assert prob != null : "null parameter";
      this.nr = nr;
      this.prob = prob;
      this.dim =  prob.getDimension();
    }

    public void run(final CyclicBarrier rowGate, final CountDownLatch endGate) {
      assert rowGate != null && endGate != null : "null parameter";
      final int k = blockSize > 0 ? blockSize : dim[1]/p + 1;

      for (; param[0] < dim[0]; param[0]++) {
        for (int o = nr * k; o < dim[1]; o += p * k) {
          for (param[1] = o; param[1] < Math.min(o + k, dim[1]); param[1]++) {
            prob.compute(param);
          }
        }

        try {
          rowGate.await();
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
        catch (BrokenBarrierException e) {
          e.printStackTrace();
        }
      }

      endGate.countDown();
    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "[" + p + (blockSize > 0 ? "," + blockSize : "") + "]";
  }
}
