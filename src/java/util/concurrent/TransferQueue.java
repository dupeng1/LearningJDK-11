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
 * A {@link BlockingQueue} in which producers may wait for consumers
 * to receive elements.  A {@code TransferQueue} may be useful for
 * example in message passing applications in which producers
 * sometimes (using method {@link #transfer}) await receipt of
 * elements by consumers invoking {@code take} or {@code poll}, while
 * at other times enqueue elements (via method {@code put}) without
 * waiting for receipt.
 * {@linkplain #tryTransfer(Object) Non-blocking} and
 * {@linkplain #tryTransfer(Object, long, TimeUnit) time-out} versions of
 * {@code tryTransfer} are also available.
 * A {@code TransferQueue} may also be queried, via {@link
 * #hasWaitingConsumer}, whether there are any threads waiting for
 * items, which is a converse analogy to a {@code peek} operation.
 *
 * <p>Like other blocking queues, a {@code TransferQueue} may be
 * capacity bounded.  If so, an attempted transfer operation may
 * initially block waiting for available space, and/or subsequently
 * block waiting for reception by a consumer.  Note that in a queue
 * with zero capacity, such as {@link SynchronousQueue}, {@code put}
 * and {@code transfer} are effectively synonymous.
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements held in this queue
 *
 * @author Doug Lea
 * @since 1.7
 */
// 数据传递接口
// 如果有消费者正在获取元素，则将队列中的元素传递给消费者。如果没有消费者，则等待消费者消费
public interface TransferQueue<E> extends BlockingQueue<E> {
    
    /**
     * Transfers the element to a consumer, waiting if necessary to do so.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long, TimeUnit) poll}),
     * else waits until the element is received by a consumer.
     *
     * @param e the element to transfer
     *
     * @throws InterruptedException     if interrupted while waiting,
     *                                  in which case the element is not left enqueued
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    // 生产者传递元素给消费者，没有消费者时阻塞，直到消费者到达时解除阻塞
    // 1、圆通快递员要将小明的2个快递送货到门，韵达快递员也想将小明的2个快递送货到门。小明一次只能拿一个，快递员必须等小明拿了一个后，才能继续给第二个
    // 2、如果当前有消费者正在等待接收元素（消费者通过take方法或超时限制的poll方法时），
    // transfer方法可以把生产者传入的元素立刻transfer（传输）给消费者。
    // 3、如果没有消费者等待接收元素，transfer方法会将元素放在队列的tail（尾）节点，并等到该元素被消费者消费了才返回。
    void transfer(E e) throws InterruptedException;
    
    /**
     * Transfers the element to a waiting consumer immediately, if possible.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long, TimeUnit) poll}),
     * otherwise returning {@code false} without enqueuing the element.
     *
     * @param e the element to transfer
     *
     * @return {@code true} if the element was transferred, else
     * {@code false}
     *
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    // 生产者尝试传递元素给消费者，没有消费者时返回false
    // 1、圆通快递员要将小明的2个快递送货到门，韵达快递员也想将小明的2个快递送货到门。发现小明不在家，就把快递直接放到菜鸟驿站了
    // 2、试探生产者传入的元素是否能直接传给消费者
    // 3、如果没有消费者等待接收元素，则返回false
    // 4、和transfer方法的区别是，无论消费者是否接收，方法立即返回。
    boolean tryTransfer(E e);
    
    /**
     * Transfers the element to a consumer if it is possible to do so
     * before the timeout elapses.
     *
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long, TimeUnit) poll}),
     * else waits until the element is received by a consumer,
     * returning {@code false} if the specified wait time elapses
     * before the element can be transferred.
     *
     * @param e       the element to transfer
     * @param timeout how long to wait before giving up, in units of
     *                {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     *
     * @return {@code true} if successful, or {@code false} if
     * the specified waiting time elapses before completion,
     * in which case the element is not left enqueued
     *
     * @throws InterruptedException     if interrupted while waiting,
     *                                  in which case the element is not left enqueued
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    // 尝试传递元素，没有消费者时则阻塞一段时间，超时后无法传递则返回false
    // 1、圆通快递员要将小明的2个快递送货到门，韵达快递员也想将小明的2个快递送货到门。发现小明不在家，
    // 于是先等了5分钟，发现小明还没有回来，就把快递直接放到菜鸟驿站了
    // 2、带有时间限制的tryTransfer方法
    // 3、带有时间限制的tryTransfer方法
    // 4、如果没有消费者消费该元素则等待指定的时间再返回
    // 5、如果超时了还没有消费元素，则返回 false
    // 6、如果在超时时间内消费了元素，则返回 true
    boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException;
    
    /**
     * Returns {@code true} if there is at least one consumer waiting
     * to receive an element via {@link #take} or
     * timed {@link #poll(long, TimeUnit) poll}.
     * The return value represents a momentary state of affairs.
     *
     * @return {@code true} if there is at least one waiting consumer
     */
    // 判断队列中是否包含消费者
    boolean hasWaitingConsumer();
    
    /**
     * Returns an estimate of the number of consumers waiting to
     * receive elements via {@link #take} or timed
     * {@link #poll(long, TimeUnit) poll}.  The return value is an
     * approximation of a momentary state of affairs, that may be
     * inaccurate if consumers have completed or given up waiting.
     * The value may be useful for monitoring and heuristics, but
     * not for synchronization control.  Implementations of this
     * method are likely to be noticeably slower than those for
     * {@link #hasWaitingConsumer}.
     *
     * @return the number of consumers waiting to receive elements
     */
    // 返回队列中包含的消费者数量
    int getWaitingConsumerCount();
}
