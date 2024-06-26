/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.annotation;

/**
 * The constants of this enumerated type provide a simple classification of the
 * syntactic locations where annotations may appear in a Java program. These
 * constants are used in {@link java.lang.annotation.Target Target}
 * meta-annotations to specify where it is legal to write annotations of a
 * given type.
 *
 * <p>The syntactic locations where annotations may appear are split into
 * <em>declaration contexts</em> , where annotations apply to declarations, and
 * <em>type contexts</em> , where annotations apply to types used in
 * declarations and expressions.
 *
 * <p>The constants {@link #ANNOTATION_TYPE}, {@link #CONSTRUCTOR}, {@link
 * #FIELD}, {@link #LOCAL_VARIABLE}, {@link #METHOD}, {@link #PACKAGE}, {@link
 * #MODULE}, {@link #PARAMETER}, {@link #TYPE}, and {@link #TYPE_PARAMETER}
 * correspond to the declaration contexts in JLS 9.6.4.1.
 *
 * <p>For example, an annotation whose type is meta-annotated with
 * {@code @Target(ElementType.FIELD)} may only be written as a modifier for a
 * field declaration.
 *
 * <p>The constant {@link #TYPE_USE} corresponds to the type contexts in JLS
 * 4.11, as well as to two declaration contexts: type declarations (including
 * annotation type declarations) and type parameter declarations.
 *
 * <p>For example, an annotation whose type is meta-annotated with
 * {@code @Target(ElementType.TYPE_USE)} may be written on the type of a field
 * (or within the type of the field, if it is a nested, parameterized, or array
 * type), and may also appear as a modifier for, say, a class declaration.
 *
 * <p>The {@code TYPE_USE} constant includes type declarations and type
 * parameter declarations as a convenience for designers of type checkers which
 * give semantics to annotation types. For example, if the annotation type
 * {@code NonNull} is meta-annotated with
 * {@code @Target(ElementType.TYPE_USE)}, then {@code @NonNull}
 * {@code class C {...}} could be treated by a type checker as indicating that
 * all variables of class {@code C} are non-null, while still allowing
 * variables of other classes to be non-null or not non-null based on whether
 * {@code @NonNull} appears at the variable's declaration.
 *
 * @author Joshua Bloch
 * @jls 9.6.4.1 @Target
 * @jls 4.1 The Kinds of Types and Values
 * @since 1.5
 */
/*
 * 表示注解应用范围的常量，使用方式：@Target(ElementType.XXX)
 * 除TYPE_PARAMETER和TYPE_USE属于类型注解，其他注解属于声明式注解
 */
// 此枚举类型的常量提供了可能出现在java程序中注解的位置进行简单分类
// 这些常量用于{@link Target java.lang.annotation.Target}元注解，以指定编写给定类型注释的合法位置
public enum ElementType {
    /** Class, interface (including annotation type), or enum declaration */
    TYPE,   // 用于类、接口、注解、枚举类型的声明
    
    /** Field declaration (includes enum constants) */
    FIELD,  // 用于字段、枚举常量的声明
    
    /** Method declaration */
    METHOD, // 用于方法声明
    
    /** Formal parameter declaration */
    PARAMETER,  // 用于形参声明（包括异常参数）
    
    /** Constructor declaration */
    CONSTRUCTOR,    // 用于构造器的声明
    
    /** Local variable declaration */
    LOCAL_VARIABLE, // 用于局部变量的声明
    
    /** Annotation type declaration */
    ANNOTATION_TYPE,    // 用于注解类型的声明
    
    /** Package declaration */
    PACKAGE,    // 用在包声明：package-info
    
    /** @since 9 Module declaration */
    MODULE,     // 用在模块声明：module-info
    
    /** @since 1.8 Type parameter declaration */
    TYPE_PARAMETER, // 用于Type parameter，如List<E>中的E
    
    /** @since 1.8 Use of a type */
    TYPE_USE,   // 用于广义的类型声明（不能用于package），比TYPE的应用范围更广，可通过AnnotatedType获取
}
