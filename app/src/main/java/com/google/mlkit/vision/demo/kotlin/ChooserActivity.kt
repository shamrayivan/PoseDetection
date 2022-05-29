/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.kotlin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.demo.R


/**
 * Demo app chooser which takes care of runtime permission requesting and allow you pick from all
 * available testing Activities.
 */

public const val CHOOSER_ARGS = "CHOOSER_ARGS"

class ChooserActivity :
    AppCompatActivity(),
    ActivityCompat.OnRequestPermissionsResultCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_chooser)

        // Set up ListView and Adapter
        val listView =
            findViewById<ListView>(R.id.test_activity_list_view)
        val adapter =
            MyArrayAdapter(
                this,
                android.R.layout.simple_list_item_2,
                TrainingMode.values()
            ) { mode ->
                openLivePreview(mode)

            }
        listView.adapter = adapter

        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            requestPermissions(allNeededPermissions)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUESTS_REQUEST_CODE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    openLivePreview()
                } else {
                    val rootView = findViewById<View>(android.R.id.content)
                    Snackbar.make(
                        rootView,
                        R.string.text_please_give_permission,
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.text_confirm_permission) { getRuntimePermissions() }.show()

                }
            }
        }
    }

    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }


    private fun requestPermissions(allNeededPermissions: ArrayList<String>) {
        ActivityCompat.requestPermissions(
            this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS_REQUEST_CODE
        )
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }


    private fun openLivePreview(mode: TrainingMode = TrainingMode.INTENSIVE) {
        val intent = Intent(this, CameraXLivePreviewActivity::class.java)
        intent.putExtra(CHOOSER_ARGS, mode)
        startActivity(intent)
    }

    private class MyArrayAdapter(
        private val ctx: Context,
        resource: Int,
        private val modes: Array<TrainingMode>,
        private val onModeClick: (TrainingMode) -> Unit
    ) : ArrayAdapter<TrainingMode>(ctx, resource, modes) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view:View = convertView?:getInflator().inflate(android.R.layout.simple_list_item_2, null)

            return view.apply {
                val mode = modes[position]
                (view.findViewById<View>(android.R.id.text1) as TextView).setText(mode.descriptionId)

                val exercisesCountDescription =
                    if (mode == TrainingMode.FREE)
                        context.getString(R.string.text_training_exercises_countless)
                    else
                        context.getString(R.string.text_training_exercises_count, mode.intensity)
                 (view.findViewById<View>(android.R.id.text2) as TextView).run {
                    text = exercisesCountDescription
                    setOnClickListener { onModeClick(mode) }
                }
            }
        }
        private fun getInflator() = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    companion object {
        private const val TAG = "ChooserActivity"
        private const val PERMISSION_REQUESTS_REQUEST_CODE = 1
    }
}

public enum class TrainingMode(val descriptionId: Int, val intensity: Int) {
    POWER(R.string.text_training_power, 5),
    INTENSIVE(R.string.text_training_intensive, 12),
    CALORIES_BURN(R.string.text_training_calories_burn, 15),
    FREE(R.string.text_training_free, Int.MAX_VALUE),
}
