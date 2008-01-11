/****************************************************************************
 * Copyright (c) 2008 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/

package org.ecllpse.ecf.tests.provider.xmpp;

import org.eclipse.ecf.tests.presence.AbstractChatRoomParticipantTest;

/**
 *
 */
public class ChatRoomParticipantTest extends AbstractChatRoomParticipantTest {

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.tests.presence.AbstractPresenceTestCase#getClientContainerName()
	 */
	protected String getClientContainerName() {
		return XMPP.CONTAINER_NAME;
	}

}
