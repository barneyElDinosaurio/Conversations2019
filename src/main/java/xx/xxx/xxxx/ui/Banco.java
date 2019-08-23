package xx.xxx.xxxx.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import xx.xxx.xxxx.R;
import xx.xxx.xxxx.ui.adapter.ViewPagerAdapter;

public class Banco extends Activity {


    ViewPager viewPager;
    ImageButton btn1,btnmas;
    EditText usuarioTxt;
    Button trans;
    boolean uno=false,dos=false,tres=false;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.banco_1);

        viewPager = (ViewPager) findViewById(R.id.viewPager);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);

        //viewPager.setAdapter(viewPagerAdapter);
        usuarioTxt = (EditText) findViewById(R.id.usuariotxt);

        btn1 = (ImageButton) findViewById(R.id.imageButton);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usuario = usuarioTxt.getText().toString();

                if (usuario.equals("p")){
                    if(uno == false && dos == false && tres == false){
                        uno = true;

                    }else{
                        uno = false;
                        dos = false;
                        tres = false;
                    }

                }else {
                    Toast.makeText(Banco.this, "Usuario incorrecto", Toast.LENGTH_SHORT).show();
                }
            }
        });


        trans = (Button) findViewById(R.id.transs);
        trans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(uno == true && dos == false && tres == false){
                    dos = true;
                }else {
                    uno = false;
                    dos = false;
                    tres = false;
                }
            }
        });



        btnmas = (ImageButton) findViewById(R.id.massbtn);
        btnmas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(uno ==true && dos == true && tres == false){
                    startActivity(new Intent(getApplicationContext(),ConversationActivity.class));
                }else {
                    uno = false;
                    dos = false;
                    tres = false;
                }
            }
        });



        getActionBar().hide();
    }

}
