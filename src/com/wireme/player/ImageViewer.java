
package com.wireme.player;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.wireme.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageViewer extends Activity {
	private final static String LOGTAG = "ImageViewer";
	
	private ImageLoader imageLoader;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_viewer);
		
        Intent intent = getIntent();
        String imageUri = intent.getStringExtra("playURI");

        imageLoader = ImageLoader.getInstance();
        ImageView imageView = (ImageView) findViewById(R.id.imageview);
        imageLoader.displayImage(imageUri, imageView);
	}
}
