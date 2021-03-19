package com.malikali.compassapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.hardware.*
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.*
import android.widget.ImageView.ScaleType
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.SphericalUtil
import com.malikali.compassapp.Service.ConnectivityReceiver
import java.io.IOException
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.text.Typography.degree


class MainActivity : AppCompatActivity(), SensorEventListener,ConnectivityReceiver.ConnectivityReceiverListener  {
    private lateinit var ivCompass:ImageView
    private lateinit var ivArrow:ImageView
    private lateinit var ivRefresh:ImageView
    private lateinit var tvDest:TextView
    private lateinit var btnSetDest:Button
    private lateinit var tvCurrentLoc:TextView
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var background = false
    private val accReading = FloatArray(3)
    private val magReading = FloatArray(3)
    private var mAzimuth:Float = 0f
    private val mGravity = FloatArray(3)
    private var currentAzimuth = 0f
    private var currentDegree = 0f
    private var currentDegreeNeedle = 0f


    private lateinit var sensorManager: SensorManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLocation = Location("")
    private var destLat = 0.0
     private var destLng = 0.0






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)




         ivCompass = findViewById<ImageView>(R.id.ivCompass)
        ivRefresh = findViewById<ImageView>(R.id.ivRefresh)
        ivArrow = findViewById<ImageView>(R.id.ivArrow)
         tvDest = findViewById<TextView>(R.id.tvDestinationDistance)
        tvCurrentLoc = findViewById<TextView>(R.id.tvCurrentLoc)
         btnSetDest = findViewById<Button>(R.id.btnSetDest)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        setUpLocation()

        btnSetDest.setOnClickListener {

            openCoordsDialog()
        }
        ivRefresh.setOnClickListener{
            ivArrow.setImageDrawable(resources.getDrawable(R.drawable.noarrow))
            tvDest.text = null
            destLat = 0.0
            destLng = 0.0
            setUpLocation()
        }

    }

    private fun setUpLocation() {


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->

                    if (location!=null)
                    {


                        val currLat = location.latitude
                        val currLng = location.longitude

                        val addressesDestination: List<Address>
                        try {
                            val geoCoder = Geocoder(this)
                            addressesDestination = geoCoder.getFromLocation(
                                currLat,
                                currLng,
                                1
                            )
                            val mPlaceLocation = addressesDestination[0].getAddressLine(0)

                            tvCurrentLoc.setText(mPlaceLocation)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }




                    }
                }
    }

     fun getBearing(currLat: Double, currLng: Double, destLat: Double, destLng: Double): Double {

        val lat1 = Math.toRadians(currLat)
        val lat2 = Math.toRadians(destLat)

        val longDiff = Math.toRadians(destLng - currLng)
        val y = sin(longDiff) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(longDiff)

        return (Math.toDegrees(atan2(y, x))+360)%360


    }






    override fun onResume() {
        super.onResume()
        sensorManager .getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { acceleromater -> sensorManager.registerListener(
            this, acceleromater, SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_UI
        )
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField -> sensorManager.registerListener(
            this, magneticField, SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_UI
        )

        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)

    }

    private fun openCoordsDialog() {

        val alert = AlertDialog.Builder(this)
        alert.setMessage(resources.getString(R.string.please))
        val coordsLay = layoutInflater.inflate(resources.getLayout(R.layout.dest_layout), null)
        val etLat = coordsLay.findViewById<EditText>(R.id.etLat)
        val etLng = coordsLay.findViewById<EditText>(R.id.etLng)

        alert.setPositiveButton(resources.getString(R.string.ok)){ _, _ ->
            
            if (etLat.text.toString().isEmpty() && etLng.text.toString().isEmpty()){

                Toast.makeText(
                    this,
                    resources.getString(R.string.pleasedistorcoords),
                    Toast.LENGTH_SHORT
                ).show()

            }
            else{


                val latCoord = etLat.text.toString()
                val lngCoord = etLng.text.toString()

                Log.e("lat", latCoord)
                Log.e("lng", lngCoord)

               getDistanceBetweenLocations(latCoord, lngCoord)


            }


        }
        alert.setNegativeButton(resources.getString(R.string.cancel)){ _, _ ->

        }
        alert.create()
        alert.setView(coordsLay)
        alert.show()



    }

    private fun getDistanceBetweenLocations(latCoord: String, lngCoord: String) {
        ivArrow.setImageDrawable(resources.getDrawable(R.drawable.outline))
        val destLat = latCoord.toDouble()
        val destLng = lngCoord.toDouble()
        val currLat = currentLocation.latitude
        val currLng = currentLocation.longitude

        val destLatLng = LatLng(destLat,destLng)
        val currLatLng = LatLng(currLat,currLng)
        val bearing = getBearing(currLat,currLng,destLat,destLng)
        Log.e("bearing",bearing.toString())


        val distance = Math.round(SphericalUtil.computeDistanceBetween(currLatLng,destLatLng)/1000)



        tvDest.text = "Distance from destination is $distance km"


        val destinationLoc = Location("")
        destinationLoc.latitude = destLat
        destinationLoc.longitude =destLng
        var bearTo: Float = currentLocation.bearingTo(destinationLoc)

            val geoField = GeomagneticField(
                currentLocation.latitude.toFloat(), currentLocation.longitude.toFloat(),
                currentLocation.altitude.toFloat(),
                System.currentTimeMillis()
            )
       val head = geoField.declination


        if (bearTo < 0) {
            bearTo += 360

        }


        var direction: Float = bearTo - head

        if (direction < 0) {
            direction += 360
        }




        val arrowAnimation = RotateAnimation(
            currentDegreeNeedle,
            direction,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        arrowAnimation.duration = 210
        arrowAnimation.fillAfter = true

        ivArrow.startAnimation(arrowAnimation)

        currentDegreeNeedle = direction
        val ra = RotateAnimation(
            currentDegree,
            -degree.toFloat(),
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )


        ra.duration = 210


        ra.fillAfter = true

        ivArrow.startAnimation(ra)

        currentDegree = -degree.toFloat()



    }

    override fun onDestroy() {

        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val alpha:Float = 0.97f
        synchronized(this){
            if (event != null) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

                    mGravity[0] = alpha*mGravity[0]+(1-alpha)*event.values[0]
                    mGravity[1] = alpha*mGravity[1]+(1-alpha)*event.values[1]
                    mGravity[2] = alpha*mGravity[2]+(1-alpha)*event.values[2]
                }

                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {

                    magReading[0] = alpha*magReading[0]+(1-alpha)*event.values[0]
                    magReading[1] = alpha*magReading[1]+(1-alpha)*event.values[1]
                    magReading[2] = alpha*magReading[2]+(1-alpha)*event.values[2]
                }

                val r = FloatArray(9)
                val i = FloatArray(9)
                val success:Boolean
                success = SensorManager.getRotationMatrix(r, i, mGravity, magReading)

                if (success)
                {
                    var orientation = FloatArray(3)
                    SensorManager.getOrientation(r, orientation)
                    mAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    mAzimuth = (mAzimuth+360)%360

                    val anim:RotateAnimation
                    anim = RotateAnimation(
                        -currentAzimuth,
                        -mAzimuth,
                        Animation.RELATIVE_TO_SELF,
                        0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f
                    )
                    currentAzimuth = mAzimuth

                    anim.duration = 500
                    anim.repeatCount = 0
                    anim.fillAfter = true

                    ivCompass.startAnimation(anim)

                    if(destLat!=null && destLng!= null){
                        val destLocation = Location("")
                        destLocation.latitude = destLat
                        destLocation.longitude = destLng

                        var bearTo = currentLocation.bearingTo(destLocation)
                        val geoField = GeomagneticField(
                            (currentLocation.latitude).toFloat(),

                            (currentLocation.longitude).toFloat(),
                            (currentLocation.altitude).toFloat(),
                            System.currentTimeMillis()
                        )


                    }





                }

            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun showSnackBar(isConnected: Boolean) {
        var message: String? = null
        var color = 0
        if (!isConnected) {
            message = resources.getString(R.string.offlinecheckconnection)
            color = resources.getColor(android.R.color.holo_red_dark)
        }
        val snackbar = Snackbar.make(
            findViewById(R.id.rootLayout),
            message!!, Snackbar.LENGTH_LONG
        )
        val view: View = snackbar.view
        view.setBackgroundColor(color)

        snackbar.show()
        val textView: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(resources.getColor(android.R.color.white))
        textView.textSize = 8f
        snackbar.show()
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {

        showSnackBar(isConnected)
    }

    private fun showLocationSettingsDialog() {
        val dialog = AlertDialog.Builder(this@MainActivity)
        dialog.setTitle(resources.getString(R.string.loc_is_off))
        dialog.setMessage(resources.getString(R.string.pleaseturnonGPS))
        dialog.setPositiveButton(
            resources.getString(R.string.open_location_settings)
        ) { paramDialogInterface, paramInt ->
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(myIntent)
            paramDialogInterface.dismiss()
        }
        dialog.setNegativeButton(
            getString(R.string.Cancel)
        ) { paramDialogInterface, paramInt ->
            paramDialogInterface.dismiss()
        }
        dialog.show()
    }

    private fun adjustArrow(azimuth: Float, currentAzimuth: Float, targetView: View) {
        val an = RotateAnimation(-currentAzimuth, -azimuth,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f)

        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true

        targetView.startAnimation(an)
    }


}