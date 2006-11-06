/*******************************************************************************
 * Copyright (c) 2004 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.filetransfer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.ecf.filetransfer.IIncomingFileTransfer;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveStartEvent;

public class UrlRetrieveFileTransfer extends AbstractRetrieveFileTransfer {

	URLConnection urlConnection;

	protected void openStreams() throws IncomingFileTransferException {
		URL theURL = null;

		try {

			theURL = getRemoteFileReference().toURL();
			urlConnection = theURL.openConnection();
			setInputStream(urlConnection.getInputStream());
			setFileLength(fileLength = urlConnection.getContentLength());

			listener
					.handleTransferEvent(new IIncomingFileTransferReceiveStartEvent() {
						private static final long serialVersionUID = -59096575294481755L;

						public URI getURI() {
							try {
								return new URI(urlConnection.getURL()
										.toString());
							} catch (Exception e) {
								return getRemoteFileReference();
							}
						}

						public IIncomingFileTransfer receive(
								File localFileToSave) throws IOException {
							setOutputStream(new BufferedOutputStream(
									new FileOutputStream(localFileToSave)));
							job = new FileTransferJob(getRemoteFileReference().toString());
							job.schedule();
							return UrlRetrieveFileTransfer.this;
						}

						public String toString() {
							StringBuffer sb = new StringBuffer(
									"IIncomingFileTransferReceiveStartEvent[");
							sb.append("isdone=").append(done).append(";");
							sb.append("bytesReceived=").append(bytesReceived)
									.append("]");
							return sb.toString();
						}

						public void cancel() {
							hardClose();
						}

					});
		} catch (Exception e) {
			throw new IncomingFileTransferException("Exception connecting to "
					+ theURL.toString(), e);
		}

	}
	

}
