/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.net;

import java.io.Serializable;

/**
 * This class represents a Socket Address with no protocol attachment.
 * As an abstract class, it is meant to be subclassed with a specific, protocol dependent, implementation.
 * <p>
 * It provides an immutable object used by sockets for binding, connecting, or as returned values.
 *
 * @see java.net.Socket
 * @see java.net.ServerSocket
 * @since 1.4
 */
/*表示【连接端点】的【地址(ip + port)】
 *
 *
 * 理论上，该类可以用于【TCP和非TCP】的socket，但实际上，当前只支持【TCP/IP socket】。
 * 实际使用的【Socket地址】都是【InetSocketAddress】的实例。
 */
public abstract class SocketAddress implements Serializable {
    static final long serialVersionUID = 5215720748342549866L;
}
