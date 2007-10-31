/*******************************************************************************
 * Copyright (c) 2005, 2007 Remy Suen
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Remy Suen <remy.suen@gmail.com> - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.protocol.msn;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import org.eclipse.ecf.protocol.msn.events.IChatSessionListener;
import org.eclipse.ecf.protocol.msn.internal.encode.StringUtils;

/**
 * <p>
 * A ChatSession is a conversation that's held between two or more participants.
 * </p>
 * 
 * <p>
 * As specified by {@link MsnClient}'s
 * {@link MsnClient#disconnect() disconnect()} method, ChatSessions are not
 * automatically disconnected when the client itself disconnects. However,
 * clean-up will be performed automatically when a
 * {@link IChatSessionListener#sessionTimedOut() sessionTimedOut()} event occurs
 * or when the last user has left (as checked by monitoring the
 * {@link IChatSessionListener#contactLeft(Contact) contactLeft(Contact)}
 * event).
 * </p>
 * 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still
 * under development and expected to change significantly before reaching
 * stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this
 * API will almost certainly be broken (repeatedly) as the API evolves.
 * </p>
 */
public final class ChatSession extends Session {

	/**
	 * The list of contacts that are currently a part of this session. Note that
	 * this does not include the client user.
	 */
	private final ArrayList contacts;

	private final ContactList contactList;

	private final String email = client.getUserEmail();

	private boolean joined = false;

	/**
	 * Create a new ChatSession that connects to the given host.
	 * 
	 * @param host
	 *            the host to be connected to
	 * @param client
	 *            the MsnClient to hook onto
	 * @throws IOException
	 *             If an I/O error occurs while attempting to connect to the
	 *             host
	 */
	ChatSession(String host, MsnClient client) throws IOException {
		super(host, client);
		listeners = new ArrayList();
		contacts = new ArrayList();
		contactList = client.getContactList();
	}

	/**
	 * Create a new ChatSession that will connect to the given server response
	 * using the specified username.
	 * 
	 * @param host
	 *            the host to be connected to
	 * @param client
	 *            the MsnClient to hook onto
	 * @param username
	 *            the username to authenticate with
	 * @param info
	 *            the authentication info
	 * @throws IOException
	 *             If an I/O error occurs while attempting to connect to the
	 *             specified host
	 */
	ChatSession(String host, MsnClient client, String username, String info) throws IOException {
		this(host, client);
		authenticate(username, info);
	}

	/**
	 * This method attempts to authenticate the user with the switchboard server
	 * that it was instantiated to.
	 * 
	 * @param username
	 *            the user's MSN email address
	 * @param info
	 *            the authentication information
	 * @throws IOException
	 */
	private void authenticate(String username, String info) throws IOException {
		write("USR", username + ' ' + info); //$NON-NLS-1$
		final String input = super.read();
		// FIXME: check if this indexOf(String) call can be replaced with
		// startsWith(String)
		if (input == null || input.indexOf("USR") == -1) { //$NON-NLS-1$
			throw new ConnectException("Authentication has failed with the " + "switchboard server.");
		}
		idle();
	}

	public void close() {
		try {
			write("OUT"); //$NON-NLS-1$
		} catch (final Exception e) {
			// ignored
		}
		super.close();
	}

	/**
	 * Invites the user with the specified email to this chat session.
	 * 
	 * @param email
	 *            the user's email address
	 * @throws IOException
	 *             If an I/O error occurs while attempting to send the
	 *             invitation to the user
	 */
	public void invite(String email) throws IOException {
		synchronized (contacts) {
			for (int i = 0; i < contacts.size(); i++) {
				if (((Contact) contacts.get(i)).getEmail().equals(email)) {
					return;
				}
			}
		}
		write("CAL", email); //$NON-NLS-1$
		while (!joined);
		joined = false;
	}

