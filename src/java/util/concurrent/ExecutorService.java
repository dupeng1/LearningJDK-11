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

import java.util.Collection;
import java.util.List;

/**
 * An {@link Executor} that provides methods to manage termination and
 * methods that can produce a {@link Future} for tracking progress of
 * one or more asynchronous tasks.
 *
 * <p>An {@code ExecutorService} can be shut down, which will cause
 * it to reject new tasks.  Two different methods are provided for
 * shutting down an {@code ExecutorService}. The {@link #shutdown}
 * method will allow previously submitted tasks to execute before
 * terminating, while the {@link #shutdownNow} method prevents waiting
 * tasks from starting and attempts to stop currently executing tasks.
 * Upon termination, an executor has no tasks actively executing, no
 * tasks awaiting execution, and no new tasks can be submitted.  An
 * unused {@code ExecutorService} should be shut down to allow
 * reclamation of its resources.
 *
 * <p>Method {@code submit} extends base method {@link
 * Executor#execute(Runnable)} by creating and returning a {@link Future}
 * that can be used to cancel execution and/or wait for completion.
 * Methods {@code invokeAny} and {@code invokeAll} perform the most
 * commonly useful forms of bulk execution, executing a collection of
 * tasks and then waiting for at least one, or all, to
 * complete. (Class {@link ExecutorCompletionService} can be used to
 * write customized variants of these methods.)
 *
 * <p>The {@link Executors} class provides factory methods for the
 * executor services provided in this package.
 *
 * <h3>Usage Examples</h3>
 *
 * Here is a sketch of a network service in which threads in a thread
 * pool service incoming requests. It uses the preconfigured {@link
 * Executors#newFixedThreadPool} factory method:
 *
 * <pre> {@code
 * class NetworkService implements Runnable {
 *   private final ServerSocket serverSocket;
 *   private final ExecutorService pool;
 *
 *   public NetworkService(int port, int poolSize)
 *       throws IOException {
 *     serverSocket = new ServerSocket(port);
 *     pool = Executors.newFixedThreadPool(poolSize);
 *   }
 *
 *   public void run() { // run the service
 *     try {
 *       for (;;) {
 *         pool.execute(new Handler(serverSocket.accept()));
 *       }
 *     } catch (IOException ex) {
 *       pool.shutdown();
 *     }
 *   }
 * }
 *
 * class Handler implements Runnable {
 *   private final Socket socket;
 *   Handler(Socket socket) { this.socket = socket; }
 *   public void run() {
 *     // read and service request on socket
 *   }
 * }}</pre>
 *
 * The following method shuts down an {@code ExecutorService} in two phases,
 * first by calling {@code shutdown} to reject incoming tasks, and then
 * calling {@code shutdownNow}, if necessary, to cancel any lingering tasks:
 *
 * <pre> {@code
 * void shutdownAndAwaitTermination(ExecutorService pool) {
 *   pool.shutdown(); // Disable new tasks from being submitted
 *   try {
 *     // Wait a while for existing tasks to terminate
 *     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
 *       pool.shutdownNow(); // Cancel currently executing tasks
 *       // Wait a while for tasks to respond to being cancelled
 *       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
 *           System.err.println("Pool did not terminate");
 *     }
 *   } catch (InterruptedException ie) {
 *     // (Re-)Cancel if current thread also interrupted
 *     pool.shutdownNow();
 *     // Preserve interrupt status
 *     Thread.currentThread().interrupt();
 *   }
 * }}</pre>
 *
 * <p>Memory consistency effects: Actions in a thread prior to the
 * submission of a {@code Runnable} or {@code Callable} task to an
 * {@code ExecutorService}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * any actions taken by that task, which in turn <i>happen-before</i> the
 * result is retrieved via {@code Future.get()}.
 *
 * @author Doug Lea
 * @since 1.5
 */
/*
 * ExecutorService表示一个【任务执行框架】
 * 典型的实现类为【任务池】ForkJoinPool与【线程池】ThreadPoolExecutor
 *
 * 客户端向【任务执行框架】提交任务后，由【任务执行框架】调度、执行任务，
 * 当任务结束后，可以选择让【任务执行框架】暂停/阻塞或停止。
 */
// 线程池定义的一个接口
public interface ExecutorService extends Executor {
    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * @throws SecurityException if a security manager exists and
     *                           shutting down this ExecutorService may manipulate
     *                           threads that the caller is not permitted to modify
     *                           because it does not hold {@link
     *                           java.lang.RuntimePermission}{@code ("modifyThread")},
     *                           or the security manager's {@code checkAccess} method
     *                           denies access.
     */
    // 有序关闭【任务执行框架】，正在执行的任务不受影响
    // 1、如果应用程序是通过main方法启动的，在这个main退出后，如果应用程序中的ExecutorService没有关闭，这个应用将一直运行
    // 之所以会出现这种情况，是因为ExecutorService中运行的线程会组织JVM关闭
    // 2、如果要关闭ExecutorService中执行的线程，可以调用ExecutorService.shutdown()方法，
    // 在调用shutdown()方法之后，ExecutorService不会立即关闭，但是它不再接收新的任务，
    // 直到当前所有线程执行完成才会关闭，所有在shutdown()执行之前提交的任务都会被执行。
    void shutdown();
    
    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so any
     * task that fails to respond to interrupts may never terminate.
     *
     * @return list of tasks that never commenced execution
     *
     * @throws SecurityException if a security manager exists and
     *                           shutting down this ExecutorService may manipulate
     *                           threads that the caller is not permitted to modify
     *                           because it does not hold {@link
     *                           java.lang.RuntimePermission}{@code ("modifyThread")},
     *                           or the security manager's {@code checkAccess} method
     *                           denies access.
     */
    // 强制关闭【任务执行框架】，包括关闭正在执行的任务。返回还未执行的任务列表
    // 1、如果我们想立即关闭ExecutorService，我们可以调用ExecutorService.shutdownNow()方法，
    // 这个动作将跳过所有正在执行的任务和被提交还没有执行的任务。但是它并不对正在执行的任务做任何保证，有可能它们都会停止，也有可能执行完成。
    List<Runnable> shutdownNow();
    
