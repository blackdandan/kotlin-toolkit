/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.adapter.pdfium.navigator

import PdfPageAdapter
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.navigator.pdf.PdfDocumentFragment
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.SingleJob
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toDebugDescription
import timber.log.Timber

@ExperimentalReadiumApi
@OptIn(InternalReadiumApi::class)
public class PdfiumDocumentFragment internal constructor(
    private val publication: Publication,
    private val href: Url,
    private val initialPageIndex: Int,
    initialSettings: PdfiumSettings,
    private val listener: Listener?,
) : PdfDocumentFragment<PdfiumSettings>() {

    // Dummy constructor to address https://github.com/readium/kotlin-toolkit/issues/395
    public constructor() : this(
        publication = Publication(
            manifest = Manifest(
                metadata = Metadata(
                    identifier = "readium:dummy",
                    localizedTitle = LocalizedString("")
                )
            )
        ),
        href = Url("publication.pdf")!!,
        initialPageIndex = 0,
        initialSettings = PdfiumSettings(
            fit = Fit.WIDTH,
            pageSpacing = 0.0,
            readingProgression = ReadingProgression.LTR,
            scrollAxis = Axis.VERTICAL
        ),
        listener = null
    )

    internal interface Listener {
        fun onResourceLoadFailed(href: Url, error: ReadError)
        fun onConfigurePdfView()
        fun onTap(point: PointF): Boolean
        fun onDrag(motionEvent: MotionEvent, start: PointF): Boolean
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PdfPageAdapter
    private var snapHelper: PagerSnapHelper? = null
    private val pageChangeListener = PdfPageChangeListener()

    private inner class PdfPageChangeListener : RecyclerView.OnScrollListener() {
        private var lastVisiblePosition = -1

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            val firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition()
            
            if (firstVisiblePosition != -1 && firstVisiblePosition != lastVisiblePosition) {
                lastVisiblePosition = firstVisiblePosition
                _pageIndex.value = firstVisiblePosition
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        recyclerView = RecyclerView(inflater.context).apply {
            layoutManager = LinearLayoutManager(inflater.context).apply {
                orientation = if (settings.scrollAxis == Axis.HORIZONTAL) {
                    LinearLayoutManager.HORIZONTAL
                } else {
                    LinearLayoutManager.VERTICAL
                }
            }
        }
        return recyclerView
    }

    @OptIn(InternalReadiumApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.addOnScrollListener(pageChangeListener)
        
        resetJob = SingleJob(viewLifecycleOwner.lifecycleScope)
        reset(pageIndex = initialPageIndex)
    }

    override fun onDestroyView() {
        recyclerView.removeOnScrollListener(pageChangeListener)
        super.onDestroyView()
    }

    private lateinit var resetJob: SingleJob

    private fun reset(pageIndex: Int = _pageIndex.value) {
        if (view == null) return
        val context = context?.applicationContext ?: return

        resetJob.launch {
            val resource = requireNotNull(publication.get(href))
            val document = PdfiumDocumentFactory(context)
                .open(resource, null)
                .getOrElse { error ->
                    Timber.e(error.toDebugDescription())
                    listener?.onResourceLoadFailed(href, error)
                    return@launch
                }

            pageCount = document.pageCount
            adapter = PdfPageAdapter(requireContext(), pdfiumCore = document.core, pdfDocument = document.document, pageCount = pageCount, settings.scrollAxis == Axis.HORIZONTAL)
            recyclerView.layoutManager = LinearLayoutManager(context).apply {
                orientation = if (settings.scrollAxis == Axis.HORIZONTAL) {
                    LinearLayoutManager.HORIZONTAL
                } else {
                    LinearLayoutManager.VERTICAL
                }
            }

            recyclerView.adapter = adapter
            // Set the initial page index
            (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPosition(pageIndex)

            // Handle snap effect for horizontal scrolling
            if (settings.scrollAxis == Axis.HORIZONTAL) {
                snapHelper = PagerSnapHelper()
                snapHelper?.attachToRecyclerView(recyclerView)
                // Ensure one page scroll at a time in horizontal mode
                (recyclerView.layoutManager as? LinearLayoutManager)?.isSmoothScrollbarEnabled = false
            } else {
                // Remove snap effect for vertical scrolling
                snapHelper?.attachToRecyclerView(null)
                (recyclerView.layoutManager as? LinearLayoutManager)?.isSmoothScrollbarEnabled = true
            }

            adapter.setonTapListener { point ->
                listener?.onTap(point) ?: false
            }
            adapter.setOnDragListener { motionEvent, start ->
                listener?.onDrag(motionEvent, start)
            }
        }
    }

    private var pageCount = 0

    private val _pageIndex = MutableStateFlow(initialPageIndex)
    override val pageIndex: StateFlow<Int> = _pageIndex.asStateFlow()

    override fun goToPageIndex(index: Int, animated: Boolean): Boolean {
        if (!isValidPageIndex(index)) {
            return false
        }
        (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
        _pageIndex.value = index
        return true
    }

    private fun isValidPageIndex(pageIndex: Int): Boolean {
        val validRange = 0 until pageCount
        return validRange.contains(pageIndex)
    }

    private fun convertPageIndexToView(page: Int): Int {
        var index = (page - 1).coerceAtLeast(0)
        if (isPagesOrderReversed) {
            index = (pageCount - 1) - index
        }
        return index
    }

    private fun convertPageIndexFromView(index: Int): Int {
        var page = index + 1
        if (isPagesOrderReversed) {
            page = (pageCount + 1) - page
        }
        return page
    }

    /**
     * Indicates whether the order of the [PDFView] pages is reversed to take into account
     * right-to-left reading progressions.
     */
    private val isPagesOrderReversed: Boolean get() =
        settings.scrollAxis == Axis.HORIZONTAL && settings.readingProgression == ReadingProgression.RTL

    private var settings: PdfiumSettings = initialSettings

    override fun applySettings(settings: PdfiumSettings) {
        if (this.settings == settings) {
            return
        }

        this.settings = settings
        reset()
    }
}
