package com.windigo.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import com.windigo.RestApiFactory;
import com.windigo.android.windigotest.R;
import com.windigo.http.client.ApacheHttpClient;
import com.windigo.http.client.HttpUrlConnectionClient;
import com.windigo.http.client.OkClient;
import com.windigo.sample.weather.ForecastResponse;
import com.windigo.sample.weather.MainResponse;
import com.windigo.sample.weather.WeatherResponse;
import com.windigo.sample.weather.WindResponse;

public class MainActivity extends Activity {
	
	private TextView responseTextView;
	private LastfmRestApi lastfmRestApi;
	private OpenWeatherApi openWeatherApi;
	
	private static final String TAG = MainActivity.class.getCanonicalName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		responseTextView = (TextView) findViewById(R.id.responseTextView);
		
		// apache http client bundled with android
		ApacheHttpClient httpClient = new ApacheHttpClient();
		
		// android http url connection client
		HttpUrlConnectionClient httpUrlConnectionClient = new HttpUrlConnectionClient();
		
		// square okhttp client
		OkClient okHttpClient = new OkClient();		
		
		// call factory method with url and interface class for rest api		
		openWeatherApi = RestApiFactory.createNewService("http://api.openweathermap.org/data/2.5", 
				OpenWeatherApi.class, httpClient);
		
		ForecastResponse forecast = openWeatherApi.getForecast(41.163267, 29.094187);
		responseTextView.setText(forecast.toString());
		
		// call factory method with url and interface class for rest api
				//lastfmRestApi = RestApiFactory.createNewService("http://ws.audioscrobbler.com", LastfmRestApi.class, httpClient);
		
		//Album album = lastfmRestApi.getAlbumInfo("album.getinfo", "49f6b21cab1c48100ee59f216645275e", 
		//		"Cher", "Believe", "json").getAlbum();
		//responseTextView.setText(album.toString());
		
		// old way
		//new RegularHttpRestTask().execute();
	}
	
	
	private class RegularHttpRestTask extends AsyncTask<Void, Integer, ForecastResponse> {

		@Override
		protected ForecastResponse doInBackground(Void... params) {
			
			final HttpParams httpParams = new BasicHttpParams();
			
	        final SchemeRegistry supportedSchemes = new SchemeRegistry();

	        final SocketFactory sf = PlainSocketFactory.getSocketFactory();
	        supportedSchemes.register(new Scheme("http", sf, 80));
	        supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
			
			HttpConnectionParams.setStaleCheckingEnabled(httpParams, false);
			HttpConnectionParams.setConnectionTimeout(httpParams, 60 * 1000);
			HttpConnectionParams.setSoTimeout(httpParams, 60 * 1000);
			HttpConnectionParams.setSocketBufferSize(httpParams, 8192);
			
			final ClientConnectionManager ccm = new ThreadSafeClientConnManager(httpParams,
	                supportedSchemes);
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet get = new HttpGet("http://api.openweathermap.org/data/2.5/weather?lat=41.163267&lon=29.094187");
			HttpResponse response = null;
			ForecastResponse forecast = new ForecastResponse();
			JSONObject responseJsonObject;
			
			try {
				response =  httpClient.execute(get);
				String responseString = EntityUtils.toString(response.getEntity());
				responseJsonObject = new JSONObject(responseString);
				
				forecast.setName(responseJsonObject.getString("name"));
				
				// get main response
				JSONObject mainJsonObject = responseJsonObject.getJSONObject("main");
				forecast.setMain(new MainResponse(mainJsonObject.getDouble("temp"), 
											mainJsonObject.getDouble("temp_min"), 
											mainJsonObject.getDouble("temp_max"), 
											mainJsonObject.getInt("humidity")));
				
				// get wind respose
				JSONObject windJsonObject = responseJsonObject.getJSONObject("wind");
				forecast.setWind(new WindResponse((float) windJsonObject.getDouble("speed")));
				
				JSONArray weathJsonArray = responseJsonObject.getJSONArray("weather");
				List<WeatherResponse> weatherResponses = new ArrayList<WeatherResponse>();
				for (int i = 0; i < weathJsonArray.length(); i++) {
					JSONObject weatherJsonObject = weathJsonArray.getJSONObject(i);
					weatherResponses.add(new WeatherResponse(weatherJsonObject.getString("description"), weatherJsonObject.getString("icon")));
				}
				forecast.setWeather(weatherResponses);
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			
			return forecast;
		}
		
		@Override
		protected void onPostExecute(ForecastResponse result) {
			super.onPostExecute(result);
			// use however you want
			//responseTextView.setText(result.getWind());
			responseTextView.setText(result.toString());
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
