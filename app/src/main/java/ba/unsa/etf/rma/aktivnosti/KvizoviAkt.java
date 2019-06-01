package ba.unsa.etf.rma.aktivnosti;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.fragmenti.DetailFrag;
import ba.unsa.etf.rma.fragmenti.ListaFrag;
import ba.unsa.etf.rma.klase.AdapterZaListuKvizova;
import ba.unsa.etf.rma.klase.FirebasePitanja;
import ba.unsa.etf.rma.klase.Kategorija;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;

import static ba.unsa.etf.rma.klase.FirebaseKvizovi.streamToStringConversion;

public class KvizoviAkt extends AppCompatActivity implements ListaFrag.OnListaFragmentListener, DetailFrag.OnDetailFragmentListener {

    private ListView listaKvizova;
    private Spinner spinnerKategorije;
    public static boolean USPRAVAN_DISPLEJ = false;
    public static boolean POSTOJI_LI_KATEGORIJA = true;


    //Lista 'kvizovi' se koristi za cuvanje svih postojecih kvizova,
    //dok se lista 'prikazaniKvizovi' koristi za prikazivanje svih/filtriranih kvizova.
    public static ArrayList<Kviz> kvizovi = new ArrayList<>();
    public static ArrayList<Pitanje> firebasePitanja = new ArrayList<>();
    public static ArrayList<Kviz> prikazaniKvizovi = new ArrayList<>();
    public static ArrayList<Kategorija> kategorije = new ArrayList<>();
    private ArrayAdapter<Kategorija> adapterZaSpinner;
    private AdapterZaListuKvizova adapterZaListuKvizova;
    private String spinnerOdabir;
    public static int pozicijaKviza;
    DetailFrag detailFrag;
    ListaFrag listaFrag;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FrameLayout layoutDetalji = (FrameLayout) findViewById(R.id.detailPlace);
            if (layoutDetalji != null) {
                USPRAVAN_DISPLEJ = false;
                kvizovi.clear();
                kategorije.clear();
                prikazaniKvizovi.clear();
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
                    PokupiFirebaseKategorije pokupiFirebaseKategorije = new PokupiFirebaseKategorije(getApplicationContext());
                    pokupiFirebaseKategorije.execute();
                }

                //Slusac koji vrsi filtriranje listView-a svih kvizova na osnovu odabrane kategorije
                //u spinneru, prikazivanje Toast poruke za korisnika, npr. "Odabrano: Svi".
                spinnerKategorije.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        adapterZaSpinner.notifyDataSetChanged();

                        //Stvaranje string vrijednosti za Toast poruku.
                        String text = parent.getItemAtPosition(position).toString();
                        spinnerOdabir = text;
                        String textBezRazmaka = text.replaceAll(" ","_RAZMAK_");
                        String textBezKosihBezRazmaka = textBezRazmaka.replaceAll("/","_KOSA_CRTA_");
                        Toast.makeText(parent.getContext(), "Odabrano: " + text, Toast.LENGTH_SHORT).show();
                        FilterKvizova filter = new FilterKvizova(getApplicationContext(),textBezKosihBezRazmaka);
                        filter.execute();


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
                        Kviz trenutniKviz = new Kviz();
                        trenutniKviz.setNaziv("Dodaj kviz");
                        trenutniKviz.setKategorija( (Kategorija)spinnerKategorije.getSelectedItem() );
                        Intent dodajKvizAkt = new Intent(KvizoviAkt.this, DodajKvizAkt.class);
                        for (int i = 0; i < kvizovi.size(); i++)
                            if (kvizovi.get(i).getNaziv().equals(((Kviz) parent.getItemAtPosition(position)).getNaziv())) {
                                if( !((Kviz) parent.getItemAtPosition(position)).getNaziv().equals("Dodaj kviz") ) {
                                    trenutniKviz.setNaziv(kvizovi.get(i).getNaziv());
                                    trenutniKviz.setKategorija(kvizovi.get(i).getKategorija());
                                    trenutniKviz.setPitanja(kvizovi.get(i).getPitanja());
                                    trenutniKviz.setNEPROMJENJIVI_ID( kvizovi.get(i).getNEPROMJENJIVI_ID() );
                                }
                                pozicijaKviza = i;
                            }
                        dodajKvizAkt.putExtra("sviKvizovi", kvizovi);
                        dodajKvizAkt.putExtra("trenutniKviz", trenutniKviz );
                        dodajKvizAkt.putExtra("sveKategorije", kategorije);

