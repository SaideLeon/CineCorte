package com.example.data.repository

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializador e deserializador seguro baseado em JSON nativo do Android.
 */
object ProjectJsonAdapter {

    fun toJson(project: Project): String {
        val root = JSONObject()
        root.put("id", project.id)
        root.put("title", project.title)
        root.put("createdAt", project.createdAt)
        root.put("lastModified", project.lastModified)
        root.put("rippleDelete", project.rippleDelete)
        root.put("masterVolume", project.masterVolume.toDouble())

        // Video clips
        val videoClipsArray = JSONArray()
        for (clip in project.videoClips) {
            val cObj = JSONObject()
            cObj.put("id", clip.id)
            cObj.put("sourceUri", clip.sourceUri)
            cObj.put("name", clip.name)
            cObj.put("startTrimMs", clip.startTrimMs)
            cObj.put("endTrimMs", clip.endTrimMs)
            cObj.put("speed", clip.speed.toDouble())
            cObj.put("isMuted", clip.isMuted)
            cObj.put("volume", clip.volume.toDouble())
            cObj.put("scale", clip.scale.toDouble())
            cObj.put("offsetX", clip.offsetX.toDouble())
            cObj.put("offsetY", clip.offsetY.toDouble())
            cObj.put("rotation", clip.rotation.toDouble())

            // Metadata
            val mObj = JSONObject()
            mObj.put("uri", clip.metadata.uri)
            mObj.put("rawWidth", clip.metadata.rawWidth)
            mObj.put("rawHeight", clip.metadata.rawHeight)
            mObj.put("rotation", clip.metadata.rotation)
            mObj.put("durationMs", clip.metadata.durationMs)
            mObj.put("fps", clip.metadata.fps.toDouble())
            mObj.put("bitrate", clip.metadata.bitrate)
            mObj.put("mimeType", clip.metadata.mimeType)
            mObj.put("title", clip.metadata.title)
            cObj.put("metadata", mObj)

            // Transitions
            val tIn = JSONObject()
            tIn.put("type", clip.transitionIn.type.name)
            tIn.put("durationMs", clip.transitionIn.durationMs)
            tIn.put("easing", clip.transitionIn.easing)
            cObj.put("transitionIn", tIn)

            val tOut = JSONObject()
            tOut.put("type", clip.transitionOut.type.name)
            tOut.put("durationMs", clip.transitionOut.durationMs)
            tOut.put("easing", clip.transitionOut.easing)
            cObj.put("transitionOut", tOut)

            // Keyframes
            val kfArray = JSONArray()
            for (kf in clip.keyframes) {
                val kObj = JSONObject()
                kObj.put("id", kf.id)
                kObj.put("timeOffsetMs", kf.timeOffsetMs)
                kObj.put("scale", kf.scale.toDouble())
                kObj.put("offsetX", kf.offsetX.toDouble())
                kObj.put("offsetY", kf.offsetY.toDouble())
                kObj.put("rotationDegrees", kf.rotationDegrees.toDouble())
                kfArray.put(kObj)
            }
            cObj.put("keyframes", kfArray)

            videoClipsArray.put(cObj)
        }
        root.put("videoClips", videoClipsArray)

        // Audio Tracks
        val audioTracksArray = JSONArray()
        for (track in project.audioTracks) {
            val tObj = JSONObject()
            tObj.put("id", track.id)
            tObj.put("type", track.type.name)
            tObj.put("name", track.name)
            tObj.put("isMuted", track.isMuted)
            tObj.put("isSolo", track.isSolo)
            tObj.put("volume", track.volume.toDouble())
            tObj.put("pan", track.pan.toDouble())
            tObj.put("autoDucking", track.autoDucking)

            val clipsArray = JSONArray()
            for (ac in track.clips) {
                val acObj = JSONObject()
                acObj.put("id", ac.id)
                acObj.put("sourceUri", ac.sourceUri)
                acObj.put("name", ac.name)
                acObj.put("timelineStartMs", ac.timelineStartMs)
                acObj.put("startTrimMs", ac.startTrimMs)
                acObj.put("endTrimMs", ac.endTrimMs)
                acObj.put("originalDurationMs", ac.originalDurationMs)
                acObj.put("speed", ac.speed.toDouble())
                acObj.put("volume", ac.volume.toDouble())
                acObj.put("pan", ac.pan.toDouble())
                acObj.put("fadeInMs", ac.fadeInMs)
                acObj.put("fadeOutMs", ac.fadeOutMs)

                // Effects
                val efObj = JSONObject()
                efObj.put("normalizeVolume", ac.effects.normalizeVolume)
                efObj.put("bassBoostDb", ac.effects.bassBoostDb.toDouble())
                efObj.put("midToneDb", ac.effects.midToneDb.toDouble())
                efObj.put("trebleDb", ac.effects.trebleDb.toDouble())
                efObj.put("noiseReduction", ac.effects.noiseReduction)
                efObj.put("pitchShiftSemitones", ac.effects.pitchShiftSemitones.toDouble())
                acObj.put("effects", efObj)

                clipsArray.put(acObj)
            }
            tObj.put("clips", clipsArray)
            audioTracksArray.put(tObj)
        }
        root.put("audioTracks", audioTracksArray)

        return root.toString()
    }

