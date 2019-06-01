package ba.unsa.etf.rma.aktivnosti;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;
import com.maltaisn.icondialog.Icon;
import com.maltaisn.icondialog.IconDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.FirebaseKategorije;
import ba.unsa.etf.rma.klase.Kategorija;

import static ba.unsa.etf.rma.klase.FirebaseKategorije.streamToStringConversion;

public class DodajKategorijuAkt extends AppCompatActivity implements IconDialog.Callback {

    private EditText etNaziv;
    private EditText etIkona;
    private Button dodajIkonu;
    private Button dodajKategoriju;
    private ArrayList<Kategorija> kategorije;
    private Icon[] selectedIcons;
    public static boolean dodajKat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_kategoriju_akt);
        etNaziv = (EditText) findViewById(R.id.etNaziv);
        etIkona = (EditText) findViewById(R.id.etIkona);
        dodajIkonu = (Button) findViewById(R.id.btnDodajIkonu);
        dodajKategoriju = (Button) findViewById(R.id.btnDodajKategoriju);
        kategorije = (ArrayList<Kategorija>) getIntent().getSerializableExtra("sveKategorije");
        etIkona.setEnabled(false);

        final IconDialog iconDialog = new IconDialog();
        etNaziv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean postoji = false;
                for (Kategorija k : kategorije)
                    if (k.getNaziv().equals(etNaziv.getText().toString()))
                        postoji = true;

                if (!postoji && !etNaziv.getText().toString().equals(""))
                    etNaziv.setBackgroundResource(R.drawable.button_border);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        dodajIkonu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconDialog.setSelectedIcons(selectedIcons);
                iconDialog.show(getSupportFragmentManager(), "icon_dialog");
            }
        });

        dodajKategoriju.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProvjeriPostojanjeKategorije provjera = new ProvjeriPostojanjeKategorije( getApplicationContext() , etNaziv.getText().toString() );
                provjera.execute();
            }
        });
    }

    @Override
    public void onIconDialogIconsSelected(Icon[] icons) {
        selectedIcons = icons;
        etIkona.setEnabled(true);
        if (selectedIcons != null) {
            int id = selectedIcons[0].getId();
            etIkona.setText(Integer.toString(id));
        }
        etIkona.setEnabled(false);
    }

    public class ProvjeriPostojanjeKategorije extends AsyncTask<String, Void, Void> {
        Context context;
        String naziv;

        public ProvjeriPostojanjeKategorije(Context context, String naziv) {
            this.context = context;
            this.naziv = naziv;
        }

        @Override
        protected Void doInBackground(String... strings) {

            GoogleCredential credential;
            try {
                InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                credential.refreshToken();
                String TOKEN = credential.getAccessToken();
                String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents:runQuery?access_token=";
                java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                CONNECTION.setDoOutput(true);
                CONNECTION.setRequestMethod("POST");
                CONNECTION.setRequestProperty("Content-Type", "application/json");
                CONNECTION.setRequestProperty("Accept", "application/json");
                String dajKategorijuQuery = "{  \n" +
                        "   \"structuredQuery\":{  \n" +
                        "      \"where\":{  \n" +
                        "         \"fieldFilter\":{  \n" +
                        "            \"field\":{  \n" +
                        "               \"fieldPath\":\"naziv\"\n" +
                        "            },\n" +
                        "            \"op\":\"EQUAL\",\n" +
                        "            \"value\":{  \n" +
                        "               \"stringValue\":\"" + naziv + "\"\n" +
                        "            }\n" +
                        "         }\n" +
                        "      },\n" +
                        "      \"select\":{  \n" +
                        "         \"fields\":[  \n" +
                        "            {  \n" +
                        "               \"fieldPath\":\"idIkonice\"\n" +
                        "            },\n" +
                        "            {  \n" +
                        "               \"fieldPath\":\"naziv\"\n" +
                        "            }\n" +
                        "         ]\n" +
                        "      },\n" +
                        "      \"from\":[  \n" +
                        "         {  \n" +
                        "            \"collectionId\":\"Kategorije\"\n" +
                        "         }\n" +
                        "      ],\n" +
                        "      \"limit\":1000\n" +
                        "   }\n" +
                        "}";
                try (OutputStream os = CONNECTION.getOutputStream()) {
                    byte[] input = dajKategorijuQuery.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                InputStream odgovor = CONNECTION.getInputStream();
                String result = "{\"documents\" : " + streamToStringConversion(odgovor) + " } ";
                JSONObject povratnaInformacija = new JSONObject(result);
                JSONArray dokumenti = povratnaInformacija.getJSONArray("documents");
                int brojacDokumenata = 0;
                for ( int i = 0; i < dokumenti.length(); i++ ) {
                    JSONObject objekat = dokumenti.getJSONObject(i);
                    try {
                        JSONObject dokument = objekat.getJSONObject("document");
                        brojacDokumenata++;
                    } catch (JSONException e) {
                        //Ignore
                    }
                }
                if (brojacDokumenata == 0) {
                    dodajKat = true;
                }
                CONNECTION.disconnect();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if( !dodajKat || etNaziv.getText().toString().equals("Svi") || etNaziv.getText().toString().equals("Dodaj kategoriju") || etNaziv.getText().toString().equals("") ){
                etNaziv.setBackgroundColor(Color.parseColor("#ff0006"));
                Toast.makeText(context, "Unesena kategorija već postoji!", Toast.LENGTH_SHORT).show();
            }
            else{
                Kategorija k = new Kategorija();
                k.setNaziv( etNaziv.getText().toString() );
                etIkona.setEnabled(true);
                String ikona = etIkona.getText().toString();
                etIkona.setEnabled(false);
                if (ikona.equals("")) ikona = "958";
                k.setId(ikona);
                try {
                    FirebaseKategorije.dodajKategoriju(k, getApplicationContext());
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                Intent resIntent = new Intent();
                resIntent.putExtra("novaKategorija", k);
                setResult(RESULT_OK, resIntent);
                finish();

            }
            dodajKat = false;
        }
    }
}
