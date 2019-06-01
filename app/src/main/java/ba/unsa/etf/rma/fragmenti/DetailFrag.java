package ba.unsa.etf.rma.fragmenti;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.aktivnosti.DodajKvizAkt;
import ba.unsa.etf.rma.aktivnosti.IgrajKvizAkt;
import ba.unsa.etf.rma.aktivnosti.KvizoviAkt;
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
                        KvizoviAkt.pozicijaKviza = i;
                startActivityForResult( dodajKvizAkt, KvizoviAkt.pozicijaKviza );
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
        if( requestCode == KvizoviAkt.pozicijaKviza ){
            if( resultCode == RESULT_OK ){
                Kviz kvizZaDodati = (Kviz)data.getExtras().get("noviKviz");
                boolean dodajNovi = (boolean)data.getExtras().get("dodajNoviKviz");
//                if( dodajNovi ) {
//                    try {
//                        NOVI_KVIZ_REGISTRUJ_BAZA_I_APLIKACIJA(kvizZaDodati,"POST");
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//                else {
//                    try {
//                        NOVI_KVIZ_REGISTRUJ_BAZA_I_APLIKACIJA(kvizZaDodati,"PATCH");
//                    } catch (ExecutionException e) {
//                        e.printStackTrace();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//                KvizoviAkt.prikazaniKvizovi.clear();
//                KvizoviAkt.prikazaniKvizovi.addAll( kvizovi );
//
//                Kviz k = new Kviz();
//                k.setNaziv("Dodaj kviz");
//                KvizoviAkt.prikazaniKvizovi.add( k );
                adapterZaListuKvizovaW550.notifyDataSetChanged();
                callback.msg1();
            }
            else{
                prikazaniKvizovi.clear();
                prikazaniKvizovi.addAll( kvizovi );
                Kviz k = new Kviz();
                k.setNaziv("Dodaj kviz");
                prikazaniKvizovi.add( k );
                adapterZaListuKvizovaW550.notifyDataSetChanged();
                callback.msg1();
            }
        }
    }

    public interface OnDetailFragmentListener {
        void msg1();
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

}
