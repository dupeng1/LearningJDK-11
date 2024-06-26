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
import java.util.Queue;

/**
 * A {@link Queue} that additionally supports operations that wait for
 * the queue to become non-empty when retrieving an element, and wait
 * for space to become available in the queue when storing an element.
 *
 * <p>{@code BlockingQueue} methods come in four forms, with different ways
 * of handling operations that cannot be satisfied immediately, but may be
 * satisfied at some point in the future:
 * one throws an exception, the second returns a special value (either
 * {@code null} or {@code false}, depending on the operation), the third
 * blocks the current thread indefinitely until the operation can succeed,
 * and the fourth blocks for only a given maximum time limit before giving
 * up.  These methods are summarized in the following table:
 *
 * <table class="plain">
 * <caption>Summary of BlockingQueue methods</caption>
 * <tr>
 * <td></td>
 * <th scope="col" style="font-weight:normal; font-style:italic">Throws exception</th>
 * <th scope="col" style="font-weight:normal; font-style:italic">Special value</th>
 * <th scope="col" style="font-weight:normal; font-style:italic">Blocks</th>
 * <th scope="col" style="font-weight:normal; font-style:italic">Times out</th>
 * </tr>
 * <tr>
 * <th scope="row" style="text-align:left">Insert</th>
 * <td>{@link #add(Object) add(e)}</td>
 * <td>{@link #offer(Object) offer(e)}</td>
 * <td>{@link #put(Object) put(e)}</td>
 * <td>{@link #offer(Object, long, TimeUnit) offer(e, time, unit)}</td>
 * </tr>
 * <tr>
 * <th scope="row" style="text-align:left">Remove</th>
 * <td>{@link #remove() remove()}</td>
 * <td>{@link #poll() poll()}</td>
 * <td>{@link #take() take()}</td>
 * <td>{@link #poll(long, TimeUnit) poll(time, unit)}</td>
 * </tr>
 * <tr>
 * <th scope="row" style="text-align:left">Examine</th>
 * <td>{@link #element() element()}</td>
 * <td>{@link #peek() peek()}</td>
 * <td style="font-style: italic">not applicable</td>
 * <td style="font-style: italic">not applicable</td>
 * </tr>
 * </table>
 *
 * <p>A {@code BlockingQueue} does not accept {@code null} elements.
 * Implementations throw {@code NullPointerException} on attempts
 * to {@code add}, {@code put} or {@code offer} a {@code null}.  A
 * {@code null} is used as a sentinel value to indicate failure of
 * {@code poll} operations.
 *
 * <p>A {@code BlockingQueue} may be capacity bounded. At any given
 * time it may have a {@code remainingCapacity} beyond which no
 * additional elements can be {@code put} without blocking.
 * A {@code BlockingQueue} without any intrinsic capacity constraints always
 * reports a remaining capacity of {@code Integer.MAX_VALUE}.
 *
 * <p>{@code BlockingQueue} implementations are designed to be used
 * primarily for producer-consumer queues, but additionally support
 * the {@link Collection} interface.  So, for example, it is
 * possible to remove an arbitrary element from a queue using
 * {@code remove(x)}. However, such operations are in general
 * <em>not</em> performed very efficiently, and are intended for only
 * occasional use, such as when a queued message is cancelled.
 *
 * <p>{@code BlockingQueue} implementations are thread-safe.  All
 * queuing methods achieve their effects atomically using internal
 * locks or other forms of concurrency control. However, the
 * <em>bulk</em> Collection operations {@code addAll},
 * {@code containsAll}, {@code retainAll} and {@code removeAll} are
 * <em>not</em> necessarily performed atomically unless specified
 * otherwise in an implementation. So it is possible, for example, for
 * {@code addAll(c)} to fail (throwing an exception) after adding
 * only some of the elements in {@code c}.
 *
 * <p>A {@code BlockingQueue} does <em>not</em> intrinsically support
 * any kind of &quot;close&quot; or &quot;shutdown&quot; operation to
 * indicate that no more items will be added.  The needs and usage of
 * such features tend to be implementation-dependent. For example, a
 * common tactic is for producers to insert special
 * <em>end-of-stream</em> or <em>poison</em> objects, that are
 * interpreted accordingly when taken by consumers.
 *
 * <p>
 * Usage example, based on a typical producer-consumer scenario.
 * Note that a {@code BlockingQueue} can safely be used with multiple
 * producers and multiple consumers.
 * <pre> {@code
 * class Producer implements Runnable {
 *   private final BlockingQueue queue;
 *   Producer(BlockingQueue q) { queue = q; }
 *   public void run() {
 *     try {
 *       while (true) { queue.put(produce()); }
 *     } catch (InterruptedException ex) { ... handle ...}
 *   }
 *   Object produce() { ... }
 * }
 *
 * class Consumer implements Runnable {
 *   private final BlockingQueue queue;
 *   Consumer(BlockingQueue q) { queue = q; }
 *   public void run() {
 *     try {
 *       while (true) { consume(queue.take()); }
 *     } catch (InterruptedException ex) { ... handle ...}
 *   }
 *   void consume(Object x) { ... }
 * }
 *
 * class Setup {
 *   void main() {
 *     BlockingQueue q = new SomeQueueImplementation();
 *     Producer p = new Producer(q);
 *     Consumer c1 = new Consumer(q);
 *     Consumer c2 = new Consumer(q);
 *     new Thread(p).start();
 *     new Thread(c1).start();
 *     new Thread(c2).start();
 *   }
 * }}</pre>
 *
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code BlockingQueue}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code BlockingQueue} in another thread.
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements held in this queue
 *
 * @author Doug Lea
 * @since 1.5
 */
