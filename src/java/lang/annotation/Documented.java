/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * If the annotation {@code @Documented} is present on the declaration
 * of an annotation type <i>A</i>, then any {@code @A} annotation on
 * an element is considered part of the element's public contract.
 *
 * In more detail, when an annotation type <i>A</i> is annotated with
 * {@code Documented}, the presence and value of annotations of type
 * <i>A</i> are a part of the public contract of the elements <i>A</i>
 * annotates.
 *
 * Conversely, if an annotation type <i>B</i> is <em>not</em>
 * annotated with {@code Documented}, the presence and value of
 * <i>B</i> annotations are <em>not</em> part of the public contract
 * of the elements <i>B</i> annotates.
 *
 * Concretely, if an annotation type is annotated with {@code
 * Documented}, by default a tool like javadoc will display
 * annotations of that type in its output while annotations of
 * annotation types without {@code Documented} will not be displayed.
 *
 * @author  Joshua Bloch
 * @since 1.5
 */
// 元注解，标记注解是否可以文档化，比如输出到JavaDoc文档中
// 1、类和方法的Annotation在缺省的情况下是不出现在javadoc中的，使用@Documented修饰该注解则表示它可以出现在javadoc中。
// 2、定义注解时，@Documented可有可无，若没有定义，则注解不会出现在javadoc中
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Documented {
}
