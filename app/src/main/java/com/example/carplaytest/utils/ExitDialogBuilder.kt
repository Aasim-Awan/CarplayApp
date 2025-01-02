package com.example.carplaytest.utils

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import androidx.appcompat.app.AlertDialog
import com.example.carplaytest.databinding.ExitDialogBinding

//import com.google.android.ads.nativetemplates.TemplateView
//import com.threedev.translator.databinding.ExitDialogBinding

class ExitDialogBuilder(val activity: Activity) {

    var message: String? = null
    var title: String? = null
    var buttonText: String = "Ok"
    var cancelText: String = "Cancel"
    var buttonClickListener: OnOkClick? = null
    var buttonCancelClickListener: OnCancelClick? = null
    var icon: Int? = null
    //  val adsManager = AdsManager.getInstance(activity)

    data class Builder(var activity: Activity) {
        private val obj = ExitDialogBuilder(activity)

        fun withMessage(message: String) = apply { obj.message = message }
        fun withTitle(title: String) = apply { obj.title = title }
        fun withIcon(icon: Int) = apply { obj.icon = icon }
        fun withButtonListener(text: String, listener: OnOkClick) = apply {
            obj.buttonText = text
            obj.buttonClickListener = listener
        }
        fun withCancelButtonListener(text: String, listener: OnCancelClick) = apply {
            obj.cancelText = text
            obj.buttonCancelClickListener = listener
        }
        fun build() = obj.buildDialog()
    }

    fun buildDialog(): AlertDialog {
        val builder = AlertDialog.Builder(activity)
        val dialogs = builder.create()
        dialogs.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogs.window!!.setDimAmount(0.6f)
        dialogs.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Use view binding to inflate the custom dialog layout
        val dialogBinding = ExitDialogBinding.inflate(activity.layoutInflater)
        dialogs.setView(dialogBinding.root)

        // Set the title, message, and icon if provided
        title?.let {
            dialogBinding.txtTitle.text = it
            dialogBinding.txtTitle.visibility = View.VISIBLE
        } ?: run {
            dialogBinding.txtTitle.visibility = View.GONE
        }

        message?.let {
            dialogBinding.txtMessage.text = it
            dialogBinding.txtMessage.visibility = View.VISIBLE
        } ?: run {
            dialogBinding.txtMessage.visibility = View.GONE
        }

//        icon?.let {
//            dialogBinding.iconImage.setImageResource(it)
//            dialogBinding.iconImage.visibility = View.VISIBLE
//        } ?: run {
//            dialogBinding.iconImage.visibility = View.GONE
//        }

        // Set button texts and listeners
        dialogBinding.btnOk.text = buttonText
        dialogBinding.btnOk.setOnClickListener {
            buttonClickListener?.onClick(dialogs) ?: dialogs.dismiss()
        }

      //  dialogBinding.btnCancel.text = cancelText
        dialogBinding.ivClose.setOnClickListener {
            buttonCancelClickListener?.onCancel(dialogs) ?: dialogs.dismiss()
        }

        dialogBinding.ivClose.setOnClickListener {
            dialogs.dismiss()
        }

        dialogs.setCancelable(false)
        return dialogs
    }


    interface OnOkClick {
        fun onClick(dialogs: AlertDialog)
    }

    interface OnCancelClick {
        fun onCancel(dialogs: AlertDialog)
    }
}
