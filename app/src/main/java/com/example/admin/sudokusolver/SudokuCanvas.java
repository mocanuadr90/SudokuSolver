package com.example.admin.sudokusolver;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Point;
import android.util.Pair;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.math.MathContext;

public class SudokuCanvas extends View {

    private int                        selectedRow;
    private int                        selectedCol;
    private int                        selectedDigit;
    private int[][]                    configuration;
    private Pair<Integer, Integer>[][] coordinates;
    private boolean                    solved;
    private boolean                    partiallySolved;
    private Paint                      black, white, red, highlight, dark_orange, transparent_red;
    private Context                    context;
    private Point                      size; //gets the size of the screen as a point whose coordinates represent the width and the height
    private int                        borderYUpper, borderYLower; //sets the upper and lower limits of the digit selection border on the oY axis
    private boolean                    taskRunning;
    ArrayList<Pair<Integer,Integer>>   invalids;

    public SudokuCanvas(Context context, AttributeSet attrs){
        super(context,attrs);

        this.context = context;
        initialize();
    }

    public Point getScreenSize(){

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);

        return size;
    }

    private void initialize(){
        selectedRow = -1;
        selectedCol = -1;

        size = getScreenSize();

        borderYUpper = size.x + 100;
        borderYLower = borderYUpper + 90;

        configuration = new int[10][10];
        coordinates = new Pair[9][9];

        invalids = new ArrayList<>();

        black = new Paint();
        black.setColor(Color.rgb(0,5,12));
        black.setTextSize(80f);

        white = new Paint();
        white.setColor(Color.WHITE);

        red = new Paint();
        red.setColor(Color.RED);
        red.setTextSize(80f);

        highlight = new Paint();
        highlight.setColor(Color.rgb(0, 246, 255));
        highlight.setStrokeWidth(10);

        dark_orange = new Paint();
        dark_orange.setColor(Color.rgb(158, 140, 75));
        dark_orange.setTextSize(70f);

        transparent_red = new Paint();
        transparent_red.setColor(Color.parseColor("#80FF0000"));

        SudokuSolver.getInstance().setListener(this);
    }

    //draw the game board
    public void drawBoard(Canvas canvas){
        int x, y;
        int offsetX, offsetY;

        canvas.drawColor(Color.rgb(140, 26, 255));
        canvas.drawRect(0,0,size.x,size.x,black);   //game board background

        offsetY=40;

        //draw the squares
        for(int row=0; row<9; row++) {
            offsetX = 40;

            if (row > 0 && row % 3 == 0) {
                offsetY += 10;
            }

            y = (row * 100) + 10 * row + offsetY;

            for (int col = 0; col < 9; col++) {

                if (col > 0 && col % 3 == 0) {
                    offsetX += 10;
                }

                x = (col * 100) + 10 * col + offsetX;

                coordinates[row][col] = new Pair<>(x, y);

                canvas.drawRect(x, y, x + 100, y + 100, white);
            }
        }
    }

    //draw digits
    public void drawDigits(Canvas canvas){
        for(int i=0; i<9; i++){
            for(int j=0; j<9; j++){

                //draw the digits inserted by the user
                if(configuration[i+1][j+1] != 0){
                    canvas.drawText(String.valueOf(configuration[i+1][j+1]),
                            coordinates[i][j].first+30,
                            coordinates[i][j].second+80,
                            black);
                }
                //draw the resulting digits after solving the game
                else{
                    if(solved || partiallySolved){
                        if(SudokuSolver.getInstance().getBoard()[i+1][j+1] != 0){
                            canvas.drawText(String.valueOf(SudokuSolver.getInstance().getBoard()[i+1][j+1]),
                                    coordinates[i][j].first+30,
                                    coordinates[i][j].second+80,
                                    red);
                        }
                    }
                }

                //highlight the selected gap
                if(!taskRunning && (i == selectedRow && j==selectedCol)) {

                    //draw the upper line
                    canvas.drawLine(coordinates[selectedRow][selectedCol].first, coordinates[selectedRow][selectedCol].second,
                            coordinates[selectedRow][selectedCol].first + 100, coordinates[selectedRow][selectedCol].second,
                            highlight);

                    //draw the right line
                    canvas.drawLine(coordinates[selectedRow][selectedCol].first + 100, coordinates[selectedRow][selectedCol].second,
                            coordinates[selectedRow][selectedCol].first + 100, coordinates[selectedRow][selectedCol].second + 100,
                            highlight);

                    //draw the lower line
                    canvas.drawLine(coordinates[selectedRow][selectedCol].first + 100, coordinates[selectedRow][selectedCol].second + 100,
                            coordinates[selectedRow][selectedCol].first, coordinates[selectedRow][selectedCol].second + 100,
                            highlight);

                    //draw the left line
                    canvas.drawLine(coordinates[selectedRow][selectedCol].first, coordinates[selectedRow][selectedCol].second + 100,
                            coordinates[selectedRow][selectedCol].first, coordinates[selectedRow][selectedCol].second,
                            highlight);
                }
            }
        }
    }

    //draw the digit selection border
    public void drawDigitSelectionBorder(Canvas canvas){
        int x, y;
        int offsetX;

        if(!taskRunning && selectedRow != -1){
            //canvas.drawRect(40,1280,1000,1000,black);  //background-ul chenarului

            offsetX = 40;
            y = borderYUpper;

            for(int col=0; col<10; col++){
                x = (col*90)+(10*(col+1))+offsetX;
                canvas.drawRect(x,y,x+90,y+90,white);

                if(col != 0) {
                    canvas.drawText(String.valueOf(col), x + 25, y + 70, dark_orange);
                }
            }
        }
    }

    //highlight invalid rows, columns or 3x3 squares
    public void highlightInvalids(Canvas canvas){
        for(int i=0; i<invalids.size(); i++){
            Pair<Integer, Integer> p = invalids.get(i);

            //0 = line
            if(p.first == 0){
                canvas.drawRect(coordinates[p.second-1][0].first, coordinates[p.second-1][0].second,
                                coordinates[p.second-1][8].first+100, coordinates[p.second-1][8].second+100,
                                transparent_red);
            }

            //1 = column
            else if(p.first == 1){
                canvas.drawRect(coordinates[0][p.second-1].first, coordinates[0][p.second-1].second,
                                coordinates[8][p.second-1].first+100, coordinates[8][p.second-1].second+100,
                                transparent_red);
            }

            //2 = box
            else{
                int row1 = ((p.second-1)/3)*3;
                int col1 = ((p.second-1)%3)*3;
                int row2 = row1+2;
                int col2 = col1+2;

                canvas.drawRect(coordinates[row1][col1].first, coordinates[row1][col1].second,
                                coordinates[row2][col2].first+100, coordinates[row2][col2].second+100,
                                transparent_red);
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas){
        try {
            drawBoard(canvas);
            drawDigits(canvas);
            drawDigitSelectionBorder(canvas);
            highlightInvalids(canvas);
        }
        catch(Exception ex){
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected void insertDigit(float x, float y){

        if(y >= borderYUpper && y <= borderYLower){
            selectedDigit = (((int) x - 40) /100) > 9 ? 9:(((int) x - 40) /100);
            configuration[selectedRow + 1][selectedCol + 1] = selectedDigit;
        }
        else if(y>=0 && y <= 1080){

            selectedCol = ((int) x - 40) / 110;
            selectedRow = ((int) y - 40) / 110;

            if (selectedCol < 0) {
                selectedCol = 0;
            }

            if (selectedCol > 8) {
                selectedCol = 8;
            }

            if (selectedRow < 0) {
                selectedRow = 0;
            }

            if (selectedRow > 8) {
                selectedRow = 8;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        float x = event.getX();
        float y = event.getY();

        if(event.getAction() == MotionEvent.ACTION_DOWN){
            insertDigit(x,y);
            invalidate();
        }
        return true;
    }

    private void deselect(){
        selectedCol = -1;
        selectedRow = -1;
    }

    public void solve(){

        try {
            SudokuSolver.getInstance().clearGame();

            for (int i = 1; i < 10; i++) {
                for (int j = 1; j < 10; j++) {
                    if (configuration[i][j] != 0) {
                        SudokuSolver.getInstance().insertDigit(i, j, configuration[i][j]);
                    }
                }
            }

            //check configuration for validity first
            invalids = SudokuSolver.getInstance().validateConfiguration();

            if(invalids.size() > 0){
                invalidate();
                Toast.makeText(context, "Game configuration is invalid!", Toast.LENGTH_LONG).show();
                return;
            }

            ((Activity)context).getWindow().getDecorView().findViewById(R.id.solve_btn).setEnabled(false);
            ((Activity)context).getWindow().getDecorView().findViewById(R.id.solve_btn).setVisibility(View.GONE);
            ((Activity)context).getWindow().getDecorView().findViewById(R.id.solve_btn_disabled).setVisibility(View.VISIBLE);

            ((Activity)context).getWindow().getDecorView().findViewById(R.id.reset_btn).setEnabled(false);
            ((Activity)context).getWindow().getDecorView().findViewById(R.id.reset_btn).setVisibility(View.GONE);
            ((Activity)context).getWindow().getDecorView().findViewById(R.id.reset_btn_disabled).setVisibility(View.VISIBLE);

            taskRunning = true;
            invalidate();
            SudokuSolver.getInstance().createNewTask().execute();
        }
        catch(Exception ex){
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void showResult(Boolean result){
        if (result) {
            solved = true;
        } else {
            partiallySolved = true;
            Toast.makeText(context, "That's the best i could do", Toast.LENGTH_LONG).show();
        }

        ((Activity) context).getWindow().getDecorView().findViewById(R.id.reset_btn_disabled).setVisibility(View.INVISIBLE);
        ((Activity) context).getWindow().getDecorView().findViewById(R.id.reset_btn).setEnabled(true);
        ((Activity) context).getWindow().getDecorView().findViewById(R.id.reset_btn).setVisibility(View.VISIBLE);
        invalidate();
    }

    public void reset(){

        //sterge configuratia jocului
        for(int i=1; i<10; i++) {
            for (int j = 1; j < 10; j++) {
                configuration[i][j] = 0;
            }
        }
        taskRunning = false;
        solved = false;
        partiallySolved = false;
        invalids = new ArrayList<>();
        deselect();
        SudokuSolver.getInstance().clearGame();
        invalidate();
    }
}