    fun fromJson(jsonStr: String): Project {
        val root = JSONObject(jsonStr)
        val id = root.optString("id", java.util.UUID.randomUUID().toString())
        val title = root.optString("title", "Projeto")
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())
        val lastModified = root.optLong("lastModified", System.currentTimeMillis())
        val rippleDelete = root.optBoolean("rippleDelete", true)
        val masterVolume = root.optDouble("masterVolume", 1.0).toFloat()

        // Video clips
        val videoClips = mutableListOf<VideoClip>()
        val videoClipsArray = root.optJSONArray("videoClips")
        if (videoClipsArray != null) {
            for (i in 0 until videoClipsArray.length()) {
                val cObj = videoClipsArray.getJSONObject(i)
                val mObj = cObj.getJSONObject("metadata")
                val metadata = VideoMetadata(
                    uri = mObj.optString("uri"),
                    rawWidth = mObj.optInt("rawWidth", 1920),
                    rawHeight = mObj.optInt("rawHeight", 1080),
                    rotation = mObj.optInt("rotation", 0),
                    durationMs = mObj.optLong("durationMs", 0L),
                    fps = mObj.optDouble("fps", 30.0).toFloat(),
                    bitrate = mObj.optLong("bitrate", 0L),
                    mimeType = mObj.optString("mimeType", "video/mp4"),
                    title = mObj.optString("title", "Vídeo")
                )

                val tInObj = cObj.optJSONObject("transitionIn")
                val tIn = if (tInObj != null) {
                    ClipTransition(
                        type = try { TransitionType.valueOf(tInObj.optString("type")) } catch (e: Exception) { TransitionType.NONE },
                        durationMs = tInObj.optLong("durationMs", 800L),
                        easing = tInObj.optString("easing", "EaseInOut")
                    )
                } else ClipTransition()

                val tOutObj = cObj.optJSONObject("transitionOut")
                val tOut = if (tOutObj != null) {
                    ClipTransition(
                        type = try { TransitionType.valueOf(tOutObj.optString("type")) } catch (e: Exception) { TransitionType.NONE },
                        durationMs = tOutObj.optLong("durationMs", 800L),
                        easing = tOutObj.optString("easing", "EaseInOut")
                    )
                } else ClipTransition()

                val keyframes = mutableListOf<Keyframe>()
                val kfArray = cObj.optJSONArray("keyframes")
                if (kfArray != null) {
                    for (k in 0 until kfArray.length()) {
                        val kObj = kfArray.getJSONObject(k)
                        keyframes.add(
                            Keyframe(
                                id = kObj.optString("id", java.util.UUID.randomUUID().toString()),
                                timeOffsetMs = kObj.optLong("timeOffsetMs", 0L),
                                scale = kObj.optDouble("scale", 1.0).toFloat(),
                                offsetX = kObj.optDouble("offsetX", 0.0).toFloat(),
                                offsetY = kObj.optDouble("offsetY", 0.0).toFloat(),
                                rotationDegrees = kObj.optDouble("rotationDegrees", 0.0).toFloat()
                            )
                        )
                    }
                }

                videoClips.add(
                    VideoClip(
                        id = cObj.optString("id", java.util.UUID.randomUUID().toString()),
                        sourceUri = cObj.optString("sourceUri"),
                        name = cObj.optString("name", "Clipe"),
                        metadata = metadata,
                        startTrimMs = cObj.optLong("startTrimMs", 0L),
                        endTrimMs = cObj.optLong("endTrimMs", metadata.durationMs),
                        speed = cObj.optDouble("speed", 1.0).toFloat(),
                        isMuted = cObj.optBoolean("isMuted", false),
                        volume = cObj.optDouble("volume", 1.0).toFloat(),
                        scale = cObj.optDouble("scale", 1.0).toFloat(),
                        offsetX = cObj.optDouble("offsetX", 0.0).toFloat(),
                        offsetY = cObj.optDouble("offsetY", 0.0).toFloat(),
                        rotation = cObj.optDouble("rotation", 0.0).toFloat(),
                        keyframes = keyframes,
                        transitionIn = tIn,
                        transitionOut = tOut
                    )
                )
            }
        }

