package com.buyon.ui.upload;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.buyon.app.BuildConfig;
import com.buyon.core.resource.Resource;
import com.buyon.repository.ImageRepository;

public final class UploadViewModel extends AndroidViewModel {

    private final ImageRepository imageRepository;
    private final MutableLiveData<Resource<String>> uploadState = new MutableLiveData<>();

    public UploadViewModel(@NonNull Application application) {
        super(application);
        this.imageRepository = new ImageRepository();
    }

    public LiveData<Resource<String>> getUploadState() {
        return uploadState;
    }

    public void clearUploadState() {
        uploadState.setValue(null);
    }

    public void uploadImage(@NonNull Uri uri) {
        uploadState.setValue(Resource.loading());

        imageRepository.uploadToCloudinary(
                getApplication(),
                uri,
                BuildConfig.CLOUDINARY_CLOUD_NAME,
                BuildConfig.CLOUDINARY_UPLOAD_PRESET,
                new ImageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(@NonNull String secureUrl) {
                        uploadState.postValue(Resource.success(secureUrl));
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        uploadState.postValue(Resource.error(new IllegalStateException(message)));
                    }
                }
        );
    }
}

