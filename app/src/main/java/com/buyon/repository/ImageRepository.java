package com.buyon.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import com.buyon.network.CloudinaryApi;
import com.buyon.network.CloudinaryResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ImageRepository {

    public interface UploadCallback {
        void onSuccess(@NonNull String secureUrl);
        void onError(@NonNull String message);
    }

    private final CloudinaryApi api;

    public ImageRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.cloudinary.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.api = retrofit.create(CloudinaryApi.class);
    }

    public void uploadToCloudinary(
            @NonNull Context context,
            @NonNull Uri imageUri,
            @NonNull String cloudName,
            @NonNull String uploadPreset,
            @NonNull UploadCallback callback
    ) {
        if (cloudName.trim().isEmpty() || uploadPreset.trim().isEmpty()) {
            callback.onError("Image upload is not configured. Add Cloudinary keys in local.properties.");
            return;
        }

        String mimeType = context.getContentResolver().getType(imageUri);
        if (mimeType == null || !mimeType.toLowerCase(Locale.US).startsWith("image/")) {
            callback.onError("Please select a valid image file.");
            return;
        }

        File tempFile;
        try {
            tempFile = copyUriToTempFile(context, imageUri, mimeType);
        } catch (IOException e) {
            callback.onError("Could not read the selected image.");
            return;
        }

        RequestBody requestFile = RequestBody.create(tempFile, MediaType.parse("image/*"));
        MultipartBody.Part filePart =
                MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);
        RequestBody presetPart =
                RequestBody.create(uploadPreset, MediaType.parse("text/plain"));

        Call<CloudinaryResponse> call = api.uploadImage(cloudName, filePart, presetPart);
        enqueueWithSingleRetry(call, tempFile, callback);
    }

    private void enqueueWithSingleRetry(
            @NonNull Call<CloudinaryResponse> call,
            @NonNull File tempFile,
            @NonNull UploadCallback callback
    ) {
        call.enqueue(new Callback<CloudinaryResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<CloudinaryResponse> activeCall,
                    @NonNull Response<CloudinaryResponse> response
            ) {
                tempFile.delete();
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getSecureUrl() != null
                        && !response.body().getSecureUrl().isEmpty()) {
                    callback.onSuccess(response.body().getSecureUrl());
                    return;
                }
                callback.onError("Upload failed. Please try again.");
            }

            @Override
            public void onFailure(@NonNull Call<CloudinaryResponse> activeCall, @NonNull Throwable t) {
                if (activeCall.isCanceled()) {
                    tempFile.delete();
                    callback.onError("Upload cancelled.");
                    return;
                }

                try {
                    Call<CloudinaryResponse> retryCall = activeCall.clone();
                    retryCall.enqueue(new Callback<CloudinaryResponse>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<CloudinaryResponse> retry,
                                @NonNull Response<CloudinaryResponse> response
                        ) {
                            tempFile.delete();
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().getSecureUrl() != null
                                    && !response.body().getSecureUrl().isEmpty()) {
                                callback.onSuccess(response.body().getSecureUrl());
                                return;
                            }
                            callback.onError("Upload failed. Please check preset and network.");
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<CloudinaryResponse> retry,
                                @NonNull Throwable error
                        ) {
                            tempFile.delete();
                            callback.onError("Network error while uploading image.");
                        }
                    });
                } catch (Exception ignored) {
                    tempFile.delete();
                    callback.onError("Network error while uploading image.");
                }
            }
        });
    }

    private File copyUriToTempFile(@NonNull Context context, @NonNull Uri uri, @NonNull String mimeType)
            throws IOException {
        ContentResolver resolver = context.getContentResolver();
        InputStream inputStream = resolver.openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Cannot open input stream");
        }

        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension == null || extension.trim().isEmpty()) {
            extension = "jpg";
        }

        File tempFile = File.createTempFile("cloudinary_upload_", "." + extension, context.getCacheDir());
        try (InputStream in = inputStream; FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8 * 1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
        }
        return tempFile;
    }
}

