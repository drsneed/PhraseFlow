package com.dstech.phraseflow.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.dstech.phraseflow.R

object ToastUtil {
    fun showCustomToast(context: Context, message: String) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)

        val text = layout.findViewById<TextView>(R.id.custom_toast_message)
        text.text = message

        val toast = Toast(context.applicationContext)
        toast.setGravity(Gravity.TOP, 0, 110)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}