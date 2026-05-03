package com.buyon.ui.admin;

import com.buyon.domain.model.Order;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class AdminRevenueAnalytics {

    final double[] last7DayRevenue;
    final String[] dayLabels;
    final double[] thisWeekDailyRevenue;
    final double[] lastWeekDailyRevenue;
    final String[] weekDayLabels;
    final double thisWeekRevenue;
    final double lastWeekRevenue;

    private AdminRevenueAnalytics(
            double[] last7DayRevenue,
            String[] dayLabels,
            double[] thisWeekDailyRevenue,
            double[] lastWeekDailyRevenue,
            String[] weekDayLabels,
            double thisWeekRevenue,
            double lastWeekRevenue) {
        this.last7DayRevenue = last7DayRevenue;
        this.dayLabels = dayLabels;
        this.thisWeekDailyRevenue = thisWeekDailyRevenue;
        this.lastWeekDailyRevenue = lastWeekDailyRevenue;
        this.weekDayLabels = weekDayLabels;
        this.thisWeekRevenue = thisWeekRevenue;
        this.lastWeekRevenue = lastWeekRevenue;
    }

    static AdminRevenueAnalytics fromOrders(List<Order> orders) {
        return fromOrders(orders, System.currentTimeMillis());
    }

    static AdminRevenueAnalytics fromOrders(List<Order> orders, long nowMillis) {
        double[] dayRevenue = new double[7];
        String[] dayLabels = new String[7];
        double[] thisWeekDaily = new double[7];
        double[] lastWeekDaily = new double[7];
        String[] weekLabels = new String[] {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);
        startOfDay(now);

        long[] dayStart = new long[7];
        Calendar dayCursor = (Calendar) now.clone();
        dayCursor.add(Calendar.DAY_OF_YEAR, -6);
        SimpleDateFormat dayLabelFormatter = new SimpleDateFormat("EEE", Locale.US);
        for (int i = 0; i < 7; i++) {
            dayStart[i] = dayCursor.getTimeInMillis();
            dayLabels[i] = dayLabelFormatter.format(dayCursor.getTime());
            dayCursor.add(Calendar.DAY_OF_YEAR, 1);
        }

        Calendar weekStartCal = (Calendar) now.clone();
        // Force Monday-start week for consistent analytics.
        weekStartCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfDay(weekStartCal);
        long thisWeekStart = weekStartCal.getTimeInMillis();
        long lastWeekStart = thisWeekStart - 7L * 24L * 60L * 60L * 1000L;

        double thisWeek = 0;
        double lastWeek = 0;

        if (orders != null) {
            for (Order order : orders) {
                if (order == null || order.getStatus() != Order.Status.DELIVERED) {
                    continue;
                }
                long created = order.getCreatedAtMillis();
                double total = order.getTotal();

                for (int i = 0; i < dayStart.length; i++) {
                    long start = dayStart[i];
                    long end = start + 24L * 60L * 60L * 1000L;
                    if (created >= start && created < end) {
                        dayRevenue[i] += total;
                        break;
                    }
                }

                if (created >= thisWeekStart) {
                    thisWeek += total;
                    int dayIndex = weekDayIndex(created);
                    thisWeekDaily[dayIndex] += total;
                } else if (created >= lastWeekStart && created < thisWeekStart) {
                    lastWeek += total;
                    int dayIndex = weekDayIndex(created);
                    lastWeekDaily[dayIndex] += total;
                }
            }
        }

        return new AdminRevenueAnalytics(
                dayRevenue,
                dayLabels,
                thisWeekDaily,
                lastWeekDaily,
                weekLabels,
                thisWeek,
                lastWeek);
    }

    double maxDayRevenue() {
        double max = 0;
        for (double value : last7DayRevenue) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private static void startOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private static int weekDayIndex(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return 0;
            case Calendar.TUESDAY:
                return 1;
            case Calendar.WEDNESDAY:
                return 2;
            case Calendar.THURSDAY:
                return 3;
            case Calendar.FRIDAY:
                return 4;
            case Calendar.SATURDAY:
                return 5;
            case Calendar.SUNDAY:
            default:
                return 6;
        }
    }
}


