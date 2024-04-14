package com.example.tallermovil

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.MapView
import org.osmdroid.config.Configuration.*
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.IOException

class MapActivity : AppCompatActivity() {
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var map : MapView? = null

    //SENSORES
    private lateinit var mSensorManager: SensorManager
    private lateinit var mLightSensor: Sensor
    private lateinit var mLightSensorListener: SensorEventListener


    //Rutas
    lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null

    private var longPressedMarker: Marker? = null

    //Geocoder
    var mGeocoder: Geocoder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        //Rutas
        roadManager = OSRMRoadManager(this, "ANDROID")
        //Attribute

        val mGeocoder = Geocoder(this)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        permisoUbicacion()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        map = findViewById<MapView>(R.id.map)
        //map.setTileSource(TileSourceFactory.OpenTopo)
        map!!.setTileSource(TileSourceFactory.MAPNIK)
        map!!.setMultiTouchControls(true)

        var darkModeLum: Boolean = false
        var lightModeLum: Boolean = true


        mLightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {

                if (event!!.values[0] < 12) {
                    darkModeLum = true
                    map!!.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)

                } else {
                    lightModeLum = true
                    map!!.overlayManager.tilesOverlay.setColorFilter(null)
                }
                if(lightModeLum && darkModeLum){
                    lightModeLum = false
                    darkModeLum = false
                    map!!.setTileSource(TileSourceFactory.MAPNIK)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }

        val editText = findViewById<EditText>(R.id.editTextGeocoder)
        val addressString = editText.toString()

        editText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                if (addressString.isNotEmpty()) {
                    try {
                        if (map != null && mGeocoder != null) {
                            val addresses = mGeocoder!!.getFromLocationName(addressString, 2)
                            if (addresses != null && addresses.isNotEmpty()) {
                                val addressResult = addresses[0]
                                val position = GeoPoint(addressResult.latitude, addressResult.longitude)
                                Log.i("GEOCODERRR", "Longitud: " + addressResult.longitude)
                                //Agregar Marcador al mapa
                                val marker = Marker(map)
                                marker.title = "Mi Marcador"
                                val myIcon = resources.getDrawable(
                                    R.drawable.baseline_location_on_24,
                                    theme)
                                marker.icon = myIcon
                                marker.position = position
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                map!!.overlays.add(marker)

                                map!!.controller.setCenter(marker.position)
                            }
                            } else {
                                Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "La dirección esta vacía", Toast.LENGTH_SHORT).show()
                }

            }
            true
        }

        map!!.overlays.add(createOverlayEvents())

    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map!!.onResume() //needed for compass, my location overlays, v6.0.0 and up

        mSensorManager.registerListener(mLightSensorListener, mLightSensor,
            SensorManager.SENSOR_DELAY_NORMAL)

        if (checkLocationPermission()) {
            mFusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                Log.i("LOCATION", "onSuccess location")
                if (location != null) {
                    Log.i("LOCATION", "Longitud: " + location.longitude)
                    Log.i("LOCATION", "Latitud: " + location.latitude)
                    val mapController = map!!.controller
                    mapController.setZoom(15)
                    val startPoint = GeoPoint(location.latitude, location.longitude);
                    mapController.setCenter(startPoint);
                } else {
                    Log.i("LOCATION", "FAIL location")
                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map!!.onPause()  //needed for compass, my location overlays, v6.0.0 and up

        mSensorManager.unregisterListener(mLightSensorListener)


}


    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun permisoUbicacion(){
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                //performAction(...)
                Toast.makeText(this, "Gracias", Toast.LENGTH_SHORT).show()

                mFusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                    Log.i("LOCATION", "onSuccess location")
                    if (location != null) {
                        Log.i("LOCATION", "Longitud: " + location.longitude)
                        Log.i("LOCATION", "Latitud: " + location.latitude)
/*
                        val mapController = map.controller
                        mapController.setZoom(10.5)
                        val startPoint = GeoPoint(location.latitude, location.longitude);
                        mapController.setCenter(startPoint);
*/

                    } else{
                        Log.i("LOCATION", "FAIL location")
                    }
                }

            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined.
                //showInContextUI(...)
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
            else -> {
                // You can directly ask for the permission.
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    Permission.MY_PERMISSION_REQUEST_LOCATION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //val textV = findViewById<TextView>(R.id.textView)
        when (requestCode) {
            Permission.MY_PERMISSION_REQUEST_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Toast.makeText(this, "Gracias", Toast.LENGTH_SHORT).show()
                } else {
                    // Explain to the user that the feature is unavailable
                }
                return
            }
            else -> {
                // Ignore all other requests.

            }
        }
    }

    private fun createOverlayEvents(): MapEventsOverlay {
        val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return false
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                longPressOnMap(p)
                return true
            }
        })
        return overlayEventos
    }

    private fun longPressOnMap(p: GeoPoint) {
        longPressedMarker?.let { map!!.overlays.remove(it) }
        longPressedMarker = createMarker(p, "location", null, R.drawable.baseline_location_on_24)
        longPressedMarker?.let { map!!.overlays.add(it) }


        if (checkLocationPermission()) {
            mFusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                Log.i("LOCATION", "onSuccess location")
                if (location != null) {
                    val mapController = map!!.controller
                    val startPoint = GeoPoint(location.latitude, location.longitude);
                    mapController.setCenter(startPoint);

                    longPressedMarker = createMarker(startPoint, "location", null, R.drawable.baseline_location_on_24)
                    longPressedMarker?.let { map!!.overlays.add(it) }

                    drawRoute(startPoint,p)
                } else {
                    Log.i("LOCATION", "FAIL location")
                }
            }
        }
    }

    private fun createMarker(p: GeoPoint, title: String?, desc: String?, iconID: Int): Marker? {
        var marker: Marker? = null
        if (map != null) {
            marker = Marker(map)
            title?.let { marker.title = it }
            desc?.let { marker.subDescription = it }
            if (iconID != 0) {
                val myIcon = resources.getDrawable(iconID, this.theme)
                marker.icon = myIcon
            }
            marker.position = p
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        return marker
    }

    private fun drawRoute(start: GeoPoint, finish: GeoPoint) {
        val routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        val road = roadManager.getRoad(routePoints)
        Log.i("OSM_acticity", "Route length: ${road.mLength} klm")
        Log.i("OSM_acticity", "Duration: ${road.mDuration / 60} min")
        if (map != null) {
            roadOverlay?.let { map!!.overlays.remove(it) }
            roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay?.outlinePaint?.color = Color.RED
            roadOverlay?.outlinePaint?.strokeWidth = 10f
            map!!.overlays.add(roadOverlay)
        }
    }
}