package ba.unsa.etf.rma.fragmenti;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

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
import ba.unsa.etf.rma.aktivnosti.DodajKvizAkt;
import ba.unsa.etf.rma.aktivnosti.IgrajKvizAkt;
import ba.unsa.etf.rma.klase.AdapterZaListuKvizovaW550;
import ba.unsa.etf.rma.klase.FirebaseKategorije;
import ba.unsa.etf.rma.klase.FirebasePitanja;
import ba.unsa.etf.rma.klase.Kategorija;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;

import static android.app.Activity.RESULT_OK;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.POSTOJI_LI_KATEGORIJA;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.firebasePitanja;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kategorije;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kvizovi;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.pozicijaKviza;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.prikazaniKvizovi;
import static ba.unsa.etf.rma.klase.FirebaseKvizovi.streamToStringConversion;

public class DetailFrag extends Fragment {

    private GridView gridKvizovi;
    private AdapterZaListuKvizovaW550 adapterZaListuKvizovaW550;
    private OnDetailFragmentListener callback;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        gridKvizovi = (GridView) rootView.findViewById( R.id.gridKvizovi );
        adapterZaListuKvizovaW550 = new AdapterZaListuKvizovaW550( getContext(), prikazaniKvizovi);
        gridKvizovi.setAdapter( adapterZaListuKvizovaW550 );
        adapterZaListuKvizovaW550.notifyDataSetChanged();

