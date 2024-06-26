/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

/**
 * Type is the common superinterface for all types in the Java
 * programming language. These include raw types, parameterized types,
 * array types, type variables and primitive types.
 *
 * @since 1.5
 */
/*
 * 代表Java中的“类型”
 *
 * 主要包括5种实现：
 * Class
 * TypeVariable
 * ParameterizedType
 * WildcardType
 * GenericArrayType
 */
// Type是java中【所有类型】的通用超级接口，这些类型包括基本类型、参数化类型、数组类型、类型变量
public interface Type {
    /**
     * Returns a string describing this type, including information about any type parameters.
     *
     * @return a string describing this type
     *
     * @implSpec The default implementation calls {@code toString}.
     * @since 1.8
     */
    // 返回对该类型的描述
    default String getTypeName() {
        return toString();
    }
}
