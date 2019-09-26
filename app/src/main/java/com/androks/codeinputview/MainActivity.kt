package com.androks.codeinputview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        inputCodeView.setOnCodeInputedListener {
            Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
        }
    }
}
