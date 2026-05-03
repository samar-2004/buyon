package com.buyon.ui.orders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.buyon.core.di.AppDependencies;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.adapters.OrderAdapter;
import com.buyon.app.databinding.FragmentOrdersBinding;
import com.buyon.ui.viewmodel.OrdersViewModel;
import com.google.android.material.snackbar.Snackbar;

public final class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private OrderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        OrdersViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(OrdersViewModel.class);

        adapter = new OrderAdapter();
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        vm.getOrders()
                .observe(
                        getViewLifecycleOwner(),
                        orders -> {
                            if (orders == null || orders.isEmpty()) {
                                binding.empty.setVisibility(View.VISIBLE);
                                binding.list.setVisibility(View.GONE);
                            } else {
                                binding.empty.setVisibility(View.GONE);
                                binding.list.setVisibility(View.VISIBLE);
                                adapter.submit(orders);
                            }
                        });

        vm.getErrorMessage()
                .observe(
                        getViewLifecycleOwner(),
                        msg -> {
                            if (msg != null && !msg.isEmpty()) {
                                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show();
                            }
                        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
