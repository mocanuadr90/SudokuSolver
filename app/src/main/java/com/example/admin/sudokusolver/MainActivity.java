package com.example.admin.sudokusolver;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private SudokuCanvas canvas;
    private Button solveBtn, solveBtnDisabled, resetBtn, resetBtnDisabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            canvas = (SudokuCanvas) findViewById(R.id.sudoku_canvas);
            solveBtn = (Button) findViewById(R.id.solve_btn);
            solveBtnDisabled = (Button) findViewById(R.id.solve_btn_disabled);
            resetBtn = (Button) findViewById(R.id.reset_btn);
            resetBtnDisabled = (Button) findViewById(R.id.reset_btn_disabled);

            solveBtnDisabled.setVisibility(View.GONE);
            resetBtnDisabled.setVisibility(View.GONE);

            solveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    canvas.solve();
                }
            });

            resetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    canvas.reset();
                    solveBtnDisabled.setVisibility(View.INVISIBLE);
                    solveBtn.setEnabled(true);
                    solveBtn.setVisibility(View.VISIBLE);
                }
            });
        }
        catch(Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
