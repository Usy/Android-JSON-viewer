package com.example.viewer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

public class TitlesViewerActivity extends Activity {

	public static final String SOURCE_ADDRESS = "http://androidtest3.apiary.io/articles";
	public static final String TAG = "TitlesViewerActivity";

	static ArrayList<Article> articleList;          // used by ArticleViewerActivity
	private ArrayAdapter<Article> titleListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		articleList = new ArrayList<Article>();

		titleListAdapter = new ArrayAdapter<Article>(this, android.R.layout.simple_list_item_1, articleList);
		ListView titlesListView = (ListView) findViewById(R.id.titlesListView);

		titlesListView.setAdapter(titleListAdapter);
		titlesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				Intent intent = new Intent(TitlesViewerActivity.this, ArticleViewerActivity.class);
				intent.putExtra(ArticleViewerActivity.EXTRA_ID, i);
				startActivity(intent);
			}
		});
		reload(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.layout.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuReload:
				reload(true);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * <br />
	 * Asynchronously reloads list of available articles. If JSON file already exists, then it is used, otherwise JSON file is downloaded and saved to local storage.
	 * @param isForced if true, then JSON file will be downloaded even if exists on local storage.
	 */
	private void reload(final boolean isForced) {
		final ProgressDialog dialog = ProgressDialog.show(this, "", getString(R.string.reloading), true);

		new AsyncTask<Void, Void, Collection<Article>>() {
			@Override
			protected Collection<Article> doInBackground(Void... voids) {
				String json;
				File dir = getExternalFilesDir(null);
				String fileName;
				if (dir != null) {
					fileName = dir.getPath() + File.separator + "json.txt";
					if (!new File(fileName).exists() || isForced) {
						json = downloadJSON();
						writeJSONToFile(json, fileName);
					} else {
						json = readJSONFromFile(fileName);
					}
				} else {
					json = downloadJSON();
				}
				return parseJSONToArticles(json);
			}

			@Override
			protected void onPostExecute(Collection<Article> articles) {
				dialog.dismiss();
				if (articles != null) {
					TitlesViewerActivity.articleList.clear();
					TitlesViewerActivity.articleList.addAll(articles);
					titleListAdapter.notifyDataSetChanged();
				} else {
					Toast.makeText(TitlesViewerActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
				}
			}
		}.execute();
	}

	/**
	 * <br />
	 * Downloads JSON string using HTTP GET method.
	 * @return JSON string or <b>null</b> if connection error occurred.
	 */
	private String downloadJSON() {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(SOURCE_ADDRESS);

		httpGet.setHeader("Content-type", "application/json");

		InputStream inputStream = null;
		String result = null;

		try {
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();

			inputStream = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			result = sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * <br />
	 * Creates {@link com.example.viewer.Article} collection from JSON string.
	 * @param json JSON string to be parsed.
	 * @return collection of Articles or <b>null</b> if JSON string was null.
	 * @see com.example.viewer.Article
	 */
	private Collection<Article> parseJSONToArticles(String json) {
		if (json == null) {
			return null;
		}
		Collection<Article> articles = null;
		try {
			JSONArray jArray = new JSONArray(json);
			articles = new ArrayList<Article>();
			for (int i = 0; i < jArray.length(); i++) {
				JSONObject jObject = jArray.getJSONObject(i);
				int id = jObject.getInt("id");
				String title = jObject.getString("title");
				String url = jObject.getString("photo");
				Article article = new Article(id, title, url);
				articles.add(article);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return articles;
	}

	/**
	 * <br />
	 * Writes given JSON string to the file with given path. Does nothing when string to be saved or file path is null.
	 * @param json JSON string to be saved.
	 * @param filePath path of the file. Directory structure must exist.
	 */
	private void writeJSONToFile(String json, String filePath) {
		if (json == null || filePath == null)
			return;
		try {
			FileWriter writer = new FileWriter(filePath);
			writer.write(json);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads JSON from the file with given path.
	 * @param filePath path of the file to be opened.
	 * @return JSON string or <b>null</b> if file couldn't be read.
	 */
	private String readJSONFromFile(String filePath) {
		String json = null;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = reader.readLine()) != null)
				sb.append(line);
			json = sb.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return json;
	}
}