        gridKvizovi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Kviz k = (Kviz)parent.getItemAtPosition(position);
                if( !k.getNaziv().equals("Dodaj kviz") ){
                    Intent intent = new Intent(getContext(), IgrajKvizAkt.class);
                    intent.putExtra("odabraniKviz", k );
                    startActivity(intent);
                }
            }
        });

        gridKvizovi.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Intent dodajKvizAkt = new Intent( getContext(), DodajKvizAkt.class );
                Kviz odabraniKviz = (Kviz)parent.getItemAtPosition(position);
                if( odabraniKviz.getNaziv().equals("Dodaj kviz") ){
                    Kategorija kategorija = new Kategorija();
                    kategorija.setNaziv("Svi");
                    kategorija.setId("5");
                    odabraniKviz.setKategorija(kategorija);
                }
                dodajKvizAkt.putExtra( "sviKvizovi", kvizovi );
                dodajKvizAkt.putExtra("trenutniKviz", odabraniKviz );
                dodajKvizAkt.putExtra( "sveKategorije", kategorije );
                for( int i = 0; i < kvizovi.size(); i++ )
                    if( kvizovi.get(i).getNaziv().equals( ((Kviz) parent.getItemAtPosition(position)).getNaziv() ) )
                        pozicijaKviza = i;
                startActivityForResult( dodajKvizAkt, pozicijaKviza );
                return true;
            }
        });

        return rootView;
    }

    public void primiNotifikaciju(String odabir) throws ExecutionException, InterruptedException {
        FilterKvizova filter = new FilterKvizova(getContext(),odabir);
        filter.execute();
    }

    public void zapocniPreuzimanje(){
        PokupiFirebasePitanja pokupiFirebasePitanja = new PokupiFirebasePitanja(getContext());
        pokupiFirebasePitanja.execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == pozicijaKviza ){
            if( resultCode == RESULT_OK) {
                adapterZaListuKvizovaW550.notifyDataSetChanged();
                callback.msg1();
                Kviz kvizZaDodati = (Kviz)data.getExtras().get("noviKviz");
                boolean dodajNovi = (boolean)data.getExtras().get("dodajNoviKviz");
                if( dodajNovi ) {
                    DodajEditujKviz dodaj = new DodajEditujKviz(getContext(),kvizZaDodati,dodajNovi);
                    dodaj.execute();
                }
                else{
                    DodajEditujKviz edituj = new DodajEditujKviz(getContext(),kvizZaDodati,dodajNovi);
                    edituj.execute();
                }
            }
            else{
                for( int i = 0; i < kvizovi.size(); i++ ){
                    for( int j = i + 1; j < kvizovi.size(); j++ )
                        if( kvizovi.get(i).getNaziv().equals( kvizovi.get(j).getNaziv() ) ) {
                            kvizovi.remove(kvizovi.get(i));
                            j--;
                        }
                }
                try {
                    callback.slanjeObavijestiZaResetKategorija();
                    callback.msg1();
                }
                catch (Exception e){
                    //ignored
                }
            }

        }
        else{
            for( int i = 0; i < kvizovi.size(); i++ ){
                for( int j = i + 1; j < kvizovi.size(); j++ )
                    if( kvizovi.get(i).getNaziv().equals( kvizovi.get(j).getNaziv() ) ) {
                        kvizovi.remove(kvizovi.get(i));
                        j--;
                    }
            }
            try {
                callback.slanjeObavijestiZaResetKategorija();
                callback.msg1();
            }
            catch (Exception e){
                //ignored
            }
        }
    }

    public interface OnDetailFragmentListener {
        void msg1();
        void slanjeObavijestiZaResetKategorija();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DetailFrag.OnDetailFragmentListener) {
            callback = (DetailFrag.OnDetailFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGreenFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    public void NOVI_KVIZ_REGISTRUJ_BAZA_I_APLIKACIJA(Kviz kvizZaDodati,String opcija) throws ExecutionException, InterruptedException {
        kvizovi.add( kvizovi.size(), kvizZaDodati );
     //   FirebaseKvizovi.DODAJ_ILI_EDITUJ_KVIZ(kvizZaDodati,getContext(),opcija);
        ArrayList<Pitanje> pitanjaZaDodati = new ArrayList<>();
        for( int i = 0; i < kvizZaDodati.getPitanja().size(); i++ ) {
            boolean pitanjeVecPostoji = false;
            for (int j = 0; j < firebasePitanja.size(); j++) {
                if (kvizZaDodati.getPitanja().get(i).getNaziv().equals( firebasePitanja.get(j).getNaziv() )) {
                    pitanjeVecPostoji = true;
                }
            }
            if( !pitanjeVecPostoji ) pitanjaZaDodati.add( kvizZaDodati.getPitanja().get(i) );
        }
        FirebasePitanja.dodajPitanja( pitanjaZaDodati, getContext() );
        FirebaseKategorije.provjeraPostojanjaKategorije( kvizZaDodati.getKategorija(), getContext() );
        if( !kvizZaDodati.getKategorija().getNaziv().equals("Svi") && !POSTOJI_LI_KATEGORIJA ) {
            FirebaseKategorije.dodajKategoriju(kvizZaDodati.getKategorija(), getContext());
            POSTOJI_LI_KATEGORIJA = true;
        }
    }

    public final class FilterKvizova extends AsyncTask<String,Void,Void> {

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
            adapterZaListuKvizovaW550.notifyDataSetChanged();
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
            PokupiFirebaseKvizove pokupiFirebaseKvizove = new PokupiFirebaseKvizove(getContext());
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
            prikazaniKvizovi.clear();
            prikazaniKvizovi.addAll(kvizovi);
            Kviz k = new Kviz();
            k.setNaziv("Dodaj kviz");
            prikazaniKvizovi.add(k);
            adapterZaListuKvizovaW550.notifyDataSetChanged();
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
                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
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
                FilterKvizova filter = new FilterKvizova(getContext(),"Svi");
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
                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                alertDialog.setTitle("Obavijest");
                alertDialog.setMessage(tekstObavjestenja);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                alertDialog.show();
                FilterKvizova filter = new FilterKvizova(getContext(), "Svi");
                filter.execute();

            }
            callback.slanjeObavijestiZaResetKategorija();
            callback.msg1();
        }

    }

}
