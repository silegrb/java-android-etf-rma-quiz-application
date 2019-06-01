package ba.unsa.etf.rma.klase;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.firebasePitanja;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kategorije;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kvizovi;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.prikazaniKvizovi;

public class FirebaseKvizovi extends AppCompatActivity {


    public static void FILTRIRAJ_KVIZOVE_FRAGMENTI(Context context,AdapterZaListuKvizovaW550 adapterZaListuKvizova,String text) throws ExecutionException, InterruptedException {
        new AsyncTask<String, Void, Void>() {

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
                        URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
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
                                JSONObject pitanja = fields.getJSONObject("pitanja");
                                JSONObject arrayValue = pitanja.getJSONObject("arrayValue");
                                JSONArray values = arrayValue.getJSONArray("values");
                                ArrayList<String> naziviPitanja = new ArrayList<>();
                                for( int j = 0; j < values.length(); j++ ){
                                    JSONObject valuesObjekat = values.getJSONObject(j);
                                    String nazivPitanja = valuesObjekat.getString("stringValue");
                                    naziviPitanja.add(nazivPitanja);
                                }
                                JSONObject naziv = fields.getJSONObject("naziv");
                                String imeKvize = naziv.getString("stringValue");
                                JSONObject id = fields.getJSONObject("id");
                                String idKviza = naziv.getString("stringValue");
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
                                k.setNEPROMJENJIVI_ID( idKviza );
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
            }


        }.execute();

    }

    public static String streamToStringConversion(InputStream is){
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        try{
            while( (line = bufferedReader.readLine()) != null )
                stringBuilder.append(line + "\n");
        }
        catch(IOException e) { } finally {
            try{
                is.close();
            }
            catch (IOException e){

            }

        }
        return stringBuilder.toString();
    }
}
