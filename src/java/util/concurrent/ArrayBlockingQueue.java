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

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A bounded {@linkplain BlockingQueue blocking queue} backed by an
 * array.  This queue orders elements FIFO (first-in-first-out).  The
 * <em>head</em> of the queue is that element that has been on the
 * queue the longest time.  The <em>tail</em> of the queue is that
 * element that has been on the queue the shortest time. New elements
 * are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 *
 * <p>This is a classic &quot;bounded buffer&quot;, in which a
 * fixed-sized array holds elements inserted by producers and
 * extracted by consumers.  Once created, the capacity cannot be
 * changed.  Attempts to {@code put} an element into a full queue
 * will result in the operation blocking; attempts to {@code take} an
 * element from an empty queue will similarly block.
 *
 * <p>This class supports an optional fairness policy for ordering
 * waiting producer and consumer threads.  By default, this ordering
 * is not guaranteed. However, a queue constructed with fairness set
 * to {@code true} grants threads access in FIFO order. Fairness
 * generally decreases throughput but reduces variability and avoids
 * starvation.
 *
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Collection} and {@link Iterator} interfaces.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this queue
 */
/*
 * 顺序有界（容量固定）循环阻塞队列，线程安全（锁）
 *
 * 注：该循环队列是通过count域的值来识别队空还是队满的
 */
