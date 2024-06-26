/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A token representing the registration of a {@link SelectableChannel} with a
 * {@link Selector}.
 *
 * <p> A selection key is created each time a channel is registered with a
 * selector.  A key remains valid until it is <i>cancelled</i> by invoking its
 * {@link #cancel cancel} method, by closing its channel, or by closing its
 * selector.  Cancelling a key does not immediately remove it from its
 * selector; it is instead added to the selector's <a
 * href="Selector.html#ks"><i>cancelled-key set</i></a> for removal during the
 * next selection operation.  The validity of a key may be tested by invoking
 * its {@link #isValid isValid} method.
 *
 * <a id="opsets"></a>
 *
 * <p> A selection key contains two <i>operation sets</i> represented as
 * integer values.  Each bit of an operation set denotes a category of
 * selectable operations that are supported by the key's channel.
 *
 * <ul>
 *
 *   <li><p> The <i>interest set</i> determines which operation categories will
 *   be tested for readiness the next time one of the selector's selection
 *   methods is invoked.  The interest set is initialized with the value given
 *   when the key is created; it may later be changed via the {@link
 *   #interestOps(int)} method. </p></li>
 *
 *   <li><p> The <i>ready set</i> identifies the operation categories for which
 *   the key's channel has been detected to be ready by the key's selector.
 *   The ready set is initialized to zero when the key is created; it may later
 *   be updated by the selector during a selection operation, but it cannot be
 *   updated directly. </p></li>
 *
 * </ul>
 *
 * <p> That a selection key's ready set indicates that its channel is ready for
 * some operation category is a hint, but not a guarantee, that an operation in
 * such a category may be performed by a thread without causing the thread to
 * block.  A ready set is most likely to be accurate immediately after the
 * completion of a selection operation.  It is likely to be made inaccurate by
 * external events and by I/O operations that are invoked upon the
 * corresponding channel.
 *
 * <p> This class defines all known operation-set bits, but precisely which
 * bits are supported by a given channel depends upon the type of the channel.
 * Each subclass of {@link SelectableChannel} defines an {@link
 * SelectableChannel#validOps() validOps()} method which returns a set
 * identifying just those operations that are supported by the channel.  An
 * attempt to set or test an operation-set bit that is not supported by a key's
 * channel will result in an appropriate run-time exception.
 *
 * <p> It is often necessary to associate some application-specific data with a
 * selection key, for example an object that represents the state of a
 * higher-level protocol and handles readiness notifications in order to
 * implement that protocol.  Selection keys therefore support the
 * <i>attachment</i> of a single arbitrary object to a key.  An object can be
 * attached via the {@link #attach attach} method and then later retrieved via
 * the {@link #attachment() attachment} method.
 *
 * <p> Selection keys are safe for use by multiple concurrent threads.  A
 * selection operation will always use the interest-set value that was current
 * at the moment that the operation began.  </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 *
 * @see SelectableChannel
 * @see Selector
 */
/*
 * "选择键"，用来代表一条由【通道】向【选择器】发起的【注册信息】。
 *
 * 每个"选择键"对象中包含四个重要的属性：
 * 通道　　：channel
 * 选择器　：selector
 * 监听事件：ops
 * 附属对象：attachment
 *
 * 其中，通道(channel)和选择器(selector)是两个关键的属性，
 * 因为用这两个属性可以确定一个"选择键"是否为新注册进来的。
 *
 * 相当于通道的指针，还可以保存一个对象的附件，一般会存储这个通道上的连接的状态
 * Selector的selectedKeys可以在Set中返回SelectionKey对象，
 */
public abstract class SelectionKey {
    
    /*
     * 以下四个常量是JAVA层的四种可选的【注册参数/事件】（不同的语境下使用不同的称呼，但意义一致）
     * 在实际运行中，它们会被转换为native层的【内核参数/事件】，参见Net类。
     *
     * 这些参数/事件均使用在【通道】发起【注册操作】的场合中，
     * 一旦某个【通道】【注册】了这些【参数/事件】，它就可以【响应来自native层的事件消息】了。
     */
    
    /**
     * Operation-set bit for 【read】 operations.
     *
     * <p> Suppose that a selection key's interest set contains
     * {@code OP_READ} at the start of a <a
     * href="Selector.html#selop">selection operation</a>.  If the selector
     * detects that the corresponding channel is ready for reading, has reached
     * end-of-stream, has been remotely shut down for further reading, or has
     * an error pending, then it will add {@code OP_READ} to the key's
     * ready-operation set.  </p>
     */
    public static final int OP_READ = 1 << 0;    // read
    
    /**
     * Operation-set bit for 【write】 operations.
     *
     * <p> Suppose that a selection key's interest set contains
     * {@code OP_WRITE} at the start of a <a
     * href="Selector.html#selop">selection operation</a>.  If the selector
     * detects that the corresponding channel is ready for writing, has been
     * remotely shut down for further writing, or has an error pending, then it
     * will add {@code OP_WRITE} to the key's ready set.  </p>
     */
    public static final int OP_WRITE = 1 << 2;    // write
    
    /**
     * Operation-set bit for 【socket-connect】 operations.
     *
     * <p> Suppose that a selection key's interest set contains
     * {@code OP_CONNECT} at the start of a <a
     * href="Selector.html#selop">selection operation</a>.  If the selector
     * detects that the corresponding socket channel is ready to complete its
     * connection sequence, or has an error pending, then it will add
     * {@code OP_CONNECT} to the key's ready set.  </p>
     */
    public static final int OP_CONNECT = 1 << 3;    // connect
    
    /**
     * Operation-set bit for 【socket-accept】 operations.
     *
     * <p> Suppose that a selection key's interest set contains
     * {@code OP_ACCEPT} at the start of a <a
     * href="Selector.html#selop">selection operation</a>.  If the selector
     * detects that the corresponding server-socket channel is ready to accept
     * another connection, or has an error pending, then it will add
     * {@code OP_ACCEPT} to the key's ready set.  </p>
     */
    public static final int OP_ACCEPT = 1 << 4;    // accept
    
    
    private volatile Object attachment; // 参与注册的附属对象
    
    // 属性attachment的地址
    private static final AtomicReferenceFieldUpdater<SelectionKey, Object> attachmentUpdater = AtomicReferenceFieldUpdater.newUpdater(SelectionKey.class, Object.class, "attachment");
    
    
    
    /*▼ 构造器 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Constructs an instance of this class.
     */
    protected SelectionKey() {
    }
    
    /*▲ 构造器 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 属性 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Returns the selector for which this key was created.  This method will
     * continue to return the selector even after the key is cancelled.
     *
     * @return This key's selector
     */
    // 返回通道注册到的【选择器】
    public abstract Selector selector();
    
    /**
     * Returns the channel for which this key was created.  This method will
     * continue to return the channel even after the key is cancelled.
     *
     * @return This key's channel
     */
    // 返回发起注册的【通道】
    public abstract SelectableChannel channel();
    
    /**
     * Retrieves the current attachment.
     *
     * @return The object currently attached to this key,
     * or {@code null} if there is no attachment
     */
    // 【返回】【参与注册】的【附属对象】
    public final Object attachment() {
        return attachment;
    }
    
    /**
     * Attaches the given object to this key.
     *
     * <p> An attached object may later be retrieved via the {@link #attachment()
     * attachment} method.  Only one object may be attached at a time; invoking
     * this method causes any previous attachment to be discarded.  The current
     * attachment may be discarded by attaching {@code null}.  </p>
     *
     * @param ob The object to be attached; may be {@code null}
     *
     * @return The previously-attached object, if any,
     * otherwise {@code null}
     */
    // 向"选择键"对象【设置】【附属对象】(attachment)属性，并返回旧值
    public final Object attach(Object attachment) {
        return attachmentUpdater.getAndSet(this, attachment);
    }
    
    /*▲ 属性 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 已就绪事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tests whether this key's channel is ready for reading.
     *
     * <p> An invocation of this method of the form {@code k.isReadable()}
     * behaves in exactly the same way as the expression
     *
     * <blockquote><pre>{@code
     * k.readyOps() & OP_READ != 0
     * }</pre></blockquote>
     *
     * <p> If this key's channel does not support read operations then this
     * method always returns {@code false}.  </p>
     *
     * @return {@code true} if, and only if,
     * {@code readyOps() & OP_READ} is nonzero
     *
     * @throws CancelledKeyException If this key has been cancelled
     */
    // 是否【可以从通道读取数据】了
    public final boolean isReadable() {
        return (readyOps() & OP_READ) != 0;
    }
    
    /**
     * Tests whether this key's channel is ready for writing.
     *
     * <p> An invocation of this method of the form {@code k.isWritable()}
     * behaves in exactly the same way as the expression
     *
     * <blockquote><pre>{@code
     * k.readyOps() & OP_WRITE != 0
     * }</pre></blockquote>
     *
     * <p> If this key's channel does not support write operations then this
     * method always returns {@code false}.  </p>
     *
     * @return {@code true} if, and only if,
     * {@code readyOps() & OP_WRITE} is nonzero
     *
     * @throws CancelledKeyException If this key has been cancelled
     */
    // 是否【可以向通道写入数据】了
    public final boolean isWritable() {
        return (readyOps() & OP_WRITE) != 0;
    }
    
    /**
     * Tests whether this key's channel has either finished, or failed to
     * finish, its socket-connection operation.
     *
     * <p> An invocation of this method of the form {@code k.isConnectable()}
     * behaves in exactly the same way as the expression
     *
     * <blockquote><pre>{@code
     * k.readyOps() & OP_CONNECT != 0
     * }</pre></blockquote>
     *
     * <p> If this key's channel does not support socket-connect operations
     * then this method always returns {@code false}.  </p>
     *
     * @return {@code true} if, and only if,
     * {@code readyOps() & OP_CONNECT} is nonzero
     *
     * @throws CancelledKeyException If this key has been cancelled
     */
    // 【客户端通道】是否【连接到了远端】
    public final boolean isConnectable() {
        return (readyOps() & OP_CONNECT) != 0;
    }
    
    /**
     * Tests whether this key's channel is ready to accept a new socket
     * connection.
     *
     * <p> An invocation of this method of the form {@code k.isAcceptable()}
     * behaves in exactly the same way as the expression
     *
     * <blockquote><pre>{@code
     * k.readyOps() & OP_ACCEPT != 0
     * }</pre></blockquote>
     *
     * <p> If this key's channel does not support socket-accept operations then
     * this method always returns {@code false}.  </p>
     *
     * @return {@code true} if, and only if,
     * {@code readyOps() & OP_ACCEPT} is nonzero
     *
     * @throws CancelledKeyException If this key has been cancelled
     */
    // 【服务端通道】【是否收到了来自客户端的连接】
    public final boolean isAcceptable() {
        return (readyOps() & OP_ACCEPT) != 0;
    }
    
    /*▲ 已就绪事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 监听事件/参数 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves this key's interest set.
     *
     * It is guaranteed that the returned set will only contain operation bits that are valid for this key's channel.
     *
     * @return This key's interest set
     *
     * @throws CancelledKeyException If this key has been cancelled
     */
    // 返回注册的监听事件：SelectionKey.XXX(需要验证当前"选择键"是否有效)
    public abstract int interestOps();
    
    
    /**
     * Sets this key's interest set to the given value.
     *
     * <p> This method may be invoked at any time.  If this method is invoked
     * while a selection operation is in progress then it has no effect upon
     * that operation; the change to the key's interest set will be seen by the
     * next selection operation.
     *
     * @param ops The new interest set
     *
     * @return This selection key
     *
     * @throws IllegalArgumentException If a bit in the set does not correspond to an operation that
     *                                  is supported by this key's channel, that is, if
     *                                  {@code (ops & ~channel().validOps()) != 0}
     * @throws CancelledKeyException    If this key has been cancelled
     */
    /*
     *【覆盖更新】当前"选择键"内的监听事件，如果新旧事件不同，则将当前"选择键"加入到选择器的"已更新键队列"(updateKeys)中。
     * 事件注册完成后，返回当前"选择键"。
     *
     * 参见：WindowsSelectorImpl#updateKeys()
     */
    public abstract SelectionKey interestOps(int ops);
    
    /**
     * Atomically sets this key's interest set to the bitwise union ("or") of
     * the existing interest set and the given value. This method is guaranteed
     * to be atomic with respect to other concurrent calls to this method or to
     * {@link #interestOpsAnd(int)}.
     *
     * <p> This method may be invoked at any time.  If this method is invoked
     * while a selection operation is in progress then it has no effect upon
     * that operation; the change to the key's interest set will be seen by the
     * next selection operation.
     *
     * @param ops The interest set to apply
     *
     * @return The previous interest set
     *
     * @throws IllegalArgumentException If a bit in the set does not correspond to an operation that
     *                                  is supported by this key's channel, that is, if
     *                                  {@code (ops & ~channel().validOps()) != 0}
     * @throws CancelledKeyException    If this key has been cancelled
     * @implSpec The default implementation synchronizes on this key and invokes
     * {@code interestOps()} and {@code interestOps(int)} to retrieve and set
     * this key's interest set.
     * @since 11
     */
    /*
     *【增量更新】当前"选择键"内的监听事件，如果新旧事件不同，则将当前"选择键"加入到选择器的"已更新键队列"(updateKeys)中。
     * 事件注册完成后，返回旧的监听事件。
     *
     * 参见：WindowsSelectorImpl#updateKeys()
     */
    public int interestOpsOr(int ops) {
        synchronized(this) {
            int oldVal = interestOps();
            interestOps(oldVal | ops);
            return oldVal;
        }
    }
    
    /**
     * Atomically sets this key's interest set to the bitwise intersection ("and")
     * of the existing interest set and the given value. This method is guaranteed
     * to be atomic with respect to other concurrent calls to this method or to
     * {@link #interestOpsOr(int)}.
     *
     * <p> This method may be invoked at any time.  If this method is invoked
     * while a selection operation is in progress then it has no effect upon
     * that operation; the change to the key's interest set will be seen by the
     * next selection operation.
     *
     * @param ops The interest set to apply
     *
     * @return The previous interest set
     *
     * @throws CancelledKeyException If this key has been cancelled
     * @apiNote Unlike the {@code interestOps(int)} and {@code interestOpsOr(int)}
     * methods, this method does not throw {@code IllegalArgumentException} when
     * invoked with bits in the interest set that do not correspond to an
     * operation that is supported by this key's channel. This is to allow
     * operation bits in the interest set to be cleared using bitwise complement
     * values, e.g., {@code interestOpsAnd(~SelectionKey.OP_READ)} will remove
     * the {@code OP_READ} from the interest set without affecting other bits.
     * @implSpec The default implementation synchronizes on this key and invokes
     * {@code interestOps()} and {@code interestOps(int)} to retrieve and set
     * this key's interest set.
     * @since 11
     */
    /*
     *【交集更新】当前"选择键"内的监听事件，如果新旧事件不同，则将当前"选择键"加入到选择器的"已更新键队列"(updateKeys)中。
     * 事件注册完成后，返回旧的监听事件。
     *
     * 参见：WindowsSelectorImpl#updateKeys()
     */
    public int interestOpsAnd(int ops) {
        synchronized(this) {
            int oldVal = interestOps();
            interestOps(oldVal & ops);
            return oldVal;
        }
    }
    
    /*▲ 监听事件/参数 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 就绪参数/事件 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Retrieves this key's ready-operation set.
     *
     * It is guaranteed that the returned set will only contain operation bits that are valid for this key's channel.
     *
     * @return This key's ready-operation set
     *
     * @throws CancelledKeyException If this key has been cancelled
     */
    // 返回已就绪事件(会验证当前"选择键"是否有效)
    public abstract int readyOps();
    
    /*▲ 就绪参数/事件 ████████████████████████████████████████████████████████████████████████████████┛ */
    
    
    
    /*▼ 状态 ████████████████████████████████████████████████████████████████████████████████┓ */
    
    /**
     * Tells whether or not this key is valid.
     *
     * <p> A key is valid upon creation and remains so until it is cancelled,
     * its channel is closed, or its selector is closed.  </p>
     *
     * @return {@code true} if, and only if, this key is valid
     */
    // 判断当前"选择键"对象是否有效
    public abstract boolean isValid();
    
    /**
     * Requests that the registration of this key's channel with its selector
     * be cancelled.  Upon return the key will be invalid and will have been
     * added to its selector's cancelled-key set.  The key will be removed from
     * all of the selector's key sets during the next selection operation.
     *
     * <p> If this key has already been cancelled then invoking this method has
     * no effect.  Once cancelled, a key remains forever invalid. </p>
     *
     * <p> This method may be invoked at any time.  It synchronizes on the
     * selector's cancelled-key set, and therefore may block briefly if invoked
     * concurrently with a cancellation or selection operation involving the
     * same selector.  </p>
     */
    // 取消当前"选择键"对象，取消之后其状态变为无效
    // 如果结束使用连接，就要撤销其SelectionKey对象的注册，这样选择器就不会浪费资源再去查询它是否准备就绪
    public abstract void cancel();
    
    /*▲ 状态 ████████████████████████████████████████████████████████████████████████████████┛ */
    
}
