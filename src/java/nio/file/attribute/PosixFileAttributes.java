/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

/**
 * File attributes associated with files on file systems used by operating systems
 * that implement the Portable Operating System Interface (POSIX) family of
 * standards.
 *
 * <p> The POSIX attributes of a file are retrieved using a {@link
 * PosixFileAttributeView} by invoking its {@link
 * PosixFileAttributeView#readAttributes readAttributes} method.
 *
 * @since 1.7
 */
/*
 * 基于POSIX标准的操作系统平台中的文件属性
 *
 * windows平台上没有实现该接口，而linux/mac系统上实现了该接口。
 * 1、读取属性
 * Path path = Paths.get("/data/logs/web.log");
 * PosixFileAttributes attributes = Files.readAttributes(path,PosixFileAttributes.class);
 * 用户组和权限
 * UserPrincipal userPrincipal = attributes.owner();
 * System.out.println(userPrincipal.toString());
 * GroupPrincipal groupPrincipal =  attributes.group();
 * System.out.println(groupPrincipal.toString());
 * Set<PosixFilePermission> permissions = attributes.permissions();
 * 2、创建文件
 * 将权限转换为文件属性，用于创建新的文件,目前文件权限也是一种属性
 * FileAttribute<Set<PosixFilePermission>> fileAttribute = PosixFilePermissions.asFileAttribute(permissions);
 * Files.createFile(Paths.get("/data/test.log"),fileAttribute);
 * 3、修改文件属性
 * 修改文件权限，可以在permissions中增减权限列表，枚举
 * Files.setPosixFilePermissions(path,permissions);
 */
public interface PosixFileAttributes extends BasicFileAttributes {
    
    /**
     * Returns the owner of the file.
     *
     * @return the file owner
     *
     * @see PosixFileAttributeView#setOwner
     */
    // 返回文件的【所有者信息】
    UserPrincipal owner();
    
    /**
     * Returns the group owner of the file.
     *
     * @return the file group owner
     *
     * @see PosixFileAttributeView#setGroup
     */
    // 返回文件的【用户组信息】
    GroupPrincipal group();
    
    /**
     * Returns the permissions of the file. The file permissions are returned
     * as a set of {@link PosixFilePermission} elements. The returned set is a
     * copy of the file permissions and is modifiable. This allows the result
     * to be modified and passed to the {@link PosixFileAttributeView#setPermissions
     * setPermissions} method to update the file's permissions.
     *
     * @return the file permissions
     *
     * @see PosixFileAttributeView#setPermissions
     */
    // 返回文件的【权限信息】
    Set<PosixFilePermission> permissions();
    
}
