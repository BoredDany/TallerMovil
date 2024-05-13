package com.example.tallermovil

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration.*
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.IOException
import kotlin.math.*
import kotlin.random.Random


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

    private var longPressedMarkerOrigin: Marker? = null
    private var longPressedMarkerEnd: Marker? = null

    private var  markerBump: Marker? = null
    private var markerBumpList: MutableList<Marker> = mutableListOf()


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

        markerBump = createMarker(GeoPoint(4.718545, -74.032458), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)

        markerBump = createMarker(GeoPoint(4.715806, -74.032535), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)

        markerBump = createMarker(GeoPoint(4.628663, -74.065137), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)

        markerBump = createMarker(GeoPoint(4.628791, -74.065899), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)

        markerBump = createMarker(GeoPoint(4.627728, -74.067147), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)

        markerBump = createMarker(GeoPoint(4.625932, -74.067475), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)

        markerBump = createMarker(GeoPoint(4.631905, -74.065597), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)

        markerBump = createMarker(GeoPoint(4.627314, -74.065449), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)

        markerBump = createMarker(GeoPoint(4.625994, -74.065750), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)

        markerBump = createMarker(GeoPoint(4.624486, -74.066098), "", null, R.drawable.baseline_location_on_24)
        markerBump?.let { map!!.overlays.add(it) }
        markerBumpList.add(markerBump!!)







        mLightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {

                if (event!!.values[0] < 8) {
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

        //encontrar lugar y mostrar
        editText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val addressString = editText.text.toString()
                if (addressString.isNotEmpty()) {
                    try {
                        if (map != null && mGeocoder != null) {
                            val addresses = mGeocoder!!.getFromLocationName(addressString, 1)
                            if (addresses != null && addresses.isNotEmpty()) {
                                val addressResult = addresses[0]
                                val position = GeoPoint(addressResult.latitude, addressResult.longitude)

                                Log.i("Geocoder", "Dirección encontrada: ${addressResult.getAddressLine(0)}")
                                Log.i("Geocoder", "Latitud: ${addressResult.latitude}, Longitud: ${addressResult.longitude}")

                                //Agregar Marcador al mapa
                                val marker = createMarker(position, addressString, null, R.drawable.baseline_location_on_24)
                                marker?.let { map!!.overlays.add(it) }
                                map!!.controller.setCenter(marker!!.position)

                            } else {
                                Log.i("Geocoder", "Dirección no encontrada:" + addressString)
                                Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT)
                                    .show()
                            }
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

        val address = getLocationName(p.latitude, p.longitude)
        val myIcon = combineDrawableWithText(resources.getDrawable(R.drawable.baseline_location_on_24, this.theme), getAddressFromCoordinates(p.latitude, p.longitude))
        longPressedMarkerEnd?.let { map!!.overlays.remove(it) }
        longPressedMarkerEnd = createMarkerWithDrawable(p, address, null, myIcon)
        longPressedMarkerEnd?.let { map!!.overlays.add(it) }
        val mapController = map!!.controller
        mapController.setCenter(p);

        if (checkLocationPermission()) {
            mFusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    Log.i("LOCATION", "onSuccess location")

                    val startPoint = GeoPoint(location.latitude, location.longitude);
                    Log.i("LOCATION", "onSuccess location:" + location.latitude + " - " + location.longitude)
                    val address = getLocationName(location.latitude, location.longitude)
                    //val myIcon = combineDrawableWithText(resources.getDrawable(R.drawable.baseline_location_on_24, this.theme), getLocationName(location.latitude, location.longitude))
                    val myIcon = combineDrawableWithText(resources.getDrawable(R.drawable.baseline_location_on_24, this.theme), getAddressFromCoordinates(location.latitude, location.longitude))

                    longPressedMarkerOrigin?.let { map!!.overlays.remove(it) }
                    longPressedMarkerOrigin = createMarkerWithDrawable(startPoint, address, "INICIO", myIcon)
                    longPressedMarkerOrigin?.let { map!!.overlays.add(it) }

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

    private fun createMarkerWithDrawable(p: GeoPoint, title: String?, desc: String?, icon: Drawable?): Marker? {
        var marker: Marker? = null
        if (map != null) {
            marker = Marker(map)
            title?.let { marker.title = it }
            desc?.let { marker.subDescription = it }
            marker.icon = icon  // Set the icon directly using the provided Drawable
            marker.position = p
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        return marker
    }

    private fun drawRoute(start: GeoPoint, finish: GeoPoint) {
        var routePoints = ArrayList<GeoPoint>()
        var routePointsForRoute = ArrayList<GeoPoint>()
        var routeWithBump: Boolean
        routePoints.add(start)
        routePoints.add(finish)

        //routePoints.add(GeoPoint(4.751933, -74.048018))

        var roadAux = roadManager.getRoad(routePoints)
        do {
            routeWithBump = false
            for (j in 0 until markerBumpList.size) {
                for (i in 0 until roadAux.mNodes.size) {
                    //SI UNO DE LOS NODOS DE LA RUTA SE ENCUENTRA CERCA AL HUECO, LA RUTA SE CORTA DESDE ESE PUNTO Y SE REASIGNA A UNA POSICION LEJANA
                    //Y NO ESTA CERCA AL PUNTO FINAL
                    if (bumpProximityDetector(
                            markerBumpList.get(j).position,
                            roadAux.mNodes.get(i).mLocation
                        ) && !bumpProximityDetector(finish, roadAux.mNodes.get(i).mLocation) && !bumpProximityDetector(start, roadAux.mNodes.get(i).mLocation)
                    ) {
                        Log.i("DISTANCE BUMP:" + j.toString() + "Route:" + i.toString(), calculateDistance(markerBumpList.get(j).position, roadAux.mNodes.get(i).mLocation).toString())
                        routeWithBump = true
                        Log.i("OLDPOINT", roadAux.mNodes.get(i).mLocation.toString())
                        val newPoint = generateRandomPoint(roadAux.mNodes.get(i).mLocation, 1.0)
                        Log.i("NEWPOINT", newPoint.toString())

                        routePoints.clear()
                        routePoints.add(start)
                        //2 llenar routePoints con los nodos de road del roadManager
                        for (k in 0 until i) {
                            routePoints.add(roadAux.mNodes.get(k).mLocation)
                        }
                        routePoints.add(newPoint)
                        routePoints.add(finish)
                        break
                        //routePoints = drawRouteAux(finish, routePoints)
                        //Log.i("SE ROMPIO EL CICLO J???", " " + j.toString())
                        //Log.i("SE ROMPIO EL CICLO i???", " " + i.toString())
                    }
                }
                if (routeWithBump){
                    roadAux = roadManager.getRoad(routePoints)
                    break
                }
            }

        }while (routeWithBump)

        //val road = roadManager.getRoad(routePointsForRoute)
        for (i in 0 until roadAux.mNodes.size) {
            Log.i("ROUTEDISTANCIAPOST", calculateDistance(markerBump!!.position, roadAux.mNodes.get(i).mLocation).toString())
        }


        Log.i("OSM_acticity", "Route length: ${roadAux.mLength} klm")
        Log.i("OSM_acticity", "Duration: ${roadAux.mDuration / 60} min")
        if (map != null) {
            roadOverlay?.let { map!!.overlays.remove(it) }
            roadOverlay = RoadManager.buildRoadOverlay(roadAux)
            roadOverlay?.outlinePaint?.color = Color.RED
            roadOverlay?.outlinePaint?.strokeWidth = 10f
            map!!.overlays.add(roadOverlay)

            Toast.makeText(this, "Distancia de la ruta: ${roadAux.mLength} km", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawRouteAux(finish: GeoPoint, routePointsList: ArrayList<GeoPoint>) : ArrayList<GeoPoint>{
        routePointsList.add(finish)
        val road = roadManager.getRoad(routePointsList)

        for (j in 0 until markerBumpList.size) {
            for (i in 0 until road.mNodes.size) {
                if (bumpProximityDetector(markerBumpList.get(j).position, road.mNodes.get(i).mLocation) && !bumpProximityDetector(finish, road.mNodes.get(i).mLocation)) {
                    val newPoint = generateRandomPoint(road.mNodes.get(i).mLocation, 0.6)
                    routePointsList.clear()
                    for (k in 0 until i) {
                        routePointsList.add(road.mNodes.get(k).mLocation)
                    }
                    routePointsList.add(newPoint)
                    return drawRouteAux(finish, routePointsList)
                }
            }
            Log.i("SALI DE i", " SALIII ")
        }
        routePointsList.clear()
        for (i in 0 until road.mNodes.size) {
            routePointsList.add(road.mNodes.get(i).mLocation)
        }

        return routePointsList
    }

    private fun getLocationName (latitude: Double, longitude: Double): String{
        var addressFound = "null"
        try {
            val maxResults = 1
            val addresses = mGeocoder?.getFromLocation(latitude, longitude, maxResults)

            if (addresses != null  && addresses.isNotEmpty()) {
                val address = addresses[0]
                Log.i("Geocoder", "Dirección encontrada: ${address.getAddressLine(0)}")
                Log.i("Geocoder", "Latitud: ${address.latitude}, Longitud: ${address.longitude}")
                addressFound = address.getAddressLine(0).toString()
            } else {
                Log.i("Geocoder", "No se encontró ninguna dirección: " + latitude + " - " + longitude)
            }
        } catch (e: IOException) {
            Log.e("Geocoder", "Error en el geocodificador: ${e.message}")
        }
        return addressFound
    }

    fun combineDrawableWithText(drawable: Drawable, text: String): Drawable {
        val paddingVertical = 40
        val paddingHorizontal = 200
        val textSize = 16f

        // Calcula ancho y alto total junto con el borde definido
        val totalWidth = drawable.intrinsicWidth + paddingHorizontal * 2
        val totalHeight = drawable.intrinsicHeight + paddingVertical * 2 + textSize

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dibujar el Drawable centrado horizontalmente
        val drawableLeft = (totalWidth - drawable.intrinsicWidth) / 2
        val drawableTop = paddingVertical
        drawable.setBounds(drawableLeft, drawableTop, drawableLeft + drawable.intrinsicWidth, drawableTop + drawable.intrinsicHeight)
        drawable.draw(canvas)

        // Dibujar el texto centrado
        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = textSize
        paint.isAntiAlias = true

        val indiceMedio = text.length / 2
        val primerLinea = text.substring(0, indiceMedio)

        val textWidth = paint.measureText(primerLinea)
        val textX = (totalWidth - textWidth) / 2
        val textY = (totalHeight - paddingVertical / 2) - 25
        canvas.drawText(primerLinea, textX, textY, paint)

        return BitmapDrawable(resources, bitmap)
    }

    //ALTERNATIVA A GEOCODER
    fun getAddressFromCoordinates1(latitude: Double, longitude: Double): String {
        val client = OkHttpClient()
        val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=17&addressdetails=1"

        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        var displayName = ""

        // Procesado del json recibido especificando el atributo requerido
        val gson = Gson()
        val jsonObject = gson.fromJson(responseBody, com.google.gson.JsonObject::class.java)
        displayName = jsonObject.getAsJsonPrimitive("display_name").asString

        return displayName
    }

    fun getAddressFromCoordinates(latitude: Double, longitude: Double): String {
        var displayName = ""
        return displayName
    }

    fun bumpProximityDetector(point1: GeoPoint, point2: GeoPoint): Boolean {
        if(calculateDistance(point1, point2) <= 0.4){
            return true
        }
        return false
    }

    fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
        val earthRadius = 6371.0 // radius in kilometers

        val latDiff = Math.toRadians(point2.latitude - point1.latitude)
        val lonDiff = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(latDiff / 2).pow(2.0) +
                cos(Math.toRadians(point1.latitude)) * cos(Math.toRadians(point2.latitude)) *
                sin(lonDiff / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun generateRandomPoint(initialPoint: GeoPoint, distance: Double): GeoPoint {
        val radius = 6371.0 // Earth's radius in kilometers
        val bearing = Random.nextDouble(2 * PI) // Random direction
        val angularDistance = distance / radius // The angular distance

        val lat1 = Math.toRadians(initialPoint.latitude)
        val lon1 = Math.toRadians(initialPoint.longitude)

        val lat2 = asin(sin(lat1) * cos(angularDistance) +
                cos(lat1) * sin(angularDistance) * cos(bearing))

        var lon2 = lon1 + atan2(sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2))

        lon2 = (lon2 + 3 * PI) % (2 * PI) - PI // Normalize to -180..+180

        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

}