package com.buyon.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.buyon.core.di.AppDependencies;
import com.buyon.ui.BuyonViewModelFactory;
import com.buyon.app.R;
import com.buyon.ui.adapters.OnboardingAdapter;
import com.buyon.app.databinding.FragmentOnboardingBinding;
import com.buyon.ui.viewmodel.OnboardingViewModel;

public final class OnboardingFragment extends Fragment {

    private FragmentOnboardingBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppDependencies deps = (AppDependencies) requireActivity().getApplication();
        OnboardingViewModel vm =
                new ViewModelProvider(this, new BuyonViewModelFactory(deps)).get(OnboardingViewModel.class);

        String[] titles =
                new String[] {
                    getString(R.string.onboarding_title_1),
                    getString(R.string.onboarding_title_2),
                    getString(R.string.onboarding_title_3)
                };
        String[] descs =
                new String[] {
                    getString(R.string.onboarding_desc_1),
                    getString(R.string.onboarding_desc_2),
                    getString(R.string.onboarding_desc_3)
                };
        binding.pager.setAdapter(new OnboardingAdapter(titles, descs));
        binding.btnNext.setText(R.string.next);
        binding.pager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        binding.btnNext.setText(
                                position == titles.length - 1
                                        ? getString(R.string.get_started)
                                        : getString(R.string.next));
                    }
                });

        binding.btnNext.setOnClickListener(
                v -> {
                    int i = binding.pager.getCurrentItem();
                    if (i < titles.length - 1) {
                        binding.pager.setCurrentItem(i + 1, true);
                    } else {
                        vm.completeOnboarding();
                        Navigation.findNavController(view)
                                .navigate(R.id.action_onboardingFragment_to_loginFragment);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