    /**
     * Blocks until all tasks have completed execution after a shutdown
     * request, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     *
     * @return {@code true} if this executor terminated and
     * {@code false} if the timeout elapsed before termination
     *
     * @throws InterruptedException if interrupted while waiting
     */
    // 等待【任务执行框架】终止后，返回true
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
    
    /**
     * Returns {@code true} if this executor has been shut down.
     *
     * @return {@code true} if this executor has been shut down
     */
    // 判断【任务执行框架】是否已关闭（不再接收新任务）
    boolean isShutdown();
    
    /**
     * Returns {@code true} if all tasks have completed following shut down.
     * Note that {@code isTerminated} is never {@code true} unless
     * either {@code shutdown} or {@code shutdownNow} was called first.
     *
     * @return {@code true} if all tasks have completed following shut down
     */
    // 判断【任务执行框架】是否已终止运行（所有任务已结束）
    boolean isTerminated();
    
    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     *
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task the task to submit
     * @param <T>  the type of the task's result
     *
     * @return a Future representing pending completion of the task
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    // 包装/提交/执行Callable型的任务
    // 和Submit(Runnable)类似，返回一个Future对象
    // Callable接口中的call()方法有一个返回值，可以返回任务的执行结果
    // Runnable接口中的run()方法是void的，没有返回值
    // future.get()方法会返回一个Callable任务执行结果，future.get()方法会产生阻塞
    <T> Future<T> submit(Callable<T> task);
    
    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param task the task to submit
     *
     * @return a Future representing pending completion of the task
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    // 包装/提交/执行Runnable型的任务
    // 可以获知任务执行结果，返回Future对象，通过该对象可以检查提交的任务是否执行完毕
    // future.get()方法会返回一个null，future.get()方法会产生阻塞
    Future<?> submit(Runnable task);
    
    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return the given result upon successful completion.
     *
     * @param task   the task to submit
     * @param result the result to return
     * @param <T>    the type of the result
     *
     * @return a Future representing pending completion of the task
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is null
     */
    // 包装/提交/执行Runnable型的任务，并预设一个返回结果
    <T> Future<T> submit(Runnable task, T result);
    
    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results when all complete.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T>   the type of the values returned from the tasks
     *
     * @return a list of Futures representing the tasks, in the same
     * sequential order as produced by the iterator for the
     * given task list, each of which has completed
     *
     * @throws InterruptedException       if interrupted while waiting, in
     *                                    which case unfinished tasks are cancelled
     * @throws NullPointerException       if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *                                    scheduled for execution
     */
    // 执行指定容器中的所有任务，返回值是所有包装后的任务列表
    // 1、接收Callable的集合，执行这个方法会返回一个Future的List，其中对应着每个Callable任务执行后的Future对象
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;
    
    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Upon return, tasks that have not completed are cancelled.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks   the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @param <T>     the type of the values returned from the tasks
     *
     * @return a list of Futures representing the tasks, in the same
     * sequential order as produced by the iterator for the
     * given task list. If the operation did not time out,
     * each task will have completed. If it did time out, some
     * of these tasks will not have completed.
     *
     * @throws InterruptedException       if interrupted while waiting, in
     *                                    which case unfinished tasks are cancelled
     * @throws NullPointerException       if tasks, any of its elements, or
     *                                    unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *                                    for execution
     */
    // 在指定时间内执行指定容器中的所有任务，返回值是所有包装后的任务列表（包括超时后被中止的任务）
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException;
    
    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do. Upon normal or exceptional return,
     * tasks that have not completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T>   the type of the values returned from the tasks
     *
     * @return the result returned by one of the tasks
     *
     * @throws InterruptedException       if interrupted while waiting
     * @throws NullPointerException       if tasks or any element task
     *                                    subject to execution is {@code null}
     * @throws IllegalArgumentException   if tasks is empty
     * @throws ExecutionException         if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *                                    for execution
     */
    // 从任一任务开始执行，只要发现某个任务已结束，就中断其他正在执行的任务，并返回首个被发现结束的任务的计算结果
    // 1、接收Callable的集合，执行这个方法不会返回Future，但是会返回所有Callable任务中其中一个任务的执行结果，
    // 这个方法也无法保证返回的是哪个任务的执行结果，反正是其中的某一个
    // 2、每次执行都会返回一个结果，并且返回的结果是变化的，可能返回Task2也可能是Task1或者其他
    <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException;
    
    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do before the given timeout elapses.
     * Upon normal or exceptional return, tasks that have not
     * completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks   the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @param <T>     the type of the values returned from the tasks
     *
     * @return the result returned by one of the tasks
     *
     * @throws InterruptedException       if interrupted while waiting
     * @throws NullPointerException       if tasks, or unit, or any element
     *                                    task subject to execution is {@code null}
     * @throws TimeoutException           if the given timeout elapses before
     *                                    any task successfully completes
     * @throws ExecutionException         if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *                                    for execution
     */
    // 运作方式同invokeAny(Collection)，不过这里限制这些操作要在指定的时间内完成，否则就抛出异常
    <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
