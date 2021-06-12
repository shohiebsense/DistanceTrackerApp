package com.shohiebsense.distancetrackerapp.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.shohiebsense.distancetrackerapp.R
import com.shohiebsense.distancetrackerapp.databinding.FragmentPermissionBinding
import com.shohiebsense.distancetrackerapp.utils.Permissions.hasLocationPermission
import com.shohiebsense.distancetrackerapp.utils.Permissions.requestLocationPermission
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog


class PermissionFragment : Fragment(), EasyPermissions.PermissionCallbacks {

    private var _permissionBinding : FragmentPermissionBinding? = null
    private val permissionBinding get() = _permissionBinding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _permissionBinding = FragmentPermissionBinding.inflate(inflater, container, false)
        permissionBinding.continueButton.setOnClickListener {
            if(hasLocationPermission(requireContext())){
                findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)
            }
            else{
                requestLocationPermission(this)
            }
        }
        // Inflate the layout for this fragment
        return permissionBinding.root
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
            SettingsDialog.Builder(requireContext()).build().show()
        }
        else{
            requestLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _permissionBinding = null
    }
}