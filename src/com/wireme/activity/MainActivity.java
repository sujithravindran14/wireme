package com.wireme.activity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.support.contentdirectory.ui.ContentBrowseActionCallback;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.PersonWithRole;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.WriteStatus;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.ImageItem;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.cling.support.model.item.VideoItem;
import org.teleal.common.logging.LoggingUtil;
import org.teleal.common.util.MimeType;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.wireme.R;
import com.wireme.player.GPlayer;
import com.wireme.mediaserver.ContentNode;
import com.wireme.mediaserver.ContentTree;
import com.wireme.mediaserver.MediaServer;
import com.wireme.util.FixedAndroidHandler;

public class MainActivity extends Activity {

	private static final Logger log = Logger.getLogger(MainActivity.class
			.getName());

	private ListView deviceListView;
	private ListView contentListView;

	private ArrayAdapter<DeviceItem> deviceListAdapter;
	private ArrayAdapter<ContentItem> contentListAdapter;

	private AndroidUpnpService upnpService;

	private DeviceListRegistryListener deviceListRegistryListener;

	private MediaServer mediaServer;

	private static boolean serverPrepared = false;

	private final static String LOGTAG = "WireMe";

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			upnpService = (AndroidUpnpService) service;
			Log.v(LOGTAG, "Connected to UPnP Service");

			if (mediaServer == null) {
				try {
					mediaServer = new MediaServer(getLocalIpAddress());
					upnpService.getRegistry()
							.addDevice(mediaServer.getDevice());
					prepareMediaServer();

				} catch (Exception ex) {
					// TODO: handle exception
					log.log(Level.SEVERE, "Creating demo device failed", ex);
					Toast.makeText(MainActivity.this,
							R.string.create_demo_failed, Toast.LENGTH_SHORT)
							.show();
					return;
				}
			}

			deviceListAdapter.clear();
			for (Device device : upnpService.getRegistry().getDevices()) {
				deviceListRegistryListener.deviceAdded(new DeviceItem(device));
			}

