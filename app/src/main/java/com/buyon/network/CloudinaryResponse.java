package com.buyon.network;

import com.google.gson.annotations.SerializedName;

public final class CloudinaryResponse {

    @SerializedName("secure_url")
    private String secureUrl;

    public String getSecureUrl() {
        return secureUrl;
    }
}

