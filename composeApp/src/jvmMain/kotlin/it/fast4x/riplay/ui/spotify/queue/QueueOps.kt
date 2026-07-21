package it.fast4x.riplay.ui.spotify.queue

import it.fast4x.riplay.player.QueuePlayer

/**
 * Queue-editing surface [QueuePanel] needs. Kept separate from [QueuePlayer] on purpose (see
 * CONTRAINTE in the F2 ticket: QueuePlayer is owned by the lead and must not change here).
 *
 * [QueuePlayer] doesn't implement this today — [asEditable] adapts it using only QueuePlayer's
 * public API. That adapter is imperfect for [removeAt]/[move] (see kdoc there); the exact
 * methods that would let QueuePlayer implement this cleanly are listed on [asEditable].
 */
interface QueueEditable {
    /** Jump playback to the item at [index] in the current queue. */
    fun playAt(index: Int)

    /** Remove the item at [index] from the queue. */
    fun removeAt(index: Int)

    /** Move the item at [from] to position [to] (list-index semantics, like `MutableList.add` after removal). */
    fun move(from: Int, to: Int)
}

/** Delegates to [QueuePlayer]'s native queue-editing methods, which keep the current track playing. */
fun QueuePlayer.asEditable(): QueueEditable = object : QueueEditable {
    override fun playAt(index: Int) = this@asEditable.playAt(index)
    override fun removeAt(index: Int) = this@asEditable.removeAt(index)
    override fun move(from: Int, to: Int) = this@asEditable.move(from, to)
}