/*
 * 单向阻塞队列
 *
 * 主要特性：
 * 1.阻塞队列是线程安全的，入队/出队互不干扰
 * 2.在不满足入队/出队条件时，可以选择阻塞操作线程，或选择快速失败
 */
// 1、很好解决了多线程中，如何高效安全传输数据的问题
// 2、多线程环境中，通过队列可以很容易实现数据共享，比如经典的“生产者”和“消费者”模型中，通过队列可以很便利地实现两者之间的数据共享。
// 3、假设我们有若干生产者线程，另外又有若干个消费者线程。如果生产者线程需要把准备好的数据共享给消费者线程，利用队列的方式来传递数据，
// 就可以很方便地解决他们之间的数据共享问题。但如果生产者和消费者在某个时间段内，万一发生数据处理速度不匹配的情况呢？理想情况下，
// 如果生产者产出数据的速度大于消费者消费的速度，并且当生产出来的数据累积到一定程度的时候，那么生产者必须暂停等待一下（阻塞生产者线程），
// 以便等待消费者线程把累积的数据处理完毕，反之亦然。然而，在concurrent包发布以前，在多线程环境下，我们每个程序员都必须去自己控制这些细节，
// 尤其还要兼顾效率和线程安全，而这会给我们的程序带来不小的复杂度。好在此时，强大的concurrent包横空出世了，而它也给我们带来了强大的BlockingQueue。（
// 在多线程领域：所谓阻塞，在某些情况下会挂起线程（即阻塞），一旦条件满足，被挂起的线程又会自动被唤醒），
// 4、add、remove         非阻塞会抛异常
//    offer、poll         非阻塞会抛异常
//    put、take           阻塞
public interface BlockingQueue<E> extends Queue<E> {
    
