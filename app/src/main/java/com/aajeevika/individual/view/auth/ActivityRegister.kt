package com.aajeevika.individual.view.auth

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.aajeevika.individual.R
import com.aajeevika.individual.baseclasses.BaseActivityVM
import com.aajeevika.individual.databinding.ActivityRegisterBinding
import com.aajeevika.individual.utility.*
import com.aajeevika.individual.utility.UtilityActions.debugOtp
import com.aajeevika.individual.utility.UtilityActions.showMessage
import com.aajeevika.individual.utility.app_enum.VerificationType
import com.aajeevika.individual.view.auth.viewmodel.RegisterViewModel
import com.aajeevika.individual.view.dialog.MediaDialog
import com.yalantis.ucrop.UCrop
import java.io.File

class ActivityRegister : BaseActivityVM<ActivityRegisterBinding, RegisterViewModel>(
    R.layout.activity_register,
    RegisterViewModel::class
) {
    private var mobileNo: String? = null
    private lateinit var userProfileImageFile: File

    private val galleryResultCallback = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            val file = File(cacheDir, Constant.USER_PROFILE)
            if(!file.exists()) file.createNewFile()

            UtilityActions.startUcropActivityForResult(
                this, it, file.toUri(), uCropCallBack, 1F, 1F, true
            )
        }
    }

    private val cameraResultCallback = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it == true) {
            val file = File(cacheDir, Constant.USER_PROFILE)

            UtilityActions.startUcropActivityForResult(
                this, file.toUri(), file.toUri(), uCropCallBack, 1F, 1F, true
            )
        }
    }

    private val uCropCallBack = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        it?.data?.let { intent ->
            UCrop.getOutput(intent)?.toFile()?.let { file ->
                viewDataBinding.userProfileImg.setImageDrawable(null)
                viewDataBinding.userProfileImg.setImageURI(file.toUri())
                userProfileImageFile = file
            }
        }
    }

    override fun observeData() {
        super.observeData()

        viewModel.otpLiveData.observe(this, {
            debugOtp(it.otp)

            val intent = Intent(this, ActivityOtpVerification::class.java)
            intent.putExtra(Constant.VERIFICATION_TYPE, VerificationType.REGISTER.name)
            intent.putExtra(Constant.MOBILE_NUMBER, mobileNo)
            startActivity(intent)
        })
    }

    override fun initListener() {
        viewDataBinding.run {
            editProfileBtn.setOnClickListener {
                MediaDialog(
                    context = this@ActivityRegister,
                    cameraFileName = Constant.USER_PROFILE,
                    galleryCallback = galleryResultCallback,
                    cameraCallback = cameraResultCallback,
                ).show()
            }

            signUpBtn.setOnClickListener {
                if(registerAgreementCheckbox.isChecked) {
                    val name = inputName.text.toString().trim()
                    val email = inputEmailId.text.toString().trim()
                    val mobileNumber = inputMobileNumber.text.toString().trim()
                    val password = inputPassword.text.toString().trim()
                    val confirmPassword = inputConfirmPassword.text.toString().trim()

                    validateFormData(name, email, mobileNumber, password, confirmPassword)?.let { error ->
                        root.showMessage(error)
                    } ?: run {
                        mobileNo = mobileNumber

                        viewModel.requestUserRegistration(userProfileImageFile, name, email, mobileNumber, password)
                    }
                } else {
                    root.showMessage(getString(R.string.accept_terms_conditions_and_privacy_policy))
                }
            }

            clickToLoginBtn.setOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun validateFormData(
        name: String,
        email: String,
        mobileNumber: String,
        password: String,
        confirmPassword: String,
    ) : String? {
        return if(!::userProfileImageFile.isInitialized) getString(R.string.select_profile_image)
        else if(name.isEmpty() || email.isEmpty() || mobileNumber.isEmpty()) getString(R.string.fill_all_the_fields)
        else if(!UtilityActions.isValidMobileNumber(mobileNumber)) getString(R.string.enter_valid_mobile_number)
        else if(password.isEmpty()) getString(R.string.enter_password)
        else if(password.length < 8) getString(R.string.password_length_error)
        else if(password != confirmPassword) getString(R.string.password_doesnt_match)
        else null
    }
}
