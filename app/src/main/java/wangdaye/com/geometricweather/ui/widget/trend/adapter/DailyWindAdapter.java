package wangdaye.com.geometricweather.ui.widget.trend.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.basic.GeoActivity;
import wangdaye.com.geometricweather.basic.model.option.unit.SpeedUnit;
import wangdaye.com.geometricweather.basic.model.weather.Daily;
import wangdaye.com.geometricweather.basic.model.weather.Weather;
import wangdaye.com.geometricweather.basic.model.weather.Wind;
import wangdaye.com.geometricweather.main.ui.MainColorPicker;
import wangdaye.com.geometricweather.main.ui.dialog.DailyWeatherDialog;
import wangdaye.com.geometricweather.ui.image.RotateDrawable;
import wangdaye.com.geometricweather.ui.widget.trend.TrendRecyclerView;
import wangdaye.com.geometricweather.ui.widget.trend.abs.TrendRecyclerViewAdapter;
import wangdaye.com.geometricweather.ui.widget.trend.chart.DoubleHistogramView;
import wangdaye.com.geometricweather.ui.widget.trend.item.DailyTrendItemView;

/**
 * Daily wind adapter.
 * */
public abstract class DailyWindAdapter extends TrendRecyclerViewAdapter<DailyWindAdapter.ViewHolder> {

    private GeoActivity activity;

    private Weather weather;
    private TimeZone timeZone;
    private MainColorPicker picker;
    private SpeedUnit unit;

    private float highestWindSpeed;

    private int[] themeColors;

    private int size;

    class ViewHolder extends RecyclerView.ViewHolder {

        private DailyTrendItemView dailyItem;
        private DoubleHistogramView doubleHistogramView;

        ViewHolder(View itemView) {
            super(itemView);
            dailyItem = itemView.findViewById(R.id.item_trend_daily);
            dailyItem.setParent(getTrendParent());
            dailyItem.setWidth(getItemWidth());
            dailyItem.setHeight(getItemHeight());

            doubleHistogramView = new DoubleHistogramView(itemView.getContext());
            dailyItem.setChartItemView(doubleHistogramView);
        }

        @SuppressLint("SetTextI18n, InflateParams")
        void onBindView(int position) {
            Context context = itemView.getContext();
            Daily daily = weather.getDailyForecast().get(position);

            if (daily.isToday(timeZone)) {
                dailyItem.setWeekText(context.getString(R.string.today));
            } else {
                dailyItem.setWeekText(daily.getWeek(context));
            }

            dailyItem.setDateText(daily.getShortDate(context));

            dailyItem.setTextColor(
                    picker.getTextContentColor(context),
                    picker.getTextSubtitleColor(context)
            );

            int daytimeWindColor = daily.day().getWind().getWindColor(context);
            int nighttimeWindColor = daily.night().getWind().getWindColor(context);

            RotateDrawable dayIcon = daily.day().getWind().isValidSpeed()
                    ? new RotateDrawable(ContextCompat.getDrawable(context, R.drawable.ic_navigation))
                    : new RotateDrawable(ContextCompat.getDrawable(context, R.drawable.ic_circle_medium));
            dayIcon.rotate(daily.day().getWind().getDegree().getDegree() + 180);
            dayIcon.setColorFilter(new PorterDuffColorFilter(themeColors[0], PorterDuff.Mode.SRC_ATOP));
            dailyItem.setDayIconDrawable(dayIcon);

            Float daytimeWindSpeed = weather.getDailyForecast().get(position).day().getWind().getSpeed();
            Float nighttimeWindSpeed = weather.getDailyForecast().get(position).night().getWind().getSpeed();
            doubleHistogramView.setData(
                    weather.getDailyForecast().get(position).day().getWind().getSpeed(),
                    weather.getDailyForecast().get(position).night().getWind().getSpeed(),
                    unit.getSpeedTextWithoutUnit(daytimeWindSpeed == null ? 0 : daytimeWindSpeed),
                    unit.getSpeedTextWithoutUnit(nighttimeWindSpeed == null ? 0 : nighttimeWindSpeed),
                    highestWindSpeed
            );
            doubleHistogramView.setLineColors(daytimeWindColor, nighttimeWindColor, picker.getLineColor(context));
            doubleHistogramView.setTextColors(picker.getTextContentColor(context));
            doubleHistogramView.setHistogramAlphas(1f, 0.5f);

            RotateDrawable nightIcon = daily.night().getWind().isValidSpeed()
                    ? new RotateDrawable(ContextCompat.getDrawable(context, R.drawable.ic_navigation))
                    : new RotateDrawable(ContextCompat.getDrawable(context, R.drawable.ic_circle_medium));
            nightIcon.rotate(daily.night().getWind().getDegree().getDegree() + 180);
            nightIcon.setColorFilter(new PorterDuffColorFilter(themeColors[1], PorterDuff.Mode.SRC_ATOP));
            dailyItem.setNightIconDrawable(nightIcon);

            dailyItem.setOnClickListener(v -> {
                if (activity.isForeground()) {
                    DailyWeatherDialog dialog = new DailyWeatherDialog();
                    dialog.setData(weather, getAdapterPosition(), themeColors[0]);
                    dialog.setColorPicker(picker);
                    dialog.show(activity.getSupportFragmentManager(), null);
                }
            });
        }
    }

