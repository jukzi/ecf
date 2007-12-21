/****************************************************************************
 * Copyright (c) 2007 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/

package org.eclipse.ecf.provider.filetransfer.browse;

import java.io.File;
import java.util.*;
import org.eclipse.ecf.filetransfer.IRemoteFileAttributes;

/**
 *
 */
public class LocalFileAttributes implements IRemoteFileAttributes {

	File file = null;

	static String[] fileAttributes = {IRemoteFileAttributes.READ_ATTRIBUTE, IRemoteFileAttributes.WRITE_ATTRIBUTE, IRemoteFileAttributes.HIDDEN_ATTRIBUTE};
	static List attributeKeys = new ArrayList(Arrays.asList(fileAttributes));

	public LocalFileAttributes(File file) {
		this.file = file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRemoteFileAttributes#getAttribute(java.lang.String)
	 */
	public String getAttribute(String key) {
		if (key == null)
			return null;
		if (key.equals(IRemoteFileAttributes.READ_ATTRIBUTE)) {
			if (file.canRead())
				return Boolean.TRUE.toString();
		} else if (key.equals(IRemoteFileAttributes.WRITE_ATTRIBUTE)) {
			if (file.canWrite())
				return Boolean.TRUE.toString();
		} else if (key.equals(IRemoteFileAttributes.HIDDEN_ATTRIBUTE)) {
			if (file.isHidden())
				return Boolean.TRUE.toString();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRemoteFileAttributes#getAttributeKeys()
	 */
	public Iterator getAttributeKeys() {
		return attributeKeys.iterator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.filetransfer.IRemoteFileAttributes#setAttribute(java.lang.String, java.lang.String)
	 */
	public void setAttribute(String key, String value) {
		// not supported
	}

}
