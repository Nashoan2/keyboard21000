package org.fossify.keyboard.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.removeUnderlines
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.keyboard.R
import org.fossify.keyboard.databinding.ItemClipOnKeyboardBinding
import org.fossify.keyboard.databinding.ItemSectionLabelBinding
import org.fossify.keyboard.extensions.clipsDB
import org.fossify.keyboard.extensions.getCurrentClip
import org.fossify.keyboard.helpers.ClipsHelper
import org.fossify.keyboard.helpers.ITEM_CLIP
import org.fossify.keyboard.helpers.ITEM_SECTION_LABEL
import org.fossify.keyboard.interfaces.RefreshClipsListener
import org.fossify.keyboard.models.Clip
import org.fossify.keyboard.models.ClipsSectionLabel
import org.fossify.keyboard.models.ListItem

class ClipsKeyboardAdapter(
    val context: Context,
    var items: ArrayList<ListItem>,
    val refreshClipsListener: RefreshClipsListener,
    val itemClick: (clip: Clip) -> Unit
) : RecyclerView.Adapter<ClipsKeyboardAdapter.ViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when (viewType) {
            ITEM_SECTION_LABEL -> ItemSectionLabelBinding.inflate(layoutInflater, parent, false)
            else -> ItemClipOnKeyboardBinding.inflate(layoutInflater, parent, false)
        }

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bindView(item) { itemView ->
            when (item) {
                is Clip -> setupClip(itemView, item)
                is ClipsSectionLabel -> setupSection(itemView, item)
            }

            (itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams).isFullSpan = item is ClipsSectionLabel
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when {
        items[position] is ClipsSectionLabel -> ITEM_SECTION_LABEL
        else -> ITEM_CLIP
    }

    private fun setupClip(view: View, clip: Clip) {
        ItemClipOnKeyboardBinding.bind(view).apply {
            clipValue.apply {
                text = clip.value
                removeUnderlines()
            }

            clipPinIndicator.visibility = if (clip.isPinned) View.VISIBLE else View.GONE

            root.setOnClickListener {
                itemClick.invoke(clip)
            }

            root.setOnLongClickListener {
                showClipOptionsPopup(clip, view)
                true
            }

            clipValue.setOnClickListener {
                itemClick.invoke(clip)
            }

            clipValue.setOnLongClickListener {
                showClipOptionsPopup(clip, view)
                true
            }

            clipPinIndicator.setOnClickListener {
                showClipOptionsPopup(clip, view)
            }
        }
    }

    private fun togglePinClip(clip: Clip) {
        ensureBackgroundThread {
            val newPinned = !clip.isPinned
            if (clip.id != null && clip.id != -1L) {
                context.clipsDB.updatePinned(clip.id!!, newPinned)
            } else {
                val newClip = Clip(null, clip.value, newPinned)
                ClipsHelper(context).insertClip(newClip)
            }
            Handler(Looper.getMainLooper()).post {
                context.toast(if (newPinned) R.string.text_pinned else R.string.text_unpinned)
                refreshClipsListener.refreshClips()
            }
        }
    }

    private fun deleteClip(clip: Clip) {
        if (clip.isPinned) {
            // CRITICAL: Pinned clips MUST NOT be deleted unless unpinned first!
            context.toast(R.string.cannot_delete_pinned)
            return
        }
        ensureBackgroundThread {
            if (clip.id != null && clip.id != -1L) {
                context.clipsDB.delete(clip.id!!)
            }
            Handler(Looper.getMainLooper()).post {
                refreshClipsListener.refreshClips()
            }
        }
    }

    /**
     * Shows a persistent options popup that does NOT disappear when lifting the finger.
     */
    fun showClipOptionsPopup(clip: Clip, anchorView: View) {
        val popupView = layoutInflater.inflate(R.layout.dialog_clip_options, null)

        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 20f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
        }

        val clipTextPreview = popupView.findViewById<TextView>(R.id.popup_clip_preview)
        val btnPin = popupView.findViewById<View>(R.id.popup_btn_pin)
        val pinText = popupView.findViewById<TextView>(R.id.popup_pin_text)
        val pinIcon = popupView.findViewById<ImageView>(R.id.popup_pin_icon)
        val btnDelete = popupView.findViewById<View>(R.id.popup_btn_delete)
        val btnCancel = popupView.findViewById<View>(R.id.popup_btn_cancel)

        clipTextPreview.text = clip.value
        if (clip.isPinned) {
            pinText.setText(R.string.unpin_text)
            pinIcon.setImageResource(R.drawable.ic_pin_cyan)
        } else {
            pinText.setText(R.string.pin_text)
            pinIcon.setImageResource(R.drawable.ic_pin_outline_gray)
        }

        btnPin.setOnClickListener {
            popup.dismiss()
            togglePinClip(clip)
        }

        btnDelete.setOnClickListener {
            popup.dismiss()
            deleteClip(clip)
        }

        btnCancel.setOnClickListener {
            popup.dismiss()
        }

        popup.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupSection(view: View, sectionLabel: ClipsSectionLabel) {
        ItemSectionLabelBinding.bind(view).apply {
            clipsSectionLabel.apply {
                text = sectionLabel.value
                setTextColor(Color.parseColor("#00D2FF"))
            }

            clipsSectionIcon.apply {
                if (sectionLabel.isCurrent) {
                    visibility = View.VISIBLE
                    setImageResource(R.drawable.ic_pin_cyan)
                    setOnClickListener {
                        ensureBackgroundThread {
                            val currentClip = context.getCurrentClip() ?: return@ensureBackgroundThread
                            val clip = Clip(null, currentClip)
                            ClipsHelper(context).insertClip(clip)
                            Handler(Looper.getMainLooper()).post {
                                refreshClipsListener.refreshClips()
                                context.toast(R.string.text_pinned)
                            }
                        }
                    }
                } else {
                    visibility = View.GONE
                }
            }
        }
    }

    open inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(any: Any, callback: (itemView: View) -> Unit): View {
            return itemView.apply {
                callback(this)

                if (any is Clip) {
                    setOnClickListener {
                        itemClick.invoke(any)
                    }
                    setOnLongClickListener {
                        showClipOptionsPopup(any, it)
                        true
                    }
                }
            }
        }
    }
}
