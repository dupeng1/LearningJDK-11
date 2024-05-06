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

package java.lang.reflect;

/**
 * TypeVariable is the common superinterface for type variables of kinds.
 * A type variable is created the first time it is needed by a reflective
 * method, as specified in this package.  If a type variable t is referenced
 * by a type (i.e, class, interface or annotation type) T, and T is declared
 * by the nth enclosing class of T (see JLS 8.1.2), then the creation of t
 * requires the resolution (see JVMS 5) of the ith enclosing class of T,
 * for i = 0 to n, inclusive. Creating a type variable must not cause the
 * creation of its bounds. Repeated creation of a type variable has no effect.
 *
 * <p>Multiple objects may be instantiated at run-time to
 * represent a given type variable. Even though a type variable is
 * created only once, this does not imply any requirement to cache
 * instances representing the type variable. However, all instances
 * representing a type variable must be equal() to each other.
 * As a consequence, users of type variables must not rely on the identity
 * of instances of classes implementing this interface.
 *
 * @param <D> the type of generic declaration that declared the
 *            underlying type variable.
 *
 * @since 1.5
 */
/*
 * 1、类型变量，描述类型，表示泛指任意或相关一类类型，也可以说狭义上的【泛型】（泛指某一类类型），一般用大写字母作为变量，比如K、V、E
 *
 * 2、所谓类型变量就是<E extends List>或者<E>, 也就是TypeVariable<D>这个接口的对应的对象，
 *
 * 3、类型变量的声明：<E>，前后需要加上尖括号
 *
 * 4、TypeVariable<D>中的D是extends GenericDeclaration的，用来通过类型变量反向获取拥有这个变量的GenericDeclaration
 *
 * 5、泛型信息在编译时会被转换成一个特定的类型，而TypeVariable就是用来反应JVM编辑该泛型前的信息（通俗讲TypeVariable就是我们常用的
 * List<T>、Map<K,V>中的T，K这种泛型变量），还可以对类型变量加上extends限定，这样会有类型变量对应的上限；值得注意的是类型变量的上限
 * 可以有多个，必须使用&连接，例如：List<T extends Number & Serializable>，其中&后必须是接口
 *
 * 示例：
 * public class Bean<X, Y extends Number> {
 * }
 *
 * Bean类中的类型变量是X和Y
 */
public interface TypeVariable<D extends GenericDeclaration> extends Type, AnnotatedElement {
    /**
     * Returns an array of {@code Type} objects representing the
     * upper bound(s) of this type variable.  If no upper bound is
     * explicitly declared, the upper bound is {@code Object}.
     *
     * <p>For each upper bound B: <ul> <li>if B is a parameterized
     * type or a type variable, it is created, (see {@link
     * java.lang.reflect.ParameterizedType ParameterizedType} for the
     * details of the creation process for parameterized types).
     * <li>Otherwise, B is resolved.  </ul>
     *
     * @return an array of {@code Type}s representing the upper
     * bound(s) of this type variable
     *
     * @throws TypeNotPresentException             if any of the
     *                                             bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *                                             bounds refer to a parameterized type that cannot be instantiated
     *                                             for any reason
     */
    // 类型变量的边界（上界）
    // 获得泛型的上限，若未明确声明上边界则默认为Object
    Type[] getBounds();
    
    /**
     * Returns the {@code GenericDeclaration} object representing the
     * generic declaration declared this type variable.
     *
     * @return the generic declaration declared for this type variable.
     *
     * @since 1.5
     */
    // 类型变量所属的泛型声明
    // 获得声明这个类型变量的类型及名称
    D getGenericDeclaration();
    
    /**
     * Returns the name of this type variable, as it occurs in the source code.
     *
     * @return the name of this type variable, as it appears in the source code
     */
    // 类型变量名称
    // 获得名称，即K、V、E之类名称
    String getName();
    
    /**
     * Returns an array of AnnotatedType objects that represent the use of
     * types to denote the upper bounds of the type parameter represented by
     * this TypeVariable. The order of the objects in the array corresponds to
     * the order of the bounds in the declaration of the type parameter. Note that
     * if no upper bound is explicitly declared, the upper bound is unannotated
     * {@code Object}.
     *
     * @return an array of objects representing the upper bound(s) of the type variable
     *
     * @since 1.8
     */
    // 上界的"类型注解+类型变量"
    AnnotatedType[] getAnnotatedBounds();
}
