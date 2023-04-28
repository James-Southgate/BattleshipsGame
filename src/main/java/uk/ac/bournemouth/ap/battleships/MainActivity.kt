package uk.ac.bournemouth.ap.battleships

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uk.ac.bournemouth.ap.battleships.GridView.OnPlayerAttackListener

class MainActivity : AppCompatActivity(), OnPlayerAttackListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val opponentGridView = findViewById<GridView>(R.id.opponent_gridview)

        opponentGridView.onPlayerAttackListener = this
    }

    override fun onPlayerAttack() {
        val opponentGridView = findViewById<PlayerView>(R.id.player_gridview)
        opponentGridView.opponentAttack()
    }
}