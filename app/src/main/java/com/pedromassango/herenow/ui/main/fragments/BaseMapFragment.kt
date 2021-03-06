package com.pedromassango.herenow.ui.main.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.pedromassango.herenow.R
import com.pedromassango.herenow.app.HereNow.Companion.logcat
import kotlinx.android.synthetic.main.fragment_maps.view.*

/**
 * Created by pedromassango on 12/28/17.
 *
 * Show the Map with friends location (If available)
 */
abstract class BaseMapFragment : Fragment(), OnMapReadyCallback, LocationListener {

    //Map
    var map: GoogleMap? = null
    private lateinit var mMapView: MapView


    // TO request device location updates
    private lateinit var locationManager: LocationManager

    // Location updates delay and distance
    private val distance = 20F
    var timeUpdate = 5000L // 5sec

    // View
    lateinit var root: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup locationManager
        locationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_maps, container, false)

        mMapView = root.maps_view
        mMapView.onCreate(savedInstanceState)
        mMapView.onResume() // To setup Map immediately

        try {
            MapsInitializer.initialize(activity!!.applicationContext)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        mMapView.getMapAsync(this)

        return root
    }

    override fun onStart() {
        super.onStart()
        mMapView.onStart()
    }

    override fun onStop() {
        mMapView.onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()

        // if GPS is disable, request enable it
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){

            val dialog = AlertDialog.Builder(activity!!)
                    .setTitle(R.string.request_gps_enable_title)
                    .setMessage(R.string.request_gps_enable_message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setPositiveButton(R.string.str_enable) { _, _ ->

                        startActivity( Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }

            dialog.create().show()
        }else{
            mMapView.getMapAsync(this)

            // resume the mapView
            mMapView.onResume()
        }
    }

    override fun onPause() {
        mMapView.onPause()

        // Remove location updates
        locationManager.removeUpdates(this)
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mMapView.onLowMemory()
    }

    override fun onDestroy() {
        mMapView.onDestroy()
        super.onDestroy()
    }

    fun loader() {
        with(root) {
            mMapView.visibility = View.GONE
            progressbar_maps.visibility = View.VISIBLE
            tv_map_info.visibility = View.VISIBLE
            tv_map_info.text = getString(R.string.please_wait)
        }
    }

    fun dismissLoader() {
        with(root) {
            mMapView.visibility = View.VISIBLE
            progressbar_maps.visibility = View.GONE
            tv_map_info.visibility = View.GONE
        }
    }

    fun requestLocationPermission(iPermissionListener: (state: Boolean) -> Unit) {
        Dexter.withActivity(activity)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {

                    override fun onPermissionGranted(response: PermissionGrantedResponse?) = iPermissionListener.invoke(true)

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                        val dialog = AlertDialog.Builder(activity!!)
                                .setTitle(R.string.request_location_permission_title)
                                .setMessage(R.string.request_location_permission_message)
                                .setCancelable(false)
                                .setPositiveButton(R.string.str_ok) { _, _ -> requestLocationPermission(iPermissionListener) }

                        dialog.create().show()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) =
                            iPermissionListener.invoke(false)
                }).check()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mMap: GoogleMap?) {
        logcat("onMapReady")

        // Get map reference
        this.map = mMap!!

        // Setting up map settings
        mMap.isBuildingsEnabled = true
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.setMinZoomPreference(10.0F)

        // Request permission to access location
        requestLocationPermission { state ->
            when (state) {
                true -> {

                    // Request location updates via GPS
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, timeUpdate, distance, this@BaseMapFragment)
                    // Request location updates via PASSIVE-PROVIDER
                    locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, timeUpdate, distance, this@BaseMapFragment)
                    // Request location updates via NETWORK
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timeUpdate, distance, this@BaseMapFragment)
                }
                false -> activity!!.finish()
            }
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = logcat("onStatusChanged:  $provider")

    override fun onProviderDisabled(provider: String?) = logcat("onProviderDisabled:  $provider")

    override fun onProviderEnabled(provider: String?) = logcat("onProviderEnabled:  $provider")
}
