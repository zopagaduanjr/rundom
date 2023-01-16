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

import android.graphics.*
import android.location.Location
import androidx.annotation.ColorInt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
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
  lateinit var currentPolyline: Polyline

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

  fun drawRoute(locations: List<LatLng>){
    if(this::currentPolyline.isInitialized){
      currentPolyline.points = locations
    }
    else{
      currentPolyline = googleMap.addPolyline(PolylineOptions()
        .addAll(locations)
      )
    }
  }

   fun clearRoute(){
     if(this::currentPolyline.isInitialized){
       currentPolyline.remove()
     }
   }

  private fun createOuterBounds(): List<LatLng?> {
    val delta = 0.01f
    return object : ArrayList<LatLng?>() {
      init {
        add(LatLng((90 - delta).toDouble(), (-180 + delta).toDouble()))
        add(LatLng(0.0, (-180 + delta).toDouble()))
        add(LatLng((-90 + delta).toDouble(), (-180 + delta).toDouble()))
        add(LatLng((-90 + delta).toDouble(), 0.0))
        add(LatLng((-90 + delta).toDouble(), (180 - delta).toDouble()))
        add(LatLng(0.0, (180 - delta).toDouble()))
        add(LatLng((90 - delta).toDouble(), (180 - delta).toDouble()))
        add(LatLng((90 - delta).toDouble(), 0.0))
        add(LatLng((90 - delta).toDouble(), (-180 + delta).toDouble()))
      }
    }
  }

  private val EARTH_RADIUS = 6371

  private fun createHole(center: LatLng, radiusMeter: Double): Iterable<LatLng> {
    val radiusKM = radiusMeter * 0.001
    val points = 50
    val radiusLatitude = Math.toDegrees((radiusKM / EARTH_RADIUS.toFloat()))
    val radiusLongitude = radiusLatitude / cos(Math.toRadians(center.latitude))
    val result: MutableList<LatLng> = ArrayList(points)
    val anglePerCircleRegion = 2 * Math.PI / points
    for (i in 0 until points) {
      val theta = i * anglePerCircleRegion
      val latitude = center.latitude + radiusLatitude * Math.sin(theta)
      val longitude = center.longitude + radiusLongitude * Math.cos(theta)
      result.add(LatLng(latitude, longitude))
    }
    return result
  }

  private fun createPolygonWithCircle(center: LatLng, radiusMeter: Double): PolygonOptions {
    return PolygonOptions()
      .fillColor(0xff789E9E9E.toInt())
      .addAll(createOuterBounds())
      .addHole(createHole(center, radiusMeter))
      .strokeWidth(0f)
  }

  fun generateRadius(location: LatLng, radiusMeter: Double){
    val polygonOptions = createPolygonWithCircle(location,radiusMeter)
    googleMap.addPolygon(polygonOptions)
//    googleMap.addCircle(CircleOptions().center(location).radius(radiusMeter))
  }

  fun generateStars(earth: Earth): Pair<ArrayList<Anchor>, ArrayList<Marker>>{
    val anchors = arrayListOf<Anchor>()
    val markers = arrayListOf<Marker>()
    val location = earth.cameraGeospatialPose
    val meterRadius = 1000
    val radiusInDegrees = (meterRadius / 111000f).toDouble()
    for (i in 1..5) {
      var isOutOfBubble = true
      lateinit var ranLatLng: LatLng
      while(isOutOfBubble){
        val random = Random()
        val u: Double = random.nextDouble()
        val v: Double = random.nextDouble()
        val w = radiusInDegrees * sqrt(u)
        val t = 2 * Math.PI * v
        val x = w * cos(t)
        val y = w * sin(t)
        val newX = x / cos(Math.toRadians(location.longitude))

        val foundLatitude: Double = newX + location.latitude
        val foundLongitude: Double = y + location.longitude
        val userLoc = Location("itemLoc")
        val ranLoc = Location("ranLoc")
        userLoc.latitude = location.latitude
        userLoc.longitude = location.longitude
        ranLoc.latitude = foundLatitude
        ranLoc.longitude = foundLongitude
        val dist = userLoc.distanceTo(ranLoc)
        if(dist < meterRadius){
          isOutOfBubble = false
          ranLatLng = LatLng(foundLatitude,foundLongitude)
        }
      }

      val marker: Marker? = googleMap.addMarker(MarkerOptions().position(ranLatLng).title("Star $i"))
      if(marker != null){
        markers.add(marker)
      }
      //anchors away
      // Place the earth anchor at the same altitude as that of the camera to make it easier to view.
      val altitude = earth.cameraGeospatialPose.altitude - 1
      // The rotation quaternion of the anchor in the East-Up-South (EUS) coordinate system.
      val qx = 0f
      val qy = 0f
      val qz = 0f
      val qw = 1f
      val anchor = earth.createAnchor(ranLatLng.latitude, ranLatLng.longitude, altitude, qx, qy, qz, qw)
      anchors.add(anchor)
    }
    generateRadius(LatLng(location.latitude,location.longitude),meterRadius.toDouble())
    return Pair(anchors,markers)
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