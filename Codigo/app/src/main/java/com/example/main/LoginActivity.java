package com.example.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.main.login.LoginErrorResponse;
import com.example.main.login.LoginRequest;
import com.example.main.login.LoginResponse;
import com.example.main.services.LoginService;
import com.google.gson.Gson;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = LoginActivity.class.getSimpleName();
    GlobalClass globalClass;
    Button btnLogin;
    Button btnRegistrar;
    EditText emailText;
    EditText passText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        globalClass = (GlobalClass)getApplicationContext();

        btnLogin = findViewById(R.id.LoginButton);
        btnRegistrar = findViewById(R.id.RegistrarButton);
        emailText = findViewById(R.id.emailText);
        passText = findViewById(R.id.PassText);

        Thread almacenar = new Thread(new AlmacenarDatos(getApplicationContext()));
        almacenar.start();
        globalClass.setRunning(true);

        /*mostrar nivel de batería*/
        Toast.makeText(getApplicationContext(),
                "El estado de la batería es de " + cargaBateria()+"%",
                Toast.LENGTH_SHORT).show();


        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });


        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
                startActivity(intent);
            }
        });
    }



    public void login() {
        LoginRequest request = new LoginRequest();
        request.setEmail(emailText.getText().toString());
        request.setPassword(passText.getText().toString());

        if (isOnline()) {

            Retrofit retrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(getString(R.string.retrofit_server)).build();

            LoginService loginService = retrofit.create(LoginService.class);

            Call<LoginResponse> call = loginService.login(request);

            call.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                    if (response.isSuccessful()){
                        Log.i(TAG,"Se logueó de forma exitosa.");

                        globalClass.setToken(response.body().getToken());
                        globalClass.setRefresh_token(response.body().getToken_refresh());


                        Retrofit retrofitEvento = new Retrofit.Builder()
                                .addConverterFactory(GsonConverterFactory.create())
                                .baseUrl(getString(R.string.retrofit_server)).build();

                        //se crea un hilo que se va a encargar de enviar el mensaje al servidor.
                        Thread evento = new Thread(new RegistrarEvento("Login","El usuario "+emailText.getText().toString()+" se logueo de forma exitosa.",getApplicationContext(),retrofitEvento));
                        evento.start();

                        Thread agregar = new Thread(new CargarDatoLista("El usuario "+emailText.getText().toString()+" se logueo de forma exitosa.",getApplicationContext()));
                        agregar.start();

                        //comienza la pantalla main
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);

                    }
                    else {
                        //se muestra por pantalla el error y se lo muestra en el log
                        Gson gson = new Gson();
                        LoginErrorResponse loginErrorResponse = new LoginErrorResponse();

                        try {
                            loginErrorResponse = gson.fromJson(
                                    response.errorBody().string(),
                                    LoginErrorResponse.class);
                            Toast.makeText(LoginActivity.this, loginErrorResponse.getMsg(), Toast.LENGTH_LONG).show();
                            Log.e(TAG,loginErrorResponse.getMsg());
                        } catch (Exception e) {
                            Log.e(TAG,e.getMessage());
                            Toast.makeText(LoginActivity.this,"Error con la comunicación con el servidor.",Toast.LENGTH_SHORT).show();

                        }

                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    CharSequence texto;
                    texto = t.getMessage();
                    Log.e(TAG, (String) texto);
                    Toast.makeText(LoginActivity.this, texto, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(LoginActivity.this, "No tienes conexión a internet.", Toast.LENGTH_SHORT).show();
        }

    }

    public boolean isOnline() {
        //se verifica si hay conexion a internet
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            connected = true;
        }
        else
            connected = false;
        return connected;
    }

    public int cargaBateria ()
    {
        //se muestra la bateria
        try
        {
            IntentFilter batIntentFilter =
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent battery =
                    this.registerReceiver(null, batIntentFilter);
            int nivelBateria = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            return nivelBateria;
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(),
                    "Error al obtener estado de la batería",
                    Toast.LENGTH_SHORT).show();
            return 0;
        }


    }

}
