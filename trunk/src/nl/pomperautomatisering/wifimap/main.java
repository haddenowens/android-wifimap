package nl.pomperautomatisering.wifimap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.util.EncodingUtils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;
import com.google.android.maps.*;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class main extends MapActivity {

    private MapView kaart;
	private MapController kaartcontroller;
	private LocationManager gpsmanager;
	private gpslistener gpslistener;
	private WifiManager wifi;
	private GeoPoint userlocation;
	private List<DataPoint> path =new ArrayList<DataPoint>();
	private Map<String,Float> currentAPs=new HashMap<String,Float>();
	private List<String> allAPs=new ArrayList<String>();
	
	 private void CenterLocation(GeoPoint centerGeoPoint)
	 {
	  kaartcontroller.animateTo(centerGeoPoint);
	 };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        kaart = (MapView)findViewById(R.id.mapview);
        kaart.setSatellite(true);
        kaartcontroller=kaart.getController();
        kaartcontroller.setZoom(20);
        
        userlocation=new GeoPoint(0, 0);
        locationoverlay overlay = new locationoverlay();
        List<Overlay> overlaylist=kaart.getOverlays();
        overlaylist.add(overlay);
        
        gpsmanager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        gpslistener = new gpslistener();
        gpsmanager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,gpslistener);
        
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi.startScan();
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifilist, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
		case R.id.menu_render:
				WebView browser = (WebView)findViewById(R.id.webview);
				browser.getSettings().setJavaScriptEnabled(true);
	            Iterator i=path.iterator();
	            String postData="";
	            int index=0;
	            while(i.hasNext()){
	            	DataPoint d=(DataPoint)i.next();
	            	postData=postData.concat(d.post(index));
	            	Log.w("debugshit", postData);
	            	index++;
	            }
				byte[] post = EncodingUtils.getBytes(postData, "BASE64");
				browser.postUrl("http://martwebdesign.nl/debug.php", post);
			break;

		default:
			break;
		}

		return true;
    }

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	private class gpslistener implements LocationListener{

		  public void onLocationChanged(Location argLocation) {
		   // TODO Auto-generated method stub
		   GeoPoint myGeoPoint = new GeoPoint(
		    (int)(argLocation.getLatitude()*1000000),
		    (int)(argLocation.getLongitude()*1000000));
		   userlocation=myGeoPoint;
		   DataPoint dpoint=new DataPoint(userlocation);
		   dpoint.accesspoints=currentAPs;
		   path.add(dpoint);
		   CenterLocation(myGeoPoint);
		  }

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
	}
	private class DataPoint{
		public GeoPoint location=null;
		public Map<String,Float> accesspoints=new HashMap<String,Float>();
		
		public DataPoint(GeoPoint loc){
			this.location=loc;
		}
		public void addAP(String name,Float power){
			this.accesspoints.put(name, power);
		}
		public String post(int ID){
			String ret="";
			Iterator i=this.accesspoints.entrySet().iterator();
			while(i.hasNext()){
				Map.Entry<String, Float> row=(Map.Entry<String, Float>)i.next();
				String drow="&datapoint["+ID+"]["+row.getKey()+"]="+row.getValue();
				ret=ret.concat(drow);
			}
			return ret;
		}
	}
    protected class locationoverlay extends com.google.android.maps.Overlay {

        @Override
        public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
            

            super.draw(canvas, mapView, shadow);
            Point myScreenCoords = new Point();

            kaart.getProjection().toPixels(userlocation, myScreenCoords);
            Paint paint = new Paint();
            paint.setStrokeWidth(1);
            paint.setARGB(255, 255, 255, 255);
            paint.setStyle(Paint.Style.STROKE);
            
            Paint line = new Paint();
            line.setStrokeWidth(4);
            line.setARGB(255, 0, 128, 255);
            line.setStyle(Paint.Style.STROKE);

            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.userlocation);
            
            canvas.drawBitmap(bmp, myScreenCoords.x-(bmp.getWidth()/2), myScreenCoords.y-(bmp.getHeight()/2), paint);
            GeoPoint lastpoint = null;
            Iterator i=path.iterator();
            while(i.hasNext()){
            	DataPoint d=(DataPoint)i.next();
            	GeoPoint p=d.location;
                Point pcoords = new Point();
                kaart.getProjection().toPixels(p, pcoords);
            	if(lastpoint!=null){
            		Point lpoint=new Point();
            		kaart.getProjection().toPixels(lastpoint, lpoint);
            		canvas.drawLine(lpoint.x, lpoint.y, pcoords.x, pcoords.y, line);
            	}
            	lastpoint=p;
            }
            return true;
        }
    }
	private BroadcastReceiver wifilist = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				LinearLayout aplist = (LinearLayout)findViewById(R.id.aplist);
				LinearLayout allaplist=(LinearLayout)findViewById(R.id.allaplist);
				aplist.removeAllViews();
				TextView aptitle=new TextView(getBaseContext());
				aptitle.setText("Accesspoints");
				aptitle.setTextAppearance(getBaseContext(), android.R.style.TextAppearance_Large_Inverse);

				aplist.addView(aptitle);
				currentAPs.clear();
				for(final ScanResult result:wifi.getScanResults()) {
					LinearLayout aprow=new LinearLayout(getBaseContext());
					aprow.addView(new CheckBox(getBaseContext()));
					TextView aptext=new TextView(getBaseContext());
					aptext.setText(result.SSID);
					aptext.setTextAppearance(getBaseContext(), android.R.style.TextAppearance_Medium_Inverse);
					aprow.addView(aptext);
					aplist.addView(aprow);
					currentAPs.put(result.SSID, new Float(result.level));
					if(!allAPs.contains(result.SSID)){
						allAPs.add(result.SSID);
						TextView aat=new TextView(getBaseContext());
						aat.setText(result.SSID);
						aat.setTextAppearance(getBaseContext(), android.R.style.TextAppearance_Medium_Inverse);
						allaplist.addView(aat);
					}
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				wifi.startScan();
			}
		}
	};
    
}
