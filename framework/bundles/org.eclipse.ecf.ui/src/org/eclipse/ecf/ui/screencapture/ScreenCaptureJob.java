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

package org.eclipse.ecf.ui.screencapture;

import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;

public class ScreenCaptureJob extends UIJob {

	final Color blackColor;

	final Color whiteColor;

	boolean isDragging = false;

	int downX = -1;

	int downY = -1;

	final ID targetID;

	final IImageSender imageSender;

	public ScreenCaptureJob(Display display, ID targetID, IImageSender imageSender) {
		super(display, "Capturing screen..."); //$NON-NLS-1$
		blackColor = new Color(display, 0, 0, 0);
		whiteColor = new Color(display, 255, 255, 255);
		this.targetID = targetID;
		this.imageSender = imageSender;
	}

	public IStatus runInUIThread(IProgressMonitor monitor) {
		final Display display = getDisplay();
		final GC context = new GC(display);
		final Image image = new Image(display, display.getBounds());
		context.copyArea(image, 0, 0);
		context.dispose();

		final Shell shell = new Shell(display, SWT.NO_TRIM);
		shell.setLayout(new FillLayout());
		shell.setBounds(display.getBounds());
		final GC gc = new GC(shell);
		shell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				gc.drawImage(image, 0, 0);
			}
		});

		shell.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				isDragging = true;
				downX = e.x;
				downY = e.y;
			}

			public void mouseUp(MouseEvent e) {
				isDragging = false;
				final int width = Math.max(downX, e.x) - Math.min(downX, e.x);
				final int height = Math.max(downY, e.y) - Math.min(downY, e.y);
				if (width != 0 && height != 0) {
					final Image copy = new Image(display, width, height);
					gc.copyArea(copy, Math.min(downX, e.x), Math.min(downY, e.y));
					blackColor.dispose();
					whiteColor.dispose();
					final Dialog dialog = new ScreenCaptureConfirmationDialog(shell, targetID, copy, width, height, imageSender);
					dialog.open();
					shell.close();
					image.dispose();
				}
			}
		});

		shell.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				if (isDragging) {
					gc.drawImage(image, 0, 0);
					gc.setForeground(blackColor);
					gc.drawRectangle(downX, downY, e.x - downX, e.y - downY);
					gc.setForeground(whiteColor);
					gc.drawRectangle(downX - 1, downY - 1, e.x - downX + 2, e.y - downY + 2);
				}
			}
		});
		shell.setCursor(new Cursor(getDisplay(), SWT.CURSOR_CROSS));
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return Status.OK_STATUS;
	}
}