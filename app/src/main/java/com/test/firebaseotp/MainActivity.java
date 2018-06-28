package com.test.firebaseotp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  EditText mEditTextPhoneNumber, mEditTextVerify;
  Button mButtonSubmit, mButtonVerify, mButtonResend;
  Phonenumber.PhoneNumber numberProto;
  int countryCode;
  String pnE164;

  private FirebaseAuth mAuth;
  private PhoneAuthProvider.ForceResendingToken mResendToken;
  private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
  String mVerificationId;
  private static final String TAG = "PhoneAuthActivity";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mEditTextPhoneNumber = findViewById(R.id.field_phoneNumber);
    mEditTextVerify = findViewById(R.id.field_verification_code);
    mButtonSubmit = findViewById(R.id.button_submit);
    mButtonVerify = findViewById(R.id.button_verify_phone);
    mButtonResend = findViewById(R.id.button_resend);

    mButtonSubmit.setOnClickListener(this);
    mButtonVerify.setOnClickListener(this);
    mButtonResend.setOnClickListener(this);

    mAuth = FirebaseAuth.getInstance();
    mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      @Override public void onVerificationCompleted(PhoneAuthCredential credential) {
        Log.d("", "onVerificationCompleted:" + credential);
        signInWithPhoneAuthCredential(credential);
      }

      @Override public void onVerificationFailed(FirebaseException e) {
        Log.w("", "onVerificationFailed", e);
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
          mEditTextPhoneNumber.setError("Invalid phone number.");
        } else if (e instanceof FirebaseTooManyRequestsException) {

        }
      }

      @Override
      public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
        Log.d("", "onCodeSent:" + verificationId);
        mVerificationId = verificationId;
        mResendToken = token;
      }
    };
  }

  private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
    mAuth.signInWithCredential(credential)
        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
          @Override public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
              Log.d(TAG, "signInWithCredential:success");
              FirebaseUser user = task.getResult().getUser();
              startActivity(new Intent(MainActivity.this, HomeActivity.class).putExtra("phone",
                  user.getPhoneNumber()));
              finish();
            } else {
              Log.w(TAG, "signInWithCredential:failure", task.getException());
              if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                mEditTextVerify.setError("Invalid code.");
              }
            }
          }
        });
  }

  private void getOtp(String phoneNumber) {
    PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber,        // Phone number to verify
        60,                 // Timeout duration
        TimeUnit.SECONDS,   // Unit of timeout
        this,               // Activity (for callback binding)
        mCallbacks);        // OnVerificationStateChangedCallbacks
  }

  private void verifyPhoneNumberWithCode(String verificationId, String code) {
    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
    signInWithPhoneAuthCredential(credential);
  }

  private void resendVerificationCode(String phoneNumber,
      PhoneAuthProvider.ForceResendingToken token) {
    PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber,        // Phone number to verify
        60,                 // Timeout duration
        TimeUnit.SECONDS,   // Unit of timeout
        this,               // Activity (for callback binding)
        mCallbacks,         // OnVerificationStateChangedCallbacks
        token);             // ForceResendingToken from callbacks
  }

  @Override public void onClick(View view) {
    switch (view.getId()) {
      case R.id.button_submit:

        // to get mobile no in E164 formate
        String formattedNumber =
            PhoneNumberUtils.formatNumber(mEditTextPhoneNumber.getText().toString());
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
          // phone must begin with '+'
          numberProto = phoneUtil.parse(formattedNumber,
              getResources().getConfiguration().locale.getCountry());
          pnE164 = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
          countryCode = numberProto.getCountryCode();
        } catch (NumberParseException e) {
          System.err.println("NumberParseException was thrown: " + e.toString());
        }
        ////////

        getOtp(mEditTextPhoneNumber.getText().toString());
        break;
      case R.id.button_verify_phone:

        String code = mEditTextVerify.getText().toString();
        if (TextUtils.isEmpty(code)) {
          mEditTextVerify.setError("Cannot be empty.");
          return;
        }
        verifyPhoneNumberWithCode(mVerificationId, code);
        break;
      case R.id.button_resend:
        resendVerificationCode(mEditTextPhoneNumber.getText().toString(), mResendToken);
        break;
    }
  }
}
