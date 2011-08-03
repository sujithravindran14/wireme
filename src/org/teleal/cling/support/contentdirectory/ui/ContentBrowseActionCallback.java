/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.support.contentdirectory.ui;

import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.SortCriterion;
import org.teleal.cling.support.contentdirectory.callback.Browse;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.Item;

import com.wireme.activity.ContentItem;

import android.R.anim;
import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Updates a tree model after querying a backend <em>ContentDirectory</em>
 * service.
 * 
 * @author Christian Bauer
 */
public class ContentBrowseActionCallback extends Browse {

	private static Logger log = Logger
			.getLogger(ContentBrowseActionCallback.class.getName());

	private Service service;
	private Container container;
	private ArrayAdapter<ContentItem> listAdapter;
	private Activity activity;

	public ContentBrowseActionCallback(Activity activity, Service service,
			Container container, ArrayAdapter<ContentItem> listadapter) {
		super(service, container.getId(), BrowseFlag.DIRECT_CHILDREN, "*", 0,
				null, new SortCriterion(true, "dc:title"));
		this.activity = activity;
		this.service = service;
		this.container = container;
		this.listAdapter = listadapter;
	}

	public void received(final ActionInvocation actionInvocation,
			final DIDLContent didl) {
		log.fine("Received browse action DIDL descriptor, creating tree nodes");
		activity.runOnUiThread(new Runnable() {
			public void run() {
				try {
					listAdapter.clear();
					// Containers first
					for (Container childContainer : didl.getContainers()) {
						log.fine("add child container " + childContainer.getTitle());
						listAdapter.add(new ContentItem(childContainer, service));
					}
					// Now items
					for (Item childItem : didl.getItems()) {
						log.fine("add child item" + childItem.getTitle());
						listAdapter.add(new ContentItem(childItem, service));
					}
				} catch (Exception ex) {
					log.fine("Creating DIDL tree nodes failed: " + ex);
					actionInvocation.setFailure(new ActionException(
							ErrorCode.ACTION_FAILED,
							"Can't create list childs: " + ex, ex));
					failure(actionInvocation, null);
				}
			}
		});
	}

	public void updateStatus(final Status status) {
	}

	@Override
	public void failure(ActionInvocation invocation, UpnpResponse operation,
			final String defaultMsg) {
	}
}