	/**
	 * Sends a notifying event to the listeners connected to this that the
	 * specified contact has joined this switchboard.
	 * 
	 * @param contact
	 *            the contact that has joined this session
	 */
	private void fireContactJoinedEvent(Contact contact) {
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				((IChatSessionListener) listeners.get(i)).contactJoined(contact);
			}
		}
	}

	/**
	 * Informs the {@link IChatSessionListener}s attached to this that the
	 * given contact has left this session.
	 * 
	 * @param contact
	 *            the contact that left this session
	 */
	private void fireContactLeftEvent(Contact contact) {
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				((IChatSessionListener) listeners.get(i)).contactLeft(contact);
			}
		}
	}

	/**
	 * This event is fired when the specified contact has started typing.
	 * 
	 * @param contact
	 *            the contact who is typing
	 */
	private void fireContactIsTypingEvent(Contact contact) {
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				((IChatSessionListener) listeners.get(i)).contactIsTyping(contact);
			}
		}
	}

	/**
	 * This event is fired when a message has been received from the given user.
	 * 
	 * @param contact
	 *            the user that sent the message
	 * @param message
	 *            the message that has been received
	 */
	private void fireMessageReceivedEvent(Contact contact, String message) {
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				((IChatSessionListener) listeners.get(i)).messageReceived(contact, message);
			}
		}
	}

	/**
	 * Notifies attached {@link IChatSessionListener} that this session has now
	 * timed out.
	 */
	private void fireSessionTimedOutEvent() {
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				((IChatSessionListener) listeners.get(i)).sessionTimedOut();
			}
		}
	}

	/**
	 * Look for a contact that is connected to this switchboard connected to the
	 * given email. Comparison is done with the String class's equals(String)
	 * method, so case sensitivity is an issue.
	 * 
	 * @param email
	 *            the email of the contact being sought after
	 * @return the contact that uses the specified email
	 * @throws IllegalArgumentException
	 *             If the contact could not be found
	 */
	private Contact findContact(String email) throws IllegalArgumentException {
		for (int i = 0; i < contacts.size(); i++) {
			final Contact contact = (Contact) contacts.get(i);
			if (contact.getEmail().equals(email)) {
				return contact;
			}
		}
		throw new IllegalArgumentException("A contact with the email " + email + " could not be found in this ChatSession.");
	}

	/**
	 * Read the contents of the packet being sent from the server and handle any
	 * events accordingly.
	 * 
	 * @return the String returned from {@link Session#read()}
	 */
	String read() throws IOException {
		final String input = super.read();
		if (input == null) {
			return null;
		}

		final String[] lines = StringUtils.split(input, "\r\n"); //$NON-NLS-1$
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].startsWith("IRO")) { //$NON-NLS-1$
				final String[] split = StringUtils.splitOnSpace(lines[i]);
				Contact contact = contactList.getContact(split[4]);
				if (contact == null) {
					contact = new Contact(split[4], split[5]);
				}
				contacts.add(contact);
				fireContactJoinedEvent(contact);
			} else if (lines[i].startsWith("JOI")) { //$NON-NLS-1$
				final String[] split = StringUtils.splitOnSpace(lines[i]);
				Contact contact = contactList.getContact(split[2]);
				if (contact == null) {
					contact = new Contact(split[1], split[2]);
				}
				contacts.add(contact);
				joined = true;
				fireContactJoinedEvent(contact);
			} else if (lines[i].startsWith("BYE")) { //$NON-NLS-1$
				final String[] split = StringUtils.splitOnSpace(lines[i]);
				if (split.length == 2) {
					final Contact contact = findContact(split[1]);
					contacts.remove(contact);
					fireContactLeftEvent(contact);
					if (contacts.isEmpty()) {
						close();
					}
				} else {
					fireSessionTimedOutEvent();
					close();
				}
			} else if (lines[i].startsWith("MSG")) { //$NON-NLS-1$
				if (input.indexOf("TypingUser:") != -1) { //$NON-NLS-1$
					final String trim = input.substring(input.indexOf("MSG")); //$NON-NLS-1$
					final String content = StringUtils.splitSubstring(trim, "\r\n", 3); //$NON-NLS-1$
					fireContactIsTypingEvent(findContact(StringUtils.splitOnSpace(content)[1]));
				} else if (input.indexOf("text/plain") != -1) { //$NON-NLS-1$
					final int index = input.indexOf("ANS") == -1 ? 2 : 3; //$NON-NLS-1$
					final String[] contents = StringUtils.split(input, "\r\n", index); //$NON-NLS-1$
					String[] split = StringUtils.splitOnSpace(contents[index - 2]);
					final Contact contact = findContact(split[1]);

					final int count = Integer.parseInt(split[3]);
					split = StringUtils.split(contents[index - 1], "\r\n\r\n"); //$NON-NLS-1$

					final int text = count - (split[0].length() + 4);
					fireMessageReceivedEvent(contact, split[1].substring(0, text));
				}
			}
		}

		return input;
	}

	/**
	 * <p>
	 * <b>Note:</b> This method will likely be modified in the future (renamed,
	 * change in return type, <tt>Contact[]</tt> <-> <tt>java.util.List</tt>,
	 * inclusion/exclusion of the current user, complete removal, etc.). Please
	 * use it at your own risk.
	 * </p>
	 * 
	 * <p>
	 * This method returns the Contacts that are currently participating in this
	 * ChatSession. Note that this does not include the current user.
	 * </p>
	 * @return array of contacts that are the participants.
	 */
	public Contact[] getParticipants() {
		return (Contact[]) contacts.toArray(new Contact[contacts.size()]);
	}

	/**
	 * Sends a message to the users connected to this chat session.
	 * 
	 * @param message
	 *            the message to be sent
	 * @throws IOException
	 *             If an I/O occurs when sending the message to the server
	 */
	public void sendMessage(String message) throws IOException {
		message = "MIME-Version: 1.0\r\n" //$NON-NLS-1$
				+ "Content-Type: text/plain; charset=UTF-8\r\n" //$NON-NLS-1$
				+ "X-MMS-IM-Format: FN=MS%20Sans%20Serif; EF=; CO=0; PF=0\r\n\r\n" //$NON-NLS-1$
				+ message;
		write("MSG", "N " + message.length() + "\r\n" + message, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Notifies the participants of this chat session that the current user is
	 * typing a message.
	 * 
	 * @throws IOException
	 *             If an I/O occurs when sending the message to the server
	 */
	public void sendTypingNotification() throws IOException {
		final String message = "MIME-Version: 1.0\r\n" //$NON-NLS-1$
				+ "Content-Type: text/x-msmsgscontrol\r\nTypingUser: " + email //$NON-NLS-1$
				+ "\r\n\r\n\r\n"; //$NON-NLS-1$
		write("MSG", "U " + message.length() + "\r\n" + message, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Adds a IChatSessionListener to this session.
	 * 
	 * @param listener
	 *            the listener to be added
	 */
	public void addChatSessionListener(IChatSessionListener listener) {
		if (listener != null) {
			synchronized (listeners) {
				if (!listeners.contains(listener)) {
					listeners.add(listener);
				}
			}
		}
	}

	/**
	 * Removes a IChatSessionListener from this session.
	 * 
	 * @param listener
	 *            the listener to be removed
	 */
	public void removeChatSessionListener(IChatSessionListener listener) {
		if (listener != null) {
			synchronized (listeners) {
				listeners.remove(listener);
			}
		}
	}
}
