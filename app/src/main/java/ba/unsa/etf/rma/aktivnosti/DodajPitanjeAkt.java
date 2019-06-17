package ba.unsa.etf.rma.aktivnosti;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;

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
import ba.unsa.etf.rma.klase.AdapterZaListuOdgovora;
import ba.unsa.etf.rma.klase.FirebasePitanja;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;
import ba.unsa.etf.rma.klase.SQLiteBaza;

import static ba.unsa.etf.rma.klase.FirebasePitanja.streamToStringConversion;

public class DodajPitanjeAkt extends AppCompatActivity {

    private Button dodajTacan;
    private Button dodajOdgovor;
    private Button dodajPitanje;
    private ListView lvOdgovori;
    private EditText etNaziv;
    private EditText etOdgovor;
    private ArrayList<String> alOdgovori = new ArrayList<>();
    private AdapterZaListuOdgovora adapterZaListuOdgovora;
    private Pitanje trenutnoPitanje = new Pitanje();
    public static boolean tacanDodan = false;
    public static String tacanOdgovor = null;
    private Kviz trenutniKviz;
    private ArrayList<Pitanje> trenutnoPrisutnaPitanja = new ArrayList<>();
    public static boolean POSTOJI_LI_PITANJE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_pitanje_akt);

        //Potrebno je dodijeliti sve vrijednosti pomocu id-a.
        dodajTacan = (Button) findViewById(R.id.btnDodajTacan);
        dodajOdgovor = (Button) findViewById(R.id.btnDodajOdgovor);
        dodajPitanje = (Button) findViewById(R.id.btnDodajPitanje);
        lvOdgovori = (ListView) findViewById(R.id.lvOdgovori);
        etNaziv = (EditText) findViewById(R.id.etNaziv);
        etOdgovor = (EditText) findViewById(R.id.etOdgovor);


        //Postavljanje adaptera.
        adapterZaListuOdgovora = new AdapterZaListuOdgovora(this, alOdgovori);
        lvOdgovori.setAdapter(adapterZaListuOdgovora);

        ArrayList<Pitanje> tempPitanja = new ArrayList<>();
        //Kupljenje podataka iz intenta.
        trenutniKviz = (Kviz)getIntent().getSerializableExtra("trenutniKviz");
        tempPitanja = (ArrayList<Pitanje>)getIntent().getSerializableExtra("trenutnaPitanja");
        trenutnoPrisutnaPitanja.addAll( tempPitanja );
        tempPitanja.clear();
        tempPitanja = (ArrayList<Pitanje>)getIntent().getSerializableExtra("mogucaPitanja");
        trenutnoPrisutnaPitanja.addAll( tempPitanja );
        tempPitanja.clear();

        //Pritiskom na dugme vrsi se dodavanje tacnog odgovora pitanja nakon cega
        //ne mozemo dodati jos jedan tacan odgovor, osim ako ga ne uklonimo iz liste odgovora (element
        //liste tacnog odgovora se boji u zeleno).
        dodajTacan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!tacanDodan && !etOdgovor.getText().toString().equals("")) {
                    boolean vecDodano = false;
                    for (String odg : alOdgovori) {
                        if (odg.equals(etOdgovor.getText().toString()))
                            vecDodano = true;
                    }
                    if (!vecDodano) {
                        trenutnoPitanje.setTacan(etOdgovor.getText().toString());
                        alOdgovori.add(etOdgovor.getText().toString());
                        tacanOdgovor = etOdgovor.getText().toString();
                        trenutnoPitanje.setOdgovori(alOdgovori);
                        etOdgovor.setText("");
                        adapterZaListuOdgovora.notifyDataSetChanged();
                        tacanDodan = true;
                        lvOdgovori.setBackgroundColor(Color.parseColor("#fafafa"));
                    }

                }
            }
        });

        //Dugme za dodavanje netacnog odgovora, validira podatke uz to.
        dodajOdgovor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!etOdgovor.getText().toString().equals("")) {
                    boolean vecDodano = false;
                    for (String odg : alOdgovori)
                        if (odg.equals(etOdgovor.getText().toString()))
                            vecDodano = true;
                    if (!vecDodano) {
                        alOdgovori.add(etOdgovor.getText().toString());
                        trenutnoPitanje.setOdgovori(alOdgovori);
                        etOdgovor.setText("");
                    }
                    adapterZaListuOdgovora.notifyDataSetChanged();
                }
            }

        });

        //Pritiskom na jedan od elemenata listView-a koji sadrzi odgovore, odabrani element se uklanja iz liste.
        lvOdgovori.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String odabraniOdgovor = parent.getItemAtPosition(position).toString();
                if (odabraniOdgovor.equals(tacanOdgovor)) {
                    tacanOdgovor = null;
                    tacanDodan = false;
                }
                alOdgovori.remove(odabraniOdgovor);
                adapterZaListuOdgovora.notifyDataSetChanged();
            }
        });

        //Dugme vrsi validaciju i dodaje pitanje u kviz koji se dodaje/azurira u prethodnoj aktivnosti.
        dodajPitanje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean pokreni = true;
                if (tacanOdgovor == null) {
                    lvOdgovori.setBackgroundColor(Color.parseColor("#ff0006"));
                    Toast.makeText(getApplicationContext(), "Dodajte tacan odgovor!", Toast.LENGTH_SHORT).show();
                    pokreni = false;
                }
                if (pokreni) {
                    if( imaInterneta() ) {
                        ProvjeriPostojanjePitanja provjera = new ProvjeriPostojanjePitanja(getApplicationContext(), etNaziv.getText().toString());
                        provjera.execute();
                    }
                    else{
                        Toast.makeText(DodajPitanjeAkt.this, "OFFLINE MODE - Ne mozete dodavati pitanja", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        //Slusac editText elementa etNaziv koji boju pozadine mijenja u default boju androida ukoliko editText
        //nije prazan. Ovaj slusac ne mijenja pozadinsku boju editText elementa u crveno ako je prazan, nego dugme dodajPitanje
        //tokom validacije podataka.
        etNaziv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!etNaziv.getText().toString().equals(""))
                    etNaziv.setBackgroundResource( R.drawable.button_border );

            }

            @Override
            public void afterTextChanged(Editable s) {
                //Do nothing
            }
        });
    }

    public class ProvjeriPostojanjePitanja extends AsyncTask<String, Void, Void> {
        Context context;
        String naziv;

        public ProvjeriPostojanjePitanja(Context context, String naziv) {
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
                String dajPitanjeQuery = "{  \n" +
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
                        "               \"fieldPath\":\"naziv\"\n" +
                        "            },\n" +
                        "            {  \n" +
                        "               \"fieldPath\":\"indexTacnog\"\n" +
                        "            },\n" +
                        "            {  \n" +
                        "               \"fieldPath\":\"odgovori\"\n" +
                        "            }\n" +
                        "         ]\n" +
                        "      },\n" +
                        "      \"from\":[  \n" +
                        "         {  \n" +
                        "            \"collectionId\":\"Pitanja\"\n" +
                        "         }\n" +
                        "      ],\n" +
                        "      \"limit\":1000\n" +
                        "   }\n" +
                        "}";
                try (OutputStream os = CONNECTION.getOutputStream()) {
                    byte[] input = dajPitanjeQuery.getBytes("utf-8");
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
                    POSTOJI_LI_PITANJE = false;
                }
                else
                    POSTOJI_LI_PITANJE = true;
                CONNECTION.disconnect();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if( POSTOJI_LI_PITANJE || etNaziv.getText().toString().equals("Dodaj pitanje") || etNaziv.getText().toString().equals("") ){
                etNaziv.setBackgroundColor(Color.parseColor("#ff0006"));
                Toast.makeText(context, "Pogreska pri dodavanju pitanja!", Toast.LENGTH_SHORT).show();
            }
            else{
                Pitanje pitanje = new Pitanje();
                pitanje.setNaziv( etNaziv.getText().toString() );
                pitanje.setOdgovori( alOdgovori );
                pitanje.setTekstPitanja( etNaziv.getText().toString() );
                pitanje.setTacan( tacanOdgovor );
                tacanOdgovor = null;
                tacanDodan = false;
                try {

                    FirebasePitanja.dodajPitanje( pitanje, getApplicationContext() );
                    SQLiteBaza db = new SQLiteBaza(getApplicationContext());
                    db.dodajPitanje( pitanje );

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                Intent resIntent = new Intent();
                resIntent.putExtra("novoPitanje", pitanje );
                setResult( RESULT_OK, resIntent );
                finish();
            }
            POSTOJI_LI_PITANJE = true;
        }
    }

    public boolean imaInterneta() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

}

