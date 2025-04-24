package org.readium.r2.navigator.input

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@ExperimentalReadiumApi
public interface InputListener {
    /**
     * Called when the user tapped the content, but nothing handled the event internally (eg.
     * by following an internal link).
     */
    public fun onTap(event: TapEvent, clickedBlank: Boolean = true, clickedLocator: Locator? = null): Boolean = false

    /**
     * Called when the user dragged the content, but nothing handled the event internally.
     */
    public fun onDrag(event: DragEvent): Boolean = false

    /**
     * Called when the user pressed or released a key, but nothing handled the event internally.
     */
    public fun onKey(event: KeyEvent): Boolean = false
}

@OptIn(ExperimentalReadiumApi::class)
internal class CompositeInputListener : InputListener {
    private val listeners = mutableListOf<InputListener>()

    fun add(listener: InputListener) {
        listeners.add(listener)
    }

    fun remove(listener: InputListener) {
        listeners.remove(listener)
    }

    override fun onTap(event: TapEvent, clickedBlank: Boolean, clickedLocator: Locator?): Boolean =
        listeners.any { it.onTap(event, clickedBlank, clickedLocator) }

    override fun onDrag(event: DragEvent): Boolean =
        listeners.any { it.onDrag(event) }

    override fun onKey(event: KeyEvent): Boolean =
        listeners.any { it.onKey(event) }
}
