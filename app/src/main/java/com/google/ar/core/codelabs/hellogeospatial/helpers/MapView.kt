/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.codelabs.hellogeospatial.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import androidx.annotation.ColorInt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import com.google.ar.core.codelabs.hellogeospatial.HelloGeoActivity
import com.google.ar.core.codelabs.hellogeospatial.R
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MapView(val activity: HelloGeoActivity, val googleMap: GoogleMap) {
  private val CAMERA_MARKER_COLOR: Int = Color.argb(255, 0, 255, 0)

  var setInitialCameraPosition = false
  val cameraMarker = createMarker(CAMERA_MARKER_COLOR)
  var cameraIdle = true

  init {
    googleMap.uiSettings.apply {
      isMapToolbarEnabled = false
      isIndoorLevelPickerEnabled = false
      isZoomControlsEnabled = true
      isTiltGesturesEnabled = false
      isScrollGesturesEnabled = false
    }

    googleMap.setOnMarkerClickListener { unused -> false }

    // Add listeners to keep track of when the GoogleMap camera is moving.
    googleMap.setOnCameraMoveListener { cameraIdle = false }
    googleMap.setOnCameraIdleListener { cameraIdle = true }
  }

  fun generateStars(earth: Earth): Array<Anchor>{
    var anchors = arrayOf<Anchor>()
    val location = earth.cameraGeospatialPose
    val meterRadius = 250
    for (i in 1..5) {
      val random = Random()
      val radiusInDegrees = (meterRadius / 111000f).toDouble()

      val u: Double = random.nextDouble()
      val v: Double = random.nextDouble()
      val w = radiusInDegrees * sqrt(u)
      val t = 2 * Math.PI * v
      val x = w * cos(t)
      val y = w * sin(t)
      val newX = x / cos(Math.toRadians(location.longitude))

      val foundLongitude: Double = newX + location.latitude
      val foundLatitude: Double = y + location.longitude
      val ranLoc = LatLng(foundLongitude, foundLatitude)
      googleMap.addMarker(MarkerOptions().position(ranLoc).title("Star $i"))

      //anchors away
      // Place the earth anchor at the same altitude as that of the camera to make it easier to view.
      val altitude = earth.cameraGeospatialPose.altitude - 1
      // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
      val qx = 0f
      val qy = 0f
      val qz = 0f
      val qw = 1f
      anchors += earth.createAnchor(ranLoc.latitude, ranLoc.longitude, altitude, qx, qy, qz, qw)
//      activity.view.mapView?.earthMarker?.apply {
//        position = ranLoc
//        isVisible = true
//      }
    }
    return anchors
  }


  fun updateMapPosition(latitude: Double, longitude: Double, heading: Double) {
    val position = LatLng(latitude, longitude)
    activity.runOnUiThread {
      // If the map is already in the process of a camera update, then don't move it.
      if (!cameraIdle) {
        return@runOnUiThread
      }
      cameraMarker.isVisible = true
      cameraMarker.position = position
      cameraMarker.rotation = heading.toFloat()

      val cameraPositionBuilder: CameraPosition.Builder = if (!setInitialCameraPosition) {
        // Set the camera position with an initial default zoom level.
        setInitialCameraPosition = true
        CameraPosition.Builder().zoom(21f).target(position)
      } else {
        // Set the camera position and keep the same zoom level.
        CameraPosition.Builder()
          .zoom(googleMap.cameraPosition.zoom)
          .target(position)
      }
      googleMap.moveCamera(
        CameraUpdateFactory.newCameraPosition(cameraPositionBuilder.build()))
    }
  }

  /** Creates and adds a 2D anchor marker on the 2D map view.  */
  private fun createMarker(
    color: Int,
  ): Marker {
    val markersOptions = MarkerOptions()
      .position(LatLng(0.0,0.0))
      .draggable(false)
      .anchor(0.5f, 0.5f)
      .flat(true)
      .visible(false)
      .icon(BitmapDescriptorFactory.fromBitmap(createColoredMarkerBitmap(color)))
    return googleMap.addMarker(markersOptions)!!
  }

  private fun createColoredMarkerBitmap(@ColorInt color: Int): Bitmap {
    val opt = BitmapFactory.Options()
    opt.inMutable = true
    val navigationIcon =
      BitmapFactory.decodeResource(activity.resources, R.drawable.ic_navigation_white_48dp, opt)
    val p = Paint()
    p.colorFilter = LightingColorFilter(color,  /* add= */1)
    val canvas = Canvas(navigationIcon)
    canvas.drawBitmap(navigationIcon,  /* left= */0f,  /* top= */0f, p)
    return navigationIcon
  }
}