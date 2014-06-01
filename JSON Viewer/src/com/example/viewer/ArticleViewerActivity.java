package com.example.viewer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ArticleViewerActivity extends Activity {

	public static final String EXTRA_ID = "com.example.viewer.extra.ID";
	public static final String TAG = "ArticleViewerActivity";
	public static final int IMAGE_MAX_SIZE = 1024;

	private Button prevButton;
	private Button nextButton;
	/**
	 * Position of currently displayed article from the article list.
	 */
	private int position;
	private TextView titleTextView;
	private ImageView photoImageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article);
		Intent intent = getIntent();

		position = intent.getIntExtra(EXTRA_ID, 0);

		photoImageView = (ImageView) findViewById(R.id.photoImageView);

		titleTextView = (TextView) findViewById(R.id.titleTextView);

		prevButton = (Button) findViewById(R.id.prevButton);
		nextButton = (Button) findViewById(R.id.nextButton);

		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int id = view.getId();
				if (id == R.id.prevButton) {
					if (position > 0)
						position--;
				} else if (id == R.id.nextButton) {
					if (position < TitlesViewerActivity.articleList.size() - 1)
						position++;
				}

				invalidate();
			}
		};

		prevButton.setOnClickListener(listener);
		nextButton.setOnClickListener(listener);
		if (savedInstanceState != null) {
			position = savedInstanceState.getInt("position");
			Log.d(TAG, "restored position = " + position);
		}
		invalidate();
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("position", position);
		Log.d(TAG, "saved position = " + position);
	}

	/**
	 * <br />
	 * Asynchronously reads article photo and prepares layout. Downloads photo from url given in current article and
	 * saves it on local storage or reads it from local storage if photo file already exists.
	 */
	private void invalidate() {
		prevButton.setEnabled(position > 0);
		nextButton.setEnabled(position < TitlesViewerActivity.articleList.size() - 1);

		titleTextView.setText(TitlesViewerActivity.articleList.get(position).getTitle());
		String url = TitlesViewerActivity.articleList.get(position).getUrl();

		final ProgressDialog dialog = ProgressDialog.show(this, "", getString(R.string.opening), true);

		new AsyncTask<String, Void, Bitmap>() {
			@Override
			protected Bitmap doInBackground(String... strings) {
				String url = strings[0];
				File dir = getExternalFilesDir(null);
				String fileName;
				Bitmap bitmap;
				if (dir != null) {
					fileName = dir.getPath() + File.separator + url.replace("http://", "").replace("https://", "");
					if (!new File(fileName).exists()) {
						bitmap = downloadImageFromUrl(url);
						saveImage(bitmap, fileName);
					} else {
						bitmap = openImage(fileName);
					}
				} else {
					bitmap = downloadImageFromUrl(url);
				}
				return bitmap;
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				dialog.dismiss();
				if (bitmap == null) {
					Toast.makeText(ArticleViewerActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
				}
				photoImageView.setImageBitmap(bitmap);
			}
		}.execute(url);
	}

	/**
	 * <br />
	 * Downloads resized image from given URL.
	 * @param address URL of the file to be downloaded.
	 * @return downloaded bitmap or null if image couldn't be downloaded.
	 */
	private Bitmap downloadImageFromUrl(String address) {
		Bitmap bitmap = null;
		Log.d(TAG, "downloading from: " + address);
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = getInSampleSize(address);

			URL url = new URL(address);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream inputStream = connection.getInputStream();
			bitmap = BitmapFactory.decodeStream(inputStream, null, options);
			Log.d(TAG, "file saved");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * <br />
	 * Computes value of {@link BitmapFactory.Options#inSampleSize} option for the image file from given address.
	 * @param address address of the image file.
	 * @return value of {@link BitmapFactory.Options#inSampleSize} option.
	 * @throws IOException
	 */
	private int getInSampleSize(String address) throws IOException {
		URL url = new URL(address);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.connect();
		InputStream inputStream = connection.getInputStream();

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeStream(inputStream, null, options);
		int imageHeight = options.outHeight;
		int imageWidth = options.outWidth;
		int inSampleSize = 1;
		final int halfHeight = imageHeight / 2;
		final int halfWidth = imageWidth / 2;

		while ((halfHeight / inSampleSize) > IMAGE_MAX_SIZE
				&& (halfWidth / inSampleSize) > IMAGE_MAX_SIZE) {
			inSampleSize *= 2;
		}
		return inSampleSize;
	}

	/**
	 * <br />
	 * Saves given bitmap to the file with given path. Does nothing if bitmap or path is null.
	 * @param bitmap bitmap to be saved.
	 * @param path path to the file to be saved. If directory structure doesn't exist, it will be created.
	 */
	private void saveImage(Bitmap bitmap, String path) {
		if (bitmap == null || path == null)
			return;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
		File file = new File(path);
		try {
			file.getParentFile().mkdirs();
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "saving file to: " + file.getAbsolutePath());
		try {
			if (fos != null) {
				fos.write(outputStream.toByteArray());
				fos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * <br />
	 * Reads image from file.
	 * @param path path to the file to be opened.
	 * @return bitmap read from the file or null if file couldn't be read.
	 */
	private Bitmap openImage(String path) {
		try {
			return BitmapFactory.decodeStream(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
