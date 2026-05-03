package com.buyon.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.buyon.app.R;
import com.buyon.app.MainActivity;
import com.buyon.app.databinding.FragmentAdminDashboardBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.viewmodel.AdminViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class AdminDashboardFragment extends Fragment {

    private enum AnalyticsMode {
        LAST_7_DAYS,
        THIS_WEEK,
        LAST_WEEK
    }

    private FragmentAdminDashboardBinding binding;
    private AdminViewModel vm;
    private AdminOrderAdapter recentOrdersAdapter;
    private AdminRevenueAnalytics latestAnalytics;
    private AnalyticsMode analyticsMode = AnalyticsMode.LAST_7_DAYS;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AtomicReference<AppDependencies> deps = new AtomicReference<>((AppDependencies) requireActivity().getApplication());
        vm = new ViewModelProvider(requireActivity(), new BuyonViewModelFactory(deps.get()))
                .get(AdminViewModel.class);

        recentOrdersAdapter = new AdminOrderAdapter(vm);
        binding.listRecentOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listRecentOrders.setAdapter(recentOrdersAdapter);
        setupAnalyticsToggles();

        String now = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(new Date());
        binding.lastUpdated.setText(getString(R.string.admin_last_updated_format, now));

        vm.getProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                binding.statProducts.setText(String.valueOf(products.size()));
            }
        });

        vm.getOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders == null) return;
            binding.statOrders.setText(String.valueOf(orders.size()));

            // Calculate revenue
            double totalRevenue = 0;
            for (com.buyon.domain.model.Order order : orders) {
                if (order.getStatus() == com.buyon.domain.model.Order.Status.DELIVERED) {
                    totalRevenue += order.getTotal();
                }
            }
            if (totalRevenue >= 1000) {
                binding.statRevenue.setText(String.format(Locale.US, "$%.1fK", totalRevenue / 1000));
            } else {
                binding.statRevenue.setText(String.format(Locale.US, "$%.0f", totalRevenue));
            }

            latestAnalytics = AdminRevenueAnalytics.fromOrders(orders);
            renderRevenueAnalytics();

            // Show recent 5 orders
            List<com.buyon.domain.model.Order> recent = orders.size() > 5
                    ? orders.subList(0, 5) : orders;
            recentOrdersAdapter.submitList(recent);
        });

        vm.getUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                int count = users.size();
                if (count >= 1000) {
                    binding.statUsers.setText(String.format(Locale.US, "%.1fK", count / 1000.0));
                } else {
                    binding.statUsers.setText(String.valueOf(count));
                }
            }
        });

        binding.btnLogout.setOnClickListener(v -> {
            deps.set((AppDependencies) requireActivity().getApplication());
            deps.get().authRepository().signOut();
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        vm.getUpdateOrderState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.getStatus() == Resource.Status.LOADING) {
                return;
            }
            if (state.getStatus() == Resource.Status.SUCCESS) {
                AppErrorHandler.showSuccess(view, getString(R.string.admin_order_status_saved));
            } else if (state.getStatus() == Resource.Status.ERROR) {
                AppErrorHandler.showError(
                        view,
                        AppErrorHandler.getFirebaseErrorMessage(state.getError()));
            }
            vm.clearUpdateOrderState();
        });
    }

    private void bindRevenueAnalytics(List<com.buyon.domain.model.Order> orders) {
        latestAnalytics = AdminRevenueAnalytics.fromOrders(orders);
        renderRevenueAnalytics();
    }

    private void setupAnalyticsToggles() {
        binding.chipAnalytics7Days.setOnClickListener(v -> {
            analyticsMode = AnalyticsMode.LAST_7_DAYS;
            renderRevenueAnalytics();
        });
        binding.chipAnalyticsThisWeek.setOnClickListener(v -> {
            analyticsMode = AnalyticsMode.THIS_WEEK;
            renderRevenueAnalytics();
        });
        binding.chipAnalyticsLastWeek.setOnClickListener(v -> {
            analyticsMode = AnalyticsMode.LAST_WEEK;
            renderRevenueAnalytics();
        });
    }

    private void renderRevenueAnalytics() {
        if (latestAnalytics == null || binding == null) {
            return;
        }

        AdminRevenueAnalytics analytics = latestAnalytics;
        double[] chartValues;
        String[] labelsForMode;
        switch (analyticsMode) {
            case THIS_WEEK:
                chartValues = analytics.thisWeekDailyRevenue;
                labelsForMode = analytics.weekDayLabels;
                break;
            case LAST_WEEK:
                chartValues = analytics.lastWeekDailyRevenue;
                labelsForMode = analytics.weekDayLabels;
                break;
            case LAST_7_DAYS:
            default:
                chartValues = analytics.last7DayRevenue;
                labelsForMode = analytics.dayLabels;
                break;
        }

        View[] bars = new View[] {
                binding.barDay0,
                binding.barDay1,
                binding.barDay2,
                binding.barDay3,
                binding.barDay4,
                binding.barDay5,
                binding.barDay6
        };
        TextView[] labels = new TextView[] {
                binding.labelDay0,
                binding.labelDay1,
                binding.labelDay2,
                binding.labelDay3,
                binding.labelDay4,
                binding.labelDay5,
                binding.labelDay6
        };

        double max = 0;
        for (double value : chartValues) {
            if (value > max) max = value;
        }
        int minBarDp = 18;
        int maxBarDp = 100;
        for (int i = 0; i < bars.length; i++) {
            labels[i].setText(labelsForMode[i]);
            double value = chartValues[i];
            int barHeightDp;
            if (max <= 0) {
                barHeightDp = minBarDp;
            } else {
                float ratio = (float) (value / max);
                barHeightDp = (int) (minBarDp + (maxBarDp - minBarDp) * ratio);
            }
            ViewGroup.LayoutParams lp = bars[i].getLayoutParams();
            lp.height = dpToPx(barHeightDp);
            bars[i].setLayoutParams(lp);
            bars[i].setAlpha(i == bars.length - 1 ? 1f : 0.75f + (0.03f * i));
        }

        applyAnalyticsChipState();

        binding.analyticsThisWeek.setText(
                getString(R.string.admin_analytics_this_week_format, formatMoney(analytics.thisWeekRevenue)));

        String comparison;
        if (analytics.lastWeekRevenue <= 0 && analytics.thisWeekRevenue <= 0) {
            comparison = getString(R.string.admin_analytics_no_week_data);
        } else if (analytics.lastWeekRevenue <= 0) {
            comparison = getString(R.string.admin_analytics_first_week);
        } else {
            double change = ((analytics.thisWeekRevenue - analytics.lastWeekRevenue)
                    / analytics.lastWeekRevenue) * 100.0;
            comparison = getString(
                    R.string.admin_analytics_week_compare_format,
                    change,
                    formatMoney(analytics.lastWeekRevenue));
        }
        binding.analyticsWeekCompare.setText(comparison);
    }

    private void applyAnalyticsChipState() {
        setChipSelected(binding.chipAnalytics7Days, analyticsMode == AnalyticsMode.LAST_7_DAYS);
        setChipSelected(binding.chipAnalyticsThisWeek, analyticsMode == AnalyticsMode.THIS_WEEK);
        setChipSelected(binding.chipAnalyticsLastWeek, analyticsMode == AnalyticsMode.LAST_WEEK);
    }

    private void setChipSelected(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_category_chip_selected : R.drawable.bg_category_chip);
        chip.setTextColor(requireContext().getColor(selected ? R.color.white : R.color.buyon_text_secondary));
    }

    private String formatMoney(double value) {
        if (value >= 1000) {
            return String.format(Locale.US, "$%.1fK", value / 1000.0);
        }
        return String.format(Locale.US, "$%.0f", value);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
