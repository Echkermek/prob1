package com.example.prob1

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

class FCMTokenManager {

    companion object {
        private const val TAG = "FCMTokenManager"

        // Данные из вашего service account файла
        private const val PROJECT_ID = "prob1-5c047"
        private const val CLIENT_EMAIL = "firebase-adminsdk-fbsvc@prob1-5c047.iam.gserviceaccount.com"
        private const val PRIVATE_KEY = """
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDFIgLtPVl9jSXa
            lJNoub66d7ViY4/SU1im3uMmj1YoMupsttN8HBaKMpTc0zKTKiplx8tJ09EAzsvL
            Q+L6MTB7KVvl2I333KzFkc0QapGMk5vNVzSQplMLU2i2bFFcRkfuazacUvvM/2m0
            JsoHyWhSC6BmpJu7KMwFT3FmMTE/7DkwTQyE4wSBdDpjFxjn5pyzoi9teA1budub
            343FlDk9p1UnVij1JngOIZAQwccjXVABpvNB6mhxL8z2Nt02tA9qbvMLx6ze/d4L
            arWsJUQWw01cpOuzVSPt/2ATzo0QiP5hDk1oHBVPSxm6prT1J9Y/SlVRAT1kPu/o
            32wPfdddAgMBAAECggEAGNJbg/vBlOl85rTmir04osoH8MeQiG8uLnTKLUTHGHCw
            y9eJcYsn22bbD6MYLyeUBxTNKBKfsnZXetSlc9NVw2xFwf8ugRMsPgRJydNhWE4N
            xCFC4SPQORDbkny5EeFXlVGyoqRgSEOwz5pCvhBCLnKAU+xOH5bXeaOzJ6byCJZB
            OwJ0R/4H6Q2xfcAAYeeEinXHW+Y9+aiYDPsJUM3YJBGBnguD+h3dlVmKHA2liQ0K
            +Wiqeb24O4l7oSbf8GTrUlvPKyoI48sRmL/R9qFSpESeth4x4t2VqtniHu6Dr1mk
            j/sGTpv1nq//J8k2QQThwldKsUvfo1bcqof/AYpRkQKBgQDv9dy91WUS9TVfZZw2
            dbS9lvLyNK2wmW76TmKkw0gDdpZ3IVC6EwqvF+05v29Hbb+LcCDJEYW7+Yvu+eLn
            MxhUAdNGxhOZYaQfmCrR2tkSQlzWjaLnBb8ZgQm8AGlNPL8OPIu/K216Ee9Cdj4v
            qMrcjOaCkWq4t0/vNwk+F/RH5wKBgQDST0u49KnAMB7Cz0WVU4wiTt6V5Rp3kNMU
            QVr2Sqs8DAJroBt1aNB4kzT5crXYuSKLaRTfiRzDgmIAJy+RheCciEc+XACjgLPK
            fiApmtgBgkZbLOqwYyXYhwhZOTk9aWbKvaa4IRBCTu32i4BIbKdYB7tmRkV1MES/
            4DnmsONuGwKBgQCNXlmn9RpQ6umAxEodw4ax00ZZ7zMMwbgh+yAeVXYNpEXM9XGr
            ziATO5Nk3CVg/YVFTs/h5VLWhZQtPEazUuTYSSMBpRxS1rVsb6d6buZH3ZT4SgVY
            +Ye2LNFhUxS2jj040+64t4sNyZcdnq/QQC8TdGcxMR6gqE6Nbe0iCda+rQKBgEVf
            ZskB26TLurnkCT3yqzz96ypiwlJCcX4y+MBZbRaHl5zM2YHnKy060bSfBCETILP/
            26TI47YSIiWOx4AnOyonQejuDD/iymu3IwDWVP5abL7SmC/K5McB890KpTj+tauW
            15HRRj25L4GuDojVXnYJRuW7tFmtrF/mpPLV0o+VAoGACNVYcZWM3GLUMcCnxTSL
            fB+Hv8lHSnXvjtOoH0NrViQbxYTtbDurqegei6E3DQYdWs/7gwyyoUuKJ3k5lyUh
            n91De375W7z1d6ksjfp65LL+27gASFeHLhrBr7+rfdvWL6ysSMHugLhDQ1Ho7ZgT
            stMm9ECd4J0ejF/9AI+NWJE=
        """

        private fun createJWT(): String {
            try {
                val header = JSONObject().apply {
                    put("alg", "RS256")
                    put("typ", "JWT")
                }

                val now = Date().time / 1000
                val payload = JSONObject().apply {
                    put("iss", CLIENT_EMAIL)
                    put("scope", "https://www.googleapis.com/auth/firebase.messaging")
                    put("aud", "https://oauth2.googleapis.com/token")
                    put("exp", now + 3600) // 1 hour
                    put("iat", now)
                }

                val headerBase64 = Base64.encodeToString(
                    header.toString().toByteArray(Charsets.UTF_8),
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )

                val payloadBase64 = Base64.encodeToString(
                    payload.toString().toByteArray(Charsets.UTF_8),
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )

                val dataToSign = "$headerBase64.$payloadBase64"

                // Подписываем JWT
                val keySpec = PKCS8EncodedKeySpec(
                    Base64.decode(PRIVATE_KEY.replace("\\s".toRegex(), ""), Base64.DEFAULT)
                )
                val keyFactory = KeyFactory.getInstance("RSA")
                val privateKey = keyFactory.generatePrivate(keySpec)

                val signature = Signature.getInstance("SHA256withRSA")
                signature.initSign(privateKey)
                signature.update(dataToSign.toByteArray(Charsets.UTF_8))

                val signatureBytes = signature.sign()
                val signatureBase64 = Base64.encodeToString(
                    signatureBytes,
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )

                return "$dataToSign.$signatureBase64"

            } catch (e: Exception) {
                Log.e(TAG, "Error creating JWT", e)
                throw e
            }
        }

        suspend fun getAccessToken(): String? {
            return withContext(Dispatchers.IO) {
                try {
                    val jwt = createJWT()

                    val client = OkHttpClient()
                    val formBody = FormBody.Builder()
                        .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        .add("assertion", jwt)
                        .build()

                    val request = Request.Builder()
                        .url("https://oauth2.googleapis.com/token")
                        .post(formBody)
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(response.body?.string() ?: "")
                        jsonResponse.getString("access_token")
                    } else {
                        Log.e(TAG, "Failed to get access token: ${response.code} - ${response.body?.string()}")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting access token", e)
                    null
                }
            }
        }
    }
}