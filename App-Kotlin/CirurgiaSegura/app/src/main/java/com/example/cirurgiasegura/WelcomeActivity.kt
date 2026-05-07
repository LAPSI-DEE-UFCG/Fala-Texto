package com.example.cirurgiasegura

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cirurgiasegura.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ao clicar em "Pular instruções", vai direto para o checklist (MainActivity)
        binding.btnSkip.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java)
            intent.putStringArrayListExtra("questions", null)
            startActivity(intent)
            finish() // fecha a WelcomeActivity para não voltar a ela ao apertar "voltar"

        }
        binding.btnFormqlq.setOnClickListener {
            val intent = Intent(this, NewListActivity::class.java)
            startActivity(intent)
            finish() // fecha a WelcomeActivity para não voltar a ela ao apertar "voltar"


        }
    }
}
