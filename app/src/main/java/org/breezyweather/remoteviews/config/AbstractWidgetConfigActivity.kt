package org.breezyweather.remoteviews.config

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.*
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.widget.NestedScrollView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.xw.repo.BubbleSeekBar
import com.xw.repo.BubbleSeekBar.OnProgressChangedListenerAdapter
import org.breezyweather.R
import org.breezyweather.background.polling.PollingManager.resetNormalBackgroundTask
import org.breezyweather.common.basic.GeoActivity
import org.breezyweather.common.basic.models.Location
import org.breezyweather.common.extensions.getTabletListAdaptiveWidth
import org.breezyweather.common.ui.widgets.insets.FitSystemBarNestedScrollView
import org.breezyweather.common.utils.helpers.SnackbarHelper
import org.breezyweather.db.repositories.LocationEntityRepository
import org.breezyweather.db.repositories.WeatherEntityRepository
import org.breezyweather.main.utils.RequestErrorType
import org.breezyweather.settings.ConfigStore
import org.breezyweather.settings.SettingsManager
import org.breezyweather.weather.WeatherHelper
import org.breezyweather.weather.WeatherHelper.OnRequestWeatherListener
import javax.inject.Inject

/**
 * Abstract widget config activity.
 */
abstract class AbstractWidgetConfigActivity : GeoActivity(), OnRequestWeatherListener {
    protected var mTopContainer: FrameLayout? = null
    protected var mWallpaper: ImageView? = null
    protected var mWidgetContainer: FrameLayout? = null
    protected var mScrollView: NestedScrollView? = null
    protected var mViewTypeContainer: RelativeLayout? = null
    protected var mCardStyleContainer: RelativeLayout? = null
    protected var mCardAlphaContainer: RelativeLayout? = null
    protected var mHideSubtitleContainer: RelativeLayout? = null
    protected var mSubtitleDataContainer: RelativeLayout? = null
    protected var mTextColorContainer: RelativeLayout? = null
    protected var mTextSizeContainer: RelativeLayout? = null
    protected var mClockFontContainer: RelativeLayout? = null
    protected var mHideLunarContainer: RelativeLayout? = null
    protected var mAlignEndContainer: RelativeLayout? = null
    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var mBottomSheetScrollView: FitSystemBarNestedScrollView? = null
    private var mSubtitleInputLayout: TextInputLayout? = null
    private var mSubtitleEditText: TextInputEditText? = null
    var locationNow: Location? = null
        protected set