    /*▼ 入队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Inserts the specified element into this queue, waiting if necessary
     * for space to become available.
     *
     * @param e the element to add
     *
     * @throws InterruptedException     if interrupted while waiting
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    // 入队，无法入队时扩容或阻塞
    // 1、把e加到BlockingQueue里，如果BlockQueue没有空间，则调用此方法的线程被阻塞，直到BlockingQueue里面有空间再继续
    void put(E e) throws InterruptedException;
    
    
    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an
     * {@code IllegalStateException} if no space is currently available.
     * When using a capacity-restricted queue, it is generally preferable to
     * use {@link #offer(Object) offer}.
     *
     * @param e the element to add
     *
     * @return {@code true} (as specified by {@link Collection#add})
     *
     * @throws IllegalStateException    if the element cannot be added at this
     *                                  time due to capacity restrictions
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    // 入队，无法入队时扩容或抛异常，不阻塞
    // 1、如果可以在不违反容量限制的情况下立即将指定元素插入此队列，则在成功时返回true，
    // 2、如果当前没有可用空间则IllegalStateException。当使用容量受限的队列时，通常最好使用offer
    boolean add(E e);
    
    
    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions, returning
     * {@code true} upon success and {@code false} if no space is currently
     * available.  When using a capacity-restricted queue, this method is
     * generally preferable to {@link #add}, which can fail to insert an
     * element only by throwing an exception.
     *
     * @param e the element to add
     *
     * @return {@code true} if the element was added to this queue, else
     * {@code false}
     *
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    // 入队，无法入队时扩容或返回false，不阻塞
    // 1、如果可能的话，将e加到BlockingQueue里，即如果BlockingQueue可以容纳，则返回true，否则返回false
    boolean offer(E e);
    
    /**
     * Inserts the specified element into this queue, waiting up to the
     * specified wait time if necessary for space to become available.
     *
     * @param e       the element to add
     * @param timeout how long to wait before giving up, in units of
     *                {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     *
     * @return {@code true} if successful, or {@code false} if
     * the specified waiting time elapses before space is available
     *
     * @throws InterruptedException     if interrupted while waiting
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    // 入队，无法入队时扩容，或阻塞一段时间，超时后无法入队则返回false
    // 1、可以设定等待的时间，如果在指定的时间内，还不能往队列中加入e，则返回失败
    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;
    
    /*▲ 入队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 出队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     *
     * @throws InterruptedException if interrupted while waiting
     */
    // 出队，无法出队时阻塞
    // 1、取走BlockingQueue里排在首位的对象，若BlockingQueue为空，阻塞进入等待状态直到BlockingQueue有新的数据被加入
    E take() throws InterruptedException;
    
    
    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.
     * Returns {@code true} if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     *
     * @param o element to be removed from this queue, if present
     *
     * @return {@code true} if this queue changed as a result of the call
     *
     * @throws ClassCastException   if the class of the specified element
     *                              is incompatible with this queue
     *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    // 移除，移除成功则返回true（该方法来自Collection）
    // 1、从此队列中移除指定元素的单个实例（如果存在），则返回true
    boolean remove(Object o);
    
    
    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *                {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     *
     * @return the head of this queue, or {@code null} if the
     * specified waiting time elapses before an element is available
     *
     * @throws InterruptedException if interrupted while waiting
     */
    // 出队，无法出队时阻塞一段时间，超时后无法出队则返回null
    // 1、从BlockingQueue取出一个队首的对象，如果在指定时间内，队列一旦有数据可取，则立即返回队列中的数据。
    // 2、否则时间超时还没有数据可取，返回失败。
    E poll(long timeout, TimeUnit unit) throws InterruptedException;
    
    
    /**
     * Removes all available elements from this queue and adds them
     * to the given collection.  This operation may be more
     * efficient than repeatedly polling this queue.  A failure
     * encountered while attempting to add elements to
     * collection {@code c} may result in elements being in neither,
     * either or both collections when the associated exception is
     * thrown.  Attempts to drain a queue to itself result in
     * {@code IllegalArgumentException}. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * @param c the collection to transfer elements into
     *
     * @return the number of elements transferred
     *
     * @throws UnsupportedOperationException if addition of elements
     *                                       is not supported by the specified collection
     * @throws ClassCastException            if the class of an element of this queue
     *                                       prevents it from being added to the specified collection
     * @throws NullPointerException          if the specified collection is null
     * @throws IllegalArgumentException      if the specified collection is this
     *                                       queue, or some property of an element of this queue prevents
     *                                       it from being added to the specified collection
     */
    // 将队列中所有元素移除，并转移到给定的容器当中
    // 1、一次性从BlockingQueue获取所有可用的数据对象，通过该方法，可以提升获取数据效率；不需要多次分批加锁或释放锁
    int drainTo(Collection<? super E> c);
    
    /**
     * Removes at most the given number of available elements from
     * this queue and adds them to the given collection.  A failure
     * encountered while attempting to add elements to
     * collection {@code c} may result in elements being in neither,
     * either or both collections when the associated exception is
     * thrown.  Attempts to drain a queue to itself result in
     * {@code IllegalArgumentException}. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * @param c           the collection to transfer elements into
     * @param maxElements the maximum number of elements to transfer
     *
     * @return the number of elements transferred
     *
     * @throws UnsupportedOperationException if addition of elements
     *                                       is not supported by the specified collection
     * @throws ClassCastException            if the class of an element of this queue
     *                                       prevents it from being added to the specified collection
     * @throws NullPointerException          if the specified collection is null
     * @throws IllegalArgumentException      if the specified collection is this
     *                                       queue, or some property of an element of this queue prevents
     *                                       it from being added to the specified collection
     */
    // 将队列中前maxElements个元素移除，并转移到给定的容器当中
    // 1、一次性从BlockingQueue获取所有可用的数据对象（还可以指定获取数据的个数），通过该方法，可以提升获取数据效率；不需要多次分批加锁或释放锁
    int drainTo(Collection<? super E> c, int maxElements);
    
    /*▲ 出队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     *
     * @return {@code true} if this queue contains the specified element
     *
     * @throws ClassCastException   if the class of the specified element
     *                              is incompatible with this queue
     *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    // 判断队列中是否包含指定的元素
    boolean contains(Object o);
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the number of additional elements that this queue can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking, or {@code Integer.MAX_VALUE} if there is no intrinsic
     * limit.
     *
     * <p>Note that you <em>cannot</em> always tell if an attempt to insert
     * an element will succeed by inspecting {@code remainingCapacity}
     * because it may be the case that another thread is about to
     * insert or remove an element.
     *
     * @return the remaining capacity
     */
    // 计算队列剩余空间
    int remainingCapacity();
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
