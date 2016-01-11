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

import java.util.concurrent.CountDownLatch;

import static vanilla.java.affinity.AffinityStrategies.ANY;
import static vanilla.java.affinity.AffinityStrategies.DIFFERENT_CORE;

/**
 * Abstract implementation of the Diagonal Frontier scheme. Leaves room for extending
 * classes to change the way the table is actually filled.
 *
 * @author Raphael Reitzig, 02.2012
 */
abstract class DiagonalFrontier implements DynProgSolver {
  final int p;

  /**
   * Creates a new instance
   * @param p The number of workers this solver will use. Has to be positive.
   */
  public DiagonalFrontier(final int p) {
    assert p > 0 : "Invalid worker count";
    this.p = p;
  }

  @Override
  public void solve(final DynProgProblem<?> problem) {
    assert problem != null : "null parameter";
    if ( !problem.isSolved() && problem.getDimension().length == 2 ) {
      final CountDownLatch endGate = new CountDownLatch(p);

      final AffinityThreadFactory factory = new Util.AffinityFactory(this + ".Worker", Util.SAME_SOCKET_DIFFERENT_CORE, DIFFERENT_CORE, ANY);
      final Notifier gates = new Notifier(p);

      for ( int i=0; i<p; i++ ) {
        final int fi = i;

        factory.newThread(new Runnable() {
          @Override
          public void run() {
            fillTable(problem, fi, gates);
            endGate.countDown();
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

  /**
   * Fills the part of problem's table that is designated to worker {@code w}
   * @param problem The problem to be solved
   * @param w Worker number.
   * @param gates Facility to keep threads in sync.
   */
  abstract void fillTable(DynProgProblem<?> problem, int w, Notifier gates);

  /**
   * Allows for a number of threads to wait for and notify each other.
   */
  protected static class Notifier {
    private final Object[] gates;

    /**
     * Creates a new instance
     * @param p The number of threads that need to be kept in sync.
     */
    Notifier(final int p) {
      gates = new Object[p];
      for ( int i=0; i<p; i++ ) {
        gates[i] = new Object();
      }
    }

    /**
     * Waits for the thread with index {@code w}.
     * @param w Index to wait for; requires {@code 0 <= w < p}.
     */
    void waitFor(final int w) {
      assert w >= 0 && w < gates.length : "invalid gate index";
      synchronized ( gates[w] ) {
        try {
          gates[w].wait();
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    /**
     * Waits for the specified thread if the specified cell is not computable.
     * @param w Index to wait for; requires {@code 0 <= w < p}.
     * @param prob Computed problem
     * @param param Valid parameter of {@code prob}
     */
    void waitIfNotComputable(final int w, final DynProgProblem<?> prob, final int[] param) {
      assert w >= 0 && w < gates.length : "invalid gate index";
      synchronized ( gates[w] ) {
        if ( !prob.isComputable(param) ) {
          waitFor(w);
        }
      }
    }

    /**
     * Waits for the specified thread if the specified cell is computed.
     * @param w Index to wait for; requires {@code 0 <= w < p}.
     * @param prob Computed problem
     * @param param Valid parameter of {@code prob}
     */
    void waitIfNotComputed(final int w, final DynProgProblem<?> prob, final int[] param) {
      assert w >= 0 && w < gates.length : "invalid gate index";
      synchronized ( gates[w] ) {
        if ( !prob.isComputed(param) ) {
          waitFor(w);
        }
      }
    }

    /**
     * Waits for the specified thread until the specified cell is computable.
     * @param w Index to wait for; requires {@code 0 <= w < p}.
     * @param prob Computed problem
     * @param param Valid parameter of {@code prob}
     */
    void waitWhileNotComputable(final int w, final DynProgProblem<?> prob, final int[] param) {
      assert w >= 0 && w < gates.length : "invalid gate index";
      synchronized ( gates[w] ) {
        while ( !prob.isComputable(param) ) {
          waitFor(w);
        }
      }
    }

    /**
     * Waits for the specified thread until the specified cell is computed.
     * @param w Index to wait for; requires {@code 0 <= w < p}.
     * @param prob Computed problem
     * @param param Valid parameter of {@code prob}
     */
    void waitWhileNotComputed(final int w, final DynProgProblem<?> prob, final int[] param) {
      assert w >= 0 && w < gates.length : "invalid gate index";
      synchronized ( gates[w] ) {
        while ( !prob.isComputed(param) ) {
          waitFor(w);
        }
      }
    }



    /**
     * Notify those threads that wait on index {@code w}.
     * @param w Index to notify; requires {@code 0 <= w < p}.
     */
    void notify(int w) {
      assert w >= 0 && w < gates.length : "invalid gate index";
      synchronized ( gates[w] ) {
        gates[w].notifyAll();
      }
    }
  }
}