    var weatherHelper: WeatherHelper? = null
        @Inject set
    protected var destroyed = false
    protected var viewTypeValueNow: String? = null
    protected var viewTypes: Array<String> = emptyArray()
    protected var viewTypeValues: Array<String> = emptyArray()
    protected var cardStyleValueNow: String? = null
    protected var cardStyles: Array<String> = emptyArray()
    protected var cardStyleValues: Array<String> = emptyArray()
    protected var cardAlpha = 0
    protected var hideSubtitle = false
    protected var subtitleDataValueNow: String? = null
    protected var subtitleData: Array<String> = emptyArray()
    protected var subtitleDataValues: Array<String> = emptyArray()
    protected var textColorValueNow: String? = null
    protected var textColors: Array<String> = emptyArray()
    protected var textColorValues: Array<String> = emptyArray()
    protected var textSize = 0
    protected var clockFontValueNow: String? = null
    protected var clockFonts: Array<String> = emptyArray()
    protected var clockFontValues: Array<String> = emptyArray()
    protected var hideLunar = false
    protected var alignEnd = false
    private var mLastBackPressedTime: Long = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)
        initData()
        readConfig()
        initView()
        updateHostView()
        locationNow?.let { location ->
            if (location.isCurrentPosition) {
                if (location.isUsable) {
                    weatherHelper!!.requestWeather(this, location, this)
                } else {
                    weatherHelper!!.requestWeather(this, Location.buildLocal(this), this)
                }
            } else {
                weatherHelper!!.requestWeather(this, location, this)
            }
        }
    }

    override fun onBackPressed() {
        if (mBottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            setBottomSheetState(true)
            return
        }
        val time = System.currentTimeMillis()
        if (time - mLastBackPressedTime < 2000) {
            super.onBackPressed()
            return
        }
        mLastBackPressedTime = time
        SnackbarHelper.showSnackbar(getString(R.string.message_tap_again_to_exit))
    }

    override fun onCreateView(
        parent: View?, name: String, context: Context,
        attrs: AttributeSet
    ): View? {
        return if (name == "ImageView") {
            ImageView(context, attrs)
        } else super.onCreateView(parent, name, context, attrs)
    }

    override fun onCreateView(
        name: String, context: Context,
        attrs: AttributeSet
    ): View? {
        return if (name == "ImageView") {
            ImageView(context, attrs)
        } else super.onCreateView(name, context, attrs)
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyed = true
        weatherHelper!!.cancel()
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        // do nothing.
    }

    @CallSuper
    open fun initData() {
        val locationList = LocationEntityRepository.readLocationList(this)
        locationNow = locationList.getOrNull(0)?.copy(weather = WeatherEntityRepository.readWeather(locationList[0]))
        val res = resources
        viewTypeValueNow = "rectangle"
        viewTypes = res.getStringArray(R.array.widget_styles)
        viewTypeValues = res.getStringArray(R.array.widget_style_values)
        cardStyleValueNow = "none"
        cardStyles = res.getStringArray(R.array.widget_card_styles)
        cardStyleValues = res.getStringArray(R.array.widget_card_style_values)
        cardAlpha = 100
        hideSubtitle = false
        subtitleDataValueNow = "time"
        val data = res.getStringArray(R.array.widget_subtitle_data)
        val dataValues = res.getStringArray(R.array.widget_subtitle_data_values)
        if (SettingsManager.getInstance(this).language.isChinese) {
            subtitleData = arrayOf(
                data[0], data[1], data[2], data[3], data[4], data[5]
            )
            subtitleDataValues = arrayOf(
                dataValues[0], dataValues[1], dataValues[2], dataValues[3], dataValues[4], dataValues[5]
            )
        } else {
            subtitleData = arrayOf(
                data[0], data[1], data[2], data[3], data[5]
            )
            subtitleDataValues = arrayOf(
                dataValues[0], dataValues[1], dataValues[2], dataValues[3], dataValues[5]
            )
        }
        textColorValueNow = "light"
        textColors = res.getStringArray(R.array.widget_text_colors)
        textColorValues = res.getStringArray(R.array.widget_text_color_values)
        textSize = 100
        clockFontValueNow = "light"
        clockFonts = res.getStringArray(R.array.widget_clock_fonts)
        clockFontValues = res.getStringArray(R.array.widget_clock_font_values)
        hideLunar = false
        alignEnd = false
    }

    private fun readConfig() {
        val config = ConfigStore(this, configStoreName!!)
        viewTypeValueNow = config.getString(getString(R.string.key_view_type), viewTypeValueNow)
        cardStyleValueNow = config.getString(getString(R.string.key_card_style), cardStyleValueNow)
        cardAlpha = config.getInt(getString(R.string.key_card_alpha), cardAlpha)
        hideSubtitle = config.getBoolean(getString(R.string.key_hide_subtitle), hideSubtitle)
        subtitleDataValueNow = config.getString(getString(R.string.key_subtitle_data), subtitleDataValueNow)
        textColorValueNow = config.getString(getString(R.string.key_text_color), textColorValueNow)
        textSize = config.getInt(getString(R.string.key_text_size), textSize)
        clockFontValueNow = config.getString(getString(R.string.key_clock_font), clockFontValueNow)
        hideLunar = config.getBoolean(getString(R.string.key_hide_lunar), hideLunar)
        alignEnd = config.getBoolean(getString(R.string.key_align_end), alignEnd)
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @CallSuper
    open fun initView() {
        mWallpaper = findViewById(R.id.activity_widget_config_wall)
        bindWallpaper(true)
        mWidgetContainer = findViewById(R.id.activity_widget_config_widgetContainer)
        val screenWidth = resources.displayMetrics.widthPixels
        val adaptiveWidth = this.getTabletListAdaptiveWidth(screenWidth)
        val paddingHorizontal = (screenWidth - adaptiveWidth) / 2
        mTopContainer = findViewById<FrameLayout>(R.id.activity_widget_config_top).apply {
            setOnApplyWindowInsetsListener { _: View?, insets: WindowInsets ->
                mWidgetContainer!!.setPadding(
                    paddingHorizontal, insets.systemWindowInsetTop,
                    paddingHorizontal, 0
                )
                insets
            }
        }
        mScrollView = findViewById(R.id.activity_widget_config_scrollView)
        mViewTypeContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_viewStyleContainer).apply {
            visibility = View.GONE
        }
        val viewTypeSpinner = findViewById<AppCompatSpinner>(R.id.activity_widget_config_styleSpinner)
        viewTypeSpinner.onItemSelectedListener = ViewTypeSpinnerSelectedListener()
        viewTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, viewTypes)
        viewTypeSpinner.setSelection(indexValue(viewTypeValues, viewTypeValueNow), true)
        mCardStyleContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_showCardContainer).apply {
            visibility = View.GONE
        }
        val cardStyleSpinner = findViewById<AppCompatSpinner>(R.id.activity_widget_config_showCardSpinner)
        cardStyleSpinner.onItemSelectedListener = CardStyleSpinnerSelectedListener()
        cardStyleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cardStyles)
        cardStyleSpinner.setSelection(indexValue(cardStyleValues, cardStyleValueNow), true)
        mCardAlphaContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_cardAlphaContainer).apply {
            visibility = View.GONE
        }
        val cardAlphaSeekBar = findViewById<BubbleSeekBar>(R.id.activity_widget_config_cardAlphaSeekBar)
        cardAlphaSeekBar.setCustomSectionTextArray { _: Int, array: SparseArray<String?> ->
            array.clear()
            array.put(0, "0%")
            array.put(1, "20%")
            array.put(2, "40%")
            array.put(3, "60%")
            array.put(4, "80%")
            array.put(5, "100%")
            array
        }
        cardAlphaSeekBar.onProgressChangedListener = CardAlphaChangedListener()
        cardAlphaSeekBar.setProgress(cardAlpha.toFloat())
        mHideSubtitleContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_hideSubtitleContainer).apply {
            visibility = View.GONE
        }
        val hideSubtitleSwitch = findViewById<Switch>(R.id.activity_widget_config_hideSubtitleSwitch)
        hideSubtitleSwitch.setOnCheckedChangeListener(HideSubtitleSwitchCheckListener())
        hideSubtitleSwitch.isChecked = hideSubtitle
        mSubtitleDataContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_subtitleDataContainer).apply {
            visibility = View.GONE
        }
        val subtitleDataSpinner = findViewById<AppCompatSpinner>(R.id.activity_widget_config_subtitleDataSpinner)
        subtitleDataSpinner.onItemSelectedListener = SubtitleDataSpinnerSelectedListener()
        subtitleDataSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subtitleData)
        subtitleDataSpinner.setSelection(
            indexValue(subtitleDataValues, if (isCustomSubtitle) "custom" else subtitleDataValueNow),
            true
        )
        mTextColorContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_blackTextContainer).apply {
            visibility = View.GONE
        }
        val textStyleSpinner = findViewById<AppCompatSpinner>(R.id.activity_widget_config_blackTextSpinner)
        textStyleSpinner.onItemSelectedListener = TextColorSpinnerSelectedListener()
        textStyleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, textColors)
        textStyleSpinner.setSelection(indexValue(textColorValues, textColorValueNow), true)
        mTextSizeContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_textSizeContainer).apply {
            visibility = View.GONE
        }
        val textSizeSeekBar = findViewById<BubbleSeekBar>(R.id.activity_widget_config_textSizeSeekBar)
        textSizeSeekBar.setCustomSectionTextArray { _: Int, array: SparseArray<String?> ->
            array.clear()
            array.put(0, "0%")
            array.put(1, "100%")
            array.put(2, "200%")
            array.put(3, "300%")
            array
        }
        textSizeSeekBar.onProgressChangedListener = TextSizeChangedListener()
        textSizeSeekBar.setProgress(textSize.toFloat())
        mClockFontContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_clockFontContainer).apply {
            visibility = View.GONE
        }
        val clockFontSpinner = findViewById<AppCompatSpinner>(R.id.activity_widget_config_clockFontSpinner)
        clockFontSpinner.onItemSelectedListener = ClockFontSpinnerSelectedListener()
        clockFontSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, clockFonts)
        clockFontSpinner.setSelection(indexValue(clockFontValues, cardStyleValueNow), true)
        mHideLunarContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_hideLunarContainer).apply {
            visibility = View.GONE
        }
        val hideLunarSwitch = findViewById<Switch>(R.id.activity_widget_config_hideLunarSwitch)
        hideLunarSwitch.setOnCheckedChangeListener(HideLunarSwitchCheckListener())
        hideLunarSwitch.isChecked = hideLunar
        mAlignEndContainer = findViewById<RelativeLayout>(R.id.activity_widget_config_alignEndContainer).apply {
            visibility = View.GONE
        }
        val alignEndSwitch = findViewById<Switch>(R.id.activity_widget_config_alignEndSwitch)
        alignEndSwitch.setOnCheckedChangeListener(AlignEndSwitchCheckListener())
        alignEndSwitch.isChecked = alignEnd
        val doneButton = findViewById<Button>(R.id.activity_widget_config_doneButton)
        doneButton.setOnClickListener {
            ConfigStore(this, configStoreName!!)
                .edit()
                .putString(getString(R.string.key_view_type), viewTypeValueNow)
                .putString(getString(R.string.key_card_style), cardStyleValueNow)
                .putInt(getString(R.string.key_card_alpha), cardAlpha)
                .putBoolean(getString(R.string.key_hide_subtitle), hideSubtitle)
                .putString(getString(R.string.key_subtitle_data), subtitleDataValueNow)
                .putString(getString(R.string.key_text_color), textColorValueNow)
                .putInt(getString(R.string.key_text_size), textSize)
                .putString(getString(R.string.key_clock_font), clockFontValueNow)
                .putBoolean(getString(R.string.key_hide_lunar), hideLunar)
                .putBoolean(getString(R.string.key_align_end), alignEnd)
                .apply()
            val intent = intent
            val extras = intent.extras
            var appWidgetId = 0
            if (extras != null) {
                appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
            }
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            resetNormalBackgroundTask(this, true)
            finish()
        }
        mBottomSheetScrollView = findViewById(R.id.activity_widget_config_custom_scrollView)
        mSubtitleInputLayout = findViewById(R.id.activity_widget_config_subtitle_inputLayout)
        mSubtitleEditText = findViewById<TextInputEditText>(R.id.activity_widget_config_subtitle_inputter).apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    // do nothing.
                }

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    // do nothing.
                }

                override fun afterTextChanged(editable: Editable) {
                    subtitleDataValueNow = editable.toString()
                    updateHostView()
                }
            })
            setText(if (isCustomSubtitle) subtitleDataValueNow else "")
        }
        val subtitleCustomKeywords = findViewById<TextView>(R.id.activity_widget_config_custom_subtitle_keywords)
        subtitleCustomKeywords.text = this.subtitleCustomKeywords
        val scrollContainer = findViewById<LinearLayout>(R.id.activity_widget_config_scrollContainer)
        scrollContainer.post {
            scrollContainer.setPaddingRelative(
                0, 0, 0, mSubtitleInputLayout!!.measuredHeight
            )
        }
        val bottomSheet = findViewById<AppBarLayout>(R.id.activity_widget_config_custom_subtitle)
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            setState(BottomSheetBehavior.STATE_HIDDEN)
        }
        bottomSheet.post {
            mBottomSheetBehavior!!.peekHeight = mSubtitleInputLayout!!.measuredHeight + mBottomSheetScrollView!!.bottomWindowInset
            setBottomSheetState(isCustomSubtitle)
        }
    }

    fun updateHostView() {
        mWidgetContainer?.let {
            it.removeAllViews()
            it.addView(
                remoteViews.apply(applicationContext, mWidgetContainer),
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    private fun setBottomSheetState(visible: Boolean) {
        if (visible) {
            mBottomSheetBehavior?.isHideable = false
            mBottomSheetBehavior?.setState(BottomSheetBehavior.STATE_COLLAPSED)
        } else {
            mBottomSheetBehavior?.isHideable = true
            mBottomSheetBehavior?.setState(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    abstract val remoteViews: RemoteViews
    abstract val configStoreName: String?
    private fun indexValue(values: Array<String>, current: String?): Int {
        for (i in values.indices) {
            if (values[i] == current) {
                return i
            }
        }
        return 0
    }

    private val isCustomSubtitle: Boolean
        get() {
            for (v in subtitleDataValues) {
                if (v != "custom" && v == subtitleDataValueNow) {
                    return false
                }
            }
            return true
        }
    private val subtitleCustomKeywords: String
        get() = """
            ${getString(R.string.widget_custom_subtitle_keyword_cw)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_cw_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_ct)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_ct_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_ctd)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_ctd_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_at)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_at_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_atd)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_atd_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_cwd)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_cwd_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_cuv)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_cuv_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_ch)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_ch_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_cps)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_cps_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_cv)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_cv_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_cdp)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_cdp_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_al)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_al_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_als)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_als_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_l)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_l_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_lat)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_lat_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_lon)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_lon_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_ut)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_ut_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_d)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_d_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_lc)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_lc_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_w)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_w_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_ws)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_ws_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_dd)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_dd_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_hd)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_hd_description)}
            ${getString(R.string.widget_custom_subtitle_keyword_enter)}${getString(R.string.colon_separator)}${getString(R.string.widget_custom_subtitle_keyword_enter_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xdw)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xdw_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xnw)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xnw_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xdt)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xdt_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xnt)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xnt_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xdtd)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xdtd_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xntd)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xntd_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xdp)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xdp_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xnp)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xnp_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xdwd)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xdwd_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xnwd)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xnwd_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xsr)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xsr_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xss)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xss_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xmr)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xmr_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xms)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xms_description)}
            
            ${getString(R.string.widget_custom_subtitle_keyword_xmp)}${getString(R.string.colon_separator)}
            ${getString(R.string.widget_custom_subtitle_keyword_xmp_description)}
            """.trimIndent()
    protected val isHideLunarContainerVisible: Int
        get() = if (SettingsManager.getInstance(this).language.isChinese) View.VISIBLE else View.GONE

    // interface.
    // on request weather listener.
    override fun requestWeatherSuccess(requestLocation: Location) {
        if (destroyed) {
            return
        }
        locationNow = requestLocation
        if (requestLocation.weather == null) {
            requestWeatherFailed(requestLocation, RequestErrorType.WEATHER_REQ_FAILED)
        } else {
            updateHostView()
        }
    }

    override fun requestWeatherFailed(requestLocation: Location, requestErrorType: RequestErrorType) {
        if (destroyed) {
            return
        }
        locationNow = requestLocation
        updateHostView()
        SnackbarHelper.showSnackbar(getString(requestErrorType.shortMessage))
    }

    @SuppressLint("MissingPermission")
    private fun bindWallpaper(checkPermissions: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkPermissions) {
            val hasPermission = checkPermissions(0)
            if (!hasPermission) {
                return
            }
        }
        try {
            WallpaperManager.getInstance(this)?.drawable?.let {
                mWallpaper?.setImageDrawable(it)
            }
        } catch (ignore: Exception) {
            // do nothing.
        }
    }

    /**
     * @return true : already got permissions.
     * false: request permissions.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun checkPermissions(requestCode: Int): Boolean {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            0 -> {
                bindWallpaper(false)
                if (textColorValueNow == "auto") {
                    updateHostView()
                }
            }

            1 -> {
                bindWallpaper(false)
                updateHostView()
            }
        }
    }

    // on check changed listener(switch).
    private inner class HideSubtitleSwitchCheckListener : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            hideSubtitle = isChecked
            updateHostView()
        }
    }

    private inner class HideLunarSwitchCheckListener : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            hideLunar = isChecked
            updateHostView()
        }
    }

    private inner class AlignEndSwitchCheckListener : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            alignEnd = isChecked
            updateHostView()
        }
    }

    // on item selected listener.
    private inner class ViewTypeSpinnerSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
            if (viewTypeValueNow != viewTypeValues[i]) {
                viewTypeValueNow = viewTypeValues[i]
                updateHostView()
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {
            // do nothing.
        }
    }

    private inner class CardStyleSpinnerSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
            if (cardStyleValueNow != cardStyleValues[i]) {
                cardStyleValueNow = cardStyleValues[i]
                updateHostView()
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {
            // do nothing.
        }
    }

    private inner class SubtitleDataSpinnerSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
            setBottomSheetState(subtitleDataValues[i] == "custom")
            if (subtitleDataValueNow != subtitleDataValues[i]) {
                subtitleDataValueNow = if (subtitleDataValues[i] == "custom") {
                    val editable = mSubtitleEditText!!.text
                    editable?.toString() ?: ""
                } else {
                    subtitleDataValues[i]
                }
                updateHostView()
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {
            // do nothing.
        }
    }

    private inner class TextColorSpinnerSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
            if (textColorValueNow != textColorValues[i]) {
                textColorValueNow = textColorValues[i]
                if (textColorValueNow != "auto") {
                    updateHostView()
                    return
                }
                var hasPermission = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    hasPermission = checkPermissions(1)
                }
                if (hasPermission) {
                    updateHostView()
                }
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {
            // do nothing.
        }
    }

    private inner class ClockFontSpinnerSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
            if (clockFontValueNow != clockFontValues[i]) {
                clockFontValueNow = clockFontValues[i]
                updateHostView()
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {
            // do nothing.
        }
    }

    private inner class CardAlphaChangedListener : OnProgressChangedListenerAdapter() {
        override fun getProgressOnActionUp(bubbleSeekBar: BubbleSeekBar, progress: Int, progressFloat: Float) {
            if (cardAlpha != progress) {
                cardAlpha = progress
                updateHostView()
            }
        }
    }

    private inner class TextSizeChangedListener : OnProgressChangedListenerAdapter() {
        override fun getProgressOnActionUp(bubbleSeekBar: BubbleSeekBar, progress: Int, progressFloat: Float) {
            if (textSize != progress) {
                textSize = progress
                updateHostView()
            }
        }
    }
}