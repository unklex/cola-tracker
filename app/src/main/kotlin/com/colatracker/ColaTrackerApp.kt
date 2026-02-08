package com.colatracker

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.BitmapFactoryDecoder

class ColaTrackerApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        android.util.Log.d("ColaTrackerApplication", "Initializing custom ImageLoader with BitmapFactoryDecoder")
        return ImageLoader.Builder(this)
            // Force usage of BitmapFactoryDecoder to avoid "unimplemented" crashes
            // with ImageDecoder on some Emulators/Devices
            .components {
                add(BitmapFactoryDecoder.Factory())
            }
            .build()
    }
}
