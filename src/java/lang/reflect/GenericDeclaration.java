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
 * A common interface for all entities that declare type variables.
 *
 * @since 1.5
 */
// 泛型元素，指有能力引入泛型声明的元素，比如类、方法、构造器
// 1、可以声明类型变量的实体的公共接口，也就是说，只有实现了该接口才能在对应的实体上声明（定义）类型变量，
// 2、用来定义哪些对象上是可以声明（定义）范型变量，所谓范型变量就是<E extends List>或者<E>, 也就是TypeVariable<D>这个接口的对应的对象
// 3、实现GenericDeclaration接口的类包括Class, Method, Constructor，也就是说只能在这几种对象上进行范型变量的声明（定义）
public interface GenericDeclaration extends AnnotatedElement {
    /**
     * Returns an array of {@code TypeVariable} objects that
     * represent the type variables declared by the generic
     * declaration represented by this {@code GenericDeclaration}
     * object, in declaration order.  Returns an array of length 0 if
     * the underlying generic declaration declares no type variables.
     *
     * @return an array of {@code TypeVariable} objects that represent
     * the type variables declared by this generic declaration
     *
     * @throws GenericSignatureFormatError if the generic
     *                                     signature of this generic declaration does not conform to
     *                                     the format specified in
     *                                     <cite>The Java&trade; Virtual Machine Specification</cite>
     */
    // 返回generic type中的type variable，如：Map<K, V>中的K和V
    // 获得声明列表上的类型变量数组
    public TypeVariable<?>[] getTypeParameters();
}
