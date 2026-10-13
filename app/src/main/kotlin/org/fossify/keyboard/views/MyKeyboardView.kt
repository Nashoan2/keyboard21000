package org.fossify.keyboard.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.inline.InlineContentView
import androidx.annotation.RequiresApi
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.updateMarginsRelative
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.EMOJI_SUPPORTED
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beInvisibleIf
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.darkenColor
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.lightenColor
import org.fossify.commons.extensions.removeUnderlines
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.FontHelper
import org.fossify.commons.helpers.HIGHER_ALPHA
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isPiePlus
import org.fossify.keyboard.R
import org.fossify.keyboard.activities.ManageClipboardItemsActivity
import org.fossify.keyboard.activities.SettingsActivity
import org.fossify.keyboard.adapters.ClipsKeyboardAdapter
import org.fossify.keyboard.adapters.EmojisAdapter
import org.fossify.keyboard.databinding.ItemEmojiCategoryBinding
import org.fossify.keyboard.databinding.KeyboardKeyPreviewBinding
import org.fossify.keyboard.databinding.KeyboardPopupKeyboardBinding
import org.fossify.keyboard.databinding.KeyboardViewKeyboardBinding
import org.fossify.keyboard.dialogs.SwitchLanguageDialog
import org.fossify.keyboard.extensions.clipsDB
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.getCurrentClip
import org.fossify.keyboard.extensions.getCurrentVoiceInputMethod
import org.fossify.keyboard.extensions.getKeyboardBackgroundColor
import org.fossify.keyboard.extensions.getStrokeColor
import org.fossify.keyboard.extensions.isDeviceLocked
import org.fossify.keyboard.extensions.onScroll
import org.fossify.keyboard.extensions.safeStorageContext
import org.fossify.keyboard.helpers.AccessHelper
import org.fossify.keyboard.helpers.EMOJI_SPEC_FILE_PATH
import org.fossify.keyboard.helpers.EmojiData
import org.fossify.keyboard.helpers.EmbeddedVoiceManager
import org.fossify.keyboard.helpers.KeyboardFeedbackManager
import org.fossify.keyboard.helpers.LANGUAGE_ARABIC
import org.fossify.keyboard.helpers.LANGUAGE_TURKISH_Q
import org.fossify.keyboard.helpers.ClipsHelper
import org.fossify.keyboard.helpers.MAX_KEYS_PER_MINI_ROW
import org.fossify.keyboard.helpers.MyKeyboard
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_DELETE
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_EMOJI_OR_LANGUAGE
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_ENTER
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_MODE_CHANGE
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_POPUP_EMOJI
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_POPUP_SETTINGS
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_SHIFT
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_SPACE
import org.fossify.keyboard.helpers.MyKeyboard.Companion.KEYCODE_SYMBOLS_MODE_CHANGE
import org.fossify.keyboard.helpers.RECENTLY_USED_EMOJIS
import org.fossify.keyboard.helpers.ShiftState
import org.fossify.keyboard.helpers.getCategoryIconRes
import org.fossify.keyboard.helpers.parseRawEmojiSpecsFile
import org.fossify.keyboard.interfaces.OnKeyboardActionListener
import org.fossify.keyboard.interfaces.RefreshClipsListener
import org.fossify.keyboard.models.Clip
import org.fossify.keyboard.models.ClipsSectionLabel
import org.fossify.keyboard.models.ListItem
import java.util.Arrays
import java.util.Locale

@SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
class MyKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleRes) {

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return if (accessHelper?.dispatchHoverEvent(event) == true) {
            true
        } else {
            super.dispatchHoverEvent(event)
        }
    }

    private var keyboardPopupBinding: KeyboardPopupKeyboardBinding? = null
    private var keyboardViewBinding: KeyboardViewKeyboardBinding? = null

    private var accessHelper: AccessHelper? = null
    private val feedbackManager by lazy { KeyboardFeedbackManager(context) }
    private var clipsTouchHelper: ItemTouchHelper? = null

    private var mKeyboard: MyKeyboard? = null
    private var mCurrentKeyIndex: Int = NOT_A_KEY

    private var mLabelTextSize = 0
    private var mKeyTextSize = 0
    private var mSpaceBarTextSize = 0

    private var mTextColor = 0
    private var mBackgroundColor = 0
    private var mKeyboardBackgroundColor = 0
    private var mPrimaryColor = 0
    private var mStrokeColor = 0
    private var mKeyColor = 0
    private var mKeyColorPressed = 0

    private var mPreviewText: TextView? = null
    private val mPreviewPopup: PopupWindow
    private var mPreviewTextSizeLarge = 0
    private var mPreviewHeight = 0

    private val mCoordinates = IntArray(2)
    private val mPopupKeyboard: PopupWindow
    private var mMiniKeyboardContainer: View? = null
    private var mMiniKeyboard: MyKeyboardView? = null
    private var mMiniKeyboardOnScreen = false
    private var mPopupParent: View
    private var mMiniKeyboardOffsetX = 0
    private var mMiniKeyboardOffsetY = 0
    private val mMiniKeyboardCache: MutableMap<MyKeyboard.Key, View?>
    private var mKeys = ArrayList<MyKeyboard.Key>()
    private var mMiniKeyboardSelectedKeyIndex = -1

    var mOnKeyboardActionListener: OnKeyboardActionListener? = null
    private var mVerticalCorrection = 0
    private var mProximityThreshold = 0
    private var mPopupPreviewX = 0
    private var mPopupPreviewY = 0
    private var mLastX = 0
    private var mLastY = 0

    private val mPaint: Paint
    private var mDownTime = 0L
    private var mLastMoveTime = 0L
    private var mLastKey = 0
    private var mLastCodeX = 0
    private var mLastCodeY = 0
    private var mLastKeyPressedCode = 0
    private var mCurrentKey: Int = NOT_A_KEY
    private var mLastKeyTime = 0L
    private var mCurrentKeyTime = 0L
    private val mKeyIndices = IntArray(12)
    private var mPopupX = 0
    private var mPopupY = 0
    private var mRepeatKeyIndex = NOT_A_KEY
    private var mRepeatCount = 0
    private var mPopupLayout = 0
    private var mAbortKey = false
    private var mCursorControlActive = false
    private var mLastSpaceMoveX = 0
    private var mPopupMaxMoveDistance = 0f
    private var mTopSmallNumberSize = 0f
    private var mTopSmallNumberMarginWidth = 0f
    private var mTopSmallNumberMarginHeight = 0f
    private val mSpaceMoveThreshold: Int
    private var ignoreTouches = false

    private var mKeyBackground: Drawable? = null
    private var mShowKeyBorders: Boolean = false
    private var mUsingSystemTheme: Boolean = true
    private var mVoiceInputMethod: String = ""

    private var mToolbarHolder: View? = null
    private var mClipboardManagerHolder: View? = null
    private var mEmojiPaletteHolder: View? = null
    private var mToolsMenuHolder: View? = null
    private var mTextEditorHolder: View? = null
    private var emojiCompatMetadataVersion = 0

    // For multi-tap
    private var mLastTapTime = 0L

    /** Whether the keyboard bitmap needs to be redrawn before it's blitted.  */
    private var mDrawPending = false

    /** The dirty region in the keyboard bitmap  */
    private val mDirtyRect = Rect()

    /** The keyboard bitmap for faster updates  */
    private var mBuffer: Bitmap? = null

    /** Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer.  */
    private var mKeyboardChanged = false

    /** The canvas for the above mutable keyboard bitmap  */
    private var mCanvas: Canvas? = null

    private var mHandler: Handler? = null

    private val mVoiceManager = EmbeddedVoiceManager(context)

    companion object {
        private const val NOT_A_KEY = -1
        private val LONG_PRESSABLE_STATE_SET = intArrayOf(R.attr.state_long_pressable)
        private const val MSG_REMOVE_PREVIEW = 1
        private const val MSG_REPEAT = 2
        private const val MSG_LONGPRESS = 3
        private const val DELAY_AFTER_PREVIEW = 100
        private const val DEBOUNCE_TIME = 70
        private const val REPEAT_INTERVAL = 45
        private const val REPEAT_START_DELAY = 280
        private const val DELETE_REPEAT_START_DELAY = 160
        private val LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()
    }

    init {
        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.MyKeyboardView, 0, defStyleRes)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val keyTextSize = 0
        val indexCnt = attributes.indexCount

        try {
            for (i in 0 until indexCnt) {
                when (val attr = attributes.getIndex(i)) {
                    R.styleable.MyKeyboardView_keyTextSize -> mKeyTextSize =
                        attributes.getDimensionPixelSize(attr, 18)
                }
            }
        } finally {
            attributes.recycle()
        }

        mPopupLayout = R.layout.keyboard_popup_keyboard
        mKeyBackground = resources.getDrawable(R.drawable.keyboard_key_selector, context.theme)
        mVerticalCorrection = resources.getDimension(R.dimen.vertical_correction).toInt()
        mLabelTextSize = resources.getDimension(R.dimen.label_text_size).toInt()
        mSpaceBarTextSize = resources.getDimension(R.dimen.space_bar_text_size).toInt()
        mPreviewHeight = resources.getDimension(R.dimen.key_height).toInt()
        mSpaceMoveThreshold = resources.getDimension(R.dimen.medium_margin).toInt()

        with(safeStorageContext) {
            mTextColor = getProperTextColor()
            mBackgroundColor = getProperBackgroundColor()
            mKeyboardBackgroundColor = getKeyboardBackgroundColor()
            mPrimaryColor = getProperPrimaryColor()
            mStrokeColor = getStrokeColor()
        }

        mPreviewPopup = PopupWindow(context)
        mPreviewText = KeyboardKeyPreviewBinding.inflate(inflater).root
        mPreviewTextSizeLarge = context.resources.getDimension(R.dimen.preview_text_size).toInt()
        mPreviewPopup.contentView = mPreviewText
        mPreviewPopup.setBackgroundDrawable(null)

        mPreviewPopup.isTouchable = false
        mPopupKeyboard = PopupWindow(context)
        mPopupKeyboard.setBackgroundDrawable(null)
        mPopupParent = this
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.textSize = keyTextSize.toFloat()
        mPaint.textAlign = Align.CENTER
        mPaint.alpha = 255
        mMiniKeyboardCache = HashMap()
        mPopupMaxMoveDistance = resources.getDimension(R.dimen.popup_max_move_distance)
        mTopSmallNumberSize = resources.getDimension(R.dimen.small_text_size)
        mTopSmallNumberMarginWidth = resources.getDimension(R.dimen.top_small_number_margin_width)
        mTopSmallNumberMarginHeight = resources.getDimension(R.dimen.top_small_number_margin_height)
    }

    @SuppressLint("HandlerLeak")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mHandler == null) {
            mHandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        MSG_REMOVE_PREVIEW -> mPreviewText!!.visibility = INVISIBLE
                        MSG_REPEAT -> if (repeatKey()) {
                            val repeat = Message.obtain(this, MSG_REPEAT)
                            sendMessageDelayed(repeat, getNextRepeatDelay())
                        }

                        MSG_LONGPRESS -> openPopupIfRequired(msg.obj as MotionEvent)
                    }
                }
            }
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        closeClipboardManager()
        closeEmojiPalette()

        if (visibility == VISIBLE) {
            setupKeyboard(changedView)
        }
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the view will re-layout itself to accommodate the keyboard.
     * @param keyboard the keyboard to display in this view
     */
    fun setKeyboard(keyboard: MyKeyboard) {
        if (mKeyboard != null) {
            showPreview(NOT_A_KEY)
        }

        closeClipboardManager()
        removeMessages()
        mKeyboard = keyboard
        val keys = mKeyboard!!.mKeys
        mKeys = keys!!.toMutableList() as ArrayList<MyKeyboard.Key>
        requestLayout()
        mKeyboardChanged = true
        invalidateAllKeys()
        computeProximityThreshold(keyboard)
        mMiniKeyboardCache.clear()
        mToolbarHolder?.beInvisibleIf(context.isDeviceLocked)

        accessHelper = AccessHelper(this, mKeyboard?.mKeys.orEmpty())
        ViewCompat.setAccessibilityDelegate(this, accessHelper)

        // Not really necessary to do every time, but will free up views
        // Switching to a different keyboard should abort any pending keys so that the key up
        // doesn't get delivered to the old or new keyboard
        mAbortKey = true // Until the next ACTION_DOWN
    }

    /** Sets the top row above the keyboard containing a couple buttons and the clipboard **/
    fun setKeyboardHolder(binding: KeyboardViewKeyboardBinding) {
        keyboardViewBinding = binding.apply {
            mToolbarHolder = toolbarHolder
            mClipboardManagerHolder = clipboardManagerHolder
            mEmojiPaletteHolder = emojiPaletteHolder
            mToolsMenuHolder = toolsMenuHolder
            mTextEditorHolder = textEditorHolder

            toolbarSticker.setOnClickListener {
                if (toolsMenuHolder.visibility == View.VISIBLE) {
                    closeAllPanels()
                } else {
                    openToolsMenu()
                }
            }

            toolbarEmoji.setOnClickListener {
                if (emojiPaletteHolder.visibility == View.VISIBLE) {
                    closeAllPanels()
                } else {
                    openEmojiPalette()
                }
            }

            voiceInputButton.setOnLongClickListener { context.toast(R.string.switch_to_voice_typing); true }
            voiceInputButton.setOnClickListener {
                if (mVoiceManager.isListening) {
                    mVoiceManager.stopListening()
                } else {
                    val started = mVoiceManager.startListening(
                        onListeningState = { listening ->
                            setVoiceListeningState(listening)
                        },
                        onResult = { text ->
                            mOnKeyboardActionListener?.onText("$text ")
                        }
                    )
                    if (!started && !mVoiceManager.hasPermission()) {
                        mVoiceManager.requestPermission()
                    }
                }
            }

            toolbarCursor.setOnClickListener {
                if (textEditorHolder.visibility == View.VISIBLE) {
                    closeAllPanels()
                } else {
                    openTextEditor()
                }
            }

            pinnedClipboardItems.setOnLongClickListener { context.toast(R.string.clipboard); true; }
            pinnedClipboardItems.setOnClickListener {
                if (clipboardManagerHolder.visibility == View.VISIBLE) {
                    closeAllPanels()
                } else {
                    openClipboardManager()
                }
            }

            toolbarCollapse.setOnClickListener {
                vibrateIfNeeded()
                closeAllPanels()
                (context as? android.inputmethodservice.InputMethodService)?.requestHideSelf(0)
            }

            // Clipboard Panel actions
            clipboardManagerClose.setOnClickListener {
                vibrateIfNeeded()
                closeAllPanels()
            }

            clipboardManagerManage.setOnClickListener {
                vibrateIfNeeded()
                Intent(context, ManageClipboardItemsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(this)
                }
            }

            clipboardManagerClearAll.setOnClickListener {
                ensureBackgroundThread {
                    val unpinned = context.clipsDB.getRecentClips()
                    for (clip in unpinned) {
                        ClipsHelper.clearSystemClipboardIfMatches(context, clip.value)
                    }
                    context.clipsDB.deleteAll()
                    Handler(Looper.getMainLooper()).post {
                        context.toast(R.string.clear_clipboard_data)
                        setupStoredClips()
                    }
                }
            }

            clipboardManagerCollapse.setOnClickListener {
                closeAllPanels()
                (context as? android.inputmethodservice.InputMethodService)?.requestHideSelf(0)
            }

            // Tools Menu actions (Image 90)
            toolsMenuClose.setOnClickListener {
                vibrateIfNeeded()
                closeAllPanels()
            }

            toolsMenuSettings.setOnClickListener {
                vibrateIfNeeded()
                Intent(context, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(this)
                }
            }

            toolItemEditor.setOnClickListener {
                vibrateIfNeeded()
                openTextEditor()
            }

            toolItemResize.setOnClickListener {
                vibrateIfNeeded()
                Intent(context, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(this)
                }
            }

            toolItemTheme.setOnClickListener {
                vibrateIfNeeded()
                Intent(context, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(this)
                }
            }

            toolItemLanguages.setOnClickListener {
                vibrateIfNeeded()
                SwitchLanguageDialog(this@MyKeyboardView) {
                    mOnKeyboardActionListener?.reloadKeyboard()
                }
            }

            // Text Editor / Cursor panel actions
            cursorBtnAbc.setOnClickListener {
                vibrateIfNeeded()
                closeAllPanels()
            }
            cursorBtnAbcHolder.setOnClickListener {
                vibrateIfNeeded()
                closeAllPanels()
            }

            cursorBtnClipboard.setOnClickListener {
                vibrateIfNeeded()
                openClipboardManager()
            }

            cursorBtnUp.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.moveCursorUp()
            }

            cursorBtnDown.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.moveCursorDown()
            }

            cursorBtnLeft.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.moveCursorLeft()
            }

            cursorBtnRight.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.moveCursorRight()
            }

            cursorBtnSelect.setOnClickListener {
                vibrateIfNeeded()
                val active = mOnKeyboardActionListener?.toggleSelectMode() ?: false
                cursorBtnSelect.setTextColor(if (active) Color.parseColor("#00E676") else Color.parseColor("#00BCD4"))
            }

            cursorBtnSelectAll.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.selectAll()
            }

            cursorBtnCopy.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.copyText()
            }

            cursorBtnCut.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.cutText()
            }

            cursorBtnPaste.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.pasteText()
            }

            cursorBtnEnd.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.moveToEnd()
            }

            cursorBtnBackspace.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        cursorBtnBackspace.isPressed = true
                        mRepeatKeyIndex = mKeys.indexOfFirst { it.code == KEYCODE_DELETE }
                        mCurrentKey = mRepeatKeyIndex
                        mRepeatCount = 0
                        vibrateIfNeeded()
                        mOnKeyboardActionListener?.onKey(KEYCODE_DELETE)
                        val msg = mHandler?.obtainMessage(MSG_REPEAT)
                        if (msg != null) mHandler?.sendMessageDelayed(msg, DELETE_REPEAT_START_DELAY.toLong())
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        resetRepeat()
                        cursorBtnBackspace.isPressed = false
                        false
                    }
                    else -> false
                }
            }

            cursorBtnSpace.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.insertSpace()
            }

            cursorBtnUndo.setOnClickListener {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.toggleSelectMode()
            }

            // Numeric Keypad actions (Matches image 100%)
            fun sendNumericChar(char: String) {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.onText(char)
            }

            fun sendNumericCode(code: Int) {
                vibrateIfNeeded()
                mOnKeyboardActionListener?.onKey(code)
            }

            numKeyPlus.setOnClickListener { sendNumericChar("+") }
            numKey1.setOnClickListener { sendNumericChar("1") }
            numKey2.setOnClickListener { sendNumericChar("2") }
            numKey3.setOnClickListener { sendNumericChar("3") }
            numKeyPercent.setOnClickListener { sendNumericChar("%") }

            numKeyMinus.setOnClickListener { sendNumericChar("-") }
            numKey4.setOnClickListener { sendNumericChar("4") }
            numKey5.setOnClickListener { sendNumericChar("5") }
            numKey6.setOnClickListener { sendNumericChar("6") }
            numKeySpace.setOnClickListener { sendNumericCode(KEYCODE_SPACE) }

            numKeyMultiply.setOnClickListener { sendNumericChar("*") }
            numKey7.setOnClickListener { sendNumericChar("7") }
            numKey8.setOnClickListener { sendNumericChar("8") }
            numKey9.setOnClickListener { sendNumericChar("9") }

            numKeyBackspace.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        numKeyBackspace.isPressed = true
                        mRepeatKeyIndex = mKeys.indexOfFirst { it.code == KEYCODE_DELETE }
                        mCurrentKey = mRepeatKeyIndex
                        mRepeatCount = 0
                        vibrateIfNeeded()
                        mOnKeyboardActionListener?.onKey(KEYCODE_DELETE)
                        val msg = mHandler?.obtainMessage(MSG_REPEAT)
                        if (msg != null) mHandler?.sendMessageDelayed(msg, DELETE_REPEAT_START_DELAY.toLong())
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        resetRepeat()
                        numKeyBackspace.isPressed = false
                        false
                    }
                    else -> false
                }
            }

            numKeyDivide.setOnClickListener { sendNumericChar("/") }
            numKeyAbc.setOnClickListener {
                vibrateIfNeeded()
                closeNumericKeypad()
            }
            numKeySymbols.setOnClickListener {
                vibrateIfNeeded()
                closeNumericKeypad()
                mOnKeyboardActionListener?.onKey(MyKeyboard.KEYCODE_SYMBOLS_MODE_CHANGE)
            }
            numKey0.setOnClickListener { sendNumericChar("0") }
            numKeyEquals.setOnClickListener { sendNumericChar("=") }
            numKeyDot.setOnClickListener { sendNumericChar(".") }
            numKeyEnter.setOnClickListener { sendNumericCode(KEYCODE_ENTER) }

            settingsCog.setOnLongClickListener { context.toast(R.string.settings); true; }
            settingsCog.setOnClickListener {
                vibrateIfNeeded()
                Intent(context, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(this)
                }
            }

            clipboardClear.setOnLongClickListener { context.toast(R.string.clear_clipboard_data); true; }
            clipboardClear.setOnClickListener {
                vibrateIfNeeded()
                clearClipboardContent()
                toggleClipboardVisibility(false)
            }

            suggestionsHolder.addOnLayoutChangeListener(object : OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View?,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int
                ) {
                    updateSuggestionsToolbarLayout()
                    binding.suggestionsHolder.removeOnLayoutChangeListener(this)
                }
            })
        }



        binding.apply {
            clipboardManagerClose.setOnClickListener {
                vibrateIfNeeded()
                closeClipboardManager()
            }

            clipboardManagerManage.setOnLongClickListener { context.toast(R.string.manage_clipboard_items); true; }
            clipboardManagerManage.setOnClickListener {
                Intent(context, ManageClipboardItemsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(this)
                }
            }

            emojiPaletteClose.setOnClickListener {
                vibrateIfNeeded()
                closeEmojiPalette()
            }
        }
    }

    fun setEditorInfo(editorInfo: EditorInfo) {
        emojiCompatMetadataVersion =
            editorInfo.extras?.getInt(EmojiCompat.EDITOR_INFO_METAVERSION_KEY, 0) ?: 0
    }

    fun setupKeyboard(changedView: View? = null) {
        with(safeStorageContext) {
            mTextColor = Color.WHITE
            mBackgroundColor = Color.parseColor("#111622")
            mKeyboardBackgroundColor = Color.parseColor("#111622")
            mPrimaryColor = Color.parseColor("#00BCD4")
            mStrokeColor = Color.parseColor("#202737")

            mShowKeyBorders = true
            mUsingSystemTheme = false
            mVoiceInputMethod = config.voiceInputMethod
        }

        val isMainKeyboard = changedView == null || changedView.id != R.id.mini_keyboard_view
        mKeyColor = Color.parseColor("#0B1626")
        mKeyColorPressed = Color.parseColor("#162842")
        mShowKeyBorders = true
        mKeyBackground = resources.getDrawable(R.drawable.keyboard_key_selector_outlined, context.theme)

        if (!isMainKeyboard) {
            val previewBackground = background as LayerDrawable
            previewBackground.findDrawableByLayerId(R.id.button_background_shape)
                .applyColorFilter(mBackgroundColor)
            previewBackground.findDrawableByLayerId(R.id.button_background_stroke)
                .applyColorFilter(mStrokeColor)
            background = previewBackground
        } else {
            background = ColorDrawable(Color.parseColor("#060B14"))
        }

        keyboardViewBinding?.apply {
            topKeyboardDivider.beGone()
            mToolbarHolder?.layoutDirection = View.LAYOUT_DIRECTION_LTR
            toolbarIconsContainer.layoutDirection = View.LAYOUT_DIRECTION_LTR
            mToolbarHolder?.background = resources.getDrawable(R.drawable.bg_toolbar_neon, context.theme)

            toolbarSticker.applyColorFilter(Color.WHITE)
            toolbarEmoji.applyColorFilter(Color.WHITE)
            if (mVoiceManager.isListening) {
                voiceInputActiveBg.visibility = View.VISIBLE
                voiceInputButton.applyColorFilter(Color.parseColor("#FF4444"))
            } else {
                voiceInputActiveBg.visibility = View.GONE
                voiceInputButton.applyColorFilter(Color.WHITE)
            }
            toolbarCursor.applyColorFilter(Color.WHITE)
            pinnedClipboardItems.applyColorFilter(Color.WHITE)
            toolbarCollapse.applyColorFilter(Color.WHITE)

            mToolbarHolder?.beInvisibleIf(context.isDeviceLocked)

            topClipboardDivider.beGone()
            clipboardManagerTopBar.background = resources.getDrawable(R.drawable.bg_toolbar_neon, context.theme)
            clipboardManagerHolder.background = ColorDrawable(Color.parseColor("#060B14"))
            clipboardManagerHolder.layoutDirection = View.LAYOUT_DIRECTION_LTR

            val isArabic = context.config.keyboardLanguage == LANGUAGE_ARABIC
            val closeLabel = if (isArabic) "أ ب ج" else "ABC"
            clipboardManagerClose.text = closeLabel
            toolsMenuClose.text = closeLabel
            cursorBtnAbc.text = closeLabel

            clipboardManagerClose.setTextColor(Color.parseColor("#00D2FF"))
            clipboardManagerManage.applyColorFilter(Color.parseColor("#00BCD4"))
            clipboardManagerClearAll.applyColorFilter(Color.parseColor("#00BCD4"))

            clipboardManagerLabel.setTextColor(Color.parseColor("#00D2FF"))
            clipboardContentPlaceholder1.setTextColor(Color.parseColor("#80A0C0"))
            clipboardContentPlaceholder2.setTextColor(Color.parseColor("#507090"))
        }

        setupEmojiPalette(
            toolbarColor = mKeyboardBackgroundColor,
            backgroundColor = mBackgroundColor,
            textColor = mTextColor
        )
        setupStoredClips()
    }

    fun vibrateIfNeeded() {
        // Disabled completely per user request
    }

    fun performKeypressFeedback(keyCode: Int) {
        feedbackManager.performKeypressFeedback(this, keyCode)
    }

    fun performHapticHandleMove() {
        // Disabled completely per user request
    }

    /**
     * Sets the state of the shift key of the keyboard, if any.
     * @param shifted whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     */
    private fun setShifted(shiftState: ShiftState) {
        if (mKeyboard?.setShifted(shiftState) == true) {
            invalidateAllKeys()
        }
    }

    /**
     * Returns the state of the shift key of the keyboard, if any.
     * @return true if the shift is in a pressed state, false otherwise
     */
    private fun isShifted(): Boolean {
        return (mKeyboard?.mShiftState ?: ShiftState.OFF) > ShiftState.OFF
    }

    private fun setPopupOffset(x: Int, y: Int) {
        mMiniKeyboardOffsetX = x
        mMiniKeyboardOffsetY = y
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
    }

    private fun adjustCase(label: CharSequence): CharSequence? {
        var newLabel: CharSequence? = label
        if (
            !newLabel.isNullOrEmpty()
            && mKeyboard!!.mShiftState != ShiftState.OFF
            && newLabel.length < 3
            && Character.isLowerCase(newLabel[0])
        ) {
            if (context.config.keyboardLanguage == LANGUAGE_TURKISH_Q) {
                newLabel = newLabel.toString().uppercase(Locale.forLanguageTag("tr"))
            } else {
                newLabel = newLabel.toString().uppercase(Locale.getDefault())
            }
        }
        return newLabel
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mKeyboard == null) {
            val defaultH = (resources.getDimension(R.dimen.key_height) * 4).toInt()
            val w = MeasureSpec.getSize(widthMeasureSpec).let { if (it > 0) it else resources.displayMetrics.widthPixels }
            setMeasuredDimension(w, defaultH)
        } else {
            var width = mKeyboard!!.mMinWidth
            val specSize = MeasureSpec.getSize(widthMeasureSpec)
            if (specSize > 0 && specSize < width + 10) {
                width = specSize
            }
            setMeasuredDimension(width, mKeyboard!!.mHeight)
        }
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and vertically) and square it to get the proximity threshold. We use a square here and
     * in computing the touch distance from a key's center to avoid taking a square root.
     * @param keyboard
     */
    private fun computeProximityThreshold(keyboard: MyKeyboard?) {
        if (keyboard == null) {
            return
        }

        val keys = mKeys
        val length = keys.size
        var dimensionSum = 0
        for (i in 0 until length) {
            val key = keys[i]
            dimensionSum += Math.min(key.width, key.height) + key.gap
        }

        if (dimensionSum < 0 || length == 0) {
            return
        }

        mProximityThreshold = (dimensionSum * 1.4f / length).toInt()
        mProximityThreshold *= mProximityThreshold // Square it
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDrawPending || mBuffer == null || mKeyboardChanged) {
            onBufferDraw()
        }
        canvas.drawBitmap(mBuffer!!, 0f, 0f, null)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun onBufferDraw() {
        if (mBuffer == null || mKeyboardChanged) {
            if (mBuffer == null || mKeyboardChanged && (mBuffer!!.width != width || mBuffer!!.height != height)) {
                // Make sure our bitmap is at least 1x1
                val width = Math.max(1, width)
                val height = Math.max(1, height)
                mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                mCanvas = Canvas(mBuffer!!)
            }
            invalidateAllKeys()
            mKeyboardChanged = false
        }

        if (mKeyboard == null) {
            return
        }

        mCanvas!!.save()
        val canvas = mCanvas
        canvas!!.clipRect(mDirtyRect)
        val paint = mPaint
        val keys = mKeys
        paint.color = mTextColor
        val customTypeface = FontHelper.getTypeface(context)
        val smallLetterPaint = Paint().apply {
            set(paint)
            color = paint.color.adjustAlpha(0.8f)
            textSize = mTopSmallNumberSize
            typeface = customTypeface
        }

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        handleClipboard()

        val keyCount = keys.size
        for (i in 0 until keyCount) {
            val key = keys[i]
            val code = key.code
            val label = adjustCase(key.label)?.toString()

            setupKeyBackground(key, code, canvas)
            if (code == KEYCODE_SPACE) {
                val density = resources.displayMetrics.density
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00E5FF")
                    strokeWidth = 3f * density
                    strokeCap = Paint.Cap.ROUND
                    style = Paint.Style.STROKE
                }
                val lineWidth = key.width * 0.45f
                val lineY = key.height - 10f * density
                val startX = (key.width - lineWidth) / 2f
                canvas.drawLine(startX, lineY, startX + lineWidth, lineY, linePaint)
            }
            val isCyanKey = label == "AR" || label == "EN"
            val textColor = when {
                key.pressed -> if (isCyanKey) Color.parseColor("#8000E5FF") else Color.WHITE.adjustAlpha(0.6f)
                isCyanKey -> Color.parseColor("#00E5FF")
                else -> Color.WHITE
            }

            // Switch the character to uppercase if shift is pressed
            if (label?.isNotEmpty() == true) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                if (label == "123" || label == "?123" || label == "AR" || label == "EN" || label == "لا") {
                    paint.textSize = mLabelTextSize.toFloat()
                } else if (code == KEYCODE_SPACE) {
                    paint.textSize = mSpaceBarTextSize.toFloat()
                } else {
                    paint.textSize = mKeyTextSize.toFloat()
                }

                paint.color = textColor

                val rows = label.split("\n")
                val textSize = paint.textSize
                val startY =
                    (key.height / 2f) + ((textSize - paint.descent()) / 2f) - ((textSize / 2f) * (rows.size - 1))
                rows.forEachIndexed { index, row ->
                    canvas.drawText(row, key.width / 2f, startY + textSize * index, paint)
                }

                if (
                    key.topSmallNumber.isNotEmpty()
                    && !(context.config.showNumbersRow && Regex("\\d").matches(key.topSmallNumber))
                ) {
                    val bounds = Rect().also {
                        smallLetterPaint.getTextBounds(
                            key.topSmallNumber,
                            0,
                            key.topSmallNumber.length,
                            it
                        )
                    }

                    smallLetterPaint.color = if (key.pressed) {
                        textColor
                    } else {
                        smallLetterPaint.color
                    }

                    canvas.drawText(
                        key.topSmallNumber,
                        key.width - bounds.width() / 2f - mTopSmallNumberMarginWidth,
                        key.y + mTopSmallNumberSize + mTopSmallNumberMarginHeight,
                        smallLetterPaint
                    )
                }

                // Draw secondary icons for label-based keys
                drawSecondaryIcon(key, canvas, textColor)

                // Turn off drop shadow
                paint.setShadowLayer(0f, 0f, 0f, 0)
            } else if (key.icon != null && mKeyboard != null) {
                if (code == KEYCODE_SHIFT) {
                    val drawableId = when (mKeyboard!!.mShiftState) {
                        ShiftState.OFF -> R.drawable.ic_caps_outline_vector
                        ShiftState.ON_ONE_CHAR -> R.drawable.ic_caps_vector
                        else -> R.drawable.ic_caps_underlined_vector
                    }
                    key.icon = resources.getDrawable(drawableId)
                }

                if (code == KEYCODE_ENTER) {
                    val enterColor = if (key.pressed) Color.WHITE.adjustAlpha(0.6f) else Color.WHITE
                    key.icon!!.applyColorFilter(enterColor)
                    key.secondaryIcon?.applyColorFilter(enterColor.adjustAlpha(0.6f))
                } else if (code == KEYCODE_DELETE) {
                    key.icon!!.alpha = if (key.pressed) 180 else 255
                } else if (
                    code in arrayOf(
                        KEYCODE_SHIFT,
                        KEYCODE_EMOJI_OR_LANGUAGE,
                        KEYCODE_POPUP_EMOJI,
                        KEYCODE_POPUP_SETTINGS
                    )
                ) {
                    key.icon!!.applyColorFilter(textColor)
                    key.secondaryIcon?.applyColorFilter(
                        if (key.pressed) {
                            textColor
                        } else {
                            mTextColor.adjustAlpha(0.6f)
                        }
                    )
                }

                val keyIcon = key.icon!!
                val secondaryIcon = key.secondaryIcon
                if (secondaryIcon != null) {
                    // When secondary icon exists, shrink main icon to 90%
                    val keyIconWidth = (keyIcon.intrinsicWidth * 0.9f).toInt()
                    val keyIconHeight = (keyIcon.intrinsicHeight * 0.9f).toInt()

                    val centerX = key.width / 2
                    val centerY = key.height / 2

                    val keyIconLeft = centerX - keyIconWidth / 2
                    val keyIconTop = centerY - keyIconHeight / 2

                    keyIcon.setBounds(
                        keyIconLeft,
                        keyIconTop,
                        keyIconLeft + keyIconWidth,
                        keyIconTop + keyIconHeight
                    )
                    keyIcon.draw(canvas)

                    drawSecondaryIcon(key, canvas, textColor)
                } else {
                    val drawableX = (key.width - keyIcon.intrinsicWidth) / 2
                    val drawableY = (key.height - keyIcon.intrinsicHeight) / 2
                    canvas.translate(drawableX.toFloat(), drawableY.toFloat())
                    keyIcon.setBounds(0, 0, keyIcon.intrinsicWidth, keyIcon.intrinsicHeight)
                    keyIcon.draw(canvas)
                    canvas.translate(-drawableX.toFloat(), -drawableY.toFloat())
                }
            }
            canvas.translate(-key.x.toFloat(), -key.y.toFloat())
        }

        // Overlay a dark rectangle to dim the keyboard
        if (mMiniKeyboardOnScreen) {
            paint.color = mKeyboardBackgroundColor.adjustAlpha(0.6f)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        mCanvas!!.restore()
        mDrawPending = false
        mDirtyRect.setEmpty()
    }

    private fun setupKeyBackground(key: MyKeyboard.Key, keyCode: Int, canvas: Canvas) {
        val keyBackground = mKeyBackground ?: return

        val bounds = keyBackground.bounds
        if (key.width != bounds.right || key.height != bounds.bottom) {
            keyBackground.setBounds(0, 0, key.width, key.height)
        }

        keyBackground.state = when {
            key.pressed -> intArrayOf(android.R.attr.state_pressed)
            key.focused -> intArrayOf(android.R.attr.state_focused)
            else -> intArrayOf()
        }

        canvas.translate(key.x.toFloat(), key.y.toFloat())
        keyBackground.draw(canvas)
    }

    private fun getSpaceKeyBackground(): Drawable? {
        if (mShowKeyBorders) {
            return mKeyBackground
        }

        val drawableId = if (mUsingSystemTheme) {
            R.drawable.keyboard_space_background_material
        } else {
            R.drawable.keyboard_space_background
        }

        return resources.getDrawable(drawableId, context.theme)
    }

    private fun getEnterKeyBackground(): Drawable? {
        val drawableId = if (mShowKeyBorders) {
            R.drawable.keyboard_enter_background_outlined
        } else {
            R.drawable.keyboard_enter_background
        }
        return resources.getDrawable(drawableId, context.theme)
    }

    private fun drawSecondaryIcon(key: MyKeyboard.Key, canvas: Canvas, textColor: Int) {
        val secondaryIcon = key.secondaryIcon ?: return
        secondaryIcon.applyColorFilter(
            if (key.pressed) textColor else mTextColor.adjustAlpha(0.6f)
        )
        val secondaryIconWidth = (secondaryIcon.intrinsicWidth * 0.5f).toInt()
        val secondaryIconHeight = (secondaryIcon.intrinsicHeight * 0.5f).toInt()
        val secondaryIconPaddingRight = 10
        val secondaryIconLeft = key.width - secondaryIconPaddingRight - secondaryIconWidth
        val secondaryIconRight = secondaryIconLeft + secondaryIconWidth
        val secondaryIconTop = 14
        val secondaryIconBottom = secondaryIconTop + secondaryIconHeight

        secondaryIcon.setBounds(
            secondaryIconLeft, secondaryIconTop, secondaryIconRight, secondaryIconBottom
        )
        secondaryIcon.draw(canvas)
    }

    fun handleClipboard() {
        if (mToolbarHolder != null && mPopupParent.id != R.id.mini_keyboard_view && context.config.showClipboardContent) {
            val clipboardContent = context.getCurrentClip()
            if (clipboardContent?.isNotEmpty() == true) {
                keyboardViewBinding?.apply {
                    clipboardValue.apply {
                        val cleanedPreview = clipboardContent.toString().trim().replace(Regex("[\\r\\n\\t]+"), " ").replace(Regex(" {2,}"), " ")
                        text = if (cleanedPreview.length > 30) {
                            cleanedPreview.take(27) + "..."
                        } else {
                            cleanedPreview
                        }
                        removeUnderlines()
                        setOnClickListener {
                            mOnKeyboardActionListener!!.onText(clipboardContent.toString())
                            vibrateIfNeeded()
                        }
                    }

                    toggleClipboardVisibility(true)
                }
            } else {
                hideClipboardViews()
            }
        } else {
            hideClipboardViews()
        }
    }

    private fun hideClipboardViews() {
        keyboardViewBinding?.apply {
            clipboardValue.beGone()
            clipboardValue.alpha = 0f
            clipboardClear.beGone()
            clipboardClear.alpha = 0f
        }
    }

    private fun clearClipboardContent() {
        val clipboardManager =
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager) ?: return
        if (isPiePlus()) {
            clipboardManager.clearPrimaryClip()
        } else {
            val clip = ClipData.newPlainText("", "")
            clipboardManager.setPrimaryClip(clip)
        }
    }

    private fun toggleClipboardVisibility(show: Boolean) {
        val binding = keyboardViewBinding ?: return
        if (
            (show && binding.clipboardValue.alpha == 0f) ||
            (!show && binding.clipboardValue.alpha == 1f)
        ) {
            val newAlpha = if (show) 1f else 0f
            val animations = ArrayList<ObjectAnimator>()
            val clipboardValueAnimation = ObjectAnimator.ofFloat(
                binding.clipboardValue, "alpha", newAlpha
            )
            animations.add(clipboardValueAnimation)

            val clipboardClearAnimation = ObjectAnimator.ofFloat(
                binding.clipboardClear, "alpha", newAlpha
            )
            animations.add(clipboardClearAnimation)

            val animSet = AnimatorSet()
            animSet.playTogether(*animations.toTypedArray())
            animSet.duration = 150
            animSet.interpolator = AccelerateInterpolator()
            animSet.doOnStart {
                if (show) {
                    keyboardViewBinding?.clipboardValue?.beVisible()
                    keyboardViewBinding?.clipboardClear?.beVisible()
                }
            }
            animSet.doOnEnd {
                if (!show) {
                    keyboardViewBinding?.clipboardValue?.beGone()
                    keyboardViewBinding?.clipboardClear?.beGone()
                }
            }
            animSet.start()
        }
    }

    private fun getPressedKeyIndex(x: Int, y: Int): Int {
        return mKeys.indexOfFirst {
            it.isInside(x, y)
        }
    }

    private fun detectAndSendKey(index: Int, x: Int, y: Int, eventTime: Long) {
        if (index != NOT_A_KEY && index in mKeys.indices) {
            val key = mKeys[index]
            getPressedKeyIndex(x, y)
            mOnKeyboardActionListener!!.onKey(key.code)
            mLastTapTime = eventTime
        }
    }

    private fun showPreview(keyIndex: Int) {
        val oldKeyIndex = mCurrentKeyIndex
        val previewPopup = mPreviewPopup
        mCurrentKeyIndex = keyIndex

        if (!context.config.showPopupOnKeypress) {
            return
        }

        // If key changed and preview is on ...
        if (oldKeyIndex != mCurrentKeyIndex) {
            if (previewPopup.isShowing) {
                if (keyIndex == NOT_A_KEY) {
                    mHandler!!.sendMessageDelayed(
                        mHandler!!.obtainMessage(MSG_REMOVE_PREVIEW), DELAY_AFTER_PREVIEW.toLong()
                    )
                }
            }

            if (keyIndex != NOT_A_KEY) {
                showKey(keyIndex)
            }
        }
    }

    private fun showKey(keyIndex: Int) {
        val previewPopup = mPreviewPopup
        val keys = mKeys
        if (keyIndex < 0 || keyIndex >= mKeys.size) {
            return
        }

        val key = keys[keyIndex]
        if (key.code == KEYCODE_SPACE) return // no popup for the language label
        if (key.icon != null) {
            mPreviewText!!.setCompoundDrawables(null, null, null, key.icon)
        } else {
            val customTypeface = FontHelper.getTypeface(context)
            if (key.label.length > 1) {
                mPreviewText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize.toFloat())
                mPreviewText!!.typeface = Typeface.create(customTypeface, Typeface.BOLD)
            } else {
                mPreviewText!!.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    mPreviewTextSizeLarge.toFloat()
                )
                mPreviewText!!.typeface = customTypeface
            }

            mPreviewText!!.setCompoundDrawables(null, null, null, null)
            try {
                mPreviewText!!.text = adjustCase(key.label)
            } catch (ignored: Exception) {
            }
        }

        val previewBackground = mPreviewText!!.background as LayerDrawable
        previewBackground.findDrawableByLayerId(R.id.button_background_shape)
            .applyColorFilter(mBackgroundColor)
        previewBackground.findDrawableByLayerId(R.id.button_background_stroke)
            .applyColorFilter(mStrokeColor)
        mPreviewText!!.background = previewBackground

        mPreviewText!!.setTextColor(mTextColor)
        mPreviewText!!.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = Math.max(mPreviewText!!.measuredWidth, key.width)
        val popupHeight = mPreviewHeight
        val lp = mPreviewText!!.layoutParams
        lp?.width = popupWidth
        lp?.height = popupHeight

        mPopupPreviewX = key.x
        mPopupPreviewY = key.y - popupHeight

        mHandler!!.removeMessages(MSG_REMOVE_PREVIEW)
        getLocationInWindow(mCoordinates)
        mCoordinates[0] += mMiniKeyboardOffsetX // Offset may be zero
        mCoordinates[1] += mMiniKeyboardOffsetY // Offset may be zero

        // Set the preview background state
        mPreviewText!!.background.state = if (key.popupResId != 0) {
            LONG_PRESSABLE_STATE_SET
        } else {
            EMPTY_STATE_SET
        }

        mPopupPreviewX += mCoordinates[0]
        mPopupPreviewY += mCoordinates[1]

        // If the popup cannot be shown above the key, put it on the side
        getLocationOnScreen(mCoordinates)
        if (mPopupPreviewY + mCoordinates[1] < 0) {
            // If the key you're pressing is on the left side of the keyboard, show the popup on
            // the right, offset by enough to see at least one key to the left/right.
            if (key.x + key.width <= width / 2) {
                mPopupPreviewX += (key.width * 2.5).toInt()
            } else {
                mPopupPreviewX -= (key.width * 2.5).toInt()
            }
            mPopupPreviewY += popupHeight
        }

        previewPopup.dismiss()

        if (
            key.label.isNotEmpty()
            && key.code != KEYCODE_MODE_CHANGE
            && key.code != KEYCODE_SYMBOLS_MODE_CHANGE
            && key.code != KEYCODE_SHIFT
        ) {
            previewPopup.width = popupWidth
            previewPopup.height = popupHeight
            previewPopup.showAtLocation(
                mPopupParent,
                Gravity.NO_GRAVITY,
                mPopupPreviewX,
                mPopupPreviewY
            )
            mPreviewText!!.visibility = VISIBLE
        }
    }

    /**
     * Requests a redraw of the entire keyboard. Calling [.invalidate] is not sufficient because the keyboard renders the keys to an off-screen buffer and
     * an invalidate() only draws the cached buffer.
     */
    fun invalidateAllKeys() {
        mDirtyRect.union(0, 0, width, height)
        mDrawPending = true
        invalidate()
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only one key is changing it's content. Any changes that
     * affect the position or size of the key may not be honored.
     * @param keyIndex the index of the key in the attached [MyKeyboard].
     */
    private fun invalidateKey(keyIndex: Int) {
        if (keyIndex < 0 || keyIndex >= mKeys.size) {
            return
        }

        val key = mKeys[keyIndex]
        mDirtyRect.union(
            key.x, key.y,
            key.x + key.width, key.y + key.height
        )
        onBufferDraw()
        invalidate(
            key.x, key.y,
            key.x + key.width, key.y + key.height
        )
    }

    private fun openPopupIfRequired(me: MotionEvent): Boolean {
        // Check if we have a popup layout specified first.
        if (mPopupLayout == 0) {
            return false
        }

        if (mCurrentKey < 0 || mCurrentKey >= mKeys.size) {
            return false
        }

        val popupKey = mKeys[mCurrentKey]
        val result = onLongPress(popupKey, me)
        if (result) {
            mAbortKey = true
            showPreview(NOT_A_KEY)
        }

        return result
    }

    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated with this key through the attributes
     * popupLayout and popupCharacters.
     * @param popupKey the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the method on the base class if the subclass doesn't wish to
     * handle the call.
     */
    private fun onLongPress(popupKey: MyKeyboard.Key, me: MotionEvent): Boolean {
        if (popupKey.code == KEYCODE_SPACE) {
            return onSpaceBarLongPressed()
        } else if (popupKey.code == KEYCODE_EMOJI_OR_LANGUAGE) {
            return onEmojiOrLanguageLongPressed()
        } else {
            val popupKeyboardId = popupKey.popupResId
            if (popupKeyboardId != 0) {
                mMiniKeyboardContainer = mMiniKeyboardCache[popupKey]

                // For 'number' and 'phone' keyboards the count of popup keys might be bigger than count of keys in the main keyboard.
                // And therefore the width of the key might be smaller than width declared in MyKeyboard.Key.width for the main keyboard.
                val popupKeyWidth = if (popupKey.popupCharacters != null) {
                    popupKey.calcKeyWidth(containerWidth = mMiniKeyboardContainer?.measuredWidth ?: width)
                } else {
                    popupKey.width
                }

                if (mMiniKeyboardContainer == null) {
                    val inflater =
                        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    keyboardPopupBinding = KeyboardPopupKeyboardBinding.inflate(inflater).apply {
                        mMiniKeyboardContainer = root
                        mMiniKeyboard = miniKeyboardView
                    }

                    val keyboard = if (popupKey.popupCharacters != null) {
                        MyKeyboard(
                            context = context,
                            layoutTemplateResId = popupKeyboardId,
                            characters = popupKey.popupCharacters!!,
                            keyWidth = popupKeyWidth
                        )
                    } else {
                        MyKeyboard(context, popupKeyboardId, 0)
                    }
                    mMiniKeyboard!!.setKeyboard(keyboard)
                    mPopupParent = this
                    mMiniKeyboardContainer!!.measure(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
                    )
                    mMiniKeyboardCache[popupKey] = mMiniKeyboardContainer
                } else {
                    mMiniKeyboard =
                        mMiniKeyboardCache[popupKey]?.let(KeyboardPopupKeyboardBinding::bind)?.miniKeyboardView
                }

                getLocationInWindow(mCoordinates)
                mPopupX = popupKey.x
                mPopupY = popupKey.y

                // Use popupCharacters length if available, otherwise use actual key count from the loaded keyboard
                val popupKeyCount = popupKey.popupCharacters?.length ?: mMiniKeyboard!!.mKeys.size
                val widthToUse = mMiniKeyboardContainer!!.measuredWidth - (popupKeyCount / 2) * popupKeyWidth
                mPopupX = mPopupX + popupKeyWidth - widthToUse
                mPopupY -= mMiniKeyboardContainer!!.measuredHeight
                val x = mPopupX + mCoordinates[0]
                val y = mPopupY + mCoordinates[1]
                val xOffset = Math.max(0, x)
                mMiniKeyboard!!.setPopupOffset(xOffset, y)

                // make sure we highlight the proper key right after long pressing it, before any ACTION_MOVE event occurs
                val miniKeyboardX = if (xOffset + mMiniKeyboard!!.measuredWidth <= measuredWidth) {
                    xOffset
                } else {
                    measuredWidth - mMiniKeyboard!!.measuredWidth
                }

                val keysCnt = mMiniKeyboard!!.mKeys.size
                var selectedKeyIndex =
                    Math.floor((me.x - miniKeyboardX) / popupKeyWidth.toDouble()).toInt()
                if (keysCnt > MAX_KEYS_PER_MINI_ROW) {
                    selectedKeyIndex += MAX_KEYS_PER_MINI_ROW
                }
                selectedKeyIndex = Math.max(0, Math.min(selectedKeyIndex, keysCnt - 1))

                for (i in 0 until keysCnt) {
                    mMiniKeyboard!!.mKeys[i].focused = i == selectedKeyIndex
                }

                mMiniKeyboardSelectedKeyIndex = selectedKeyIndex
                mMiniKeyboard!!.invalidateAllKeys()

                val miniShiftStatus = if (isShifted()) ShiftState.ON_PERMANENT else ShiftState.OFF
                mMiniKeyboard!!.setShifted(miniShiftStatus)
                mPopupKeyboard.contentView = mMiniKeyboardContainer
                mPopupKeyboard.width = mMiniKeyboardContainer!!.measuredWidth
                mPopupKeyboard.height = mMiniKeyboardContainer!!.measuredHeight
                mPopupKeyboard.showAtLocation(this, Gravity.NO_GRAVITY, x, y)
                mMiniKeyboardOnScreen = true
                invalidateAllKeys()
                return true
            }
        }
        return false
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        val action = me.action

        if (ignoreTouches) {
            if (action == MotionEvent.ACTION_UP) {
                ignoreTouches = false

                // fix a glitch with long pressing backspace, then clicking some letter
                if (mRepeatKeyIndex != NOT_A_KEY) {
                    val key = mKeys.getOrNull(mRepeatKeyIndex)
                    if (key?.code == KEYCODE_DELETE) {
                        mHandler?.removeMessages(MSG_REPEAT)
                        mRepeatKeyIndex = NOT_A_KEY
                    }
                }
            }
            return true
        }

        // handle moving between alternative popup characters by swiping
        if (mPopupKeyboard.isShowing) {
            when (action) {
                MotionEvent.ACTION_MOVE -> {
                    if (mMiniKeyboard != null) {
                        val coords = intArrayOf(0, 0)
                        mMiniKeyboard!!.getLocationOnScreen(coords)
                        val keysCnt = mMiniKeyboard!!.mKeys.size
                        val lastRowKeyCount = if (keysCnt > MAX_KEYS_PER_MINI_ROW) {
                            Math.max(keysCnt % MAX_KEYS_PER_MINI_ROW, 1)
                        } else {
                            keysCnt
                        }

                        val widthPerKey = if (keysCnt > MAX_KEYS_PER_MINI_ROW) {
                            mMiniKeyboard!!.width / MAX_KEYS_PER_MINI_ROW
                        } else {
                            mMiniKeyboard!!.width / lastRowKeyCount
                        }

                        var selectedKeyIndex =
                            Math.floor((me.x - coords[0]) / widthPerKey.toDouble()).toInt()
                        if (keysCnt > MAX_KEYS_PER_MINI_ROW) {
                            selectedKeyIndex = Math.max(0, selectedKeyIndex)
                            selectedKeyIndex += MAX_KEYS_PER_MINI_ROW
                        }

                        selectedKeyIndex = Math.max(0, Math.min(selectedKeyIndex, keysCnt - 1))
                        if (selectedKeyIndex != mMiniKeyboardSelectedKeyIndex) {
                            for (i in 0 until keysCnt) {
                                mMiniKeyboard!!.mKeys[i].focused = i == selectedKeyIndex
                            }
                            mMiniKeyboardSelectedKeyIndex = selectedKeyIndex
                            mMiniKeyboard!!.invalidateAllKeys()
                        }

                        if (coords[0] > 0 || coords[1] > 0) {
                            if (coords[0] - me.x > mPopupMaxMoveDistance ||                                         // left
                                me.x - (coords[0] + mMiniKeyboard!!.measuredWidth) > mPopupMaxMoveDistance          // right
                            ) {
                                dismissPopupKeyboard()
                            }
                        }
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mMiniKeyboard?.mKeys?.firstOrNull { it.focused }?.apply {
                        mOnKeyboardActionListener!!.onKey(code)
                    }
                    mMiniKeyboardSelectedKeyIndex = -1
                    dismissPopupKeyboard()
                }
            }
        }

        return onModifiedTouchEvent(me)
    }

    private fun onModifiedTouchEvent(me: MotionEvent): Boolean {
        var touchX = me.x.toInt()
        var touchY = me.y.toInt()
        if (touchY >= -mVerticalCorrection) {
            touchY += mVerticalCorrection
        }

        val action = me.actionMasked
        val eventTime = me.eventTime
        val keyIndex = getPressedKeyIndex(touchX, touchY)

        // Ignore all motion events until a DOWN.
        if (mAbortKey && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        // Needs to be called after the gesture detector gets a turn, as it may have displayed the mini keyboard
        if (mMiniKeyboardOnScreen && action != MotionEvent.ACTION_CANCEL) {
            return true
        }

        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                // if the user presses a key while still holding down the previous, type in both chars and ignore the later gestures
                // can happen at fast typing, easier to reproduce by increasing LONGPRESS_TIMEOUT
                ignoreTouches = true
                mHandler!!.removeMessages(MSG_LONGPRESS)
                dismissPopupKeyboard()
                detectAndSendKey(keyIndex, me.x.toInt(), me.y.toInt(), eventTime)

                val newPointerX = me.getX(1).toInt()
                val newPointerY = me.getY(1).toInt()
                val secondKeyIndex = getPressedKeyIndex(newPointerX, newPointerY)
                showPreview(secondKeyIndex)

                detectAndSendKey(secondKeyIndex, newPointerX, newPointerY, eventTime)

                val secondKeyCode = mKeys.getOrNull(secondKeyIndex)?.code
                if (secondKeyCode != null) {
                    mOnKeyboardActionListener!!.onPress(secondKeyCode)
                }

                showPreview(NOT_A_KEY)
                setCurrentKeyPressed(false)
                return true
            }

            MotionEvent.ACTION_DOWN -> {
                mAbortKey = false
                mLastCodeX = touchX
                mLastCodeY = touchY
                mLastKeyTime = 0
                mCurrentKeyTime = 0
                mLastKey = NOT_A_KEY
                mCurrentKey = keyIndex
                mDownTime = me.eventTime
                mLastMoveTime = mDownTime

                val onPressKey = if (keyIndex != NOT_A_KEY) {
                    mKeys[keyIndex].code
                } else {
                    0
                }
                mOnKeyboardActionListener!!.onPress(onPressKey)
                mLastKeyPressedCode = onPressKey

                var wasHandled = false
                if (mCurrentKey >= 0 && mKeys[mCurrentKey].repeatable) {
                    mRepeatKeyIndex = mCurrentKey
                    mRepeatCount = 0

                    val msg = mHandler!!.obtainMessage(MSG_REPEAT)
                    val delay = if (mKeys[mCurrentKey].code == KEYCODE_DELETE) DELETE_REPEAT_START_DELAY.toLong() else REPEAT_START_DELAY.toLong()
                    mHandler!!.sendMessageDelayed(msg, delay)
                    // if the user long presses space bar, move the cursor after swiping left/right
                    if (mKeys[mCurrentKey].code == KEYCODE_SPACE) {
                        mLastSpaceMoveX = -1
                        mCursorControlActive = false
                    } else {
                        repeatKey()
                    }

                    // Delivering the key could have caused an abort
                    if (mAbortKey) {
                        resetRepeat()
                        wasHandled = true
                    }
                }

                if (!wasHandled && mCurrentKey != NOT_A_KEY) {
                    val msg = mHandler!!.obtainMessage(MSG_LONGPRESS, me)
                    mHandler!!.sendMessageDelayed(msg, LONGPRESS_TIMEOUT.toLong())
                }

                if (mPopupParent.id != R.id.mini_keyboard_view) {
                    showPreview(keyIndex)
                    setCurrentKeyPressed(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                var continueLongPress = false
                if (keyIndex != NOT_A_KEY) {
                    if (mCurrentKey == NOT_A_KEY) {
                        mCurrentKey = keyIndex
                        mCurrentKeyTime = eventTime - mDownTime
                    } else {
                        if (keyIndex == mCurrentKey) {
                            mCurrentKeyTime += eventTime - mLastMoveTime
                            continueLongPress = true
                        } else if (mRepeatKeyIndex == NOT_A_KEY) {
                            setCurrentKeyPressed(false)
                            mLastKey = mCurrentKey
                            mLastCodeX = mLastX
                            mLastCodeY = mLastY
                            mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime
                            mCurrentKey = keyIndex
                            mCurrentKeyTime = 0
                            setCurrentKeyPressed(true)
                        }
                    }
                }

                // activate cursor control immediately on sufficient movement
                val currentKey = mKeys.getOrNull(mCurrentKey)
                if (currentKey?.code == KEYCODE_SPACE && mLastSpaceMoveX != 0) {
                    if (mLastSpaceMoveX == -1) {
                        mLastSpaceMoveX = mLastX
                    }

                    val diff = mLastX - mLastSpaceMoveX
                    if (diff < -mSpaceMoveThreshold) {
                        for (i in diff / mSpaceMoveThreshold until 0) {
                            mOnKeyboardActionListener?.moveCursorLeft()
                        }
                        mLastSpaceMoveX = mLastX
                        if (!mCursorControlActive) mHandler?.removeMessages(MSG_LONGPRESS)
                        mCursorControlActive = true
                    } else if (diff > mSpaceMoveThreshold) {
                        for (i in 0 until diff / mSpaceMoveThreshold) {
                            mOnKeyboardActionListener?.moveCursorRight()
                        }
                        mLastSpaceMoveX = mLastX
                        if (!mCursorControlActive) mHandler?.removeMessages(MSG_LONGPRESS)
                        mCursorControlActive = true
                    }
                } else if (!continueLongPress) {
                    // Cancel old long-press
                    mHandler!!.removeMessages(MSG_LONGPRESS)
                    // Start new long-press if key has changed
                    if (keyIndex != NOT_A_KEY) {
                        val msg = mHandler!!.obtainMessage(MSG_LONGPRESS, me)
                        mHandler!!.sendMessageDelayed(msg, LONGPRESS_TIMEOUT.toLong())
                    }

                    if (mPopupParent.id != R.id.mini_keyboard_view) {
                        showPreview(mCurrentKey)
                    }
                    mLastMoveTime = eventTime
                }
            }

            MotionEvent.ACTION_UP -> {
                setCurrentKeyPressed(false)

                mLastSpaceMoveX = 0
                removeMessages()
                if (keyIndex == mCurrentKey) {
                    mCurrentKeyTime += eventTime - mLastMoveTime
                } else {
                    mLastKey = mCurrentKey
                    mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime
                    mCurrentKey = keyIndex
                    mCurrentKeyTime = 0
                }

                if (mCurrentKeyTime < mLastKeyTime && mCurrentKeyTime < DEBOUNCE_TIME && mLastKey != NOT_A_KEY) {
                    mCurrentKey = mLastKey
                    touchX = mLastCodeX
                    touchY = mLastCodeY
                }
                showPreview(NOT_A_KEY)
                Arrays.fill(mKeyIndices, NOT_A_KEY)

                val currentKeyCode = mKeys.getOrNull(mCurrentKey)?.code

                // If we're not on a repeating key (which sends on a DOWN event)
                if (mRepeatKeyIndex == NOT_A_KEY && !mMiniKeyboardOnScreen && !mAbortKey) {
                    detectAndSendKey(mCurrentKey, touchX, touchY, eventTime)
                } else if (currentKeyCode == KEYCODE_SPACE && !mCursorControlActive) {
                    detectAndSendKey(mCurrentKey, touchX, touchY, eventTime)
                }

                resetRepeat()
                mOnKeyboardActionListener!!.onActionUp()
                mCursorControlActive = false
            }

            MotionEvent.ACTION_CANCEL -> {
                mCursorControlActive = false
                mLastSpaceMoveX = 0
                removeMessages()
                dismissPopupKeyboard()
                mAbortKey = true
                showPreview(NOT_A_KEY)
                setCurrentKeyPressed(false)
            }
        }

        mLastX = touchX
        mLastY = touchY
        return true
    }

    private fun repeatKey(): Boolean {
        if (mRepeatKeyIndex == NOT_A_KEY || mRepeatKeyIndex !in mKeys.indices) {
            return false
        }
        val key = mKeys[mRepeatKeyIndex]
        if (key.code != KEYCODE_SPACE) {
            mRepeatCount++
            if (key.code == KEYCODE_DELETE) {
                // Accelerate deletion quickly when long pressing delete
                val deleteBatch = when {
                    mRepeatCount > 15 -> 3 // Ultra fast delete (3 characters per cycle)
                    mRepeatCount > 5 -> 2  // Accelerated delete (2 characters per cycle)
                    else -> 1              // Single character delete initially
                }
                for (i in 0 until deleteBatch) {
                    mOnKeyboardActionListener?.onKey(KEYCODE_DELETE)
                }
            } else {
                detectAndSendKey(mCurrentKey, key.x, key.y, mLastTapTime)
            }
        }
        return true
    }

    private fun getNextRepeatDelay(): Long {
        val key = mKeys.getOrNull(mRepeatKeyIndex)
        return if (key?.code == KEYCODE_DELETE) {
            when {
                mRepeatCount > 15 -> 18L // Ultra fast
                mRepeatCount > 8 -> 25L  // Very fast
                mRepeatCount > 3 -> 35L  // Accelerated
                else -> 45L              // Snappy initial repeat
            }
        } else {
            REPEAT_INTERVAL.toLong()
        }
    }

    private fun resetRepeat() {
        mHandler?.removeMessages(MSG_REPEAT)
        mRepeatKeyIndex = NOT_A_KEY
        mRepeatCount = 0
    }

    private fun setCurrentKeyPressed(pressed: Boolean) {
        mKeys.getOrNull(mCurrentKey)?.pressed = pressed
        invalidateKey(mCurrentKey)
    }

    private fun isAnyPanelOpen(): Boolean {
        return keyboardViewBinding?.run {
            clipboardManagerHolder.visibility == View.VISIBLE ||
                emojiPaletteHolder.visibility == View.VISIBLE ||
                toolsMenuHolder.visibility == View.VISIBLE ||
                textEditorHolder.visibility == View.VISIBLE ||
                numericKeypadHolder.visibility == View.VISIBLE
        } ?: false
    }

    fun closeAllPanels() {
        mVoiceManager.stopListening()
        keyboardViewBinding?.apply {
            clipboardManagerHolder.beGone()
            emojiPaletteHolder.beGone()
            toolsMenuHolder.beGone()
            textEditorHolder.beGone()
            numericKeypadHolder.beGone()
            suggestionsHolder.showAllInlineContentViews()
        }
    }

    fun openNumericKeypad() {
        closeAllPanels()
        keyboardViewBinding?.apply {
            val isArabic = context.config.keyboardLanguage == LANGUAGE_ARABIC
            numKeyAbc.text = if (isArabic) "أ ب ج" else "ABC"
            numericKeypadHolder.beVisible()
            suggestionsHolder.hideAllInlineContentViews()
        }
    }

    fun closeNumericKeypad() {
        keyboardViewBinding?.apply {
            numericKeypadHolder.beGone()
            if (!isAnyPanelOpen()) {
                suggestionsHolder.showAllInlineContentViews()
            }
        }
    }

    fun closeClipboardManager() {
        keyboardViewBinding?.apply {
            clipboardManagerHolder.beGone()
            if (!isAnyPanelOpen()) {
                suggestionsHolder.showAllInlineContentViews()
            }
        }
    }

    fun openClipboardManager() {
        closeAllPanels()
        keyboardViewBinding?.apply {
            clipboardManagerHolder.layoutDirection = View.LAYOUT_DIRECTION_LTR
            clipsList.layoutDirection = View.LAYOUT_DIRECTION_RTL
            val isArabic = context.config.keyboardLanguage == LANGUAGE_ARABIC
            clipboardManagerClose.text = if (isArabic) "أ ب ج" else "ABC"
            clipboardManagerHolder.beVisible()
            suggestionsHolder.hideAllInlineContentViews()
        }
        setupStoredClips()
    }

    fun openToolsMenu() {
        closeAllPanels()
        keyboardViewBinding?.apply {
            val isArabic = context.config.keyboardLanguage == LANGUAGE_ARABIC
            toolsMenuClose.text = if (isArabic) "أ ب ج" else "ABC"
            toolsMenuHolder.beVisible()
            suggestionsHolder.hideAllInlineContentViews()
        }
    }

    fun openTextEditor() {
        closeAllPanels()
        keyboardViewBinding?.apply {
            val isArabic = context.config.keyboardLanguage == LANGUAGE_ARABIC
            cursorBtnAbc.text = if (isArabic) "أ ب ج" else "ABC"
            textEditorHolder.beVisible()
            suggestionsHolder.hideAllInlineContentViews()
        }
    }

    private fun onSpaceBarLongPressed(): Boolean {
        return if (!mCursorControlActive) {
            setCurrentKeyPressed(false)
            mRepeatKeyIndex = NOT_A_KEY
            mHandler?.removeMessages(MSG_REPEAT)
            vibrateIfNeeded()
            SwitchLanguageDialog(this) {
                mOnKeyboardActionListener?.reloadKeyboard()
            }
            true
        } else false
    }

    private fun onEmojiOrLanguageLongPressed(): Boolean {
        setCurrentKeyPressed(false)
        if (context.config.showEmojiKey) {
            openEmojiPalette()
        } else {
            SwitchLanguageDialog(this) {
                mOnKeyboardActionListener?.reloadKeyboard()
            }
        }

        return true
    }

    private var mPrimaryClipChangedListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    fun registerClipboardListener() {
        // Lifecycle is managed centrally in SimpleKeyboardIME to avoid duplicate listeners and battery drain
    }

    fun unregisterClipboardListener() {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            mPrimaryClipChangedListener?.let { cm.removePrimaryClipChangedListener(it) }
            mPrimaryClipChangedListener = null
        } catch (ignored: Exception) {
        }
    }

    fun syncClipboard() {
        if (!isShown) return
        try {
            val currentIme = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.DEFAULT_INPUT_METHOD
            ) ?: return
            if (!currentIme.contains(context.packageName)) return

            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                ensureBackgroundThread {
                    var addedAny = false
                    for (i in 0 until clip.itemCount) {
                        val clipText = clip.getItemAt(i)?.text?.toString()?.trim()
                        if (!clipText.isNullOrEmpty()) {
                            // If this text already exists in recent (unpinned) clips, delete the older copy so the new copy jumps to the top
                            context.clipsDB.deleteRecentClipWithValue(clipText)

                            // Insert the newly copied text as unpinned in Recent clips!
                            val newClip = Clip(null, clipText, isPinned = false)
                            context.clipsDB.insertOrUpdate(newClip)
                            addedAny = true
                        }
                    }

                    if (addedAny) {
                        // Keep up to 3500 unpinned items (accommodating >3000 clips)
                        context.clipsDB.trimRecentClips()

                        Handler(Looper.getMainLooper()).post {
                            handleClipboard()
                            setupStoredClips()
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    fun setupStoredClips() {
        ensureBackgroundThread {
            val clips = ArrayList<ListItem>()
            val pinnedClips = context.clipsDB.getPinnedClips()
            val recentClips = context.clipsDB.getRecentClips()

            if (recentClips.isNotEmpty()) {
                val section = ClipsSectionLabel(context.getString(R.string.recent_clips), false)
                clips.add(section)
                clips.addAll(recentClips)
            }

            if (pinnedClips.isNotEmpty()) {
                val section = ClipsSectionLabel(context.getString(R.string.pinned_clips), false)
                clips.add(section)
                clips.addAll(pinnedClips)
            }

            Handler(Looper.getMainLooper()).post {
                setupClipsAdapter(clips)
            }
        }
    }

    private fun setupClipsAdapter(clips: ArrayList<ListItem>) {
        keyboardViewBinding?.apply {
            clipboardContentPlaceholder1.beVisibleIf(clips.isEmpty())
            clipboardContentPlaceholder2.beVisibleIf(clips.isEmpty())
            clipsList.beVisibleIf(clips.isNotEmpty())
        }

        val refreshClipsListener = object : RefreshClipsListener {
            override fun refreshClips() {
                setupStoredClips()
            }
        }

        val adapter = ClipsKeyboardAdapter(
            context = safeStorageContext,
            items = clips,
            refreshClipsListener = refreshClipsListener
        ) { clip ->
            mOnKeyboardActionListener?.onText(clip.value)
            vibrateIfNeeded()
            closeAllPanels()
        }

        keyboardViewBinding?.clipsList?.adapter = adapter

        if (clipsTouchHelper == null) {
            val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    val currentAdapter = recyclerView.adapter as? ClipsKeyboardAdapter ?: return 0
                    val position = viewHolder.bindingAdapterPosition
                    if (position !in 0 until currentAdapter.items.size) return 0
                    val item = currentAdapter.items[position]
                    if (item !is Clip) return 0
                    // Strictly swipe from Right to Left
                    return ItemTouchHelper.LEFT
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    val currentAdapter = keyboardViewBinding?.clipsList?.adapter as? ClipsKeyboardAdapter ?: return
                    if (position !in 0 until currentAdapter.items.size) return
                    val item = currentAdapter.items[position]
                    if (item is Clip) {
                        if (item.isPinned) {
                            // CRITICAL: Pinned texts MUST NOT be deleted unless unpinned!
                            currentAdapter.notifyItemChanged(position)
                            context.toast(R.string.cannot_delete_pinned)
                            return
                        }
                        ClipsHelper.clearSystemClipboardIfMatches(context, item.value)
                        ensureBackgroundThread {
                            if (item.id != null && item.id != -1L) {
                                context.clipsDB.delete(item.id!!)
                            }
                            Handler(Looper.getMainLooper()).post {
                                setupStoredClips()
                            }
                        }
                    }
                }
            }
            clipsTouchHelper = ItemTouchHelper(swipeCallback)
            keyboardViewBinding?.clipsList?.let { rv ->
                clipsTouchHelper!!.attachToRecyclerView(rv)
            }
        }
    }

    private fun setupEmojiPalette(toolbarColor: Int, backgroundColor: Int, textColor: Int) {
        keyboardViewBinding?.apply {
            emojiPaletteTopBar.background = ColorDrawable(toolbarColor)
            emojiPaletteHolder.background = ColorDrawable(backgroundColor)
            emojiPaletteClose.applyColorFilter(textColor)
            emojiPaletteLabel.setTextColor(textColor)

            emojiPaletteBottomBar.background = ColorDrawable(toolbarColor)
            emojiPaletteModeChange.apply {
                setTextColor(textColor)
                setOnClickListener {
                    vibrateIfNeeded()
                    closeEmojiPalette()
                }
            }

            emojiPaletteBackspace.apply {
                applyColorFilter(textColor)
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isPressed = true
                            mRepeatKeyIndex = mKeys.indexOfFirst { it.code == KEYCODE_DELETE }
                            mCurrentKey = mRepeatKeyIndex
                            mRepeatCount = 0
                            vibrateIfNeeded()
                            mOnKeyboardActionListener!!.onKey(KEYCODE_DELETE)
                            // setup repeating backspace with fast start delay
                            val msg = mHandler!!.obtainMessage(MSG_REPEAT)
                            mHandler!!.sendMessageDelayed(msg, DELETE_REPEAT_START_DELAY.toLong())
                            true
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            resetRepeat()
                            isPressed = false
                            false
                        }

                        else -> false
                    }
                }
            }
        }

        setupEmojis()
    }

    fun openEmojiPalette() {
        closeAllPanels()
        keyboardViewBinding!!.emojiPaletteHolder.beVisible()
        keyboardViewBinding!!.suggestionsHolder.beGone()
        setupEmojis()
    }

    fun closeEmojiPalette() {
        keyboardViewBinding?.apply {
            emojiPaletteHolder.beGone()
            if (!isAnyPanelOpen()) {
                suggestionsHolder.showAllInlineContentViews()
            }
        }
    }

    private fun setupEmojis() {
        ensureBackgroundThread {
            val fullEmojiList = parseRawEmojiSpecsFile(context, EMOJI_SPEC_FILE_PATH)
            Handler(Looper.getMainLooper()).post {
                setupEmojiAdapter(fullEmojiList)
            }
        }
    }

    private fun prepareEmojiCategories(emojis: List<EmojiData>): Map<String, List<EmojiData>> {
        return mapOf("smileys_emotion" to emojis)
    }

    private fun prepareEmojiItems(categories: Map<String, List<EmojiData>>): List<EmojisAdapter.Item> {
        val emojiItems = mutableListOf<EmojisAdapter.Item>()
        categories.values.forEach { emojis ->
            emojiItems.addAll(emojis.map(EmojisAdapter.Item::Emoji))
        }
        return emojiItems
    }

    private fun setupEmojiAdapter(emojis: List<EmojiData>) {
        val emojiCategories = prepareEmojiCategories(emojis)
        var emojiItems = prepareEmojiItems(emojiCategories)

        val emojiLayoutManager = AutoGridLayoutManager(
            context = context,
            itemWidth = context.resources.getDimensionPixelSize(R.dimen.emoji_item_size)
        ).apply {
            spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (emojiItems[position] is EmojisAdapter.Item.Category) {
                        spanCount
                    } else {
                        1
                    }
                }
            }
        }

        val emojiCategoryIds = mutableMapOf<Int, String>()
        val emojiCategoryColor = mTextColor.adjustAlpha(0.8f)
        keyboardViewBinding?.emojiCategoriesStrip?.apply {
            weightSum = emojiCategories.count().toFloat()
            val strip = this
            removeAllViews()
            emojiCategories.entries.forEach { (category, _) ->
                ItemEmojiCategoryBinding.inflate(
                    LayoutInflater.from(context),
                    this,
                    true
                ).root.apply {
                    id = generateViewId()
                    emojiCategoryIds[id] = category
                    setImageResource(getCategoryIconRes(category))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                    )
                    setOnClickListener {
                        strip.children.filterIsInstance<ImageButton>().forEach {
                            it.applyColorFilter(emojiCategoryColor)
                        }
                        applyColorFilter(mPrimaryColor)
                        keyboardViewBinding?.emojisList?.stopScroll()
                        emojiLayoutManager.scrollToPositionWithOffset(
                            emojiItems.indexOfFirst { it is EmojisAdapter.Item.Category && it.value == category },
                            0
                        )
                    }
                    applyColorFilter(emojiCategoryColor)
                }
            }
        }

        keyboardViewBinding?.emojisList?.apply {
            layoutManager = emojiLayoutManager
            adapter = EmojisAdapter(context = safeStorageContext, items = emojiItems) { emoji ->
                mOnKeyboardActionListener!!.onText(emoji.emoji)
                vibrateIfNeeded()

                context.config.addRecentEmoji(emoji.emoji)
                (adapter as? EmojisAdapter)?.apply {
                    emojiItems = prepareEmojiItems(prepareEmojiCategories(emojis))
                    updateItems(emojiItems)
                }
            }

            clearOnScrollListeners()
            onScroll { offset ->
                keyboardViewBinding!!.emojiPaletteTopBar.elevation = when {
                    offset > 4 -> context.resources.getDimensionPixelSize(R.dimen.one_dp).toFloat()
                    else -> 0f
                }

                emojiLayoutManager.findFirstCompletelyVisibleItemPosition()
                    .also { firstVisibleIndex ->
                        emojiItems
                            .withIndex()
                            .lastOrNull { it.value is EmojisAdapter.Item.Category && it.index <= firstVisibleIndex }
                            ?.also { activeCategory ->
                                val id = emojiCategoryIds.entries.first {
                                    it.value == (activeCategory.value as EmojisAdapter.Item.Category).value
                                }.key

                                keyboardViewBinding
                                    ?.emojiCategoriesStrip
                                    ?.children
                                    ?.filterIsInstance<ImageButton>()
                                    ?.forEach { button ->
                                        val selected = button.id == id
                                        button.applyColorFilter(
                                            if (selected) mPrimaryColor else emojiCategoryColor
                                        )
                                    }
                            }
                    }
            }
        }
    }

    private fun closing() {
        if (mPreviewPopup.isShowing) {
            mPreviewPopup.dismiss()
        }
        removeMessages()
        dismissPopupKeyboard()
        mBuffer = null
        mCanvas = null
        mMiniKeyboardCache.clear()
    }

    private fun removeMessages() {
        mHandler?.apply {
            removeMessages(MSG_REPEAT)
            removeMessages(MSG_LONGPRESS)
        }
        mRepeatCount = 0
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mVoiceManager.destroy()
        unregisterClipboardListener()
        removeMessages()
        closing()
    }

    fun setVoiceListeningState(isListening: Boolean) {
        keyboardViewBinding?.apply {
            if (isListening) {
                voiceInputActiveBg.visibility = View.VISIBLE
                voiceInputButton.applyColorFilter(Color.parseColor("#FF4444"))
            } else {
                voiceInputActiveBg.visibility = View.GONE
                voiceInputButton.applyColorFilter(Color.WHITE)
            }
        }
    }

    private fun dismissPopupKeyboard() {
        if (mPopupKeyboard.isShowing) {
            mPopupKeyboard.dismiss()
            mMiniKeyboardOnScreen = false
            setCurrentKeyPressed(false)
            invalidateAllKeys()
        }
    }

    private fun getKeyColor(): Int {
        return Color.parseColor("#202737")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun addToClipboardViews(it: InlineContentView, addToFront: Boolean = false) {
        if (keyboardViewBinding?.autofillSuggestionsHolder != null) {
            val newLayoutParams = LinearLayout.LayoutParams(it.layoutParams)
            newLayoutParams.updateMarginsRelative(start = resources.getDimensionPixelSize(R.dimen.normal_margin))
            it.layoutParams = newLayoutParams
            if (addToFront) {
                keyboardViewBinding?.autofillSuggestionsHolder?.addView(it, 0)
            } else {
                keyboardViewBinding?.autofillSuggestionsHolder?.addView(it)
            }
            updateSuggestionsToolbarLayout()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun clearClipboardViews() {
        keyboardViewBinding?.autofillSuggestionsHolder?.removeAllViews()
        updateSuggestionsToolbarLayout()
    }

    private fun updateSuggestionsToolbarLayout() {
        keyboardViewBinding?.apply {
            if (hasInlineViews()) {
                // make room on suggestion toolbar for inline views
                suggestionsItemsHolder.gravity = Gravity.NO_GRAVITY
                clipboardValue.maxWidth =
                    resources.getDimensionPixelSize(R.dimen.suggestion_max_width)
            } else {
                // restore original clipboard toolbar appearance
                suggestionsItemsHolder.gravity = Gravity.CENTER_HORIZONTAL
                suggestionsHolder.measuredWidth.also { maxWidth ->
                    clipboardValue.maxWidth = maxWidth
                }
            }
        }
    }

    /**
     * Returns true if there are [InlineContentView]s in [autofill_suggestions_holder]
     */
    private fun hasInlineViews() =
        (keyboardViewBinding?.autofillSuggestionsHolder?.childCount ?: 0) > 0

    /**
     * Returns: Popup Key width depends on popup keys count
     */
    private fun MyKeyboard.Key.calcKeyWidth(containerWidth: Int): Int {
        val popupKeyCount = this.popupCharacters!!.length

        return if (popupKeyCount > containerWidth / this.width) {
            containerWidth / popupKeyCount
        } else {
            this.width
        }
    }
}
