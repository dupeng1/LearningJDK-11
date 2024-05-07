/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * Indicates how long annotations with the annotated type are to
 * be retained.  If no Retention annotation is present on
 * an annotation type declaration, the retention policy defaults to
 * {@code RetentionPolicy.CLASS}.
 *
 * <p>A Retention meta-annotation has effect only if the
 * meta-annotated type is used directly for annotation.  It has no
 * effect if the meta-annotated type is used as a member type in
 * another annotation type.
 *
 * @author Joshua Bloch
 * @jls 9.6.4.2 @Retention
 * @since 1.5
 */
// 元注解，标记注解的保留范围，默认是RetentionPolicy.CLASS
// 1、RetentionPolicy是Annotation（注解）的策略属性，而@Retention的作用，就是指定Annotation（注解）的策略属性
// 2、@Retention(RetentionPolicy.RUNTIME)指定该Annotation(注解)的策略属性是RetentionPolicy.RUNTIME，
// 编译器会将该Annotation信息保存在对应类的.class文件中，并且能被jvm读取
// 3、定义Annotation（注解）时，@Retention可有可无，若没有@Retention，则默认是RetentionPolicy.CLASS（默认行为）
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Retention {
    /**
     * Returns the retention policy.
     *
     * @return the retention policy
     */
    RetentionPolicy value();
}
