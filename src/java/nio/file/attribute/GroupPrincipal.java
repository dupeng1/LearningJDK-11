/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

/**
 * A {@code UserPrincipal} representing a <em>group identity</em>, used to
 * determine access rights to objects in a file system. The exact definition of
 * a group is implementation specific, but typically, it represents an identity
 * created for administrative purposes so as to determine the access rights for
 * the members of the group. Whether an entity can be a member of multiple
 * groups, and whether groups can be nested, are implementation specified and
 * therefore not specified.
 *
 * @see UserPrincipalLookupService#lookupPrincipalByGroupName
 * @since 1.7
 */
/*
 * 用来标识对象的【"用户组"信息】，以指示其关联文件在文件系统中的【访问权限】
 *
 * 这里只是简单地继承了UserPrincipal，表示它们共用一套API，由不同的参数来决定获取哪个属性值
 */
public interface GroupPrincipal extends UserPrincipal {
}
