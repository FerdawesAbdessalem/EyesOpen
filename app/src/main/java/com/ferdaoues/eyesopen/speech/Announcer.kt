package com.ferdaoues.eyesopen.speech

import com.ferdaoues.eyesopen.detection.Position

/** What the announcer needs to know about a detected object (no Android types → unit-testable). */
data class Sighting(val label: String, val position: Position, val close: Boolean)

/** Words used in the spoken sentences (provided from string resources). */
data class Phrases(
    val left: String,
    val ahead: String,
    val right: String,
    val veryClose: String,
    val nothing: String,
    val and: String,
)

/**
 * Decides **what** to say and **when**.
 *
 * The first version spoke every label on every camera frame (~30 times per
 * second), so the speech queue never stopped. Here an object is announced
 * only when it appears, when it moves to another side, or after [repeatAfterMs].
 */
class Announcer(
    private val phrases: Phrases,
    private val repeatAfterMs: Long = 6_000,
    private val maxPerAnnouncement: Int = 2,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Memory(val position: Position, val at: Long)

    private val lastSeen = mutableMapOf<String, Memory>()

    /** Returns the sentence to speak for this frame, or null if nothing new. */
    fun onFrame(sightings: List<Sighting>, withPosition: Boolean): String? {
        val now = clock()
        val fresh = sightings
            .distinctBy { it.label }
            .filter { s ->
                val memory = lastSeen[s.label]
                memory == null || now - memory.at >= repeatAfterMs || (withPosition && memory.position != s.position)
            }
            .take(maxPerAnnouncement)

        if (fresh.isEmpty()) return null
        fresh.forEach { lastSeen[it.label] = Memory(it.position, now) }
        return fresh.joinToString(" ${phrases.and} ") { describe(it, withPosition) }
    }

    /** Full description on demand ("What do I see?"), ignoring the anti-repeat memory. */
    fun describeScene(sightings: List<Sighting>, withPosition: Boolean): String {
        val unique = sightings.distinctBy { it.label }
        if (unique.isEmpty()) return phrases.nothing
        return unique.take(4).joinToString(", ") { describe(it, withPosition) }
    }

    fun reset() = lastSeen.clear()

    private fun describe(s: Sighting, withPosition: Boolean): String = buildString {
        append(s.label)
        if (withPosition) {
            append(' ')
            append(
                when (s.position) {
                    Position.LEFT -> phrases.left
                    Position.AHEAD -> phrases.ahead
                    Position.RIGHT -> phrases.right
                }
            )
        }
        if (s.close) append(", ").append(phrases.veryClose)
    }
}
