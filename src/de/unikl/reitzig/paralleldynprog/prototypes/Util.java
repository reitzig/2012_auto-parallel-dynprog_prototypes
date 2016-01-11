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

package de.unikl.reitzig.paralleldynprog.prototypes;

import vanilla.java.affinity.AffinityLock;
import vanilla.java.affinity.AffinityStrategy;
import vanilla.java.affinity.AffinityThreadFactory;
import vanilla.java.affinity.CpuLayout;

import java.util.Random;

/**
 * Provides some methods of general use.
 *
 * @author Raphael Reitzig, 02.2012
 */
public class Util {
  private static final Random random;
  private final static char[] SYMB = new char[] {'a','b','c','d','e','f','g','h','i','j','k','l','m',
                                                 'n','o','p','q','r','s','t','u','v','w','x','y','z'};

  static {
    final long seed = System.currentTimeMillis();
    System.out.println("Seed for random input generation: " + seed);
    random = new Random(seed);
  }

  public static String randomString(final int minLength, final int maxLength) {
    return randomString(random.nextInt(maxLength - minLength) + minLength);
  }

  public static String randomString(final int length) {
    final StringBuilder res = new StringBuilder(length);
    for ( int i=0; i<length; i++ ) {
      res.append(SYMB[random.nextInt(26)]);
    }
    return res.toString();
  }

  public static int[] randomArray(final int length) {
    final int[] numbers = new int[length];
    for ( int i=0;i<length; i++ ) {
      numbers[i] = random.nextInt(2*length) - length;
    }
    return numbers;
  }

  public static final AffinityStrategy SAME_SOCKET_DIFFERENT_CORE = new AffinityStrategy() {
    @Override
    public boolean matches(int cpuId, int cpuId2) {
      CpuLayout cpuLayout = AffinityLock.cpuLayout();
      return     cpuLayout.socketId(cpuId) == cpuLayout.socketId(cpuId2)
              && cpuLayout.coreId(cpuId) != cpuLayout.coreId(cpuId2);
    }
  };

  /**
   * Same as {@link AffinityThreadFactory} but creates threads with highes priority and
   * useful exception handler.
   */
  public static class AffinityFactory extends AffinityThreadFactory {
    /**
     * Creates a new instance.
     * @param name Name blueprint for creates threads.
     * @param strategies Applies affinity strategies
     * @see AffinityThreadFactory
     */
    public AffinityFactory(String name, AffinityStrategy... strategies) {
      super(name, false, strategies);
    }

    @Override
    public Thread newThread(Runnable r) {
      final Thread t = super.newThread(r);

      t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
          System.err.println("Exception in Thread " + t + ":");
          e.printStackTrace();
        }
      });
      t.setPriority(Thread.MAX_PRIORITY);

      return t;
    }
  }
}