                        KvizoviAkt.this.startActivityForResult(dodajKvizAkt, pozicijaKviza);
                        return true;
                    }
                });

                listaKvizova.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Kviz k = (Kviz) parent.getItemAtPosition(position);
                        if (!k.getNaziv().equals("Dodaj kviz")) {
                            Intent intent = new Intent(KvizoviAkt.this, IgrajKvizAkt.class);
                            intent.putExtra("odabraniKviz", k);
                            startActivity(intent);
                        }
                    }
                });
            }

//        } else {
//            FragmentManager fragmentManager = getSupportFragmentManager();
//            FrameLayout layoutDetalji = (FrameLayout) findViewById(R.id.detailPlace);
//            if (layoutDetalji != null) {
//
//                listaFrag = new ListaFrag();
//                detailFrag = new DetailFrag();
//                FragmentManager fragmentManagerFinal = getSupportFragmentManager();
//                FragmentTransaction fragmentTransaction = fragmentManagerFinal.beginTransaction();
//                fragmentTransaction.replace(R.id.listPlace, listaFrag);
//                fragmentTransaction.replace(R.id.detailPlace, detailFrag);
//                fragmentTransaction.commit();
//                kvizovi.clear();
//                kategorije.clear();
//                firebasePitanja.clear();
//            }
//            else{
//                //Potrebno je dodijeliti sve vrijednosti pomocu id-a.
//                listaKvizova = (ListView) findViewById(R.id.lvKvizovi);
//                spinnerKategorije = (Spinner) findViewById(R.id.spPostojeceKategorije);
//
//                //Postavljanje adaptera.
//                adapterZaSpinner = new ArrayAdapter<Kategorija>(this, android.R.layout.simple_list_item_1, kategorije);
//                spinnerKategorije.setAdapter(adapterZaSpinner);
//                adapterZaListuKvizova = new AdapterZaListuKvizova(this, prikazaniKvizovi);
//                listaKvizova.setAdapter(adapterZaListuKvizova);
//
//                //Slusac koji vrsi filtriranje listView-a svih kvizova na osnovu odabrane kategorije
//                //u spinneru, prikazivanje Toast poruke za korisnika, npr. "Odabrano: Svi".
//                spinnerKategorije.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//                    @Override
//                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                        adapterZaSpinner.notifyDataSetChanged();
//
//                        //Stvaranje string vrijednosti za Toast poruku.
//                        String text = parent.getItemAtPosition(position).toString();
//                        spinnerOdabir = text;
//                        String textBezRazmaka = text.replaceAll(" ","_RAZMAK_");
//                        String textBezKosihBezRazmaka = textBezRazmaka.replaceAll("/","_KOSA_CRTA_");
//                        Toast.makeText(parent.getContext(), "Odabrano: " + text, Toast.LENGTH_SHORT).show();
//                        FilterKvizova filter = new FilterKvizova(getApplicationContext(),textBezKosihBezRazmaka);
//                        filter.execute();
//
//                    }
//
//                    @Override
//                    public void onNothingSelected(AdapterView<?> parent) {
//                        //Ukoliko ne selektujemo nista, nista se nece desiti.
//                    }
//                });
//
//                //Ukoliko kliknemo DUGO na neki od elemenata kviza, otvara se nova aktivnost za kreiranje novog kviza
//                //ukoliko je odabran element "Dodaj kviz", odnosno za uredjivanje ukoliko je odabran bilo koji drugi element.
//                listaKvizova.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//                    @Override
//                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                        Kviz trenutniKviz = new Kviz();
//                        trenutniKviz.setNaziv("Dodaj kviz");
//                        trenutniKviz.setKategorija( (Kategorija)spinnerKategorije.getSelectedItem() );
//                        System.out.print( trenutniKviz.getKategorija().getNaziv() );
//                        Intent dodajKvizAkt = new Intent(KvizoviAkt.this, DodajKvizAkt.class);
//                        for (int i = 0; i < kvizovi.size(); i++)
//                            if (kvizovi.get(i).getNaziv().equals(((Kviz) parent.getItemAtPosition(position)).getNaziv())) {
//                                if( !((Kviz) parent.getItemAtPosition(position)).getNaziv().equals("Dodaj kviz") ) {
//                                    trenutniKviz.setNaziv(kvizovi.get(i).getNaziv());
//                                    trenutniKviz.setKategorija(kvizovi.get(i).getKategorija());
//                                    trenutniKviz.setPitanja(kvizovi.get(i).getPitanja());
//                                    trenutniKviz.setNEPROMJENJIVI_ID( kvizovi.get(i).getNEPROMJENJIVI_ID() );
//                                }
//                                pozicijaKviza = i;
//                            }
//                        dodajKvizAkt.putExtra("sviKvizovi", kvizovi);
//                        dodajKvizAkt.putExtra("trenutniKviz", trenutniKviz );
//                        dodajKvizAkt.putExtra("sveKategorije", kategorije);
//
//                        KvizoviAkt.this.startActivityForResult(dodajKvizAkt, pozicijaKviza);
//                        return true;
//                    }
//                });
//
//                listaKvizova.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                        Kviz k = (Kviz) parent.getItemAtPosition(position);
//                        if (!k.getNaziv().equals("Dodaj kviz")) {
//                            Intent intent = new Intent(KvizoviAkt.this, IgrajKvizAkt.class);
//                            intent.putExtra("odabraniKviz", k);
//                            startActivity(intent);
//                        }
//                    }
//                });
//            }
//
//        }
    }

    private void napuniPodacima() {
        //FUNKCIJU MOZEMO KORISTITI UKOLIKO ZELIMO NAPUNITI APLIKACIJU RANDOM PODACIMA ODMAH NAKON
        //POKRETANJA APLIKACIJE I TO TAKO STO LINIJU KODA 55 'inicijalizirajApp();' ZAMIJENIMO SA
        // 'napuniPodacima();'.

        //Kreirajmo kategoriju
        Kategorija k1 = new Kategorija();
        k1.setNaziv("Lagana");
        k1.setId("0");

        Kategorija k2 = new Kategorija();
        k2.setNaziv("Srednja");
        k2.setId("1");

        //Dodajmo par pitanja
        ArrayList<String> odgovori1 = new ArrayList<>();
        odgovori1.add("Plavo");
        odgovori1.add("Crveno");
        odgovori1.add("Zeleno");
        Pitanje p1 = new Pitanje();
        p1.setNaziv("Pitanje 1");
        p1.setTekstPitanja("Koje je boje nebo?");
        p1.setOdgovori(odgovori1);
        p1.setTacan("Plavo");

        ArrayList<String> odgovori2 = new ArrayList<>();
        odgovori2.add("Luk");
        odgovori2.add("Paprika");
        odgovori2.add("Jabuka");
        Pitanje p2 = new Pitanje();
        p2.setNaziv("Pitanje 2");
        p2.setTekstPitanja("Sta od sljedeceg je voce?");
        p2.setOdgovori(odgovori2);
        p2.setTacan("Jabuka");

        ArrayList<String> odgovori3 = new ArrayList<>();
        odgovori3.add("C4");
        odgovori3.add("A3");
        odgovori3.add("X5");
        Pitanje p3 = new Pitanje();
        p3.setNaziv("Pitanje 3");
        p3.setTekstPitanja("Koji model Audija postoji?");
        p3.setOdgovori(odgovori3);
        p3.setTacan("A3");

        ArrayList<String> odgovori4 = new ArrayList<>();
        odgovori4.add("7");
        odgovori4.add("6");
        odgovori4.add("5");
        Pitanje p4 = new Pitanje();
        p4.setNaziv("Pitanje 4");
        p4.setTekstPitanja("Koliko ima dana u sedmici?");
        p4.setOdgovori(odgovori4);
        p4.setTacan("7");

        ArrayList<String> odgovori5 = new ArrayList<>();
        odgovori5.add("3");
        odgovori5.add("4");
        odgovori5.add("5");
        Pitanje p5 = new Pitanje();
        p5.setNaziv("Pitanje 5");
        p5.setTekstPitanja("Koliko ima godisnjih doba?");
        p5.setOdgovori(odgovori5);
        p5.setTacan("4");

        //Napravimo i kvizove
        ArrayList<Pitanje> pitanjaZaKviz1 = new ArrayList<>();
        pitanjaZaKviz1.add(p1);
        pitanjaZaKviz1.add(p2);
        pitanjaZaKviz1.add(p3);
        Kviz kviz1 = new Kviz();
        kviz1.setNaziv("Kviz 1");
        kviz1.setKategorija(k1);
        kviz1.setPitanja(pitanjaZaKviz1);

        ArrayList<Pitanje> pitanjaZaKviz2 = new ArrayList<>();
        pitanjaZaKviz2.add(p3);
        pitanjaZaKviz2.add(p4);
        pitanjaZaKviz2.add(p5);
        Kviz kviz2 = new Kviz();
        kviz2.setNaziv("Kviz 2");
        kviz2.setKategorija(k2);
        kviz2.setPitanja(pitanjaZaKviz2);

        Kategorija apstraktnaKategorija = new Kategorija();
        apstraktnaKategorija.setNaziv("Svi");
        apstraktnaKategorija.setId("10");
        Kviz apstraktniKviz = new Kviz();
        apstraktniKviz.setNaziv("Dodaj kviz");

        kvizovi.add(kviz1);
        kvizovi.add(kviz2);
       // kvizovi.add(apstraktniKviz);

        kategorije.add(apstraktnaKategorija);
        kategorije.add(k1);
        kategorije.add(k2);
        adapterZaSpinner.notifyDataSetChanged();

        prikazaniKvizovi.add( kviz1 );
        prikazaniKvizovi.add( kviz2 );
        prikazaniKvizovi.add( apstraktniKviz );
        adapterZaListuKvizova.notifyDataSetChanged();
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
                    DodajEditujKviz dodaj = new DodajEditujKviz(getApplicationContext(),kvizZaDodati,dodajNovi);
                    dodaj.execute();
                }
                else{
                    DodajEditujKviz edituj = new DodajEditujKviz(getApplicationContext(),kvizZaDodati,dodajNovi);
                    edituj.execute();
                }
                }
                else{
                    spinnerKategorije.setSelection(0);
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
            for( int i = 0; i < kategorije.size(); i++ )
                if( kategorije.get(i).getNaziv().equals( text ) )
                    spinnerKategorije.setSelection(i);
            adapterZaSpinner.notifyDataSetChanged();
            adapterZaListuKvizova.notifyDataSetChanged();
        }
    }

    public class DodajEditujKviz extends AsyncTask<String,Void,Void>{

        Context context;
        Kviz kviz;
        boolean dodajKviz;

        public DodajEditujKviz(Context context,Kviz kviz,boolean dodajKviz){
            this.context = context;
            this.kviz = new Kviz();
            this.kviz.setNEPROMJENJIVI_ID( kviz.getNEPROMJENJIVI_ID() );
            this.kviz.setNaziv( kviz.getNaziv() );
            this.kviz.setKategorija( kviz.getKategorija() );
            this.kviz.setPitanja( kviz.getPitanja() );
            this.dodajKviz = dodajKviz;
        }
                @Override
                protected void onPreExecute(){

                    super.onPreExecute();
                }

                @Override
                protected Void doInBackground(String... strings) {
                    //Provjeriti da li je vec u bazi.

                    GoogleCredential credential;
                    try{

                        InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                        credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                        credential.refreshToken();
                        String TOKEN = credential.getAccessToken();
                        String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Kvizovi/"+ kviz.getNEPROMJENJIVI_ID() +"?access_token=";
                        URL urlOBJ = new URL( URL + URLEncoder.encode(TOKEN,"UTF-8"));
                        HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                        CONNECTION.setDoOutput(true);
                        CONNECTION.setRequestMethod("PATCH");
                        CONNECTION.setRequestProperty("Content-Type","application/json");
                        CONNECTION.setRequestProperty("Accept","application/json");
                        String index_sa_kosom_crtom = kviz.getKategorija().getNaziv().replace(" ", "_RAZMAK_");
                        String index = index_sa_kosom_crtom.replaceAll("/", "_KOSA_CRTA_");
                        String noviDokument = "{ \"fields\":   { \"id\": { \"stringValue\" : \"" + kviz.getNEPROMJENJIVI_ID() + "\" }, \"naziv\": { \"stringValue\" : \"" + kviz.getNaziv() + "\" }, \"idKategorije\" : { \"stringValue\" : \"" +
                                index + "\" }, \"pitanja\": { \"arrayValue\" : { \"values\": [";
                        for( int i = 0; i < kviz.getPitanja().size(); i++ ){
                            String jsonPITANJE = "{ \"stringValue\" : \"";
                            jsonPITANJE += kviz.getPitanja().get(i).getNaziv();
                            jsonPITANJE += "\" }";
                            noviDokument += jsonPITANJE;
                            if( i < kviz.getPitanja().size() - 1   ) noviDokument += ",";
                        }
                        noviDokument += " ] } } } }";
                        try(OutputStream os = CONNECTION.getOutputStream()){
                            byte[] input = noviDokument.getBytes("utf-8");
                            os.write(input,0,input.length);
                        }
                        //int CODE = conn.getResponseCode();
                        InputStream odgovor = CONNECTION.getInputStream();
                        try(BufferedReader br = new BufferedReader(
                                new InputStreamReader(odgovor,"utf-8"))) {
                            StringBuilder response = new StringBuilder();
                            String responseLine = null;
                            while((responseLine = br.readLine()) != null){
                                response.append(responseLine.trim());
                            }
                            Log.d("ODGOVOR",response.toString());
                        }
                        CONNECTION.disconnect();
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result){
                    super.onPostExecute(result);
                    if(dodajKviz){
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
                        FilterKvizova filter = new FilterKvizova(getApplicationContext(),"Svi");
                        filter.execute();
                    }
                    else {
                        String tekstObavjestenja = "KVIZ USPJESNO UREDJEN!\n\n";
                        for( int i = 0; i < kvizovi.size(); i++ ){
                            if( i == pozicijaKviza ){
                                tekstObavjestenja += "Stari naziv: " + kvizovi.get(i).getNaziv() + "\nStara kategorija: " + kvizovi.get(i).getKategorija().getNaziv() + "\nStari broj pitanja: " + String.valueOf(kvizovi.get(i).getPitanja().size());
                                kvizovi.get(i).setNEPROMJENJIVI_ID( kviz.getNEPROMJENJIVI_ID() );
                                kvizovi.get(i).setNaziv( kviz.getNaziv() );
                                kvizovi.get(i).setKategorija( kviz.getKategorija() );
                                kvizovi.get(i).setPitanja( kviz.getPitanja() );
                                tekstObavjestenja += "\n\nNovi naziv: " + kvizovi.get(i).getNaziv() + "\nNova kategorija: " + kvizovi.get(i).getKategorija().getNaziv() + "\nNovi broj pitanja: " + String.valueOf(kvizovi.get(i).getPitanja().size());

                            }
                        }
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
                        FilterKvizova filter = new FilterKvizova(getApplicationContext(), ((Kategorija)spinnerKategorije.getSelectedItem()).getNaziv());
                        filter.execute();
                    }
                }

        }

    public class PokupiFirebasePitanja extends AsyncTask<String,Void,Void>{

        private Context context;

        public PokupiFirebasePitanja(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute(){
            firebasePitanja.clear();
        }

        @Override
        protected Void doInBackground(String... strings) {
            //Provjeriti da li je vec u bazi.
            GoogleCredential credential;
            try{

                InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                credential.refreshToken();
                String TOKEN = credential.getAccessToken();
                String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Pitanja?access_token=";
                java.net.URL urlOBJ = new URL( URL + URLEncoder.encode(TOKEN,"UTF-8"));
                HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                InputStream inputStream = new BufferedInputStream(CONNECTION.getInputStream());
                String result = FirebasePitanja.streamToStringConversion(inputStream);
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
                        try {
                            JSONObject stringValue = odgovori.getJSONObject(j);
                            String odgovor = stringValue.getString("stringValue");
                            odgovoriLista.add(odgovor);
                        }
                        catch (JSONException e){
                            //Ignore
                        }
                    }
                    Pitanje novoPitanje = new Pitanje();
                    novoPitanje.setNaziv( nazivString );
                    novoPitanje.setTacan( odgovoriLista.get(indexTacnogINT) );
                    novoPitanje.setOdgovori( odgovoriLista );
                    firebasePitanja.add( novoPitanje );
                }

                CONNECTION.disconnect();
            }
            catch (IOException  | JSONException e){
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result){
            super.onPostExecute(result);
            PokupiFirebaseKvizove pokupiFirebaseKvizove = new PokupiFirebaseKvizove(getApplicationContext());
            pokupiFirebaseKvizove.execute();
        }

    }

    public class PokupiFirebaseKvizove extends AsyncTask<String,Void,Void>{

        private Context context;

        public PokupiFirebaseKvizove(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute(){
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
                    noviKviz.setNEPROMJENJIVI_ID( id );
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
        protected void onPostExecute(Void result){
            super.onPostExecute(result);
            adapterZaListuKvizova.notifyDataSetChanged();
            prikazaniKvizovi.clear();
            prikazaniKvizovi.addAll(kvizovi);
            Kviz k = new Kviz();
            k.setNaziv("Dodaj kviz");
            prikazaniKvizovi.add(k);
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
                String result = streamToStringConversion(inputStream);
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
            PokupiFirebasePitanja pokupiFirebasePitanja = new PokupiFirebasePitanja(getApplicationContext());
            pokupiFirebasePitanja.execute();
            adapterZaSpinner.notifyDataSetChanged();
        }
    }
}


