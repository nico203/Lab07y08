package ns.dam.isi.frsf.utn.edu.ar.lab07u08;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class ReclamoActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnInfoWindowClickListener
{
    static final int CODIGO_RESULTADO_ALTA_RECLAMO = 1;

    private ArrayList<Reclamo> reclamos;
    private ArrayList<Polyline> polyLineas;
    private GoogleMap myMap;
    private SupportMapFragment map;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reclamo);

        map = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        map.getMapAsync(this);
        reclamos = new ArrayList<>();
        polyLineas = new ArrayList<>();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_reclamo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        myMap = googleMap;
        myMap.getUiSettings().setZoomControlsEnabled(true);
        myMap.setOnMapLongClickListener(this);
        myMap.setOnInfoWindowClickListener(this);
        actualizarMapa();
    }


    private void actualizarMapa(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    9999);

            return;
        }
        myMap.setMyLocationEnabled(true);

        //Actualizamos la ubicación llevándola a la posición actual
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                Log.d("LOcalizacion", "" + location.getLatitude() + location.getLongitude());
                LatLng ubicacion = new LatLng(location.getLatitude(), location.getLongitude());
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(ubicacion)
                        .zoom(12)
                        .build();
                myMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 3000, null);
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) { }
            public void onProviderDisabled(String provider) { }
        };
        //Obtenemos una vez la ubicación
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 9999: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    actualizarMapa();
                }
                break;
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Intent i = new Intent(ReclamoActivity.this, AltaReclamoActivity.class);
        i.putExtra("coordenadas",latLng);
        startActivityForResult(i, CODIGO_RESULTADO_ALTA_RECLAMO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != CODIGO_RESULTADO_ALTA_RECLAMO || resultCode != RESULT_OK) {
            return;
        }

        Reclamo nuevoReclamo = (Reclamo) data.getExtras().get("reclamo");
        reclamos.add(nuevoReclamo);

        MarkerOptions marcador = new MarkerOptions();
        marcador.position(new LatLng(nuevoReclamo.getLatitud(), nuevoReclamo.getLongitud()));
        marcador.title(nuevoReclamo.getEmail());
        marcador.snippet(nuevoReclamo.getTitulo());
        myMap.addMarker(marcador);
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        final View infoWindowView = getLayoutInflater().inflate(R.layout.content_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle("Marcar Reclamos");
        builder.setView(infoWindowView);
        builder.setPositiveButton("Listo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                LatLng actual = marker.getPosition();
                EditText kmET = (EditText) infoWindowView.findViewById(R.id.kmEditText);

                //Borramos todas las lineas que haya
                for(Polyline linea : polyLineas) {
                    linea.remove();
                }
                polyLineas.clear();

                Double kilometros = Double.parseDouble(kmET.getText().toString()) * 1000;
                Log.v("Count reclamos", "" + reclamos.size());
                ArrayList<Reclamo> marcarReclamos = new ArrayList<>();
                for (Reclamo reclamo : reclamos) {
                    if(actual.latitude == reclamo.getLatitud() && actual.longitude == reclamo.getLongitud()) {
                        continue;
                    }
                    float[] results = new float[3];
                    Location.distanceBetween(actual.latitude, actual.longitude,
                            reclamo.getLatitud(), reclamo.getLongitud(),
                            results);
                    Log.d("distancia", results[0] + "");
                    if(kilometros >= (double) results[0]){
                        marcarReclamos.add(reclamo);
                    }
                }

                for(Reclamo reclamo: marcarReclamos) {
                    LatLng destino = new LatLng(reclamo.getLatitud(), reclamo.getLongitud());
                    PolylineOptions linea = new PolylineOptions();
                    linea.add(actual).add(destino);

                    Polyline l = myMap.addPolyline(linea);
                    polyLineas.add(l);
                }
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
