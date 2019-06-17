package ba.unsa.etf.rma.aktivnosti;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.fragmenti.DetailFrag;
import ba.unsa.etf.rma.fragmenti.ListaFrag;
import ba.unsa.etf.rma.klase.AdapterZaListuKvizova;
import ba.unsa.etf.rma.klase.FirebasePitanja;
import ba.unsa.etf.rma.klase.KalendarEventi;
import ba.unsa.etf.rma.klase.Kategorija;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;
import ba.unsa.etf.rma.klase.RangListaKlasa;
import ba.unsa.etf.rma.klase.SQLiteBaza;

import static ba.unsa.etf.rma.fragmenti.DetailFrag.prikazaniKvizoviFragment;
import static ba.unsa.etf.rma.klase.FirebaseKvizovi.streamToStringConversion;

public class KvizoviAkt extends AppCompatActivity implements ListaFrag.OnListaFragmentListener, DetailFrag.OnDetailFragmentListener {

    private ListView listaKvizova;
    private Spinner spinnerKategorije;
    public static boolean USPRAVAN_DISPLEJ = false;
    public static boolean POSTOJI_LI_KATEGORIJA = true;
    public static SQLiteBaza db;
    public final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( imaInterneta() ){
                FilterKvizova filterKvizova = new FilterKvizova(getApplicationContext(),"Svi");
            }
            else{
                //Ignored
            }
        }
    };




    //Lista 'kvizovi' se koristi za cuvanje svih postojecih kvizova,
    //dok se lista 'prikazaniKvizovi' koristi za prikazivanje svih/filtriranih kvizova.
    public static ArrayList<Kviz> kvizovi = new ArrayList<>();
    public static ArrayList<RangListaKlasa> RANG_LISTE = new ArrayList<>();
    public static ArrayList<Pitanje> firebasePitanja = new ArrayList<>();
    public static ArrayList<Kviz> prikazaniKvizovi = new ArrayList<>();
    public static ArrayList<Kategorija> kategorije = new ArrayList<>();
    private ArrayAdapter<Kategorija> adapterZaSpinner;
    private AdapterZaListuKvizova adapterZaListuKvizova;
    private String spinnerOdabir;
    public static int pozicijaKviza;
    DetailFrag detailFrag;
    ListaFrag listaFrag;
