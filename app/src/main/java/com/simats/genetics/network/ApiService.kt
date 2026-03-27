// ApiService.kt
package com.simats.genetics.network

import com.simats.genetics.network.requests.*
import com.simats.genetics.network.responses.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // =========================
    // AUTH
    // =========================
    @POST("register/{role}/")
    fun registerUser(
        @Path("role") role: String,
        @Body body: RegisterRequest
    ): Call<RegisterResponse>

    @POST("login/")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse>

    @POST("forgot-password/")
    fun forgotPassword(@Body body: ForgotPasswordRequest): Call<ForgotPasswordResponse>

    @POST("reset-password/")
    fun resetPassword(@Body body: ResetPasswordRequest): Call<ResetPasswordResponse>

    // =========================
    // DOCTOR
    // =========================
    @GET("doctor/dashboard/")
    fun getDoctorDashboard(): Call<DoctorDashboardResponse>

    @GET("doctor/patients/")
    fun getDoctorPatients(): Call<DoctorPatientsListResponse>

    @GET("doctor/pedigree-patients/")
    fun getDoctorPedigreePatients(): Call<DoctorPatientsListResponse>

    @GET("doctor/report-patients/")
    fun getDoctorReportPatients(): Call<DoctorReportPatientsListResponse>

    @GET("doctor/patient/{patient_id}/")
    fun getDoctorPatientDetail(@Path("patient_id") patientId: Int): Call<DoctorPatientDetailResponse>

    // NOTE: keep this ONLY if you really add this Django URL:
    // path("doctor/patient/<int:patient_id>/pedigree/", ...)
    @GET("doctor/patient/{patient_id}/pedigree/")
    fun getDoctorPatientPedigreeChart(@Path("patient_id") patientId: Int): Call<PedigreeChartResponse>

    @GET("doctor/report/{patient_id}/")
    fun getDoctorPatientReportDetail(@Path("patient_id") patientId: Int): Call<DoctorPatientReportDetailResponse>

    // =========================
    // CLINICAL NOTES (Doctor)
    // =========================
    @GET("doctor/patient/{patient_id}/clinical-notes/")
    fun getClinicalNotes(@Path("patient_id") patientId: Int): Call<ClinicalNotesListResponse>

    @POST("doctor/patient/{patient_id}/clinical-notes/")
    fun createClinicalNote(
        @Path("patient_id") patientId: Int,
        @Body body: ClinicalNoteCreateRequest
    ): Call<ClinicalNoteCreateResponse>

    // =========================
    // PATIENT
    // =========================
    @GET("patient/home/")
    fun getPatientHome(): Call<PatientHomeResponse>

    @POST("patient/family-members/")
    fun createFamilyMember(@Body body: FamilyMemberCreateRequest): Call<FamilyMemberCreateResponse>

    @PUT("patient/family-members/{member_id}/")
    fun updateFamilyMember(
        @Path("member_id") memberId: Int,
        @Body body: FamilyMemberUpdateRequest
    ): Call<FamilyMemberUpdateResponse>

    @DELETE("patient/family-members/{member_id}/delete/")
    fun deleteFamilyMember(@Path("member_id") memberId: Int): Call<ApiResponse>

    @GET("patient/family-overview/")
    fun getFamilyOverview(): Call<FamilyOverviewResponse>

    @GET("patient/pedigree/")
    fun getPedigreeChart(): Call<PedigreeChartResponse>

    // =========================
    // PROFILE (Doctor + Patient)
    // =========================
    @GET("me/")
    fun getMyProfile(): Call<MyProfileResponse>

    @PUT("me/")
    fun updateMyProfile(@Body body: UpdateProfileRequest): Call<UpdateProfileResponse>

    // =========================
    // NOTIFICATIONS
    // =========================
    @GET("notifications/")
    fun getNotifications(
        @Query("tab") tab: String = "all" // "all" or "unread"
    ): Call<NotificationsListResponse>

    @PATCH("notifications/{notif_id}/read/")
    fun markNotificationRead(@Path("notif_id") notifId: Int): Call<MarkNotificationReadResponse>

    @PATCH("notifications/read-all/")
    fun markAllNotificationsRead(): Call<ApiResponse>

    // =========================
    // FEEDBACK
    // =========================
    @POST("feedback/")
    fun submitFeedback(@Body body: FeedbackCreateRequest): Call<FeedbackCreateResponse>

    // =========================
    // DNA (Doctor)
    // =========================

    // Callback version
    @Multipart
    @POST("dna/upload/")
    fun uploadDnaReport(
        @Part file: MultipartBody.Part,
        @Part("patient_id") patientId: RequestBody
    ): Call<DnaUploadResponse>

    @POST("dna/run-detection/")
    fun runPatternDetection(@Body body: RunPatternDetectionRequest): Call<RunDetectionResponse>

    @GET("dna/latest-analysis/")
    fun getLatestAnalysis(): Call<LatestAnalysisResponse>

    // Coroutine version
    @Multipart
    @POST("dna/upload/")
    suspend fun uploadDnaReportSuspend(
        @Part file: MultipartBody.Part,
        @Part("patient_id") patientId: RequestBody
    ): Response<DnaUploadResponse>

    @POST("dna/run-detection/")
    suspend fun runPatternDetectionSuspend(
        @Body body: RunPatternDetectionRequest
    ): Response<RunDetectionResponse>

    @Multipart
    @POST("predict-pdf/")
    suspend fun predictFromPdf(
        @Part file: MultipartBody.Part
    ): Response<PredictPdfResponse>

    // =========================
    // PATTERN DETAIL
    // =========================
    @GET("patterns/{pattern_key}/")
    fun getPatternDetail(@Path("pattern_key") patternKey: String): Call<DoctorPatternDetailResponse>

    @GET("dna/recent-analyses/")
    fun getRecentAnalyses(): Call<RecentAnalysesResponse>

    @GET("dna/analysis/{analysis_id}/report/")
    fun getAnalysisReport(
        @Path("analysis_id") analysisId: Int
    ): Call<AnalysisReportResponse>


    @GET("dna/analysis/{analysis_id}/result/")
    fun getAnalysisResult(@Path("analysis_id") analysisId: Int): Call<AnalysisResultResponse>

    

}