    @SuppressLint("SimpleDateFormat")
    public DailyWindAdapter(GeoActivity activity, TrendRecyclerView parent,
                            @Px float cardMarginsVertical, @Px float cardMarginsHorizontal,
                            int itemCountPerLine, @Px float itemHeight,
                            @NonNull Weather weather, @NonNull TimeZone timeZone,
                            int[] themeColors, MainColorPicker picker, SpeedUnit unit) {
        super(activity, parent, cardMarginsVertical, cardMarginsHorizontal, itemCountPerLine, itemHeight);
        this.activity = activity;

        this.weather = weather;
        this.timeZone = timeZone;
        this.picker = picker;
        this.unit = unit;

        highestWindSpeed = Integer.MIN_VALUE;
        Float daytimeWindSpeed;
        Float nighttimeWindSpeed;
        boolean valid = false;
        for (int i = weather.getDailyForecast().size() - 1; i >= 0; i --) {
            daytimeWindSpeed = weather.getDailyForecast().get(i).day().getWind().getSpeed();
            nighttimeWindSpeed = weather.getDailyForecast().get(i).night().getWind().getSpeed();
            if (daytimeWindSpeed != null && daytimeWindSpeed > highestWindSpeed) {
                highestWindSpeed = daytimeWindSpeed;
            }
            if (nighttimeWindSpeed != null && nighttimeWindSpeed > highestWindSpeed) {
                highestWindSpeed = nighttimeWindSpeed;
            }
            if ((daytimeWindSpeed != null && daytimeWindSpeed != 0)
                    || (nighttimeWindSpeed != null && nighttimeWindSpeed != 0)
                    || valid) {
                valid = true;
                size ++;
            }
        }
        if (highestWindSpeed == 0) {
            highestWindSpeed = Wind.WIND_SPEED_11;
        }

        this.themeColors = themeColors;

        List<TrendRecyclerView.KeyLine> keyLineList = new ArrayList<>();
        keyLineList.add(
                new TrendRecyclerView.KeyLine(
                        Wind.WIND_SPEED_3,
                        activity.getString(R.string.wind_3),
                        unit.getSpeedTextWithoutUnit(Wind.WIND_SPEED_3),
                        TrendRecyclerView.KeyLine.ContentPosition.ABOVE_LINE
                )
        );
        keyLineList.add(
                new TrendRecyclerView.KeyLine(
                        Wind.WIND_SPEED_7,
                        activity.getString(R.string.wind_7),
                        unit.getSpeedTextWithoutUnit(Wind.WIND_SPEED_7),
                        TrendRecyclerView.KeyLine.ContentPosition.ABOVE_LINE
                )
        );
        keyLineList.add(
                new TrendRecyclerView.KeyLine(
                        -Wind.WIND_SPEED_3,
                        activity.getString(R.string.wind_3),
                        unit.getSpeedTextWithoutUnit(Wind.WIND_SPEED_3),
                        TrendRecyclerView.KeyLine.ContentPosition.BELOW_LINE
                )
        );
        keyLineList.add(
                new TrendRecyclerView.KeyLine(
                        -Wind.WIND_SPEED_7,
                        activity.getString(R.string.wind_7),
                        unit.getSpeedTextWithoutUnit(Wind.WIND_SPEED_7),
                        TrendRecyclerView.KeyLine.ContentPosition.BELOW_LINE
                )
        );
        parent.setLineColor(picker.getLineColor(activity));
        parent.setData(keyLineList, highestWindSpeed, -highestWindSpeed);
    }

    @NonNull
    @Override
    public DailyWindAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trend_daily, parent, false);
        return new DailyWindAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyWindAdapter.ViewHolder holder, int position) {
        holder.onBindView(position);
    }

    @Override
    public int getItemCount() {
        return size;
    }
}