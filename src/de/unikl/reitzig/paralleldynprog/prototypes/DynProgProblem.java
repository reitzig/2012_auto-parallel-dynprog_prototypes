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

/**
 * Abstraction of dynamic programming problems.
 * Do not try to solve a single instance concurrently with different solvers.
 *
 * @param <T> Solution type of this problem
 * @author Raphael Reitzig, 02.2012
 */
public interface DynProgProblem<T> extends Cloneable {
  /**
   * Result can safely be changed by the caller.
   * @return the problem table's size for each dimension.
   */
  int[] getDimension();

  /**
   * Callee does not change the passed array {@code i}.
   * @param i Index of a table cell. {@code 0 <= i[j] < getDimension()[j]} must hold for all {@code j}.
   * @return {@code true} iff the specified cell has already been computed
   */
  boolean isComputed(int[] i);

  /**
   * Callee does not change the passed array {@code i}.
   * @param i Index of a table cell. {@code 0 <= i[j] < getDimension()[j]} must hold for all {@code j}.
   * @return {@code true} iff the specified cell's dependencies have already been computed
   */
  boolean isComputable(int[] i);

  /**
   * Computes the specified cell.
   * Callee does not change the passed array {@code i}.
   * @param i Index of a table cell. {@code 0 <= i[j] < getDimension()[j]} must hold for all {@code j}.
   */
  void compute(int[] i);

  /**
   * Throws a runtime error if {@code isSolved() == false}.
   * @return This problem's solution.
   */
  T getSolution();

  /**
   * @return {@code true} iff this problem has been solved.
   */
  boolean isSolved();

  /**
   * @return a clean copy of this problem, i.e. just like before any solver touched it.
   */
  DynProgProblem<T> clone();
}
