package com.ferdaoues.eyesopen

import com.ferdaoues.eyesopen.detection.Position
import com.ferdaoues.eyesopen.detection.Spatial
import com.ferdaoues.eyesopen.speech.Announcer
import com.ferdaoues.eyesopen.speech.Phrases
import com.ferdaoues.eyesopen.speech.Sighting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncerTest {

    private val phrases = Phrases("à gauche", "devant vous", "à droite", "très proche", "Rien", "et")
    private var now = 0L
    private val announcer = Announcer(phrases, repeatAfterMs = 5_000, clock = { now })

    private val chair = Sighting("chaise", Position.AHEAD, close = false)

    @Test
    fun `a new object is announced once, not on every frame`() {
        assertEquals("chaise devant vous", announcer.onFrame(listOf(chair), withPosition = true))
        now += 33 // next camera frame
        assertNull(announcer.onFrame(listOf(chair), withPosition = true))
    }

    @Test
    fun `the same object is repeated after the delay`() {
        announcer.onFrame(listOf(chair), withPosition = false)
        now += 5_000
        assertEquals("chaise", announcer.onFrame(listOf(chair), withPosition = false))
    }

    @Test
    fun `an object that changes side is announced again`() {
        announcer.onFrame(listOf(chair), withPosition = true)
        now += 500
        val moved = chair.copy(position = Position.LEFT)
        assertEquals("chaise à gauche", announcer.onFrame(listOf(moved), withPosition = true))
    }

    @Test
    fun `close objects are flagged and several objects are joined`() {
        val person = Sighting("personne", Position.RIGHT, close = true)
        assertEquals(
            "personne à droite, très proche et chaise devant vous",
            announcer.onFrame(listOf(person, chair), withPosition = true),
        )
    }

    @Test
    fun `describe scene works even with an empty frame`() {
        assertEquals("Rien", announcer.describeScene(emptyList(), withPosition = true))
    }

    @Test
    fun `spatial helpers split the frame in thirds`() {
        assertEquals(Position.LEFT, Spatial.position(0.1f))
        assertEquals(Position.AHEAD, Spatial.position(0.5f))
        assertEquals(Position.RIGHT, Spatial.position(0.9f))
        assertTrue(Spatial.isClose(0.6f, 0.6f))
        assertFalse(Spatial.isClose(0.2f, 0.2f))
    }
}
