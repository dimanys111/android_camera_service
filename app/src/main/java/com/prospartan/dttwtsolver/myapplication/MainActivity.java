package com.prospartan.dttwtsolver.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.planets_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (WalkingIconService.Ser!=null)
                    WalkingIconService.Ser.setCameraID(position);
                else
                    startService(new Intent(MainActivity.this, WalkingIconService.class).putExtra("camID", position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        WalkingIconService.editText=(EditText) findViewById(R.id.editText);

        WalkingIconService.b = (Button) findViewById(R.id.NachFot);
        if (WalkingIconService.Ser!=null)
        {
            if (!WalkingIconService.Ser.zap) {
                if (WalkingIconService.Ser.siem) {
                    WalkingIconService.b.setText("?????????????????? ??????????????");
                } else {
                    WalkingIconService.b.setText("???????????? ??????????????");
                }
            }
        }

        WalkingIconService.b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WalkingIconService.Ser!=null)
                {
                    if (!WalkingIconService.Ser.zap) {
                        Button b = (Button) v;
                        if (!WalkingIconService.Ser.siem) {
                            b.setText("?????????????????? ??????????????");
                        } else {
                            b.setText("???????????? ??????????????");
                        }
                        WalkingIconService.Ser.On_Of_StartFoto();
                    }
                }
            }
        });

        Button b1 = (Button) findViewById(R.id.pok);
        if (WalkingIconService.Ser!=null)
        {
            if (WalkingIconService.Ser.pokazBoll)
                b1.setText("????????????????");
            else
                b1.setText("????????????");
        }
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WalkingIconService.Ser!=null)
                {
                    Button b = (Button) v;
                    if (!WalkingIconService.Ser.pokazBoll)
                        b.setText("????????????????");
                    else
                        b.setText("????????????");
                    WalkingIconService.Ser.setSVpok();
                }
            }
        });

        Button b2 = (Button) findViewById(R.id.NachZap);
        if (WalkingIconService.Ser!=null)
        {
            if (!WalkingIconService.Ser.siem) {
                if (WalkingIconService.Ser.zap) {
                    b2.setText("???????????????????? ????????????");
                } else {
                    b2.setText("???????????? ????????????");
                }
            }
        }
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WalkingIconService.Ser!=null)
                {
                    if (!WalkingIconService.Ser.siem) {
                        Button b = (Button) v;
                        if (!WalkingIconService.Ser.zap) {
                            b.setText("???????????????????? ????????????");
                        } else {
                            b.setText("???????????? ????????????");
                        }
                        WalkingIconService.Ser.On_Of_StartRecord();
                    }
                }
            }
        });

        Button b3 = (Button) findViewById(R.id.zavServ);
        if (WalkingIconService.Ser==null)
        {
            b3.setText("?????????????????? ????????????");
        }
        else
        {
            b3.setText("???????????????????? ????????????");
        }
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (WalkingIconService.Ser!=null)
                {
                    stopService(new Intent(MainActivity.this, WalkingIconService.class));
                    b.setText("?????????????????? ????????????");
                }
                else
                {
                    startService(new Intent(MainActivity.this, WalkingIconService.class).putExtra("camID", 0));
                    b.setText("???????????????????? ????????????");
                }
            }
        });
    }

    private static long back_pressed;

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), "?????????????? ?????? ?????? ?????? ????????????!",
                    Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}