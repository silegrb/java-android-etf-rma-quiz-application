package ba.unsa.etf.rma.aktivnosti;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.AdapterZaListuMogucihPitanja;
import ba.unsa.etf.rma.klase.AdapterZaListuTrenutnihPitanja;
import ba.unsa.etf.rma.klase.CSVReader;
import ba.unsa.etf.rma.klase.FirebaseKategorije;
import ba.unsa.etf.rma.klase.FirebaseKvizovi;
import ba.unsa.etf.rma.klase.FirebasePitanja;
import ba.unsa.etf.rma.klase.Kategorija;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.firebasePitanja;
import static ba.unsa.etf.rma.klase.FirebasePitanja.streamToStringConversion;


public class DodajKvizAkt extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;
    private Button btnDodajKviz;
    private Button btnImportKviz;
    private EditText etNaziv;
    private ArrayList<Kategorija> kategorije;
    private ArrayList<Kviz> kvizovi;
    private Kviz trenutniKviz;
    private static ArrayAdapter<Kategorija> adapterZaSpinner;
    private Spinner kategorijeSpinner;
    private ListView lvTrenutnihPitanja;
    private ListView lvMogucihPitanja;
    private ArrayList<Pitanje> alTrenutnaPitanja = new ArrayList<>();
    private ArrayList<Pitanje> alMogucaPitanja = new ArrayList<>();
    private AdapterZaListuTrenutnihPitanja adapterZaListuTrenutnihPitanja;
    private AdapterZaListuMogucihPitanja adapterZaListuMogucihPitanja;

    public static boolean POSTOJI_LI_KVIZ = false;
    private static boolean POSTOJI_LI_KATEGORIJA = false;
    private static boolean importuj = true;
    private static boolean importUradjen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_kviz_akt);

        //Dodajmo sve vrijednosti koristeci id.
        etNaziv = (EditText) findViewById(R.id.etNaziv);
        btnDodajKviz = (Button) findViewById(R.id.btnDodajKviz);
        btnImportKviz = (Button) findViewById(R.id.btnImportKviz);
        kategorijeSpinner = (Spinner) findViewById(R.id.spKategorije);
        lvMogucihPitanja = (ListView) findViewById(R.id.lvMogucaPitanja);
        lvTrenutnihPitanja = (ListView) findViewById(R.id.lvDodanaPitanja);

        //Kupljenje podataka iz intenta.
        trenutniKviz = (Kviz) getIntent().getSerializableExtra("trenutniKviz");
        kategorije = (ArrayList<Kategorija>) getIntent().getSerializableExtra("sveKategorije");
        kvizovi = (ArrayList<Kviz>) getIntent().getSerializableExtra("sviKvizovi");

        //Postavljanje adaptera.
        adapterZaSpinner = new ArrayAdapter<Kategorija>(this, android.R.layout.simple_list_item_1, kategorije);
        kategorijeSpinner.setAdapter(adapterZaSpinner);
        adapterZaListuTrenutnihPitanja = new AdapterZaListuTrenutnihPitanja(this, alTrenutnaPitanja);
        lvTrenutnihPitanja.setAdapter(adapterZaListuTrenutnihPitanja);
        adapterZaListuMogucihPitanja = new AdapterZaListuMogucihPitanja(this, alMogucaPitanja);
        lvMogucihPitanja.setAdapter(adapterZaListuMogucihPitanja);

        //Dodavanje imaginarne kategorije, pomocu kojeg mozemo dodati novu kategoriju.
        final Kategorija kategorijaZaDodavanje = new Kategorija();
        kategorijaZaDodavanje.setNaziv("Dodaj kategoriju");
        kategorije.add(kategorijaZaDodavanje);

        //Ukoliko je trenutniKviz apstraktni kviz za dodavanje, spinner kategorija postavljamo na odabranu kategoriju, a
        //ako nije (else dio) onda cemo sve vrijednosti postaviti na podatke odabranog kviza.
        if (trenutniKviz.getNaziv().equals("Dodaj kviz")) {
            Kategorija odabranaKategorijaUPrethodnojAktivnosti = trenutniKviz.getKategorija();
            for( int i = 0; i < KvizoviAkt.kategorije.size(); i++ )
                if( KvizoviAkt.kategorije.get(i).getNaziv().equals( odabranaKategorijaUPrethodnojAktivnosti.getNaziv() ) )
                    kategorijeSpinner.setSelection(i);

        }
        else {
            int position = -1;
            for (int i = 0; i < kategorije.size(); i++)
                if (kategorije.get(i).getNaziv().equals(trenutniKviz.getKategorija().getNaziv())) {
                    position = i;
                    break;
                }
            kategorijeSpinner.setSelection(position);
            alTrenutnaPitanja.addAll(trenutniKviz.getPitanja());
            etNaziv.setText(trenutniKviz.getNaziv());
        }

        //Dodajmo u listu trenutnih pitanja i apstraktni element pomocu kojeg mozemo dodati i novo pitanje.
        final Pitanje dodajPitanje = new Pitanje();
        dodajPitanje.setNaziv("Dodaj pitanje");
        alTrenutnaPitanja.add(dodajPitanje);
        adapterZaListuTrenutnihPitanja.notifyDataSetChanged();

        PokupiMogucaPitanja pokupiMogucaPitanja = new PokupiMogucaPitanja(getApplicationContext());
        pokupiMogucaPitanja.execute();

        //Akcija pritiska elementa lvTrenutnihPitanja prebacuje pritisnuti element u listu mogucih pitanja.
        lvTrenutnihPitanja.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pitanje odabrano = (Pitanje) parent.getItemAtPosition(position);
                if (!odabrano.getNaziv().equals("Dodaj pitanje")) {
                    alTrenutnaPitanja.remove(odabrano);
                    alMogucaPitanja.add(odabrano);
                } else {
                    Intent dodajPitanjeAkt = new Intent(DodajKvizAkt.this, DodajPitanjeAkt.class);
                    dodajPitanjeAkt.putExtra("trenutniKviz", trenutniKviz);
                    dodajPitanjeAkt.putExtra("trenutnaPitanja", alTrenutnaPitanja);
                    dodajPitanjeAkt.putExtra("mogucaPitanja", alMogucaPitanja);
                    DodajKvizAkt.this.startActivityForResult(dodajPitanjeAkt, 777);
                }
                adapterZaListuMogucihPitanja.notifyDataSetChanged();
                adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
            }
        });

        //Akcija pritiska elementa lvMogucihPitanja prebacuje pritisnuti element u listu trenutnih pitanja.
        lvMogucihPitanja.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pitanje odabrano = (Pitanje) parent.getItemAtPosition(position);
                alMogucaPitanja.remove(odabrano);
                alTrenutnaPitanja.add(alTrenutnaPitanja.size() - 1, odabrano);

                adapterZaListuMogucihPitanja.notifyDataSetChanged();
                adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
            }
        });

        //Dugme ispod vrsi validaciju unesenih podataka za novi kviz, te ukoliko je validacija zadovoljena
        //dodajemo novi kviz u listu svih kvizova.
        btnDodajKviz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( trenutniKviz.getNaziv().equals( "Dodaj kviz" ) ) {
                    String naziv = etNaziv.getText().toString();
                    ProvjeriPostojanjeKviza provjera = new ProvjeriPostojanjeKviza(getApplicationContext(), naziv,false);
                    provjera.execute();
                }
                else{
                    if( trenutniKviz.getNaziv().equals( etNaziv.getText().toString() ) ){
                        String naziv = etNaziv.getText().toString();
                        ProvjeriPostojanjeKviza provjera = new ProvjeriPostojanjeKviza(getApplicationContext(), naziv,true);
                        provjera.execute();
                    }
                    else{
                        String naziv = etNaziv.getText().toString();
                        ProvjeriPostojanjeKviza provjera = new ProvjeriPostojanjeKviza(getApplicationContext(), naziv,false);
                        provjera.execute();
                    }
                }
            }
        });

        btnImportKviz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });

        //Listener na etNaziv vraca pozadinsku boju na defaultnu boju ukoliko korisnik unese bilo sto (potrebno je samo da etNaziv ne bude prazan)
        etNaziv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.equals(""))
                    etNaziv.setBackgroundResource(R.drawable.button_border);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //Ukoliko pritisnemo na kategoriju 'Dodaj kategoriju' pokrece se nova aktivnost za dodavanje nove kategorije.
        kategorijeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getItemAtPosition(position).toString().equals("Dodaj kategoriju")) {
                    Intent dodajKategorijuAkt = new Intent(DodajKvizAkt.this, DodajKategorijuAkt.class);
                    dodajKategorijuAkt.putExtra("sveKategorije", kategorije);
                    DodajKvizAkt.this.startActivityForResult(dodajKategorijuAkt, 69);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 777) {
            if (resultCode == RESULT_OK) {
                Pitanje pitanje = (Pitanje) data.getExtras().get("novoPitanje");
                alTrenutnaPitanja.add(alTrenutnaPitanja.size() - 1, pitanje);
                adapterZaListuMogucihPitanja.notifyDataSetChanged();
                adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
            }
        }
        if (requestCode == 69) {
            if (resultCode == RESULT_OK) {
                Kategorija k = (Kategorija) data.getExtras().get("novaKategorija");
                kategorije.add(kategorije.size() - 1, k);
                kategorijeSpinner.setSelection(kategorije.size() - 2);
                adapterZaSpinner.notifyDataSetChanged();
            } else {
                kategorijeSpinner.setSelection(kategorije.size() - 2);
                adapterZaSpinner.notifyDataSetChanged();
            }
        }
        if (requestCode == READ_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri uri = null;
                if (data != null) {
                    uri = data.getData();
                    try {
                        //Ovdje ide provjera validnosti.
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        CSVReader csvReader = new CSVReader(inputStream);
                        ArrayList<String[]> procitaniCsvFajl = csvReader.read();
                        String[] prviRed = {""};
                        ArrayList<Pitanje> pitanja = new ArrayList<>();
                        boolean greskaSeVecDesila = false;
                        if (procitaniCsvFajl.size() == 0) {
                            importuj = false;
                            greskaSeVecDesila = true;
                            AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                            alertDialog.setTitle("Upozorenje");
                            alertDialog.setMessage("Datoteka je prazna");
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });

                            alertDialog.show();
                        } else {
                            prviRed = procitaniCsvFajl.get(0);
                            if (prviRed.length < 3) {
                                greskaSeVecDesila = true;
                                importuj = false;
                                greskaSeVecDesila = true;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Neispravan format prvog reda!");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                            if (!greskaSeVecDesila) {
                                boolean vecPostoji = false;
                                for (int i = 0; i < kvizovi.size(); i++)
                                    if (kvizovi.get(i).getNaziv().equals(prviRed[0]))
                                        vecPostoji = true;
                                if (!greskaSeVecDesila && vecPostoji) {
                                    importuj = false;
                                    greskaSeVecDesila = true;
                                    AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                    alertDialog.setTitle("Upozorenje");
                                    alertDialog.setMessage("Kviz kojeg importujete već postoji!");
                                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    alertDialog.show();
                                }
                            }
                            if (!greskaSeVecDesila && Integer.parseInt(prviRed[2]) != procitaniCsvFajl.size() - 1) {
                                //Broj pitanja nije ispravan.
                                importuj = false;
                                greskaSeVecDesila = true;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Kviz kojeg imporujete ima neispravan broj pitanja!");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }

                            boolean neispravanFormatPitanja = false;
                            for (int i = 1; i < procitaniCsvFajl.size(); i++)
                                if (procitaniCsvFajl.get(i).length < 4)
                                    neispravanFormatPitanja = true;

                            if (!greskaSeVecDesila && neispravanFormatPitanja) {
                                importuj = false;
                                greskaSeVecDesila = true;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Neispravan format nekog od pitanja");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }

                            boolean neispravanBrojOdgovora = false;
                            boolean neispravanIndeksTacnogOdgovora = false;
                            boolean odgovorSePonovio = false;
                            if (!greskaSeVecDesila) {
                                for (int i = 1; i < procitaniCsvFajl.size(); i++) {
                                    String[] tempStringNiz = procitaniCsvFajl.get(i);
                                    Pitanje p = new Pitanje();
                                    p.setNaziv(tempStringNiz[0]);
                                    int brojOdgovora = Integer.parseInt(tempStringNiz[1]);
                                    int indeksTacnogOdgovora = Integer.parseInt(tempStringNiz[2]);
                                    if (brojOdgovora != tempStringNiz.length - 3)
                                        neispravanBrojOdgovora = true;
                                    if (indeksTacnogOdgovora < 0 || indeksTacnogOdgovora > brojOdgovora - 1)
                                        neispravanIndeksTacnogOdgovora = true;
                                    ArrayList<String> odgovori = new ArrayList<>();
                                    for (int j = 3; j < tempStringNiz.length; j++)
                                        odgovori.add(tempStringNiz[j]);
                                    if (!neispravanBrojOdgovora && !neispravanIndeksTacnogOdgovora) {
                                        p.setTekstPitanja(tempStringNiz[0]);
                                        p.setOdgovori(odgovori);
                                        p.setTacan(odgovori.get(indeksTacnogOdgovora));
                                        pitanja.add(p);
                                    }
                                }

                                if (!neispravanBrojOdgovora && !neispravanIndeksTacnogOdgovora) {
                                    for (int i = 0; i < pitanja.size(); i++) {
                                        ArrayList<String> odgovori = pitanja.get(i).getOdgovori();
                                        ArrayList<String> odgovoriKojiSeNePonavljaju = new ArrayList<>();
                                        for (int j = 0; j < odgovori.size(); j++)
                                            if (!odgovoriKojiSeNePonavljaju.contains(odgovori.get(j)))
                                                odgovoriKojiSeNePonavljaju.add(odgovori.get(j));

                                        if (odgovori.size() != odgovoriKojiSeNePonavljaju.size())
                                            odgovorSePonovio = true;
                                    }
                                }

                            }

                            if (!greskaSeVecDesila && neispravanBrojOdgovora) {
                                importuj = false;
                                greskaSeVecDesila = true;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Kviz kojeg importujete ima neispravan broj odgovora!");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }

                            if (!greskaSeVecDesila && neispravanIndeksTacnogOdgovora) {
                                importuj = false;
                                greskaSeVecDesila = true;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Kviz kojeg importujete ima neispravan index tačnog odgovora!");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }

                            if (!greskaSeVecDesila && odgovorSePonovio) {
                                importuj = false;
                                greskaSeVecDesila = true;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Kviz kojeg importujete nije ispravan postoji ponavljanje odgovora!");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                        }



                        if (importuj) {

                            boolean GRESKA_ISTOG_PITANJA = false;
                            for( int i = 0; i < pitanja.size(); i++ ) {
                                boolean pitanjeVecPostoji = false;
                                for (int j = 0; j < firebasePitanja.size(); j++) {
                                    if (pitanja.get(i).getNaziv().equals( firebasePitanja.get(j).getNaziv() )) {
                                        pitanjeVecPostoji = true;
                                    }
                                }
                                if( pitanjeVecPostoji ) GRESKA_ISTOG_PITANJA = true;
                            }

                            if( GRESKA_ISTOG_PITANJA ){
                                importuj = false;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Kviz kojeg importujete ima pitanja koja se vec nalaze u bazi!");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }

                            if( importuj ) {
                                FirebasePitanja.dodajPitanja(pitanja, getApplicationContext());
                                ProvjeriPostojanjeKategorije provjera = new ProvjeriPostojanjeKategorije(getApplicationContext(), prviRed[1]);
                                provjera.execute();
                                etNaziv.getText().clear();
                                etNaziv.setText(prviRed[0]);
                                alTrenutnaPitanja.clear();
                                alTrenutnaPitanja.addAll(pitanja);
                                Pitanje pDp = new Pitanje();
                                pDp.setNaziv("Dodaj pitanje");
                                alTrenutnaPitanja.add(pDp);
                                importUradjen = true;
                                PokupiMogucaPitanja pokupiMogucaPitanja = new PokupiMogucaPitanja(getApplicationContext());
                                pokupiMogucaPitanja.execute();
                                adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
                                adapterZaSpinner.notifyDataSetChanged();
                            }
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    public class ProvjeriPostojanjeKviza extends AsyncTask<String, Void, Void> {
        Context context;
        String naziv;
        boolean nazivUredjivanogKvizaIsti;

        public ProvjeriPostojanjeKviza(Context context, String naziv,boolean nazivUredjivanogKvizaIsti) {
            this.context = context;
            this.naziv = naziv;
            this.nazivUredjivanogKvizaIsti = nazivUredjivanogKvizaIsti;
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
                String dajKvizQuery = "{  \n" +
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
                        "               \"fieldPath\":\"idKategorije\"\n" +
                        "            },\n" +
                        "            {  \n" +
                        "               \"fieldPath\":\"pitanja\"\n" +
                        "            }\n" +
                        "         ]\n" +
                        "      },\n" +
                        "      \"from\":[  \n" +
                        "         {  \n" +
                        "            \"collectionId\":\"Kvizovi\"\n" +
                        "         }\n" +
                        "      ],\n" +
                        "      \"limit\":1000\n" +
                        "   }\n" +
                        "}";
                try (OutputStream os = CONNECTION.getOutputStream()) {
                    byte[] input = dajKvizQuery.getBytes("utf-8");
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
                    POSTOJI_LI_KVIZ = false;
                }
                else
                    POSTOJI_LI_KVIZ = true;
                if( nazivUredjivanogKvizaIsti )
                    POSTOJI_LI_KVIZ = false;
                CONNECTION.disconnect();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if( POSTOJI_LI_KVIZ || etNaziv.getText().toString().equals("Dodaj kviz") || etNaziv.getText().toString().equals("") ){
                etNaziv.setBackgroundColor(Color.parseColor("#ff0006"));
                Toast.makeText(context, "Pogreska pri dodavanju kviza!", Toast.LENGTH_SHORT).show();
            }
            else{
                if( trenutniKviz.getNaziv().equals("Dodaj kviz") ){
                    Kviz povratniKviz = new Kviz();
                    for (int i = 0; i < alTrenutnaPitanja.size(); i++)
                        if (alTrenutnaPitanja.get(i).getNaziv().equals("Dodaj pitanje")) {
                            alTrenutnaPitanja.remove(i);
                            i--;
                        }
                    Kategorija kategorija = (Kategorija) kategorijeSpinner.getSelectedItem();
                    povratniKviz.setNEPROMJENJIVI_ID( trenutniKviz.getNEPROMJENJIVI_ID() );
                    povratniKviz.setKategorija(kategorija);
                    povratniKviz.setNaziv(etNaziv.getText().toString());
                    povratniKviz.setPitanja(alTrenutnaPitanja);
                    Intent resIntent = new Intent();
                   resIntent.putExtra("noviKviz", povratniKviz);
                   resIntent.putExtra("dodajNoviKviz", true);
                    setResult(RESULT_OK, resIntent);
                  //  kvizovi.add(kvizovi.size(), povratniKviz);
                    finish();
                }
                else{
                    Kviz povratniKviz = new Kviz();
                    for (int i = 0; i < alTrenutnaPitanja.size(); i++)
                        if (alTrenutnaPitanja.get(i).getNaziv().equals("Dodaj pitanje")) {
                            alTrenutnaPitanja.remove(i);
                            i--;
                        }
                    Kategorija kategorija = (Kategorija) kategorijeSpinner.getSelectedItem();
                    povratniKviz.setNEPROMJENJIVI_ID( trenutniKviz.getNEPROMJENJIVI_ID() );
                    povratniKviz.setKategorija(kategorija);
                    povratniKviz.setNaziv(etNaziv.getText().toString());
                    povratniKviz.setPitanja(alTrenutnaPitanja);
                    Intent resIntent = new Intent();
                    resIntent.putExtra("noviKviz", povratniKviz);
                    resIntent.putExtra("dodajNoviKviz", false);
                    setResult(RESULT_OK, resIntent);
                    finish();
                }
            }
            POSTOJI_LI_KVIZ = true;

        }
    }

    public class PokupiMogucaPitanja extends AsyncTask<String,Void,Void>{

        private Context context;

        public PokupiMogucaPitanja(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute(){
            alMogucaPitanja.clear();
        }

        @Override
        protected Void doInBackground(String... strings) {
            //Provjeriti da li je vec u bazi.
            GoogleCredential credential;
            try{
                firebasePitanja.clear();
                InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                credential.refreshToken();
                String TOKEN = credential.getAccessToken();
                String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Pitanja?access_token=";
                java.net.URL urlOBJ = new URL( URL + URLEncoder.encode(TOKEN,"UTF-8"));
                HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                InputStream inputStream = new BufferedInputStream(CONNECTION.getInputStream());
                String result = streamToStringConversion(inputStream);
                JSONObject jo = new JSONObject(result);
                JSONArray dokumentovanaPitanja;
                try{
                    dokumentovanaPitanja  = jo.getJSONArray("documents");
                }
                catch (Exception e){
                    return null;
                }
                for( int i = 0; i < dokumentovanaPitanja.length(); i++ ){
                    JSONObject dokument = dokumentovanaPitanja.getJSONObject(i);
                    JSONObject field =  dokument.getJSONObject("fields");
                    JSONObject nazivOBJEKAT = field.getJSONObject("naziv");
                    String nazivString = nazivOBJEKAT.getString("stringValue");
                    JSONObject indexTacnogOBJEKAT = field.getJSONObject("indexTacnog");
                    int indexTacnogINT = indexTacnogOBJEKAT.getInt("integerValue");
                    JSONObject odgovoriOBJEKAT = field.getJSONObject("odgovori");
                    JSONObject odgovoriARRAY = odgovoriOBJEKAT.getJSONObject("arrayValue");
                    JSONArray odgovori = odgovoriARRAY.getJSONArray("values");
                    ArrayList<String> odgovoriLista = new ArrayList<>();
                    for( int j = 0; j < odgovori.length(); j++ ){
                        JSONObject stringValue = odgovori.getJSONObject(j);
                        String odgovor = stringValue.getString("stringValue");
                        odgovoriLista.add(odgovor);
                    }
                    Pitanje novoPitanje = new Pitanje();
                    novoPitanje.setNaziv( nazivString );
                    novoPitanje.setTacan( odgovoriLista.get(indexTacnogINT) );
                    novoPitanje.setOdgovori( odgovoriLista );
                    firebasePitanja.add( novoPitanje );
                }

                CONNECTION.disconnect();
            }
            catch (IOException e){
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(Void result){
                alMogucaPitanja.addAll(firebasePitanja);
                if( !trenutniKviz.getNaziv().equals("Dodaj kviz") ) {
                    for (int i = 0; i < trenutniKviz.getPitanja().size(); i++)
                        for (int j = 0; j < alMogucaPitanja.size(); j++)
                            if (trenutniKviz.getPitanja().get(i).getNaziv().equals(alMogucaPitanja.get(j).getNaziv())) {
                                alMogucaPitanja.remove(j);
                                j--;
                            }
                }
                if( importUradjen ){
                    for (int i = 0; i < alTrenutnaPitanja.size(); i++)
                        for (int j = 0; j < alMogucaPitanja.size(); j++)
                            if (alTrenutnaPitanja.get(i).getNaziv().equals(alMogucaPitanja.get(j).getNaziv())) {
                                alMogucaPitanja.remove(j);
                                j--;
                            }
                            importUradjen = false;
                }
            adapterZaListuMogucihPitanja.notifyDataSetChanged();

        }

    }
    public class PokupiFirebaseKategorije extends AsyncTask<String,Void,Void>{

        private Context context;

        public PokupiFirebaseKategorije(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute(){
            kategorije.clear();
            Kategorija kategorijaSvi = new Kategorija();
            kategorijaSvi.setNaziv("Svi");
            kategorijaSvi.setId("5");
            kategorije.add(kategorijaSvi);
        }

        @Override
        protected Void doInBackground(String... strings) {
            //Provjeriti da li je vec u bazi.
            GoogleCredential credential;
            try {

                InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                credential.refreshToken();
                String TOKEN = credential.getAccessToken();
                String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Kategorije?access_token=";
                java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                InputStream inputStream = new BufferedInputStream(CONNECTION.getInputStream());
                String result = FirebaseKvizovi.streamToStringConversion(inputStream);
                JSONObject jo = new JSONObject(result);
                JSONArray dokumentovaniKvizovi;
                try {
                    dokumentovaniKvizovi = jo.getJSONArray("documents");
                } catch (Exception e) {
                    return null;
                }

                for (int i = 0; i < dokumentovaniKvizovi.length(); i++) {
                    JSONObject dokument = dokumentovaniKvizovi.getJSONObject(i);
                    JSONObject field = dokument.getJSONObject("fields");
                    JSONObject nazivOBJEKAT = field.getJSONObject("naziv");
                    String nazivString = nazivOBJEKAT.getString("stringValue");
                    JSONObject idIkoniceOBJEKAT = field.getJSONObject("idIkonice");
                    int idIkoniceINT = idIkoniceOBJEKAT.getInt("integerValue");
                    String idIkonice = String.valueOf(idIkoniceINT);
                    Kategorija novaKategorija = new Kategorija();
                    novaKategorija.setNaziv(nazivString);
                    novaKategorija.setId(idIkonice);
                    kategorije.add(novaKategorija);
                }

                CONNECTION.disconnect();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;

        }
        @Override
        protected void onPostExecute(Void result){
            super.onPostExecute(result);
        }
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
                String result = "{\"documents\" : " + FirebaseKategorije.streamToStringConversion(odgovor) + " } ";
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
                    POSTOJI_LI_KATEGORIJA = false;
                }
                else
                    POSTOJI_LI_KATEGORIJA = true;
                CONNECTION.disconnect();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if( POSTOJI_LI_KATEGORIJA || etNaziv.getText().toString().equals("Svi") || etNaziv.getText().toString().equals("Dodaj kategoriju") || etNaziv.getText().toString().equals("") ){
                for (int i = 0; i < kategorije.size(); i++)
                    if (kategorije.get(i).getNaziv().equals(naziv)) {
                        kategorijeSpinner.setSelection(i);
                        break;
                    }
            }
            else{
                Kategorija kategorija = new Kategorija();
                kategorija.setNaziv(naziv);
                kategorija.setId("958");
                kategorije.add(kategorije.size() - 1, kategorija);
                if( !naziv.equals("Svi") && !naziv.equals("Dodaj kategoriju") ) {
                    try {
                        FirebaseKategorije.dodajKategoriju(kategorija,getApplicationContext());
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                kategorijeSpinner.setSelection(kategorije.size() - 2);

            }
            POSTOJI_LI_KATEGORIJA = true;
        }
    }
}

