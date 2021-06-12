package com.shohiebsense.distancetrackerapp.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.shohiebsense.distancetrackerapp.R
import com.shohiebsense.distancetrackerapp.databinding.FragmentMapsBinding
import com.shohiebsense.distancetrackerapp.models.Result
import com.shohiebsense.distancetrackerapp.services.TrackerService
import com.shohiebsense.distancetrackerapp.utils.Constants
import com.shohiebsense.distancetrackerapp.utils.Constants.ACTION_SERVICE_STOP
import com.shohiebsense.distancetrackerapp.utils.ExtensionFunctions.disable
import com.shohiebsense.distancetrackerapp.utils.ExtensionFunctions.enable
import com.shohiebsense.distancetrackerapp.utils.ExtensionFunctions.hide
import com.shohiebsense.distancetrackerapp.utils.ExtensionFunctions.show
import com.shohiebsense.distancetrackerapp.utils.MapUtils
import com.shohiebsense.distancetrackerapp.utils.MapUtils.setCameraPosition
import com.shohiebsense.distancetrackerapp.utils.Permissions.hasBackgroundLocationPermission
import com.shohiebsense.distancetrackerapp.utils.Permissions.requestBackgroundLocationPermission
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, EasyPermissions.PermissionCallbacks,
    GoogleMap.OnMarkerClickListener {

    private var _mapsBinding: FragmentMapsBinding? = null
    private val mapsBinding get() = _mapsBinding!!
    private var locationList = mutableListOf<LatLng>()
    private var markerList = mutableListOf<Marker>()

    val isStarted = MutableLiveData(false)
    private var startTime = 0L
    private var stopTime = 0L

    private lateinit var map: GoogleMap
    private var polyLineList = mutableListOf<Polyline>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _mapsBinding = FragmentMapsBinding.inflate(inflater, container, false)
        mapsBinding.lifecycleOwner = this
        mapsBinding.tracking = this
        mapsBinding.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        mapsBinding.stopButton.setOnClickListener {
            onStopButtonClicked()
        }
        mapsBinding.resetButton.setOnClickListener {
            onResetButtonClicked()

        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return mapsBinding.root
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
            SettingsDialog.Builder(requireActivity()).build().show()
        } else {
            requestBackgroundLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }

    private fun onStartButtonClicked() {
        if(hasBackgroundLocationPermission(requireContext())){
            startCountDown()
            mapsBinding.startButton.disable()
            mapsBinding.startButton.hide()
            mapsBinding.stopButton.show()
        } else {
            requestBackgroundLocationPermission(this)
        }
    }

    private fun onStopButtonClicked() {
        stopForegroundService()
        mapsBinding.stopButton.hide()
        mapsBinding.startButton.show()
    }



    private fun startCountDown() {
        mapsBinding.timerTextView.show()
        mapsBinding.stopButton.disable()
        val timer : CountDownTimer = object : CountDownTimer(4000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if(currentSecond.toString() == "0"){
                    mapsBinding.timerTextView.text = "GO"
                    mapsBinding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.black
                    ))
                }
                else {
                    mapsBinding.timerTextView.text = currentSecond.toString()
                    mapsBinding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.red
                    ))
                }
            }

            override fun onFinish() {
                sendActionCommandToService(Constants.ACTION_SERVICE_START)
                mapsBinding.timerTextView.hide()
            }
        }
        timer.start()
    }

    private fun sendActionCommandToService(action: String){
        Intent(requireContext(), TrackerService::class.java).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    private fun stopForegroundService(){
        mapsBinding.startButton.disable()
        sendActionCommandToService(ACTION_SERVICE_STOP)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        this.map = map
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = false
            isScrollGesturesEnabled = false
        }
        observeTrackerService()
    }

    private fun observeTrackerService(){
        TrackerService.locationList.observe(viewLifecycleOwner, {
            if(it != null){
                locationList = it
                Log.e("MapsFragment", locationList.toString())
                drawPolyline()
                followPolyline()
                if(locationList.isNotEmpty()){
                    mapsBinding.stopButton.enable()
                }
            }
        })
        TrackerService.started.observe(viewLifecycleOwner, {
            isStarted.value = it
        })
        TrackerService.startTime.observe(viewLifecycleOwner, {
            startTime = it
        })
        TrackerService.stopTime.observe(viewLifecycleOwner, {
            stopTime = it
            if(stopTime != 0L){
                showOverallLine()
                displayResults()
            }
        })
    }

    private fun showOverallLine() {
        val bounds = LatLngBounds.Builder()
        for(location in locationList){
            bounds.include(location)
        }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100), 2000, null)
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    private fun addMarker(position: LatLng){
        val marker = map.addMarker(MarkerOptions().position(position))
        markerList.add(marker)
    }

    private fun displayResults(){
        val result = Result(
            MapUtils.calculateTheDistance(locationList),
            MapUtils.calculateElapsedTime(startTime, stopTime)
        )
        lifecycleScope.launch {
            delay(2500)
            val directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)
            mapsBinding.startButton.apply {
                hide()
                enable()
            }
            mapsBinding.stopButton.apply {
                hide()
            }

            mapsBinding.resetButton.show()
        }
    }

    private fun drawPolyline(){
        var polyLine = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.RED)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
        polyLineList.add(polyLine)
    }

    private fun followPolyline(){
        if(locationList.isEmpty()) return
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                MapUtils.setCameraPosition(locationList.last())
            ), 1000, null
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _mapsBinding = null
    }

    override fun onMyLocationButtonClick(): Boolean {
        mapsBinding.hintTextView.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2500)
            mapsBinding.hintTextView.hide()
            mapsBinding.startButton.show()
        }
        return false
    }

    private fun onResetButtonClicked() {
        resetMap()
    }

    @SuppressLint("MissingPermission")
    private fun resetMap(){
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastKnownLocation = LatLng(
                it.result.latitude,
                it.result.longitude
            )
            for(polyLine in polyLineList){
                polyLine.remove()
            }
            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                setCameraPosition(lastKnownLocation)
            ))
        }
        locationList.clear()
        markerList.clear()
        mapsBinding.resetButton.hide()
        mapsBinding.startButton.show()
    }

    override fun onMarkerClick(marker: Marker): Boolean {

        return true
    }
}