public int check;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new SQLiteBaza(KvizoviAkt.this);
        check = 0;
            FragmentManager fragmentManager = getSupportFragmentManager();
            FrameLayout layoutDetalji = (FrameLayout) findViewById(R.id.detailPlace);
            if (layoutDetalji != null) {
                USPRAVAN_DISPLEJ = false;
                kvizovi.clear();
                kategorije.clear();
                prikazaniKvizoviFragment.clear();
                listaFrag = new ListaFrag();
                detailFrag = new DetailFrag();
                FragmentManager fragmentManagerFinal = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManagerFinal.beginTransaction();
                fragmentTransaction.replace(R.id.listPlace, listaFrag);
                fragmentTransaction.replace(R.id.detailPlace, detailFrag);
                fragmentTransaction.commit();
            } else {
                USPRAVAN_DISPLEJ = true;
                //Prvo svima skinemo vrijednosti!
                kvizovi.clear();
                prikazaniKvizovi.clear();
                kategorije.clear();
                adapterZaSpinner = null;
                adapterZaListuKvizova = null;
                spinnerOdabir = null;

                //Potrebno je dodijeliti sve vrijednosti pomocu id-a.
                listaKvizova = (ListView) findViewById(R.id.lvKvizovi);
                spinnerKategorije = (Spinner) findViewById(R.id.spPostojeceKategorije);

                //Postavljanje adaptera.
                adapterZaSpinner = new ArrayAdapter<Kategorija>(this, android.R.layout.simple_list_item_1, kategorije);
                spinnerKategorije.setAdapter(adapterZaSpinner);
                adapterZaListuKvizova = new AdapterZaListuKvizova(this, prikazaniKvizovi);
                listaKvizova.setAdapter(adapterZaListuKvizova);

                //Aplikacija se na pocetku ne puni nikakvim podacima, osim onim potrebnim za sam rad aplikacije.
                inicijalizirajApp();
                if( USPRAVAN_DISPLEJ ) {
                        if( imaInterneta() ) {
                            PokupiFirebaseKategorije pokupiFirebaseKategorije = new PokupiFirebaseKategorije(getApplicationContext());
                            pokupiFirebaseKategorije.execute();
                        }
                        else{
                            kvizovi.clear();
                            kategorije.clear();
                            firebasePitanja.clear();
                            RANG_LISTE.clear();
                            db.pokupiKategorije();
                            adapterZaSpinner.notifyDataSetChanged();
                            db.pokupiPitanja();
                            db.pokupiKvizove();
                            adapterZaListuKvizova.notifyDataSetChanged();
                            db.pokupiRangliste();
                        }

                }

                //Slusac koji vrsi filtriranje listView-a svih kvizova na osnovu odabrane kategorije
                //u spinneru, prikazivanje Toast poruke za korisnika, npr. "Odabrano: Svi".
                spinnerKategorije.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if( imaInterneta() ) {
                            adapterZaSpinner.notifyDataSetChanged();
                            //Stvaranje string vrijednosti za Toast poruku.
                            String text = parent.getItemAtPosition(position).toString();
                            spinnerOdabir = text;
                            String textBezRazmaka = text.replaceAll(" ", "_RAZMAK_");
                            String textBezKosihBezRazmaka = textBezRazmaka.replaceAll("/", "_KOSA_CRTA_");
                            Toast.makeText(parent.getContext(), "Odabrano: " + text, Toast.LENGTH_SHORT).show();
                            if (++check > 1) {

                                FilterKvizova filter = new FilterKvizova(getApplicationContext(), textBezKosihBezRazmaka);
                                filter.execute();

                            }
                        }
                        else{
                            adapterZaSpinner.notifyDataSetChanged();
                            prikazaniKvizovi.clear();
                            String text = parent.getItemAtPosition(position).toString();
                            if( text.equals("Svi") )
                                prikazaniKvizovi.addAll(kvizovi);
                            else {
                                for (int i = 0; i < kvizovi.size(); i++)
                                    if (kvizovi.get(i).getKategorija().getNaziv().equals(text))
                                        prikazaniKvizovi.add(kvizovi.get(i));
                            }
                            Kviz k = new Kviz();
                            k.setNaziv("Dodaj kviz");
                            prikazaniKvizovi.add( k );
                            adapterZaListuKvizova.notifyDataSetChanged();

                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        //Ukoliko ne selektujemo nista, nista se nece desiti.
                    }
                });

                //Ukoliko kliknemo DUGO na neki od elemenata kviza, otvara se nova aktivnost za kreiranje novog kviza
                //ukoliko je odabran element "Dodaj kviz", odnosno za uredjivanje ukoliko je odabran bilo koji drugi element.
                listaKvizova.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        if( imaInterneta() ) {
                            Kviz trenutniKviz = new Kviz();
                            trenutniKviz.setNaziv("Dodaj kviz");
                            trenutniKviz.setKategorija((Kategorija) spinnerKategorije.getSelectedItem());
                            Intent dodajKvizAkt = new Intent(KvizoviAkt.this, DodajKvizAkt.class);
                            for (int i = 0; i < kvizovi.size(); i++)
                                if (kvizovi.get(i).getNaziv().equals(((Kviz) parent.getItemAtPosition(position)).getNaziv())) {
                                    if (!((Kviz) parent.getItemAtPosition(position)).getNaziv().equals("Dodaj kviz")) {
                                        trenutniKviz.setNaziv(kvizovi.get(i).getNaziv());
                                        trenutniKviz.setKategorija(kvizovi.get(i).getKategorija());
                                        trenutniKviz.setPitanja(kvizovi.get(i).getPitanja());
                                        trenutniKviz.setNEPROMJENJIVI_ID(kvizovi.get(i).getNEPROMJENJIVI_ID());
                                    }
                                    pozicijaKviza = i;
                                }
                            dodajKvizAkt.putExtra("sviKvizovi", kvizovi);
                            dodajKvizAkt.putExtra("trenutniKviz", trenutniKviz);
                            dodajKvizAkt.putExtra("sveKategorije", kategorije);

                            KvizoviAkt.this.startActivityForResult(dodajKvizAkt, pozicijaKviza);

                        }
                        else{

                            Toast.makeText(parent.getContext(), "OFFLINE MODE - Ne mozete dodavati/uredjivati kvizove", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });

                listaKvizova.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Kviz k = (Kviz) parent.getItemAtPosition(position);
                        if (!k.getNaziv().equals("Dodaj kviz")) {

                            if( k.getPitanja().size() != 0 ){
                                int trajanjeKvizaUMinutama = k.getPitanja().size()/2;
                                if( k.getPitanja().size() % 2 == 1 ) trajanjeKvizaUMinutama++;
                                final long ONE_MINUTE_IN_MILLIS=60000;
                                Calendar calendar = Calendar.getInstance();
                                long pocetak = calendar.getTimeInMillis();
                                long kraj = pocetak;
                                kraj += trajanjeKvizaUMinutama*ONE_MINUTE_IN_MILLIS;
                                Date vrijemeAlarma = new Date(kraj);
                                if (ContextCompat.checkSelfPermission(KvizoviAkt.this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                                    // Permission is not granted
                                    ActivityCompat.requestPermissions(KvizoviAkt.this, new String[]{Manifest.permission.READ_CALENDAR}, 0);
                                }
                                KalendarEventi kalendarEventi = new KalendarEventi(KvizoviAkt.this,pocetak,kraj);
                                Pair<Pair<Boolean,String>,Pair<Long,String>> povratniInfoKalendara = kalendarEventi.provjeriEvente();

                                if( povratniInfoKalendara.first.first ) {
                                    if( povratniInfoKalendara.first.second.equals("Will start") ){
                                        android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(KvizoviAkt.this).create();
                                        alertDialog.setTitle("Upozorenje");
                                        long temp = povratniInfoKalendara.second.first/60000;
                                        alertDialog.setMessage("Imate dogadjaj za " + String.valueOf(temp) + " minuta!");
                                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                    }
                                                });

                                        alertDialog.show();
                                    }
                                    else if( povratniInfoKalendara.first.second.equals("In progress") ){
                                        android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(KvizoviAkt.this).create();
                                        alertDialog.setTitle("Upozorenje");
                                        String naslovDogadjaja = povratniInfoKalendara.second.second;
                                        String porukaZaAlert = "Dogadjaj bez naslova jos traje!";
                                        if( !naslovDogadjaja.equals("") ){
                                            porukaZaAlert = "Dogadjaj " + naslovDogadjaja + " jos traje!";
                                        }
                                        alertDialog.setMessage(porukaZaAlert);
                                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                    }
                                                });

                                        alertDialog.show();
                                    }
                                }
                                else{

                                    Intent intent = new Intent(KvizoviAkt.this, IgrajKvizAkt.class);
                                    intent.putExtra("odabraniKviz", k);
                                    intent.putExtra("vrijemeAlarma",vrijemeAlarma);
                                    startActivity(intent);


                            }



                            }

                        }
                    }
                });
            }
        IntentFilter intentFilter =new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(broadcastReceiver,intentFilter);

    }

    private void inicijalizirajApp(){
        //Podaci za inicijalizaciju aplikacije.
        adapterZaListuKvizova.notifyDataSetChanged();
        adapterZaSpinner.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FrameLayout layoutDetalji = (FrameLayout) findViewById(R.id.detailPlace);
        if( layoutDetalji != null ) {
            kategorije.clear();
            detailFrag.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if( requestCode == pozicijaKviza ){
            if( resultCode == RESULT_OK) {
                adapterZaSpinner.notifyDataSetChanged();
                adapterZaListuKvizova.notifyDataSetChanged();
                Kviz kvizZaDodati = (Kviz)data.getExtras().get("noviKviz");
                boolean dodajNovi = (boolean)data.getExtras().get("dodajNoviKviz");
                if( dodajNovi ) {

                        DodajEditujKviz dodaj = new DodajEditujKviz(getApplicationContext(), kvizZaDodati, dodajNovi);
                        dodaj.execute();
                }
                else{

                        DodajEditujKviz edituj = new DodajEditujKviz(getApplicationContext(), kvizZaDodati, dodajNovi);
                        edituj.execute();

                }
                }
                else{
                  //  spinnerKategorije.setSelection(0);
                    adapterZaSpinner.notifyDataSetChanged();
                }

            }
        }


    @Override
    public void msg(String odabir) throws ExecutionException, InterruptedException {
        detailFrag.primiNotifikaciju( odabir );
    }

    @Override
    public void slanjeObavijestiZaPocetakPreuzimanja(){
        detailFrag.zapocniPreuzimanje();
    }

    @Override
    public void msg1() {
        listaFrag.primiNotifikaciju();
    }

    @Override
    public void slanjeObavijestiZaResetKategorija(){
        listaFrag.funkty();
    }

    public final class FilterKvizova extends AsyncTask<String,Void,Void>{

        Context context;
        String text;

        public FilterKvizova(Context context,String text){
            this.context = context;
            this.text = text;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            prikazaniKvizovi.clear();
        }

        @Override
        protected Void doInBackground(String... strings) {
            //Provjeriti da li je vec u bazi.
            GoogleCredential credential;
            try {
                if( text.equals("Svi") )
                    prikazaniKvizovi.addAll(kvizovi);
                else{
                    InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                    credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                    credential.refreshToken();
                    String TOKEN = credential.getAccessToken();
                    String dajKategorijuQuery = "{  \n" +
                            "   \"structuredQuery\":{  \n" +
                            "      \"where\":{  \n" +
                            "         \"fieldFilter\":{  \n" +
                            "            \"field\":{  \n" +
                            "               \"fieldPath\":\"idKategorije\"\n" +
                            "            },\n" +
                            "            \"op\":\"EQUAL\",\n" +
                            "            \"value\":{  \n" +
                            "               \"stringValue\":\"" + text + "\"\n" +
                            "            }\n" +
                            "         }\n" +
                            "      },\n" +
                            "      \"select\":{  \n" +
                            "         \"fields\":[  \n" +
                            "            {  \n" +
                            "               \"fieldPath\":\"idKategorije\"\n" +
                            "            },\n" +
                            "            {  \n" +
                            "               \"fieldPath\":\"naziv\"\n" +
                            "            },\n" +
                            "            {  \n" +
                            "               \"fieldPath\":\"id\"\n" +
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
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents:runQuery?access_token=";
                    java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                    HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                    CONNECTION.setDoOutput(true);
                    CONNECTION.setRequestMethod("POST");
                    CONNECTION.setRequestProperty("Content-Type", "application/json");
                    CONNECTION.setRequestProperty("Accept", "application/json");
                    String result = "";
                    try (OutputStream os = CONNECTION.getOutputStream()) {
                        byte[] input = dajKategorijuQuery.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                    //int CODE = conn.getResponseCode();
                    InputStream odgovor = CONNECTION.getInputStream();
                    result = "{\"documents\" : ";
                    result += streamToStringConversion(odgovor);
                    result += " }";
                    JSONObject jsonObject  = new JSONObject(result);
                    JSONArray dokumenti = jsonObject.getJSONArray("documents");
                    for( int i = 0; i < dokumenti.length(); i++ ) {
                        JSONObject objekat = dokumenti.getJSONObject(i);
                        try {
                            JSONObject dokument = objekat.getJSONObject("document");
                            JSONObject fields = dokument.getJSONObject("fields");
                            ArrayList<String> naziviPitanja = new ArrayList<>();
                            try {
                                JSONObject pitanja = fields.getJSONObject("pitanja");
                                JSONObject arrayValue = pitanja.getJSONObject("arrayValue");
                                JSONArray values = arrayValue.getJSONArray("values");
                                for (int j = 0; j < values.length(); j++) {
                                    JSONObject valuesObjekat = values.getJSONObject(j);
                                    String nazivPitanja = valuesObjekat.getString("stringValue");
                                    naziviPitanja.add(nazivPitanja);
                                }
                            }
                            catch (JSONException e){

                            }
                            JSONObject naziv = fields.getJSONObject("naziv");
                            JSONObject id = fields.getJSONObject("id");
                            String imeKvize = naziv.getString("stringValue");
                            String idKviza = id.getString("stringValue");
                            JSONObject idKategorije = fields.getJSONObject("idKategorije");
                            String idKategorijeKviza_saKosimCrtama = idKategorije.getString("stringValue");
                            String idKategorijeKviza_saRazmacima_bezKosihCrti = idKategorijeKviza_saKosimCrtama.replaceAll("_KOSA_CRTA_","/");
                            String idKateogijeKviza = idKategorijeKviza_saRazmacima_bezKosihCrti.replaceAll("_RAZMAK_", " ");
                            ArrayList<Pitanje> pitanjaZaKviz = new ArrayList<>();
                            for( int k = 0; k < firebasePitanja.size(); k++ )
                                for( int l = 0; l < naziviPitanja.size(); l++ )
                                    if( naziviPitanja.get(l).equals( firebasePitanja.get(k).getNaziv() ) )
                                        pitanjaZaKviz.add( firebasePitanja.get(k) );
                            Kviz k = new Kviz();
                            for( int m = 0; m < kategorije.size(); m++ )
                                if( kategorije.get(m).getNaziv().equals( idKateogijeKviza ) )
                                    k.setKategorija(kategorije.get(m));
                            k.setNaziv( imeKvize );
                            k.setPitanja( pitanjaZaKviz );
                            k.setNEPROMJENJIVI_ID(idKviza);
                            prikazaniKvizovi.add( k );
                        }
                        catch (JSONException e){
                            //  e.printStackTrace();
                        }
                    }
                    CONNECTION.disconnect();
                }
                Kviz dodajKviz = new Kviz();
                dodajKviz.setNaziv("Dodaj kviz");
                prikazaniKvizovi.add( dodajKviz );

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            super.onPostExecute(result);
            adapterZaSpinner.notifyDataSetChanged();
            adapterZaListuKvizova.notifyDataSetChanged();
        }
    }

    public class DodajEditujKviz extends AsyncTask<String,Void,Void> {

        Context context;
        Kviz kviz;
        boolean dodajKviz;

        public DodajEditujKviz(Context context, Kviz kviz, boolean dodajKviz) {
            this.context = context;
            this.kviz = new Kviz();
            this.kviz.setNEPROMJENJIVI_ID(kviz.getNEPROMJENJIVI_ID());
            this.kviz.setNaziv(kviz.getNaziv());
            this.kviz.setKategorija(kviz.getKategorija());
            this.kviz.setPitanja(kviz.getPitanja());
            this.dodajKviz = dodajKviz;
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
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
                String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Kvizovi/" + kviz.getNEPROMJENJIVI_ID() + "?access_token=";
                URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                CONNECTION.setDoOutput(true);
                CONNECTION.setRequestMethod("PATCH");
                CONNECTION.setRequestProperty("Content-Type", "application/json");
                CONNECTION.setRequestProperty("Accept", "application/json");
                String index_sa_kosom_crtom = kviz.getKategorija().getNaziv().replace(" ", "_RAZMAK_");
                String index = index_sa_kosom_crtom.replaceAll("/", "_KOSA_CRTA_");
                String noviDokument = "{ \"fields\":   { \"id\": { \"stringValue\" : \"" + kviz.getNEPROMJENJIVI_ID() + "\" }, \"naziv\": { \"stringValue\" : \"" + kviz.getNaziv() + "\" }, \"idKategorije\" : { \"stringValue\" : \"" +
                        index + "\" }, \"pitanja\": { \"arrayValue\" : { \"values\": [";
                for (int i = 0; i < kviz.getPitanja().size(); i++) {
                    String jsonPITANJE = "{ \"stringValue\" : \"";
                    jsonPITANJE += kviz.getPitanja().get(i).getNaziv();
                    jsonPITANJE += "\" }";
                    noviDokument += jsonPITANJE;
                    if (i < kviz.getPitanja().size() - 1) noviDokument += ",";
                }
                noviDokument += " ] } } } }";
                try (OutputStream os = CONNECTION.getOutputStream()) {
                    byte[] input = noviDokument.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                //int CODE = conn.getResponseCode();
                InputStream odgovor = CONNECTION.getInputStream();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(odgovor, "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    Log.d("ODGOVOR", response.toString());
                }
                CONNECTION.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (dodajKviz) {
                kvizovi.add(kviz);
                AlertDialog alertDialog = new AlertDialog.Builder(KvizoviAkt.this).create();
                alertDialog.setTitle("Obavijest");
                String tekstObavjestenja = "NOVI KVIZ USPJESNO DODAN!\n\nNaziv: " + kviz.getNaziv() + "\nKategorija: " + kviz.getKategorija().getNaziv() + "\nBroj pitanja: " + String.valueOf(kviz.getPitanja().size());
                alertDialog.setMessage(tekstObavjestenja);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                alertDialog.show();
                RangListaKlasa rangListaKlasa = new RangListaKlasa();
                rangListaKlasa.setNazivKviza(kviz.getNaziv());
                db.dodajKviz(kviz,rangListaKlasa);
                    DodajEditujRangListu dodaj = new DodajEditujRangListu(getApplicationContext(), kviz, rangListaKlasa, true);
                    dodaj.execute();
                    FilterKvizova filter = new FilterKvizova(getApplicationContext(), "Svi");
                    filter.execute();

            } else {
                String tekstObavjestenja = "KVIZ USPJESNO UREDJEN!\n\n";
                String stariNaziv = "";
                String noviNaziv = "";
                for (int i = 0; i < kvizovi.size(); i++) {
                    if (i == pozicijaKviza) {
                        tekstObavjestenja += "Stari naziv: " + kvizovi.get(i).getNaziv() + "\nStara kategorija: " + kvizovi.get(i).getKategorija().getNaziv() + "\nStari broj pitanja: " + String.valueOf(kvizovi.get(i).getPitanja().size());
                        stariNaziv = kvizovi.get(i).getNaziv();
                        kvizovi.get(i).setNEPROMJENJIVI_ID(kviz.getNEPROMJENJIVI_ID());
                        kvizovi.get(i).setNaziv(kviz.getNaziv());
                        kvizovi.get(i).setKategorija(kviz.getKategorija());
                        kvizovi.get(i).setPitanja(kviz.getPitanja());
                        noviNaziv = kvizovi.get(i).getNaziv();
                        tekstObavjestenja += "\n\nNovi naziv: " + kvizovi.get(i).getNaziv() + "\nNova kategorija: " + kvizovi.get(i).getKategorija().getNaziv() + "\nNovi broj pitanja: " + String.valueOf(kvizovi.get(i).getPitanja().size());

                    }
                }
                for (int i = 0; i < RANG_LISTE.size(); i++)
                    if (RANG_LISTE.get(i).getNazivKviza().equals(stariNaziv))
                        RANG_LISTE.get(i).setNazivKviza(noviNaziv);
                    RangListaKlasa rangListaKlasa = new RangListaKlasa();
                    for (int i = 0; i < RANG_LISTE.size(); i++)
                        if (RANG_LISTE.get(i).getNazivKviza().equals(noviNaziv))
                            rangListaKlasa = RANG_LISTE.get(i);

                            DodajEditujRangListu edituj = new DodajEditujRangListu(getApplicationContext(), kviz, rangListaKlasa, false);
                            edituj.execute();

                    AlertDialog alertDialog = new AlertDialog.Builder(KvizoviAkt.this).create();
                    alertDialog.setTitle("Obavijest");
                    alertDialog.setMessage(tekstObavjestenja);
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                    alertDialog.show();
                        db.editujKviz(kviz);
                        FilterKvizova filter = new FilterKvizova(getApplicationContext(), ((Kategorija) spinnerKategorije.getSelectedItem()).getNaziv());
                        filter.execute();

                }
            }

        }


        public class PokupiFirebasePitanja extends AsyncTask<String, Void, Void> {

            private Context context;

            public PokupiFirebasePitanja(Context context) {
                this.context = context;
            }

            @Override
            protected void onPreExecute() {
                firebasePitanja.clear();
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
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Pitanja?access_token=";
                    java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                    HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                    InputStream inputStream = new BufferedInputStream(CONNECTION.getInputStream());
                    String result = FirebasePitanja.streamToStringConversion(inputStream);
                    JSONObject jo = new JSONObject(result);
                    JSONArray dokumentovanaPitanja;
                    try {
                        dokumentovanaPitanja = jo.getJSONArray("documents");
                    } catch (Exception e) {
                        return null;
                    }
                    for (int i = 0; i < dokumentovanaPitanja.length(); i++) {
                        JSONObject dokument = dokumentovanaPitanja.getJSONObject(i);
                        JSONObject field = dokument.getJSONObject("fields");
                        JSONObject nazivOBJEKAT = field.getJSONObject("naziv");
                        String nazivString = nazivOBJEKAT.getString("stringValue");
                        JSONObject indexTacnogOBJEKAT = field.getJSONObject("indexTacnog");
                        int indexTacnogINT = indexTacnogOBJEKAT.getInt("integerValue");
                        JSONObject odgovoriOBJEKAT = field.getJSONObject("odgovori");
                        JSONObject odgovoriARRAY = odgovoriOBJEKAT.getJSONObject("arrayValue");
                        JSONArray odgovori = odgovoriARRAY.getJSONArray("values");
                        ArrayList<String> odgovoriLista = new ArrayList<>();
                        for (int j = 0; j < odgovori.length(); j++) {
                            try {
                                JSONObject stringValue = odgovori.getJSONObject(j);
                                String odgovor = stringValue.getString("stringValue");
                                odgovoriLista.add(odgovor);
                            } catch (JSONException e) {
                                //Ignore
                            }
                        }
                        Pitanje novoPitanje = new Pitanje();
                        novoPitanje.setNaziv(nazivString);
                        novoPitanje.setTacan(odgovoriLista.get(indexTacnogINT));
                        novoPitanje.setOdgovori(odgovoriLista);
                        firebasePitanja.add(novoPitanje);
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

                    PokupiFirebaseKvizove pokupiFirebaseKvizove = new PokupiFirebaseKvizove(getApplicationContext());
                    pokupiFirebaseKvizove.execute();

            }

        }

        public class PokupiFirebaseKvizove extends AsyncTask<String, Void, Void> {

            private Context context;

            public PokupiFirebaseKvizove(Context context) {
                this.context = context;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                kvizovi.clear();
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
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Kvizovi?access_token=";
                    java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                    HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                    InputStream inputStream = new BufferedInputStream(CONNECTION.getInputStream());
                    String result = streamToStringConversion(inputStream);
                    JSONObject jo = new JSONObject(result);
                    JSONArray dokumentovaniKvizovi;
                    try {
                        dokumentovaniKvizovi = jo.getJSONArray("documents");
                    } catch (Exception e) {
                        return null;
                    }
                    //Prvo napunimo kvizove.
                    for (int i = 0; i < dokumentovaniKvizovi.length(); i++) {
                        JSONObject dokument = dokumentovaniKvizovi.getJSONObject(i);
                        JSONObject field = dokument.getJSONObject("fields");
                        JSONObject nazivOBJEKAT = field.getJSONObject("naziv");
                        JSONObject idOBJEKAT = field.getJSONObject("id");
                        String id = idOBJEKAT.getString("stringValue");
                        String nazivString = nazivOBJEKAT.getString("stringValue");
                        JSONObject idKategorijeOBJEKAT = field.getJSONObject("idKategorije");
                        String idKategorijeString_NIJE_DEKODIRAN = idKategorijeOBJEKAT.getString("stringValue");
                        String idKategorijeString_DEKODIRAN_KOSIM = idKategorijeString_NIJE_DEKODIRAN.replaceAll("_KOSA_CRTA_", "/");
                        String idKategorijeString_DEKODIRAN_RAZMACIMA = idKategorijeString_DEKODIRAN_KOSIM.replaceAll("_RAZMAK_", " ");
                        JSONObject pitanjaOBJECT = field.getJSONObject("pitanja");
                        JSONObject pitanjaARRAY = pitanjaOBJECT.getJSONObject("arrayValue");
                        ArrayList<String> pitanjaLista = new ArrayList<>();
                        try {
                            JSONArray pitanja = pitanjaARRAY.getJSONArray("values");
                            for (int j = 0; j < pitanja.length(); j++) {
                                JSONObject stringValue = pitanja.getJSONObject(j);
                                String string = stringValue.getString("stringValue");
                                pitanjaLista.add(string);
                            }
                        } catch (Exception e) {

                        }
                        ArrayList<Pitanje> pitanjaZaKviz = new ArrayList<>();
                        for (int k = 0; k < firebasePitanja.size(); k++)
                            for (int l = 0; l < pitanjaLista.size(); l++)
                                if (pitanjaLista.get(l).equals(firebasePitanja.get(k).getNaziv()))
                                    pitanjaZaKviz.add(firebasePitanja.get(k));
                        Kviz noviKviz = new Kviz();
                        noviKviz.setNaziv(nazivString);
                        for (int m = 0; m < kategorije.size(); m++)
                            if (kategorije.get(m).getNaziv().equals(idKategorijeString_DEKODIRAN_RAZMACIMA))
                                noviKviz.setKategorija(kategorije.get(m));
                        noviKviz.setPitanja(pitanjaZaKviz);
                        noviKviz.setNEPROMJENJIVI_ID(id);
                        kvizovi.add(kvizovi.size(), noviKviz);
                    }
                    CONNECTION.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                prikazaniKvizovi.addAll(kvizovi);
                Kviz k = new Kviz();
                k.setNaziv("Dodaj kviz");
                prikazaniKvizovi.add(k);
                adapterZaListuKvizova.notifyDataSetChanged();

                    PokupiFirebaseRangliste pokupiFirebaseRangliste = new PokupiFirebaseRangliste(getApplicationContext());
                    pokupiFirebaseRangliste.execute();

            }
        }

        public class PokupiFirebaseKategorije extends AsyncTask<String, Void, Void> {

            private Context context;

            public PokupiFirebaseKategorije(Context context) {
                this.context = context;
            }

            @Override
            protected void onPreExecute() {
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
                    String result = streamToStringConversion(inputStream);
                    JSONObject jo = new JSONObject(result);
                    JSONArray dokumentovaneRangliste;
                    try {
                        dokumentovaneRangliste = jo.getJSONArray("documents");
                    } catch (Exception e) {
                        return null;
                    }

                    for (int i = 0; i < dokumentovaneRangliste.length(); i++) {
                        JSONObject dokument = dokumentovaneRangliste.getJSONObject(i);
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
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                    PokupiFirebasePitanja pokupiFirebasePitanja = new PokupiFirebasePitanja(getApplicationContext());
                    pokupiFirebasePitanja.execute();

                adapterZaSpinner.notifyDataSetChanged();
            }
        }

    public class DodajEditujRangListu extends AsyncTask<String,Void,Void> {

        private Context context;
        private Kviz kviz;
        private RangListaKlasa rangListaKlasa;
        private boolean dodaj;

        public DodajEditujRangListu(Context context, Kviz kviz, RangListaKlasa rangListaKlasa,boolean dodaj) {
            this.context = context;
            this.kviz = kviz;
            this.rangListaKlasa = rangListaKlasa;
            this.dodaj = dodaj;

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
                String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Rangliste/" + rangListaKlasa.getNEPROMJENJIVI_ID() + "?access_token=";
                URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                CONNECTION.setDoOutput(true);
                CONNECTION.setRequestMethod("PATCH");
                CONNECTION.setRequestProperty("Content-Type", "application/json");
                CONNECTION.setRequestProperty("Accept", "application/json");
                String noviDokument = "{ \"fields\": { \"id\": {\"stringValue\" : \"" + rangListaKlasa.getNEPROMJENJIVI_ID() + "\"}, \"nazivKviza\": { \"stringValue\": \"" + kviz.getNaziv() + "\"}, \"lista\": {\"mapValue\": {\"fields\": {";
                int VELICINA_MAPE = rangListaKlasa.getMapa().size();
                int brojac = 0;
                for (Map.Entry<Integer, Pair<String, Double>> entry : rangListaKlasa.getMapa().entrySet()) {
                    Integer pozicijaPokusaja = entry.getKey();
                    Pair<String, Double> podaciOPokusaju = entry.getValue();
                    noviDokument += "\"" + pozicijaPokusaja + "\": {\"mapValue\": {\"fields\": {\"" + podaciOPokusaju.first + "\": {\"doubleValue\": " + String.valueOf(podaciOPokusaju.second) + "}}}}";
                    if (brojac < VELICINA_MAPE - 1) noviDokument += ",";
                    brojac++;
                }
                noviDokument += "} } } } }";
                try (OutputStream os = CONNECTION.getOutputStream()) {
                    byte[] input = noviDokument.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                //int CODE = conn.getResponseCode();
                InputStream odgovor = CONNECTION.getInputStream();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(odgovor, "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    Log.d("ODGOVOR", response.toString());
                }
                if( dodaj )
                    RANG_LISTE.add(rangListaKlasa);
                CONNECTION.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    public class PokupiFirebaseRangliste extends AsyncTask<String, Void, Void> {

        private Context context;

        public PokupiFirebaseRangliste(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            RANG_LISTE.clear();

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
                String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Rangliste?access_token=";
                java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                InputStream inputStream = new BufferedInputStream(CONNECTION.getInputStream());
                String result = streamToStringConversion(inputStream);
                JSONObject jo = new JSONObject(result);
                JSONArray dokumentovaniKvizovi;
                try {
                    dokumentovaniKvizovi = jo.getJSONArray("documents");
                } catch (Exception e) {
                    return null;
                }
                //Prvo napunimo kvizove.
                for (int i = 0; i < dokumentovaniKvizovi.length(); i++) {
                    JSONObject dokument = dokumentovaniKvizovi.getJSONObject(i);
                    JSONObject field = dokument.getJSONObject("fields");
                    JSONObject nazivKvizaOBJEKAT = field.getJSONObject("nazivKviza");
                    JSONObject idOBJEKAT = field.getJSONObject("id");
                    String id = idOBJEKAT.getString("stringValue");
                    String nazivKvizaString = nazivKvizaOBJEKAT.getString("stringValue");
                    Map<Integer,Pair<String,Double>> povratnaMapa = new TreeMap<>();
                    try {
                        JSONObject lista = field.getJSONObject("lista");
                        JSONObject mapValue = lista.getJSONObject("mapValue");
                        JSONObject mapFields = mapValue.getJSONObject("fields");
                        int redniBroj = 1;
                        while (true) {
                            try {
                                String stringRedniBroj = String.valueOf(redniBroj);
                                JSONObject pozicijaURangListi = mapFields.getJSONObject(stringRedniBroj);
                                //Uzeli smo sada, ako nije bacen izuzetak, objekat koji ima vrijednost mapValue,
                                //ciji je kljuc ime igraca (objekat koji cemo dobit iz fields), te vrijednost doubleValue procenat
                                JSONObject vrijednostMAPA = pozicijaURangListi.getJSONObject("mapValue");
                                JSONObject vrijednostMAPAfields = vrijednostMAPA.getJSONObject("fields");
                                String vrijednostMAPAfields_string = vrijednostMAPAfields.toString();
                                String nazivUcesnikaKviza = "";
                                for (int j = 0; j < vrijednostMAPAfields_string.length(); j++) {
                                    if (vrijednostMAPAfields_string.charAt(j) == '\"') {
                                        j++;
                                        while (vrijednostMAPAfields_string.charAt(j) != '\"') {
                                            nazivUcesnikaKviza += String.valueOf(vrijednostMAPAfields_string.charAt(j));
                                            j++;
                                        }
                                        break;
                                    }
                                }
                                JSONObject ucesnik = vrijednostMAPAfields.getJSONObject(nazivUcesnikaKviza);
                                Double procenatTacnih = ucesnik.getDouble("doubleValue");
                                povratnaMapa.put(redniBroj, new Pair<>(nazivUcesnikaKviza, procenatTacnih));
                                redniBroj++;
                            } catch (Exception e) {
                                break;
                            }
                        }
                    }
                    catch (JSONException e){
                        //Ignored
                    }
                    RangListaKlasa novaRangListaKlasa = new RangListaKlasa();
                    novaRangListaKlasa.setNEPROMJENJIVI_ID(id);
                    novaRangListaKlasa.setNazivKviza(nazivKvizaString);
                    novaRangListaKlasa.setMapa(povratnaMapa);
                    RANG_LISTE.add(novaRangListaKlasa);
                }
                CONNECTION.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    public boolean imaInterneta() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }
   @Override
    protected void onStop(){
        super.onStop();
        unregisterReceiver(broadcastReceiver);
   }

   @Override
    protected void onRestart(){
        super.onRestart();
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(broadcastReceiver,intentFilter);
   }
}



