package psycho.euphoria.tools.videos

import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import com.googlecode.mp4parser.authoring.Track
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack
import psycho.euphoria.tools.commons.Tracker
import psycho.euphoria.tools.commons.triggerScanFile
import java.io.File
import java.io.FileOutputStream
import java.util.*


class SplitVideo(private val path: String) : AsyncTask<Double, Void, Void>() {
    private var videoTime: Double = 0.toDouble()
    private var set: Boolean = false


    private fun correctTimeToSyncSample(track: Track, cutHere: Double, next: Boolean): Double {
        val timeOfSyncSamples = DoubleArray(track.syncSamples.size)
        var currentSample: Long = 0
        var currentTime = 0.0
        for (i in 0 until track.sampleDurations.size) {
            val delta = track.sampleDurations[i]
            if (Arrays.binarySearch(track.syncSamples, currentSample + 1) >= 0) {
                timeOfSyncSamples[Arrays.binarySearch(track.syncSamples, currentSample + 1)] = currentTime
            }
            currentTime += delta.toDouble() / track.trackMetaData.timescale.toDouble()
            currentSample++
        }
        var previous = 0.0
        for (timeOfSyncSample in timeOfSyncSamples) {
            if (timeOfSyncSample >= cutHere) {
                return if (next) {
                    timeOfSyncSample
                } else {
                    previous
                }
            }
            previous = timeOfSyncSample
        }
        return timeOfSyncSamples[timeOfSyncSamples.size - 1]
    }

    override fun doInBackground(vararg doubles: Double?): Void? {
        val file = File(Environment.getExternalStorageDirectory(), File(path).name)
        try {
            doubles?.let {
                if (it.size > 1)
                    performSplit(it[0]!!, it[1]!!, path, file.absolutePath)
            }

        } catch (e: Exception) {
            Tracker.e("doInBackground", e.message ?: "")
        }


        return null
    }

    private fun performSplit(startTime: Double, endTime: Double, videoPath: String, outputPath: String): Boolean {
        var startTime = startTime
        var endTime = endTime
        val movie = MovieCreator.build(videoPath)
        val tracks = movie.tracks
        movie.tracks = LinkedList()
        var timeCorrected = false
        for (track in tracks) {
            if (track.syncSamples != null && track.syncSamples.size > 0) {
                if (timeCorrected) {
                    throw RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.")
                }
                startTime = correctTimeToSyncSample(track, startTime, true)
                endTime = correctTimeToSyncSample(track, endTime, true)
                timeCorrected = true
                if (!set) {
                    videoTime = correctTimeToSyncSample(track, 10000.0, true)
                    set = true
                }
            }
        }
        if (startTime == endTime)
            return false
        for (track in tracks) {
            var currentSample: Long = 0
            var currentTime = 0.0
            var lastTime = 0.0
            var startSample1: Long = 0
            var endSample1: Long = -1
            for (i in 0 until track.sampleDurations.size) {
                val delta = track.sampleDurations[i]
                if (currentTime > lastTime && currentTime <= startTime) {
                    startSample1 = currentSample
                }
                if (currentTime > lastTime && currentTime <= endTime) {
                    endSample1 = currentSample
                }
                lastTime = currentTime
                currentTime += delta.toDouble() / track.trackMetaData.timescale.toDouble()
                currentSample++
            }
            Log.i("DASH", "Start time = $startTime, End time = $endTime")
            movie.addTrack(CroppedTrack(track, startSample1, endSample1))
        }
        val start1 = System.currentTimeMillis()
        val out = DefaultMp4Builder().build(movie)
        val start2 = System.currentTimeMillis()
        val fos = FileOutputStream(outputPath)
        val fc = fos.getChannel()
        out.writeContainer(fc)
        fc.close()
        fos.close()
        outputPath.triggerScanFile()
        val start3 = System.currentTimeMillis()
        Log.i("DASH", "Building IsoFile took : " + (start2 - start1) + "ms")
        Log.i("DASH", "Writing IsoFile took  : " + (start3 - start2) + "ms")
        return true
    }
}