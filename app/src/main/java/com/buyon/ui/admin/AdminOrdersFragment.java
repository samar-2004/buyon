package com.buyon.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.buyon.app.R;
import com.buyon.app.databinding.FragmentAdminOrdersBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.resource.Resource;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.viewmodel.AdminViewModel;

public final class AdminOrdersFragment extends Fragment {

    private FragmentAdminOrdersBinding binding;
    private AdminViewModel vm;
    private AdminOrderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        vm = new ViewModelProvider(requireActivity(), new BuyonViewModelFactory(deps))
                .get(AdminViewModel.class);

        adapter = new AdminOrderAdapter(vm);
        binding.listOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listOrders.setAdapter(adapter);

        vm.getOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) {
                adapter.submitList(orders);
                binding.orderCount.setText(
                        getString(R.string.admin_orders_count_format, orders.size()));
            }
        });

        vm.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                AppErrorHandler.showError(view,
                        AppErrorHandler.getFirebaseErrorMessage(new Exception(msg)));
            }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
