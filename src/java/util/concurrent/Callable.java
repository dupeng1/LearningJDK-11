/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * A task that returns a result and may throw an exception.
 * Implementors define a single method with no arguments called
 * {@code call}.
 *
 * <p>The {@code Callable} interface is similar to {@link
 * java.lang.Runnable}, in that both are designed for classes whose
 * instances are potentially executed by another thread.  A
 * {@code Runnable}, however, does not return a result and cannot
 * throw a checked exception.
 *
 * <p>The {@link Executors} class contains utility methods to
 * convert from other common forms to {@code Callable} classes.
 *
 * @param <V> the result type of method {@code call}
 *
 * @author Doug Lea
 * @see Executor
 * @since 1.5
 */
/*
 * Callable表示一类带有返回值的任务，这类任务在计算过程中可能抛出异常
 *
 * Callable类任务通常由【任务执行器】Executor来执行
 * 它可以与Runnable与Future协作
 *
 * 该接口已函数化：
 * Callable callable = new Callable<Result>() {
 *     @Override
 *     public Result call() throws Exception {
 *         System.out.println("Callable");
 *         return new Result();
 *     }
 * };
 * 可以简写为：
 * Callable callable = (Callable<Result>) () -> {
 *     System.out.println("Callable");
 *     return new Result();
 * };
 */
// Callable的call方法可以有返回值，可以声明抛出异常，和Callable配合的有一个Future类，通过Future可以了解任务执行情况，或者取消任务的执行，
// 还可获取任务执行的结果，这些功能都是Runnable做不到的，Callable的功能要比Runnable强大
@FunctionalInterface
public interface Callable<V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     *
     * @throws Exception if unable to compute a result
     */
    // 任务执行入口
    V call() throws Exception;
}
