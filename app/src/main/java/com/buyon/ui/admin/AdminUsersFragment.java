package com.buyon.ui.admin;

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

import com.buyon.app.databinding.FragmentAdminUsersBinding;
import com.buyon.core.di.AppDependencies;
import com.buyon.core.util.AppErrorHandler;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.ui.viewmodel.AdminViewModel;

public final class AdminUsersFragment extends Fragment {

    private FragmentAdminUsersBinding binding;
    private AdminViewModel vm;
    private AdminUserAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        vm = new ViewModelProvider(requireActivity(), new BuyonViewModelFactory(deps))
                .get(AdminViewModel.class);

        adapter = new AdminUserAdapter();
        binding.listUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listUsers.setAdapter(adapter);

        vm.getUsers().observe(getViewLifecycleOwner(), users -> {
            if (users == null) return;
            adapter.submitList(users);
            boolean empty = users.isEmpty();
            if (binding.emptyUsers != null) {
                binding.emptyUsers.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
            binding.listUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        vm.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                AppErrorHandler.showError(view,
                        AppErrorHandler.getFirebaseErrorMessage(new Exception(msg)));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
