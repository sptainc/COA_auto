package com.callcenter.ftcjsc.services;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public class ApiClient {
    private static APIService instance = null;
    public interface APIService {
        @GET()
        Call<String> sendRequest(@Url String url);

        @Streaming
        @GET()
        Call<ResponseBody> downloadFile(@Url String fileUrl);

        @Multipart
        @POST("content/download/")
        Call<ResponseBody> uploadFile(
                @Part MultipartBody.Part file,
                @Part("file") RequestBody name);
    }

    public static final String REQUEST_URL = "http://ibsmanage.com/Active/";
    public static final String DOWNLOAD_URL = "http://ibsmanage.com/Content/Download/";
    public static final String UPLOAD_URL = "http://webapi.ibsmanage.com/";

    public static APIService getInstance() {
        return RetrofitClient.getClient(REQUEST_URL).create(APIService.class);
    }

    public static APIService getInstanceDownload() {
        return RetrofitClient.getClient(DOWNLOAD_URL).create(APIService.class);
    }

    public static APIService getInstanceUpload() {
        return RetrofitClient.getClient(UPLOAD_URL).create(APIService.class);
    }
}
