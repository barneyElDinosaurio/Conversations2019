package xx.xxx.xxxx.ui;

/**
 * Created by m on 22/06/17.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nisrulz.sensey.OrientationDetector;
import com.github.nisrulz.sensey.ProximityDetector;
import com.github.nisrulz.sensey.Sensey;
import com.github.nisrulz.sensey.ShakeDetector;
import com.github.nisrulz.sensey.TouchTypeDetector;
import com.github.nisrulz.sensey.WaveDetector;

import xx.xxx.xxxx.R;
import xx.xxx.xxxx.receivers.USBReceiver;

public class Board extends Activity {


    int i = 0;
    int j = 0;
    boolean acercado = false;
    boolean paso1= false;
    boolean paso2 = false;
    boolean paso3 = false;


    private USBReceiver mEventReceiver = new USBReceiver();

    public Sensey miSensei;

    private int size;
    TableLayout mainBoard;
    TextView tv_turn;
    char [][] board;
    char turn;
    Button entrar,entrar2,entrar3;

    ProximityDetector.ProximityListener proximityListener;
    WaveDetector.WaveListener waveListener;
    ShakeDetector.ShakeListener shakeListener;
    TouchTypeDetector.TouchTypListener touchTyperListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        registerReceiver(mEventReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));

        size = Integer.parseInt(getString(R.string.size_of_board));
        board = new char [size][size];
        mainBoard = (TableLayout) findViewById(R.id.mainBoard);
        tv_turn = (TextView) findViewById(R.id.turn);


        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.GRAY));


        resetBoard();
        tv_turn.setText("Turno de: "+turn);

        for(int i = 0; i<mainBoard.getChildCount(); i++){
            TableRow row = (TableRow) mainBoard.getChildAt(i);
            for(int j = 0; j<row.getChildCount(); j++){
                TextView tv = (TextView) row.getChildAt(j);
                tv.setText("       ");
                tv.setOnClickListener(Move(i, j, tv));
            }
        }

        Button rstbtn = (Button) findViewById(R.id.reset);
        rstbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent current = getIntent();
                finish();
                startActivity(current);
            }
        });


        //Botones

        entrar3 = (Button) findViewById(R.id.entrar3);
        entrar2 = (Button) findViewById(R.id.entrar2);
        entrar = (Button) findViewById(R.id.entrar);

        entrar.setVisibility(View.INVISIBLE);
        entrar2.setVisibility(View.INVISIBLE);
        entrar3.setVisibility(View.INVISIBLE);

        entrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paso1 = true;
                paso2 = false;
                //startActivity(new Intent(getApplicationContext(),ConversationActivity.class));

            }
        });


        entrar2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(paso1 == true){
                    paso2 = true;
                }else {
                    paso1 = false;
                    paso2=false;
                }
            }
        });


        entrar3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paso1 == true && paso2 == true){
                    startActivity(new Intent(getApplicationContext(),ConversationActivity.class));
                    paso1 = false;
                    paso2 = false;

                    entrar.setVisibility(View.INVISIBLE);
                    entrar2.setVisibility(View.INVISIBLE);
                    entrar3.setVisibility(View.INVISIBLE);

                }else{
                    paso1 = false;
                    paso2 = false;
                }
            }
        });

        //entrada



        miSensei.getInstance().init(getApplicationContext());//sensey



        proximityListener=new ProximityDetector.ProximityListener() {


            @Override public void onNear() {
                // Near to device
                // Toast.makeText(Board.this, "near #"+i , Toast.LENGTH_SHORT).show();

              //  if(acercado == true){//aqui ya paso los 5 tapasos
                    i +=1;
                 //   Toast.makeText(Board.this, "Acercado", Toast.LENGTH_SHORT).show();
                    //reseteando all

                    if(i>=5){
                        i=0;
                        miSensei.getInstance().stopProximityDetection(proximityListener);
                       // startActivity(new Intent(getApplicationContext(),ConversationActivity.class));
                        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                        // Vibrate for 500 milliseconds
                        v.vibrate(500);
                        //acitvar botones ocultos

                        entrar.setVisibility(View.VISIBLE);
                        entrar2.setVisibility(View.VISIBLE);
                        entrar3.setVisibility(View.VISIBLE);

                    }

                }



            @Override public void onFar() {
                // Far from device
               // if(i>=5)i=0;//other reset iterator
                //  Toast.makeText(EditAccountActivity.this, "Lejos", Toast.LENGTH_SHORT).show();
            }
        };

        miSensei.getInstance().startProximityDetection(proximityListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        miSensei.getInstance().startProximityDetection(proximityListener);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        miSensei.getInstance().stopProximityDetection(proximityListener);


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miSensei.getInstance() != null)
        miSensei.getInstance().stopProximityDetection(proximityListener);


    }

    @Override
    protected void onStop() {
        super.onStop();
        if (miSensei.getInstance() != null)
        miSensei.getInstance().stopProximityDetection(proximityListener);


    }

    protected void resetBoard(){
        turn = 'X';
        for(int i = 0; i<size; i++){
            for(int j = 0; j<size; j++){
                board[i][j] = ' ';
            }
        }
    }

    protected int gameStatus(){

        //0 Continue
        //1 X Wins
        //2 O Wins
        //-1 Draw

        int rowX = 0, colX = 0, rowO = 0, colO = 0;
        for(int i = 0; i<size; i++){
            if(check_Row_Equality(i,'X'))
                return 1;
            if(check_Column_Equality(i, 'X'))
                return 1;
            if(check_Row_Equality(i,'O'))
                return 2;
            if(check_Column_Equality(i,'O'))
                return 2;
            if(check_Diagonal('X'))
                return 1;
            if(check_Diagonal('O'))
                return 2;
        }

        boolean boardFull = true;
        for(int i = 0; i<size; i++){
            for(int j= 0; j<size; j++){
                if(board[i][j]==' ')
                    boardFull = false;
            }
        }
        if(boardFull)
            return -1;
        else return 0;
    }

    protected boolean check_Diagonal(char player){
        int count_Equal1 = 0,count_Equal2 = 0;
        for(int i = 0; i<size; i++)
            if(board[i][i]==player)
                count_Equal1++;
        for(int i = 0; i<size; i++)
            if(board[i][size-1-i]==player)
                count_Equal2++;
        if(count_Equal1==size || count_Equal2==size)
            return true;
        else return false;
    }

    protected boolean check_Row_Equality(int r, char player){
        int count_Equal=0;
        for(int i = 0; i<size; i++){
            if(board[r][i]==player)
                count_Equal++;
        }

        if(count_Equal==size)
            return true;
        else
            return false;
    }

    protected boolean check_Column_Equality(int c, char player){
        int count_Equal=0;
        for(int i = 0; i<size; i++){
            if(board[i][c]==player)
                count_Equal++;
        }

        if(count_Equal==size)
            return true;
        else
            return false;
    }

    protected boolean Cell_Set(int r, int c){
        return !(board[r][c]==' ');
    }

    protected void stopMatch(){
        for(int i = 0; i<mainBoard.getChildCount(); i++){
            TableRow row = (TableRow) mainBoard.getChildAt(i);
            for(int j = 0; j<row.getChildCount(); j++){
                TextView tv = (TextView) row.getChildAt(j);
                tv.setOnClickListener(null);
            }
        }
    }

    View.OnClickListener Move(final int r, final int c, final TextView tv){

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!Cell_Set(r,c)) {
                    board[r][c] = turn;
                    if (turn == 'X') {
                        tv.setText(R.string.X);
                        turn = 'O';
                    } else if (turn == 'O') {
                        tv.setText(R.string.O);
                        turn = 'X';
                    }
                    if (gameStatus() == 0) {
                        tv_turn.setText("Turno de: " + turn);
                    }
                    else if(gameStatus() == -1){
                        tv_turn.setText("Game: Draw");
                        stopMatch();
                    }
                    else{
                        tv_turn.setText(turn+" Perdistes!");
                        stopMatch();
                    }
                }
                else{
                    tv_turn.setText(tv_turn.getText()+" Selecciona una celda q no este ocupada");
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_board, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