// 并发阻塞是通过ReentrantLock和Condition来实现的，ArrayBlockingQueue内部只有一把锁，意味着同一时刻只有一个线程能进行入队或者出队的操作。
// enqueue：元素入队，putIndex + 1，队尾加1
// dequeue：元素出队，takeIndex + 1，队头加1
public class ArrayBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
    
    /*
     * Much of the implementation mechanics, especially the unusual
     * nested loops, are shared and co-maintained with ArrayDeque.
     */
    
    /** The queued items */
    // 存储队列中的元素
    final Object[] items;
    
    /** items index for next take, poll, peek or remove */
    // 队头
    int takeIndex;
    
    /** items index for next put, offer, or add */
    // 队尾（为null，等待接收元素）
    int putIndex;
    
    /** Number of elements in the queue */
    // 队列中的元素个数
    int count;
    
    /*
     * Concurrency control uses the classic two-condition algorithm found in any textbook.
     */
    
    /** Main lock guarding all access */
    // 队列锁
    final ReentrantLock lock;
    
    /** Condition for waiting takes */
    // 出队条件
    private final Condition notEmpty;
    
    /** Condition for waiting puts */
    // 入队条件
    private final Condition notFull;
    
    /**
     * Shared state for currently active iterators, or null if there
     * are known not to be any.  Allows queue operations to update
     * iterator state.
     */
    // 记录作用于该队列的迭代器（一个链表）
    transient Itrs itrs;
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Creates an {@code ArrayBlockingQueue} with the given (fixed)
     * capacity and default access policy.
     *
     * @param capacity the capacity of this queue
     *
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public ArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }
    
    /**
     * Creates an {@code ArrayBlockingQueue} with the given (fixed)
     * capacity, the specified access policy and initially containing the
     * elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param capacity the capacity of this queue
     * @param fair     if {@code true} then queue accesses for threads blocked
     *                 on insertion or removal, are processed in FIFO order;
     *                 if {@code false} the access order is unspecified.
     * @param c        the collection of elements to initially contain
     *
     * @throws IllegalArgumentException if {@code capacity} is less than
     *                                  {@code c.size()}, or less than 1.
     * @throws NullPointerException     if the specified collection or any
     *                                  of its elements are null
     */
    // 用指定容器中的数据初始化阻塞队列
    public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) {
        this(capacity, fair);
        
        final ReentrantLock lock = this.lock;
        lock.lock(); // Lock only for visibility, not mutual exclusion
        try {
            final Object[] items = this.items;
            int i = 0;
            try {
                for(E e : c) {
                    items[i++] = Objects.requireNonNull(e);
                }
            } catch(ArrayIndexOutOfBoundsException ex) {
                throw new IllegalArgumentException();
            }
            count = i;
            putIndex = (i == capacity) ? 0 : i;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Creates an {@code ArrayBlockingQueue} with the given (fixed)
     * capacity and the specified access policy.
     *
     * @param capacity the capacity of this queue
     * @param fair     if {@code true} then queue accesses for threads blocked
     *                 on insertion or removal, are processed in FIFO order;
     *                 if {@code false} the access order is unspecified.
     *
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    // fair表示插入或者删除的线程是否按照先入先出顺序，锁是否是公平锁
    public ArrayBlockingQueue(int capacity, boolean fair) {
        if(capacity<=0) {
            throw new IllegalArgumentException();
        }
        
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 入队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Inserts the specified element at the tail of this queue if it is
     * possible to do so immediately without exceeding the queue's capacity,
     * returning {@code true} upon success and {@code false} if this queue
     * is full.  This method is generally preferable to method {@link #add},
     * which can fail to insert an element only by throwing an exception.
     *
     * @throws NullPointerException if the specified element is null
     */
    // 入队，线程安全，队满时不阻塞，直接返回false
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        final ReentrantLock lock = this.lock;
        // 申请独占锁，允许阻塞带有中断标记的线程
        lock.lock();
        try {
            // 如果队列满了，返回false
            if(count == items.length) {
                return false;
            }
            // 入队
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Inserts the specified element at the tail of this queue, waiting
     * up to the specified wait time for space to become available if
     * the queue is full.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    // 入队，线程安全，队满时阻塞一段时间，如果在指定的时间内没有机会插入元素，则返回false
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(e);
        
        long nanos = unit.toNanos(timeout);
        
        final ReentrantLock lock = this.lock;
        
        // 申请独占锁，不允许阻塞带有中断标记的线程
        lock.lockInterruptibly();
        try {
            // 如果队列满了，阻塞一段时间
            while(count == items.length) {
                if(nanos<=0L) {
                    // 如果超时，返回false
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            // 入队
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * Inserts the specified element at the tail of this queue, waiting
     * for space to become available if the queue is full.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    // 入队，线程安全，队满时线程被阻塞
    // 1、ArrayBlockingQueue不允许元素为null
    // 2、ArrayBlockingQueue在队列已满时将会调用notFull的await()方法释放锁并处于阻塞状态
    // 3、一旦ArrayBlockingQueue不为满的状态，就将元素入队
    public void put(E e) throws InterruptedException {
        Objects.requireNonNull(e);
        final ReentrantLock lock = this.lock;
        
        // 申请独占锁，不允许阻塞带有中断标记的线程
        lock.lockInterruptibly();
        try {
            // 如果队列满了，需要阻塞“入队”线程
            while(count == items.length) {
                notFull.await();
            }
            // 入队
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * Inserts the specified element at the tail of this queue if it is
     * possible to do so immediately without exceeding the queue's capacity,
     * returning {@code true} upon success and throwing an
     * {@code IllegalStateException} if this queue is full.
     *
     * @param e the element to add
     *
     * @return {@code true} (as specified by {@link Collection#add})
     *
     * @throws IllegalStateException if this queue is full
     * @throws NullPointerException  if the specified element is null
     */
    // 入队/添加，不满足入队/添加条件时不会阻塞，但会抛异常
    public boolean add(E e) {
        return super.add(e);
    }
    
    /*▲ 入队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 出队 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 出队，线程安全，队空时不阻塞，直接返回null
    public E poll() {
        final ReentrantLock lock = this.lock;
        // 申请独占锁，允许阻塞带有中断标记的线程
        lock.lock();
        try {
            // 如果队列为空，返回null
            if(count == 0) {
                return null;
            }
            
            // 出队
            return dequeue();
        } finally {
            lock.unlock();
        }
    }
    
    // 出队，线程安全，队空时阻塞一段时间，如果在指定的时间内没有机会取出元素，则返回null
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        
        final ReentrantLock lock = this.lock;
        
        // 申请独占锁，不允许阻塞带有中断标记的线程
        lock.lockInterruptibly();
        try {
            // 如果队列为空，阻塞一段时间
            while(count == 0) {
                if(nanos<=0L) {
                    // 如果超时，返回null
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            // 出队
            return dequeue();
        } finally {
            lock.unlock();
        }
    }
    
    
    // 出队，线程安全，队空时线程被阻塞
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        
        // 申请独占锁，不允许阻塞带有中断标记的线程
        lock.lockInterruptibly();
        try {
            // 如果队列为空，需要阻塞“出队”线程
            while(count == 0) {
                notEmpty.await();
            }
            // 出队
            return dequeue();
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.
     * Returns {@code true} if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     *
     * <p>Removal of interior elements in circular array based queues
     * is an intrinsically slow and disruptive operation, so should
     * be undertaken only in exceptional circumstances, ideally
     * only when the queue is known not to be accessible by other
     * threads.
     *
     * @param o element to be removed from this queue, if present
     *
     * @return {@code true} if this queue changed as a result of the call
     */
    // 移除指定的元素
    public boolean remove(Object o) {
        if(o == null) {
            return false;
        }
        
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if(count>0) {
                final Object[] items = this.items;
                // 定位待移除元素的索引
                for(int i = takeIndex, end = putIndex, to = (i<end) ? end : items.length; ; i = 0, to = end) {
                    for(; i<to; i++) {
                        if(o.equals(items[i])) {
                            // 移除指定索引处的元素，非线程安全
                            removeAt(i);
                            return true;
                        }
                    }
                    if(to == end) {
                        break;
                    }
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Deletes item at array index removeIndex.
     * Utility for remove(Object) and iterator.remove.
     * Call only when holding lock.
     */
    // 移除指定索引处的元素，非线程安全，仅限内部使用
    void removeAt(final int removeIndex) {
        // assert lock.isHeldByCurrentThread();
        // assert lock.getHoldCount() == 1;
        // assert items[removeIndex] != null;
        // assert removeIndex >= 0 && removeIndex < items.length;
        
        final Object[] items = this.items;
        
        // 移除队头元素
        if(removeIndex == takeIndex) {
            // removing front item; just advance
            items[takeIndex] = null;
            if(++takeIndex == items.length) {
                takeIndex = 0;
            }
            count--;
            if(itrs != null) {
                itrs.elementDequeued();
            }
            
            // 移除非队头元素（需要移动元素）
        } else {
            // an "interior" remove
            
            // slide over all others up through putIndex.
            for(int i = removeIndex, putIndex = this.putIndex; ; ) {
                int pred = i;
                if(++i == items.length) {
                    i = 0;
                }
                if(i == putIndex) {
                    items[pred] = null;
                    this.putIndex = pred;
                    break;
                }
                items[pred] = items[i];
            }
            count--;
            if(itrs != null) {
                itrs.removedAt(removeIndex);
            }
        }
        notFull.signal();
    }
    
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 移除所有满足过滤条件的元素，不阻塞（线程安全）
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        return bulkRemove(filter);
    }
    
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // (匹配则移除)移除队列中所有与给定容器中的元素匹配的元素，不阻塞（线程安全）
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> c.contains(e));
    }
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // (不匹配则移除)移除队列中所有与给定容器中的元素不匹配的元素，不阻塞（线程安全）
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> !c.contains(e));
    }
    
    
    /**
     * Atomically removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     */
    // 清空，即移除所有元素，不阻塞（线程安全）
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int k;
            if((k = count)>0) {
                circularClear(items, takeIndex, putIndex);
                takeIndex = putIndex;
                count = 0;
                if(itrs != null) {
                    itrs.queueIsEmpty();
                }
                for(; k>0 && lock.hasWaiters(notFull); k--) {
                    notFull.signal();
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    // 将队列中所有元素移除，并转移到给定的容器当中
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }
    
    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    // 将队列中前maxElements个元素移除，并转移到给定的容器当中
    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c);
        if(c == this)
            throw new IllegalArgumentException();
        if(maxElements<=0)
            return 0;
        final Object[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(maxElements, count);
            int take = takeIndex;
            int i = 0;
            try {
                while(i<n) {
                    @SuppressWarnings("unchecked")
                    E e = (E) items[take];
                    c.add(e);
                    items[take] = null;
                    if(++take == items.length)
                        take = 0;
                    i++;
                }
                return n;
            } finally {
                // Restore invariants even if c.add() threw
                if(i>0) {
                    count -= i;
                    takeIndex = take;
                    if(itrs != null) {
                        if(count == 0)
                            itrs.queueIsEmpty();
                        else if(i>take)
                            itrs.takeIndexWrapped();
                    }
                    for(; i>0 && lock.hasWaiters(notFull); i--)
                        notFull.signal();
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 出队 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 取值 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    // 返回（查看）队头元素，线程安全
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return itemAt(takeIndex); // null when queue is empty
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 取值 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 包含查询 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     *
     * @return {@code true} if this queue contains the specified element
     */
    // 判断队列中是否包含元素o
    public boolean contains(Object o) {
        if(o == null) {
            return false;
        }
        
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if(count>0) {
                final Object[] items = this.items;
                for(int i = takeIndex, end = putIndex, to = (i<end) ? end : items.length; ; i = 0, to = end) {
                    for(; i<to; i++)
                        if(o.equals(items[i]))
                            return true;
                    if(to == end)
                        break;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 包含查询 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 视图 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Object[] items = this.items;
            final int end = takeIndex + count;
            final Object[] a = Arrays.copyOfRange(items, takeIndex, end);
            if(end != putIndex) {
                System.arraycopy(items, 0, a, items.length - takeIndex, putIndex);
            }
            return a;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array.  If the queue fits in the specified array, it
     * is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this queue.
     *
     * <p>If this queue fits in the specified array with room to spare
     * (i.e., the array has more elements than this queue), the element in
     * the array immediately following the end of the queue is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     *
     * @return an array containing all of the elements in this queue
     *
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this queue
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Object[] items = this.items;
            final int count = this.count;
            final int firstLeg = Math.min(items.length - takeIndex, count);
            if(a.length<count) {
                a = (T[]) Arrays.copyOfRange(items, takeIndex, takeIndex + count, a.getClass());
            } else {
                System.arraycopy(items, takeIndex, a, 0, firstLeg);
                if(a.length>count) {
                    a[count] = null;
                }
            }
            if(firstLeg<count) {
                System.arraycopy(items, 0, a, firstLeg, putIndex);
            }
            return a;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 视图 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 迭代 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * @throws NullPointerException {@inheritDoc}
     */
    // 遍历所有元素，并执行相应的择取操作
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if(count>0) {
                final Object[] items = this.items;
                for(int i = takeIndex, end = putIndex, to = (i<end) ? end : items.length; ; i = 0, to = end) {
                    for(; i<to; i++) {
                        action.accept(itemAt(items, i));
                    }
                    if(to == end) {
                        break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * Returns an iterator over the elements in this queue in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this queue in proper sequence
     */
    // 返回当前队列的迭代器
    public Iterator<E> iterator() {
        return new Itr();
    }
    
    /**
     * Returns a {@link Spliterator} over the elements in this queue.
     *
     * <p>The returned spliterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#ORDERED}, and {@link Spliterator#NONNULL}.
     *
     * @return a {@code Spliterator} over the elements in this queue
     *
     * @implNote The {@code Spliterator} implements {@code trySplit} to permit limited
     * parallelism.
     * @since 1.8
     */
    // 返回描述此队列中元素的Spliterator
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, (Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT));
    }
    
    /*▲ 迭代 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 杂项 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * this doc comment is overridden to remove the reference to collections greater in size than Integer.MAX_VALUE
     *
     * Returns the number of elements in this queue.
     *
     * @return the number of elements in this queue
     */
    // 返回队列中元素数量
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * this doc comment is a modified copy of the inherited doc comment, without the reference to unlimited queues.
     *
     * Returns the number of additional elements that this queue can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking. This is always equal to the initial capacity of this queue
     * less the current {@code size} of this queue.
     *
     * <p>Note that you <em>cannot</em> always tell if an attempt to insert
     * an element will succeed by inspecting {@code remainingCapacity}
     * because it may be the case that another thread is about to
     * insert or remove an element.
     */
    // 返回队列的剩余容量
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return items.length - count;
        } finally {
            lock.unlock();
        }
    }
    
    /*▲ 杂项 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 序列化 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Serialization ID. This class relies on default serialization
     * even for the items array, which is default-serialized, even if
     * it is empty. Otherwise it could not be declared final, which is
     * necessary here.
     */
    private static final long serialVersionUID = -817911632652898426L;
    
    
    /**
     * Reconstitutes this queue from a stream (that is, deserializes it).
     *
     * @param s the stream
     *
     * @throws ClassNotFoundException         if the class of a serialized object
     *                                        could not be found
     * @throws java.io.InvalidObjectException if invariants are violated
     * @throws java.io.IOException            if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        
        // Read in items array and various fields
        s.defaultReadObject();
        
        if(!invariantsSatisfied())
            throw new java.io.InvalidObjectException("invariants violated");
    }
    
    /*▲ 序列化 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    public String toString() {
        return Helpers.collectionToString(this);
    }
    
    
    
    /**
     * Inserts element at current put position, advances, and signals.
     * Call only when holding lock.
     */
    // 入队，非线程安全，仅内部使用
    // 1、队尾元素置为e
    // 2、队尾索引加1，如果队尾索引到达数组长度则置为0
    // 3、队列元素个数加1
    // 4、notEmpty.signal
    private void enqueue(E e) {
        // assert lock.isHeldByCurrentThread();
        // assert lock.getHoldCount() == 1;
        // assert items[putIndex] == null;
        
        final Object[] items = this.items;
        
        // 元素入队
        items[putIndex] = e;
        
        // 游标递增（轮转）
        if(++putIndex == items.length) {
            putIndex = 0;
        }
        
        // 元素数量增一
        count++;
        
        // 唤醒出队线程
        notEmpty.signal();
    }
    
    /**
     * Extracts element at current take position, advances, and signals.
     * Call only when holding lock.
     */
    // 出队，非线程安全，仅内部使用
    // 1、获取队头元素
    // 2、队头元素置为null
    // 3、队头索引加1，如果队头索引到达数组长度则置为0
    // 4、队列元素个数减1
    // 5、notFull.signal
    private E dequeue() {
        // assert lock.isHeldByCurrentThread();
        // assert lock.getHoldCount() == 1;
        // assert items[takeIndex] != null;
        
        final Object[] items = this.items;
        
        // 元素出队
        @SuppressWarnings("unchecked")
        E e = (E) items[takeIndex];
        
        // 防止内存泄露
        items[takeIndex] = null;
        
        // 游标递增（轮转）
        if(++takeIndex == items.length) {
            takeIndex = 0;
        }
        
        // 元素数量减一
        count--;
        
        if(itrs != null) {
            itrs.elementDequeued();
        }
        
        // 唤醒入队线程
        notFull.signal();
        
        // 返回元素
        return e;
    }
    
    /** Implementation of bulk remove methods. */
    // 批量删除
    private boolean bulkRemove(Predicate<? super E> filter) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if(itrs == null) { // check for active iterators
                if(count>0) {
                    final Object[] items = this.items;
                    // Optimize for initial run of survivors
                    // i为遍历开始索引；end为遍历结束索引；to为临时遍历结束索引
                    for(int i = takeIndex, end = putIndex, to = (i<end) ? end : items.length; ; i = 0, to = end) {
                        for(; i<to; i++) {
                            // 如果该元素满足过滤条件
                            if(filter.test(itemAt(items, i))) {
                                return bulkRemoveModified(filter, i);
                            }
                        }
                        if(to == end) {
                            break;
                        }
                    }
                }
                return false;
            }
        } finally {
            lock.unlock();
        }
        // Active iterators are too hairy!
        // Punting (for now) to the slow n^2 algorithm ...
        return super.removeIf(filter);
    }
    
    /**
     * Helper for bulkRemove, in case of at least one deletion.
     * Tolerate predicates that reentrantly access the collection for
     * read (but not write), so traverse once to find elements to
     * delete, a second pass to physically expunge.
     *
     * @param beg valid index of first element to be deleted
     */
    private boolean bulkRemoveModified(Predicate<? super E> filter, final int beg) {
        final Object[] es = items;
        final int capacity = items.length;
        final int end = putIndex;
        final long[] deathRow = nBits(distanceNonEmpty(beg, putIndex));
        deathRow[0] = 1L;   // set bit 0
        for(int i = beg + 1, to = (i<=end) ? end : es.length, k = beg; ; i = 0, to = end, k -= capacity) {
            for(; i<to; i++) {
                if(filter.test(itemAt(es, i))) {
                    setBit(deathRow, i - k);
                }
            }
            if(to == end) {
                break;
            }
        }
        // a two-finger traversal, with hare i reading, tortoise w writing
        int w = beg;
        for(int i = beg + 1, to = (i<=end) ? end : es.length, k = beg; ; w = 0) { // w rejoins i on second leg
            // In this loop, i and w are on the same leg, with i > w
            for(; i<to; i++) {
                if(isClear(deathRow, i - k)) {
                    es[w++] = es[i];
                }
            }
            if(to == end) {
                break;
            }
            // In this loop, w is on the first leg, i on the second
            for(i = 0, to = end, k -= capacity; i<to && w<capacity; i++) {
                if(isClear(deathRow, i - k)) {
                    es[w++] = es[i];
                }
            }
            if(i >= to) {
                if(w == capacity) {
                    w = 0; // "corner" case
                }
                break;
            }
        }
        count -= distanceNonEmpty(w, end);
        circularClear(es, putIndex = w, end);
        return true;
    }
    
    /**
     * Nulls out slots starting at array index i, upto index end.
     * Condition i == end means "full" - the entire array is cleared.
     */
    private static void circularClear(Object[] items, int i, int end) {
        // assert 0 <= i && i < items.length;
        // assert 0 <= end && end < items.length;
        // i为开始索引，end为结束索引，to为临时结束索引
        for(int to = (i<end) ? end : items.length; ; i = 0, to = end) {
            for(; i<to; i++) {
                items[i] = null;
            }
            if(to == end) {
                break;
            }
        }
    }
    
    /**
     * Returns element at array index i.
     * This is a slight abuse of generics, accepted by javac.
     */
    @SuppressWarnings("unchecked")
    static <E> E itemAt(Object[] items, int i) {
        return (E) items[i];
    }
    
    /**
     * Returns item at index i.
     */
    @SuppressWarnings("unchecked")
    final E itemAt(int i) {
        return (E) items[i];
    }
    
    /**
     * Increments i, mod modulus.
     * Precondition and postcondition: 0 <= i < modulus.
     */
    static final int inc(int i, int modulus) {
        if(++i >= modulus)
            i = 0;
        return i;
    }
    
    /**
     * Decrements i, mod modulus.
     * Precondition and postcondition: 0 <= i < modulus.
     */
    static final int dec(int i, int modulus) {
        if(--i<0)
            i = modulus - 1;
        return i;
    }
    
    private static long[] nBits(int n) {
        return new long[((n - 1) >> 6) + 1];
    }
    
    private static void setBit(long[] bits, int i) {
        bits[i >> 6] |= 1L << i;
    }
    
    private static boolean isClear(long[] bits, int i) {
        return (bits[i >> 6] & (1L << i)) == 0;
    }
    
    /**
     * Returns circular distance from i to j, disambiguating i == j to
     * items.length; never returns 0.
     */
    private int distanceNonEmpty(int i, int j) {
        if((j -= i)<=0) {
            j += items.length;
        }
        return j;
    }
    
    private boolean invariantsSatisfied() {
        // Unlike ArrayDeque, we have a count field but no spare slot.
        // We prefer ArrayDeque's strategy (and the names of its fields!),
        // but our field layout is baked into the serial form, and so is
        // too annoying to change.
        //
        // putIndex == takeIndex must be disambiguated by checking count.
        int capacity = items.length;
        return capacity>0
            && items.getClass() == Object[].class
            && (takeIndex | putIndex | count) >= 0
            && takeIndex<capacity
            && putIndex<capacity
            && count<=capacity
            && (putIndex - takeIndex - count) % capacity == 0
            && (count == 0 || items[takeIndex] != null)
            && (count == capacity || items[putIndex] == null)
            && (count == 0 || items[dec(putIndex, capacity)] != null);
    }
    
    /** debugging */
    void checkInvariants() {
        // meta-assertions
        // assert lock.isHeldByCurrentThread();
        if(!invariantsSatisfied()) {
            String detail = String.format("takeIndex=%d putIndex=%d count=%d capacity=%d items=%s", takeIndex, putIndex, count, items.length, Arrays.toString(items));
            System.err.println(detail);
            throw new AssertionError(detail);
        }
    }
    
    
    
    
    
    
    /**
     * Shared data between iterators and their queue, allowing queue
     * modifications to update iterators when elements are removed.
     *
     * This adds a lot of complexity for the sake of correctly
     * handling some uncommon operations, but the combination of
     * circular-arrays and supporting interior removes (i.e., those
     * not at head) would cause iterators to sometimes lose their
     * places and/or (re)report elements they shouldn't.  To avoid
     * this, when a queue has one or more iterators, it keeps iterator
     * state consistent by:
     *
     * (1) keeping track of the number of "cycles", that is, the
     *     number of times takeIndex has wrapped around to 0.
     * (2) notifying all iterators via the callback removedAt whenever
     *     an interior element is removed (and thus other elements may
     *     be shifted).
     *
     * These suffice to eliminate iterator inconsistencies, but
     * unfortunately add the secondary responsibility of maintaining
     * the list of iterators.  We track all active iterators in a
     * simple linked list (accessed only when the queue's lock is
     * held) of weak references to Itr.  The list is cleaned up using
     * 3 different mechanisms:
     *
     * (1) Whenever a new iterator is created, do some O(1) checking for
     *     stale list elements.
     *
     * (2) Whenever takeIndex wraps around to 0, check for iterators
     *     that have been unused for more than one wrap-around cycle.
     *
     * (3) Whenever the queue becomes empty, all iterators are notified
     *     and this entire data structure is discarded.
     *
     * So in addition to the removedAt callback that is necessary for
     * correctness, iterators have the shutdown and takeIndexWrapped
     * callbacks that help remove stale iterators from the list.
     *
     * Whenever a list element is examined, it is expunged if either
     * the GC has determined that the iterator is discarded, or if the
     * iterator reports that it is "detached" (does not need any
     * further state updates).  Overhead is maximal when takeIndex
     * never advances, iterators are discarded before they are
     * exhausted, and all removals are interior removes, in which case
     * all stale iterators are discovered by the GC.  But even in this
     * case we don't increase the amortized complexity.
     *
     * Care must be taken to keep list sweeping methods from
     * reentrantly invoking another such method, causing subtle
     * corruption bugs.
     */
    // 迭代器链表
    class Itrs {
        /** Incremented whenever takeIndex wraps around to 0 */
        int cycles;
        
        private static final int SHORT_SWEEP_PROBES = 4;
        private static final int LONG_SWEEP_PROBES = 16;
        
        /** Linked list of weak iterator references */
        private Node head;
        
        /** Used to expunge stale iterators */
        private Node sweeper;
        
        Itrs(Itr initial) {
            register(initial);
        }
        
        /**
         * Sweeps itrs, looking for and expunging stale iterators.
         * If at least one was found, tries harder to find more.
         * Called only from iterating thread.
         *
         * @param tryHarder whether to start in try-harder mode, because
         *                  there is known to be at least one iterator to collect
         */
        void doSomeSweeping(boolean tryHarder) {
            // assert lock.isHeldByCurrentThread();
            // assert head != null;
            int probes = tryHarder ? LONG_SWEEP_PROBES : SHORT_SWEEP_PROBES;
            Node o, p;
            final Node sweeper = this.sweeper;
            boolean passedGo;   // to limit search to one full sweep
            
            if(sweeper == null) {
                o = null;
                p = head;
                passedGo = true;
            } else {
                o = sweeper;
                p = o.next;
                passedGo = false;
            }
            
            for(; probes>0; probes--) {
                if(p == null) {
                    if(passedGo)
                        break;
                    o = null;
                    p = head;
                    passedGo = true;
                }
                final Itr it = p.get();
                final Node next = p.next;
                if(it == null || it.isDetached()) {
                    // found a discarded/exhausted iterator
                    probes = LONG_SWEEP_PROBES; // "try harder"
                    // unlink p
                    p.clear();
                    p.next = null;
                    if(o == null) {
                        head = next;
                        if(next == null) {
                            // We've run out of iterators to track; retire
                            itrs = null;
                            return;
                        }
                    } else
                        o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }
            
            this.sweeper = (p == null) ? null : o;
        }
        
        /**
         * Adds a new iterator to the linked list of tracked iterators.
         */
        void register(Itr itr) {
            // assert lock.isHeldByCurrentThread();
            head = new Node(itr, head);
        }
        
        /**
         * Called whenever takeIndex wraps around to 0.
         *
         * Notifies all iterators, and expunges any that are now stale.
         */
        void takeIndexWrapped() {
            // assert lock.isHeldByCurrentThread();
            cycles++;
            for(Node o = null, p = head; p != null; ) {
                final Itr it = p.get();
                final Node next = p.next;
                if(it == null || it.takeIndexWrapped()) {
                    // unlink p
                    // assert it == null || it.isDetached();
                    p.clear();
                    p.next = null;
                    if(o == null)
                        head = next;
                    else
                        o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }
            if(head == null)   // no more iterators to track
                itrs = null;
        }
        
        /**
         * Called whenever an interior remove (not at takeIndex) occurred.
         *
         * Notifies all iterators, and expunges any that are now stale.
         */
        void removedAt(int removedIndex) {
            for(Node o = null, p = head; p != null; ) {
                final Itr it = p.get();
                final Node next = p.next;
                if(it == null || it.removedAt(removedIndex)) {
                    // unlink p
                    // assert it == null || it.isDetached();
                    p.clear();
                    p.next = null;
                    if(o == null)
                        head = next;
                    else
                        o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }
            if(head == null)   // no more iterators to track
                itrs = null;
        }
        
        /**
         * Called whenever the queue becomes empty.
         *
         * Notifies all active iterators that the queue is empty,
         * clears all weak refs, and unlinks the itrs datastructure.
         */
        void queueIsEmpty() {
            // assert lock.isHeldByCurrentThread();
            for(Node p = head; p != null; p = p.next) {
                Itr it = p.get();
                if(it != null) {
                    p.clear();
                    it.shutdown();
                }
            }
            head = null;
            itrs = null;
        }
        
        /**
         * Called whenever an element has been dequeued (at takeIndex).
         */
        void elementDequeued() {
            // assert lock.isHeldByCurrentThread();
            if(count == 0)
                queueIsEmpty();
            else if(takeIndex == 0)
                takeIndexWrapped();
        }
        
        /**
         * Node in a linked list of weak iterator references.
         */
        private class Node extends WeakReference<Itr> {
            Node next;
            
            Node(Itr iterator, Node next) {
                super(iterator);
                this.next = next;
            }
        }
    }
    
    /**
     * Iterator for ArrayBlockingQueue.
     *
     * To maintain weak consistency with respect to puts and takes, we
     * read ahead one slot, so as to not report hasNext true but then
     * not have an element to return.
     *
     * We switch into "detached" mode (allowing prompt unlinking from
     * itrs without help from the GC) when all indices are negative, or
     * when hasNext returns false for the first time.  This allows the
     * iterator to track concurrent updates completely accurately,
     * except for the corner case of the user calling Iterator.remove()
     * after hasNext() returned false.  Even in this case, we ensure
     * that we don't remove the wrong element by keeping track of the
     * expected element to remove, in lastItem.  Yes, we may fail to
     * remove lastItem from the queue if it moved due to an interleaved
     * interior remove while in detached mode.
     *
     * Method forEachRemaining, added in Java 8, is treated similarly
     * to hasNext returning false, in that we switch to detached mode,
     * but we regard it as an even stronger request to "close" this
     * iteration, and don't bother supporting subsequent remove().
     */
    // 用于当前队列的外部迭代器
    private class Itr implements Iterator<E> {
        /** Special index value indicating "not available" or "undefined" */
        private static final int NONE = -1;
        /**
         * Special index value indicating "removed elsewhere", that is,
         * removed by some operation other than a call to this.remove().
         */
        private static final int REMOVED = -2;
        /** Special value for prevTakeIndex indicating "detached mode" */
        private static final int DETACHED = -3;
        /** Index to look for new nextItem; NONE at end */
        private int cursor;
        /** Element to be returned by next call to next(); null if none */
        private E nextItem;
        /** Index of nextItem; NONE if none, REMOVED if removed elsewhere */
        private int nextIndex;
        /** Last element returned; null if none or not detached. */
        private E lastItem;
        /** Index of lastItem, NONE if none, REMOVED if removed elsewhere */
        private int lastRet;
        /** Previous value of takeIndex, or DETACHED when detached */
        private int prevTakeIndex;
        /** Previous value of iters.cycles */
        private int prevCycles;
        
        Itr() {
            lastRet = NONE;
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if(count == 0) {
                    // assert itrs == null;
                    cursor = NONE;
                    nextIndex = NONE;
                    prevTakeIndex = DETACHED;
                } else {
                    final int takeIndex = ArrayBlockingQueue.this.takeIndex;
                    
                    prevTakeIndex = takeIndex;
                    
                    // 初始化为队头元素
                    nextItem = itemAt(nextIndex = takeIndex);
                    cursor = incCursor(takeIndex);
                    if(itrs == null) {
                        itrs = new Itrs(this);
                    } else {
                        itrs.register(this); // in this order
                        itrs.doSomeSweeping(false);
                    }
                    prevCycles = itrs.cycles;
                    // assert takeIndex >= 0;
                    // assert prevTakeIndex == takeIndex;
                    // assert nextIndex >= 0;
                    // assert nextItem != null;
                }
            } finally {
                lock.unlock();
            }
        }
        
        /**
         * For performance reasons, we would like not to acquire a lock in
         * hasNext in the common case.  To allow for this, we only access
         * fields (i.e. nextItem) that are not modified by update operations
         * triggered by queue modifications.
         */
        public boolean hasNext() {
            if(nextItem != null) {
                return true;
            }
            noNext();
            return false;
        }
        
        public E next() {
            final E e = nextItem;
            if(e == null) {
                throw new NoSuchElementException();
            }
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if(!isDetached()) {
                    incorporateDequeues();
                }
                // assert nextIndex != NONE;
                // assert lastItem == null;
                lastRet = nextIndex;
                final int cursor = this.cursor;
                if(cursor >= 0) {
                    nextItem = itemAt(nextIndex = cursor);
                    // assert nextItem != null;
                    this.cursor = incCursor(cursor);
                } else {
                    nextIndex = NONE;
                    nextItem = null;
                    if(lastRet == REMOVED) {
                        detach();
                    }
                }
            } finally {
                lock.unlock();
            }
            return e;
        }
        
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                final E e = nextItem;
                if(e == null) {
                    return;
                }
                
                if(!isDetached()) {
                    incorporateDequeues();
                }
                
                action.accept(e);
                
                if(isDetached() || cursor<0) {
                    return;
                }
                
                final Object[] items = ArrayBlockingQueue.this.items;
                for(int i = cursor, end = putIndex, to = (i<end) ? end : items.length; ; i = 0, to = end) {
                    for(; i<to; i++) {
                        action.accept(itemAt(items, i));
                    }
                    if(to == end) {
                        break;
                    }
                }
            } finally {
                // Calling forEachRemaining is a strong hint that this
                // iteration is surely over; supporting remove() after
                // forEachRemaining() is more trouble than it's worth
                cursor = nextIndex = lastRet = NONE;
                nextItem = lastItem = null;
                detach();
                lock.unlock();
            }
        }
        
        public void remove() {
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            // assert lock.getHoldCount() == 1;
            try {
                if(!isDetached()) {
                    incorporateDequeues(); // might update lastRet or detach
                }
                final int lastRet = this.lastRet;
                this.lastRet = NONE;
                if(lastRet >= 0) {
                    if(!isDetached()) {
                        // 移除指定索引处的元素，非线程安全
                        removeAt(lastRet);
                    } else {
                        final E lastItem = this.lastItem;
                        // assert lastItem != null;
                        this.lastItem = null;
                        if(itemAt(lastRet) == lastItem) {
                            // 移除指定索引处的元素，非线程安全
                            removeAt(lastRet);
                        }
                    }
                } else if(lastRet == NONE) {
                    throw new IllegalStateException();
                }
                // else lastRet == REMOVED and the last returned element was
                // previously asynchronously removed via an operation other
                // than this.remove(), so nothing to do.
                
                if(cursor<0 && nextIndex<0)
                    detach();
            } finally {
                lock.unlock();
                // assert lastRet == NONE;
                // assert lastItem == null;
            }
        }
        
        boolean isDetached() {
            // assert lock.isHeldByCurrentThread();
            return prevTakeIndex<0;
        }
        
        /**
         * Called to notify the iterator that the queue is empty, or that it
         * has fallen hopelessly behind, so that it should abandon any
         * further iteration, except possibly to return one more element
         * from next(), as promised by returning true from hasNext().
         */
        void shutdown() {
            // assert lock.isHeldByCurrentThread();
            cursor = NONE;
            if(nextIndex >= 0)
                nextIndex = REMOVED;
            if(lastRet >= 0) {
                lastRet = REMOVED;
                lastItem = null;
            }
            prevTakeIndex = DETACHED;
            // Don't set nextItem to null because we must continue to be
            // able to return it on next().
            //
            // Caller will unlink from itrs when convenient.
        }
        
        /**
         * Called whenever an interior remove (not at takeIndex) occurred.
         *
         * @return true if this iterator should be unlinked from itrs
         */
        boolean removedAt(int removedIndex) {
            // assert lock.isHeldByCurrentThread();
            if(isDetached()) {
                return true;
            }
            
            final int takeIndex = ArrayBlockingQueue.this.takeIndex;
            final int prevTakeIndex = this.prevTakeIndex;
            final int len = items.length;
            // distance from prevTakeIndex to removedIndex
            final int removedDistance = len * (itrs.cycles - this.prevCycles + ((removedIndex<takeIndex) ? 1 : 0)) + (removedIndex - prevTakeIndex);
            // assert itrs.cycles - this.prevCycles >= 0;
            // assert itrs.cycles - this.prevCycles <= 1;
            // assert removedDistance > 0;
            // assert removedIndex != takeIndex;
            int cursor = this.cursor;
            if(cursor >= 0) {
                int x = distance(cursor, prevTakeIndex, len);
                if(x == removedDistance) {
                    if(cursor == putIndex)
                        this.cursor = cursor = NONE;
                } else if(x>removedDistance) {
                    // assert cursor != prevTakeIndex;
                    this.cursor = cursor = dec(cursor, len);
                }
            }
            int lastRet = this.lastRet;
            if(lastRet >= 0) {
                int x = distance(lastRet, prevTakeIndex, len);
                if(x == removedDistance)
                    this.lastRet = lastRet = REMOVED;
                else if(x>removedDistance)
                    this.lastRet = lastRet = dec(lastRet, len);
            }
            int nextIndex = this.nextIndex;
            if(nextIndex >= 0) {
                int x = distance(nextIndex, prevTakeIndex, len);
                if(x == removedDistance)
                    this.nextIndex = nextIndex = REMOVED;
                else if(x>removedDistance)
                    this.nextIndex = nextIndex = dec(nextIndex, len);
            }
            if(cursor<0 && nextIndex<0 && lastRet<0) {
                this.prevTakeIndex = DETACHED;
                return true;
            }
            return false;
        }
        
        /**
         * Called whenever takeIndex wraps around to zero.
         *
         * @return true if this iterator should be unlinked from itrs
         */
        boolean takeIndexWrapped() {
            // assert lock.isHeldByCurrentThread();
            if(isDetached())
                return true;
            if(itrs.cycles - prevCycles>1) {
                // All the elements that existed at the time of the last
                // operation are gone, so abandon further iteration.
                shutdown();
                return true;
            }
            return false;
        }
        
        private int incCursor(int index) {
            // assert lock.isHeldByCurrentThread();
            if(++index == items.length)
                index = 0;
            if(index == putIndex)
                index = NONE;
            return index;
        }
        
        /**
         * Returns true if index is invalidated by the given number of
         * dequeues, starting from prevTakeIndex.
         */
        private boolean invalidated(int index, int prevTakeIndex, long dequeues, int length) {
            if(index<0)
                return false;
            int distance = index - prevTakeIndex;
            if(distance<0)
                distance += length;
            return dequeues>distance;
        }
        
        /**
         * Adjusts indices to incorporate all dequeues since the last operation on this iterator.
         * Call only from iterating thread.
         */
        private void incorporateDequeues() {
            // assert lock.isHeldByCurrentThread();
            // assert itrs != null;
            // assert !isDetached();
            // assert count > 0;
            
            final int cycles = itrs.cycles;
            final int prevCycles = this.prevCycles;
            
            final int takeIndex = ArrayBlockingQueue.this.takeIndex;
            final int prevTakeIndex = this.prevTakeIndex;
            
            if(cycles != prevCycles || takeIndex != prevTakeIndex) {
                final int len = items.length;
                // how far takeIndex has advanced since the previous operation of this iterator
                long dequeues = (long) (cycles - prevCycles) * len + (takeIndex - prevTakeIndex);
                
                // Check indices for invalidation
                if(invalidated(lastRet, prevTakeIndex, dequeues, len))
                    lastRet = REMOVED;
                if(invalidated(nextIndex, prevTakeIndex, dequeues, len))
                    nextIndex = REMOVED;
                if(invalidated(cursor, prevTakeIndex, dequeues, len))
                    cursor = takeIndex;
                
                if(cursor<0 && nextIndex<0 && lastRet<0)
                    detach();
                else {
                    this.prevCycles = cycles;
                    this.prevTakeIndex = takeIndex;
                }
            }
        }
        
        /**
         * Called when itrs should stop tracking this iterator, either
         * because there are no more indices to update (cursor < 0 &&
         * nextIndex < 0 && lastRet < 0) or as a special exception, when
         * lastRet >= 0, because hasNext() is about to return false for the
         * first time.  Call only from iterating thread.
         */
        private void detach() {
            // Switch to detached mode
            // assert lock.isHeldByCurrentThread();
            // assert cursor == NONE;
            // assert nextIndex < 0;
            // assert lastRet < 0 || nextItem == null;
            // assert lastRet < 0 ^ lastItem != null;
            if(prevTakeIndex >= 0) {
                // assert itrs != null;
                prevTakeIndex = DETACHED;
                // try to unlink from itrs (but not too hard)
                itrs.doSomeSweeping(true);
            }
        }
        
        private void noNext() {
            final ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                // assert cursor == NONE;
                // assert nextIndex == NONE;
                if(!isDetached()) {
                    // assert lastRet >= 0;
                    incorporateDequeues(); // might update lastRet
                    if(lastRet >= 0) {
                        lastItem = itemAt(lastRet);
                        // assert lastItem != null;
                        detach();
                    }
                }
                // assert isDetached();
                // assert lastRet < 0 ^ lastItem != null;
            } finally {
                lock.unlock();
            }
        }
        
        private int distance(int index, int prevTakeIndex, int length) {
            int distance = index - prevTakeIndex;
            if(distance<0)
                distance += length;
            return distance;
        }
        
//         /** Uncomment for debugging. */
//         public String toString() {
//             return ("cursor=" + cursor + " " +
//                     "nextIndex=" + nextIndex + " " +
//                     "lastRet=" + lastRet + " " +
//                     "nextItem=" + nextItem + " " +
//                     "lastItem=" + lastItem + " " +
//                     "prevCycles=" + prevCycles + " " +
//                     "prevTakeIndex=" + prevTakeIndex + " " +
//                     "size()=" + size() + " " +
//                     "remainingCapacity()=" + remainingCapacity());
//         }
    }
}
