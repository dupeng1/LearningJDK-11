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
 * A service that decouples the production of new asynchronous tasks
 * from the consumption of the results of completed tasks.  Producers
 * {@code submit} tasks for execution. Consumers {@code take}
 * completed tasks and process their results in the order they
 * complete.  A {@code CompletionService} can for example be used to
 * manage asynchronous I/O, in which tasks that perform reads are
 * submitted in one part of a program or system, and then acted upon
 * in a different part of the program when the reads complete,
 * possibly in a different order than they were requested.
 *
 * <p>Typically, a {@code CompletionService} relies on a separate
 * {@link Executor} to actually execute the tasks, in which case the
 * {@code CompletionService} only manages an internal completion
 * queue. The {@link ExecutorCompletionService} class provides an
 * implementation of this approach.
 *
 * <p>Memory consistency effects: Actions in a thread prior to
 * submitting a task to a {@code CompletionService}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions taken by that task, which in turn <i>happen-before</i>
 * actions following a successful return from the corresponding {@code take()}.
 *
 * @since 1.5
 */
/*
 * 【任务执行-剥离框架】
 *
 * 先通过submit()提交任务，随后不断将已结束的任务（不管是否正常完成）存储到一个容器当中，
 * 后续可通过take()/poll()取出这些已结束任务，并获取它们的计算结果或任务状态。
 * 这样一来，已结束的任务和未结束（包括还未开始）的任务就被分离开了。
 */
// 1、根据线程池中Task的执行结果按执行完成的先后顺序排序，任务先完成的可优先获取到
// 内部维护了一个阻塞队列，当任务执行结束就把任务执行结果的Future对象加入到阻塞队列中
// 相比ExecutorService，CompletionService可以更精确和简便地完成异步任务的执行
// 2、Callable+Future虽然可以实现多个task并行执行，但是如果遇到前面的task执行较慢时需要阻塞等待前面的task执行完后面task才能取得结果，
// 而CompletionService的主要功能就是一边生成任务,一边获取任务的返回值，让两件事分开执行，任务之间不会互相阻塞，可以实现先执行完的先取结果，
// 不再依赖任务顺序了
// 3、CompletionService内部通过阻塞队列+FutureTask，实现了任务先完成可优先获取到，即结果按照完成先后顺序排序，内部有一个先进先出的阻塞队列，
// 用于保存已经执行完成的Future，通过调用它的take()或poll()可以获取到一个已经执行完成的Future，进而通过调用Future接口实现类的get方法获取最终的结果
// 4、当需要批量提交异步任务的时候建议使用CompletionService，CompletionService将线程池Executor和阻塞队列BlockingQueue的功能融合在了一起，
// 能够让批量异步任务的管理更简单
// 5、CompletionService能够让异步任务的执行结果有序化。先执行完的先进入阻塞队列，利用这个特性，你可以轻松实现后续处理的有序性，避免无谓的等待，
// 同时还可以快速实现诸如Forking Cluster这样的需求
public interface CompletionService<V> {
    
    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task.  Upon completion,
     * this task may be taken or polled.
     *
     * @param task the task to submit
     *
     * @return a Future representing pending completion of the task
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    // 提交/执行任务
    // 提交异步任务Callable
    Future<V> submit(Callable<V> task);
    
    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task.  Upon completion, this task may be
     * taken or polled.
     *
     * @param task   the task to submit
     * @param result the result to return upon successful completion
     *
     * @return a Future representing pending completion of the task,
     * and whose {@code get()} method will return the given
     * result value upon completion
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    // 提交/执行任务
    // 提交异步任务Runnable
    Future<V> submit(Runnable task, V result);
    
    /**
     * Retrieves and removes the Future representing the next
     * completed task, waiting if none are yet present.
     *
     * @return the Future representing the next completed task
     *
     * @throws InterruptedException if interrupted while waiting
     */
    // 取出一个已结束任务，可能会被阻塞
    // 从阻塞队列中获取并移除阻塞队列第一个元素，如果队列为空，当前线程阻塞
    Future<V> take() throws InterruptedException;
    
    /**
     * Retrieves and removes the Future representing the next
     * completed task, or {@code null} if none are present.
     *
     * @return the Future representing the next completed task, or
     * {@code null} if none are present
     */
    // 取出一个已结束任务，不会被阻塞，但可能返回null
    // 从阻塞队列中获取并移除阻塞队列第一个元素，如果队列为空，当前线程会返回null
    Future<V> poll();
    
    /**
     * Retrieves and removes the Future representing the next
     * completed task, waiting if necessary up to the specified wait
     * time if none are yet present.
     *
     * @param timeout how long to wait before giving up, in units of
     *                {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     *
     * @return the Future representing the next completed task or
     * {@code null} if the specified waiting time elapses
     * before one is present
     *
     * @throws InterruptedException if interrupted while waiting
     */
    // 取出一个已结束任务，如果没有合适任务，会在指定的时间内阻塞，超时后返回null
    // 从阻塞队列中获取并移除阻塞队列第一个元素，如果超时时间到，队列还是空，那么该方法会返回null
    Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException;
    
}