        // Audio tracks
        val audioTracks = mutableListOf<AudioTrack>()
        val audioTracksArray = root.optJSONArray("audioTracks")
        if (audioTracksArray != null) {
            for (i in 0 until audioTracksArray.length()) {
                val tObj = audioTracksArray.getJSONObject(i)
                val type = try {
                    AudioTrackType.valueOf(tObj.optString("type"))
                } catch (e: Exception) {
                    AudioTrackType.MUSIC
                }
                val clips = mutableListOf<AudioClip>()
                val clipsArray = tObj.optJSONArray("clips")
                if (clipsArray != null) {
                    for (j in 0 until clipsArray.length()) {
                        val acObj = clipsArray.getJSONObject(j)
                        val efObj = acObj.optJSONObject("effects")
                        val effects = if (efObj != null) {
                            AudioEffects(
                                normalizeVolume = efObj.optBoolean("normalizeVolume", false),
                                bassBoostDb = efObj.optDouble("bassBoostDb", 0.0).toFloat(),
                                midToneDb = efObj.optDouble("midToneDb", 0.0).toFloat(),
                                trebleDb = efObj.optDouble("trebleDb", 0.0).toFloat(),
                                noiseReduction = efObj.optBoolean("noiseReduction", false),
                                pitchShiftSemitones = efObj.optDouble("pitchShiftSemitones", 0.0).toFloat()
                            )
                        } else AudioEffects()

                        clips.add(
                            AudioClip(
                                id = acObj.optString("id", java.util.UUID.randomUUID().toString()),
                                sourceUri = acObj.optString("sourceUri"),
                                name = acObj.optString("name", "Áudio"),
                                timelineStartMs = acObj.optLong("timelineStartMs", 0L),
                                startTrimMs = acObj.optLong("startTrimMs", 0L),
                                endTrimMs = acObj.optLong("endTrimMs", 10000L),
                                originalDurationMs = acObj.optLong("originalDurationMs", 10000L),
                                speed = acObj.optDouble("speed", 1.0).toFloat(),
                                volume = acObj.optDouble("volume", 1.0).toFloat(),
                                pan = acObj.optDouble("pan", 0.0).toFloat(),
                                fadeInMs = acObj.optLong("fadeInMs", 0L),
                                fadeOutMs = acObj.optLong("fadeOutMs", 0L),
                                effects = effects
                            )
                        )
                    }
                }

                audioTracks.add(
                    AudioTrack(
                        id = tObj.optString("id", java.util.UUID.randomUUID().toString()),
                        type = type,
                        name = tObj.optString("name", type.labelPt),
                        isMuted = tObj.optBoolean("isMuted", false),
                        isSolo = tObj.optBoolean("isSolo", false),
                        volume = tObj.optDouble("volume", 1.0).toFloat(),
                        pan = tObj.optDouble("pan", 0.0).toFloat(),
                        autoDucking = tObj.optBoolean("autoDucking", false),
                        clips = clips
                    )
                )
            }
        }

        return Project(
            id = id,
            title = title,
            createdAt = createdAt,
            lastModified = lastModified,
            videoClips = videoClips,
            audioTracks = if (audioTracks.isNotEmpty()) audioTracks else listOf(
                AudioTrack(type = AudioTrackType.VOICE, name = AudioTrackType.VOICE.labelPt),
                AudioTrack(type = AudioTrackType.MUSIC, name = AudioTrackType.MUSIC.labelPt),
                AudioTrack(type = AudioTrackType.SFX, name = AudioTrackType.SFX.labelPt)
            ),
            rippleDelete = rippleDelete,
            masterVolume = masterVolume
        )
    }
}
