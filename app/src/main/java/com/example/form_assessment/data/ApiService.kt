package com.example.form_assessment.data
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
//    @GET("b/687374506063391d31aca23a")
//    suspend fun getSurvey(): ApiResponse
@GET("b/687374506063391d31aca23a")
suspend fun getSurvey(): Response<ApiResponse>
}