package com.pedromassango.herenow.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.facebook.accountkit.AccountKitLoginResult
import com.facebook.accountkit.ui.AccountKitActivity
import com.pedromassango.herenow.R
import com.pedromassango.herenow.app.AccountKitSettings
import com.pedromassango.herenow.app.HereNow.Companion.logcat
import com.pedromassango.herenow.data.PreferencesHelper

/**
 * Created by pedromassango on 12/28/17.
 */
class LoginActivity : AppCompatActivity(), LoginContract.View {

    lateinit var presenter : LoginPresenter

    // To send resultIntent back to main activity
    lateinit var resultIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        presenter = LoginPresenter(this,
                PreferencesHelper(this))

        presenter.startLoginRequest()

    }

    override fun startAccountKitActivity() {
        val intent = Intent(this, AccountKitActivity::class.java)
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, AccountKitSettings.build(this))
        startActivityForResult(intent, LoginContract.ACCOUNT_KIT_LOGIN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LoginContract.ACCOUNT_KIT_LOGIN_REQUEST_CODE) {
            logcat("onActivityResult:  ACCOUNT_KIT_LOGIN_REQUEST_CODE")

            val loginResult = data!!.getParcelableExtra<AccountKitLoginResult>(AccountKitLoginResult.RESULT_KEY)
            val toastMessage: String

            // Login Error

            if (loginResult.error != null) {
                logcat("onActivityResult:  ACCOUNT_KIT_LOGIN_REQUEST_CODE - error")
                toastMessage = loginResult.error!!.userFacingMessage
                showToast(toastMessage)
                showToast(R.string.login_error)

                // show account kit activity again
               presenter.startLoginRequest()
                return
            }

            // Login cancelled

            if (loginResult.wasCancelled()) {
                logcat("onActivityResult: ACCOUNT_KIT_LOGIN_REQUEST_CODE - cancelled")
                showToast(R.string.login_cancelled)

                // show account kit activity again
                presenter.startLoginRequest()
                return
            }

            // Login success

            // Authorization code OR accessToken
            val code: String

            if (loginResult.accessToken != null) {
                logcat("onActivityResult: ACCOUNT_KIT_LOGIN_REQUEST_CODE - Success")

                val token = loginResult.accessToken!!.token
                code = loginResult.accessToken!!.accountId
                logcat("Success: " +String.format("Success: <TOKEN: %s> \n<ACCOUNT ID: %s> ", token, code))
            } else {
                logcat("onActivityResult: ACCOUNT_KIT_LOGIN_REQUEST_CODE - Success")

                code = loginResult.authorizationCode!!.substring(0, 10)

                logcat("Success: " + String.format("Success: <AUTH CODE: %s> ", code))
            }

            // If you have an authorization code, retrieve it from
            // loginResult.getAuthorizationCode()
            // and pass it to your server and exchange it for an access token.

            // update resultIntent intent, will be used later
            resultIntent = Intent()
            resultIntent.putExtra(LoginContract.ACTIVITY_LOGIN_RESULT_KEY, code)

            // Success! save account information
            presenter.saveAccountInfo()
        }
    }

    private fun showToast(toastMessage: String?) {
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(@StringRes toastMessage: Int ) {
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
    }

    /*
        Return to the activity that start this activity, with the login resultIntent.
     */
    override fun closeActivityAndSendResultBAck() {
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}