			// Getting ready for future device advertisements
			upnpService.getRegistry().addListener(deviceListRegistryListener);
			// Refresh device list
			upnpService.getControlPoint().search();
		}

		public void onServiceDisconnected(ComponentName className) {
			upnpService = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Fix the logging integration between java.util.logging and Android
		// internal logging
		LoggingUtil.resetRootHandler(new FixedAndroidHandler());
		Logger.getLogger("org.teleal.cling").setLevel(Level.INFO);

		/*
		 * Enable this for debug logging:
		 * 
		 * // UDP communication
		 * Logger.getLogger("org.teleal.cling.transport.spi.DatagramIO"
		 * ).setLevel(Level.FINE);
		 * Logger.getLogger("org.teleal.cling.transport.spi.MulticastReceiver"
		 * ).setLevel(Level.FINE);
		 * 
		 * // Discovery
		 * Logger.getLogger("org.teleal.cling.protocol.ProtocolFactory"
		 * ).setLevel(Level.FINER);
		 * Logger.getLogger("org.teleal.cling.protocol.async"
		 * ).setLevel(Level.FINER);
		 * 
		 * // Description
		 * Logger.getLogger("org.teleal.cling.protocol.ProtocolFactory"
		 * ).setLevel(Level.FINER);
		 * Logger.getLogger("org.teleal.cling.protocol.RetrieveRemoteDescriptors"
		 * ).setLevel(Level.FINE);
		 * Logger.getLogger("org.teleal.cling.transport.spi.StreamClient"
		 * ).setLevel(Level.FINEST);
		 * 
		 * Logger.getLogger("org.teleal.cling.protocol.sync.ReceivingRetrieval").
		 * setLevel(Level.FINE);
		 * Logger.getLogger("org.teleal.cling.binding.xml.DeviceDescriptorBinder"
		 * ).setLevel(Level.FINE);
		 * Logger.getLogger("org.teleal.cling.binding.xml.ServiceDescriptorBinder"
		 * ).setLevel(Level.FINE);
		 * Logger.getLogger("org.teleal.cling.transport.spi.SOAPActionProcessor"
		 * ).setLevel(Level.FINEST);
		 * 
		 * // Registry
		 * Logger.getLogger("org.teleal.cling.registry.Registry").setLevel
		 * (Level.FINER);
		 * Logger.getLogger("org.teleal.cling.registry.LocalItems"
		 * ).setLevel(Level.FINER);
		 * Logger.getLogger("org.teleal.cling.registry.RemoteItems"
		 * ).setLevel(Level.FINER);
		 */

		setContentView(R.layout.main);

		deviceListView = (ListView) findViewById(R.id.deviceList);
		contentListView = (ListView) findViewById(R.id.contentList);

		deviceListAdapter = new ArrayAdapter<DeviceItem>(this,
				android.R.layout.simple_list_item_1);
		deviceListRegistryListener = new DeviceListRegistryListener();
		deviceListView.setAdapter(deviceListAdapter);
		deviceListView.setOnItemClickListener(deviceItemClickListener);

		contentListAdapter = new ArrayAdapter<ContentItem>(this,
				android.R.layout.simple_list_item_1);
		contentListView.setAdapter(contentListAdapter);
		contentListView.setOnItemClickListener(contentItemClickListener);

		getApplicationContext().bindService(
				new Intent(this, WireUpnpService.class), serviceConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (upnpService != null) {
			upnpService.getRegistry()
					.removeListener(deviceListRegistryListener);
		}
		getApplicationContext().unbindService(serviceConnection);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.search_lan).setIcon(
				android.R.drawable.ic_menu_search);
		menu.add(0, 1, 0, R.string.toggle_debug_logging).setIcon(
				android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			searchNetwork();
			break;
		case 1:
			Logger logger = Logger.getLogger("org.teleal.cling");
			if (logger.getLevel().equals(Level.FINEST)) {
				Toast.makeText(this, R.string.disabling_debug_logging,
						Toast.LENGTH_SHORT).show();
				logger.setLevel(Level.INFO);
			} else {
				Toast.makeText(this, R.string.enabling_debug_logging,
						Toast.LENGTH_SHORT).show();
				logger.setLevel(Level.FINEST);
			}
			break;
		}
		return false;
	}

	protected void searchNetwork() {
		if (upnpService == null)
			return;
		Toast.makeText(this, R.string.searching_lan, Toast.LENGTH_SHORT).show();
		upnpService.getRegistry().removeAllRemoteDevices();
		upnpService.getControlPoint().search();
	}

	OnItemClickListener deviceItemClickListener = new OnItemClickListener() {

		protected Container createRootContainer(Service service) {
			Container rootContainer = new Container();
			rootContainer.setId("0");
			rootContainer.setTitle("Content Directory on "
					+ service.getDevice().getDisplayString());
			return rootContainer;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			// TODO Auto-generated method stub
			Device device = deviceListAdapter.getItem(position).getDevice();
			Service service = device.findService(new UDAServiceType(
					"ContentDirectory"));
			upnpService.getControlPoint().execute(
					new ContentBrowseActionCallback(MainActivity.this, service,
							createRootContainer(service), contentListAdapter));
		}
	};

	OnItemClickListener contentItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			// TODO Auto-generated method stub
			ContentItem content = contentListAdapter.getItem(position);
			if (content.isContainer()) {
				upnpService.getControlPoint().execute(
						new ContentBrowseActionCallback(MainActivity.this,
								content.getService(), content.getContainer(),
								contentListAdapter));
			} else {
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, GPlayer.class);
				intent.putExtra("playURI", content.getItem().getFirstResource()
						.getValue());
				startActivity(intent);
			}
		}
	};

	public class DeviceListRegistryListener extends DefaultRegistryListener {

		/* Discovery performance optimization for very slow Android devices! */

		@Override
		public void remoteDeviceDiscoveryStarted(Registry registry,
				RemoteDevice device) {
		}

		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry,
				final RemoteDevice device, final Exception ex) {
		}

		/*
		 * End of optimization, you can remove the whole block if your Android
		 * handset is fast (>= 600 Mhz)
		 */

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {

			if (device.getType().getNamespace().equals("schemas-upnp-org")
					&& device.getType().getType().equals("MediaServer")) {
				final DeviceItem display = new DeviceItem(device, device
						.getDetails().getFriendlyName(),
						device.getDisplayString(), "(REMOTE) "
								+ device.getType().getDisplayString());
				deviceAdded(display);
			}
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			final DeviceItem display = new DeviceItem(device,
					device.getDisplayString());
			deviceRemoved(display);
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			final DeviceItem display = new DeviceItem(device, device
					.getDetails().getFriendlyName(), device.getDisplayString(),
					"(REMOTE) " + device.getType().getDisplayString());
			deviceAdded(display);
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			final DeviceItem display = new DeviceItem(device,
					device.getDisplayString());
			deviceRemoved(display);
		}

		public void deviceAdded(final DeviceItem di) {
			runOnUiThread(new Runnable() {
				public void run() {

					int position = deviceListAdapter.getPosition(di);
					if (position >= 0) {
						// Device already in the list, re-set new value at same
						// position
						deviceListAdapter.remove(di);
						deviceListAdapter.insert(di, position);
					} else {
						deviceListAdapter.add(di);
					}

					// Sort it?
					// listAdapter.sort(DISPLAY_COMPARATOR);
					// listAdapter.notifyDataSetChanged();
				}
			});
		}

		public void deviceRemoved(final DeviceItem di) {
			runOnUiThread(new Runnable() {
				public void run() {
					deviceListAdapter.remove(di);
				}
			});
		}
	}

	private void prepareMediaServer() {

		if (serverPrepared)
			return;

		ContentNode rootNode = ContentTree.getRootNode();
		// Video Container
		Container videoContainer = new Container();
		videoContainer.setClazz(new DIDLObject.Class("object.container"));
		videoContainer.setId(ContentTree.VIDEO_ID);
		videoContainer.setParentID(ContentTree.ROOT_ID);
		videoContainer.setTitle("Videos");
		videoContainer.setRestricted(true);
		videoContainer.setWriteStatus(WriteStatus.NOT_WRITABLE);
		videoContainer.setChildCount(0);

		rootNode.getContainer().addContainer(videoContainer);
		rootNode.getContainer().setChildCount(
				rootNode.getContainer().getChildCount() + 1);
		ContentTree.addNode(ContentTree.VIDEO_ID, new ContentNode(
				ContentTree.VIDEO_ID, videoContainer));

		Cursor cursor;
		String[] videoColumns = { MediaStore.Video.Media._ID,
				MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DATA,
				MediaStore.Video.Media.ARTIST,
				MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.SIZE,
				MediaStore.Video.Media.DURATION,
				MediaStore.Video.Media.RESOLUTION };
		cursor = managedQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				videoColumns, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				String id = ContentTree.VIDEO_PREFIX
						+ cursor.getInt(cursor
								.getColumnIndex(MediaStore.Video.Media._ID));
				String title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
				String creator = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST));
				String filePath = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
				String mimeType = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
				long size = cursor.getLong(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
				long duration = cursor
						.getLong(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
				String resolution = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION));
				Res res = new Res(new MimeType(mimeType.substring(0,
						mimeType.indexOf('/')), mimeType.substring(mimeType
						.indexOf('/') + 1)), size, "http://"
						+ mediaServer.getAddress() + "/" + id);
				res.setDuration(duration / (1000 * 60 * 60) + ":"
						+ (duration % (1000 * 60 * 60)) / (1000 * 60) + ":"
						+ (duration % (1000 * 60)) / 1000);
				res.setResolution(resolution);

				VideoItem videoItem = new VideoItem(id, ContentTree.VIDEO_ID,
						title, creator, res);
				videoContainer.addItem(videoItem);
				videoContainer
						.setChildCount(videoContainer.getChildCount() + 1);
				ContentTree.addNode(id,
						new ContentNode(id, videoItem, filePath));

				Log.v(LOGTAG, "added video item " + title + "from " + filePath);
			} while (cursor.moveToNext());
		}

		// Audio Container
		Container audioContainer = new Container(ContentTree.AUDIO_ID,
				ContentTree.ROOT_ID, "Audios", "GNaP MediaServer",
				new DIDLObject.Class("object.container"), 0);
		audioContainer.setRestricted(true);
		audioContainer.setWriteStatus(WriteStatus.NOT_WRITABLE);
		rootNode.getContainer().addContainer(audioContainer);
		rootNode.getContainer().setChildCount(
				rootNode.getContainer().getChildCount() + 1);
		ContentTree.addNode(ContentTree.AUDIO_ID, new ContentNode(
				ContentTree.AUDIO_ID, audioContainer));

		String[] audioColumns = { MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.SIZE,
				MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM };
		cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				audioColumns, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				String id = ContentTree.AUDIO_PREFIX
						+ cursor.getInt(cursor
								.getColumnIndex(MediaStore.Audio.Media._ID));
				String title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
				String creator = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
				String filePath = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
				String mimeType = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
				long size = cursor.getLong(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
				long duration = cursor
						.getLong(cursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
				String album = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
				Res res = new Res(new MimeType(mimeType.substring(0,
						mimeType.indexOf('/')), mimeType.substring(mimeType
						.indexOf('/') + 1)), size, "http://"
						+ mediaServer.getAddress() + "/" + id);
				res.setDuration(duration / (1000 * 60 * 60) + ":"
						+ (duration % (1000 * 60 * 60)) / (1000 * 60) + ":"
						+ (duration % (1000 * 60)) / 1000);

				// Music Track must have `artist' with role field, or
				// DIDLParser().generate(didl) will throw nullpointException
				MusicTrack musicTrack = new MusicTrack(id,
						ContentTree.AUDIO_ID, title, creator, album,
						new PersonWithRole(creator, "Performer"), res);
				audioContainer.addItem(musicTrack);
				audioContainer
						.setChildCount(audioContainer.getChildCount() + 1);
				ContentTree.addNode(id, new ContentNode(id, musicTrack,
						filePath));

				Log.v(LOGTAG, "added audio item " + title + "from " + filePath);
			} while (cursor.moveToNext());
		}

		// Image Container
		Container imageContainer = new Container(ContentTree.IMAGE_ID,
				ContentTree.ROOT_ID, "Images", "GNaP MediaServer",
				new DIDLObject.Class("object.container"), 0);
		imageContainer.setRestricted(true);
		imageContainer.setWriteStatus(WriteStatus.NOT_WRITABLE);
		rootNode.getContainer().addContainer(imageContainer);
		rootNode.getContainer().setChildCount(
				rootNode.getContainer().getChildCount() + 1);
		ContentTree.addNode(ContentTree.IMAGE_ID, new ContentNode(
				ContentTree.IMAGE_ID, imageContainer));

		String[] imageColumns = { MediaStore.Images.Media._ID,
				MediaStore.Images.Media.TITLE, MediaStore.Images.Media.DATA,
				MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.SIZE };
		cursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				imageColumns, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				String id = ContentTree.IMAGE_PREFIX
						+ cursor.getInt(cursor
								.getColumnIndex(MediaStore.Images.Media._ID));
				String title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE));
				String creator = "unkown";
				String filePath = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
				String mimeType = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
				long size = cursor.getLong(cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));

				Res res = new Res(new MimeType(mimeType.substring(0,
						mimeType.indexOf('/')), mimeType.substring(mimeType
						.indexOf('/') + 1)), size, "http://"
						+ mediaServer.getAddress() + "/" + id);

				ImageItem imageItem = new ImageItem(id, ContentTree.IMAGE_ID,
						title, creator, res);
				imageContainer.addItem(imageItem);
				imageContainer
						.setChildCount(imageContainer.getChildCount() + 1);
				ContentTree.addNode(id,
						new ContentNode(id, imageItem, filePath));

				Log.v(LOGTAG, "added image item " + title + "from " + filePath);
			} while (cursor.moveToNext());
		}

		serverPrepared = true;
	}

	// FIXME: now only can get wifi address
	private InetAddress getLocalIpAddress() throws UnknownHostException {
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		return InetAddress.getByName(String.format("%d.%d.%d.%d",
				(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
				(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)));
	}
}