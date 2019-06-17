package ba.unsa.etf.rma.klase;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;

import static ba.unsa.etf.rma.aktivnosti.DodajKategorijuAkt.dodajKat;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.POSTOJI_LI_KATEGORIJA;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kategorije;

public class FirebaseKategorije {

    private Context context;

    public FirebaseKategorije(Context context){
        this.context = context;
    }

    public static void dodajKategoriju(Kategorija kategorija, Context context) throws ExecutionException, InterruptedException {
        new AsyncTask<String, Void, Void>() {

            @Override
            protected Void doInBackground(String... strings) {
                //Provjeriti da li je vec u bazi.

                GoogleCredential credential;
                try {
                    String index_sa_kosom_crtom = kategorija.getNaziv().replaceAll( " ", "_RAZMAK_" );
                    String index = index_sa_kosom_crtom.replaceAll( "/", "_KOSA_CRTA_" );
                    InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                    credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                    credential.refreshToken();
                    String TOKEN = credential.getAccessToken();
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Kategorije?documentId="+ index  +"&access_token=";
                    java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                    HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                    CONNECTION.setDoOutput(true);
                    CONNECTION.setRequestMethod("POST");
                    CONNECTION.setRequestProperty("Content-Type", "application/json");
                    CONNECTION.setRequestProperty("Accept", "application/json");
                    String noviDokument = "{ \"fields\": { \"naziv\": { \"stringValue\" : \"" + kategorija.getNaziv() + "\" }, \"idIkonice\" : { \"integerValue\" : \"" +
                            kategorija.getId() + "\" } } }";
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
                    kategorije.add(kategorije.size(), kategorija);
                    CONNECTION.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result){
                super.onPostExecute(result);
                SQLiteBaza db = new SQLiteBaza(context);
                db.dodajKategoriju(kategorija);
            }


        }.execute();


    }

    public static String streamToStringConversion(InputStream is) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        try {
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line + "\n");
        } catch (IOException e) {
        } finally {
            try {
                is.close();
            } catch (IOException e) {

            }

        }
        return stringBuilder.toString();
    }

    public static void provjeraPostojanjaKategorije(Kategorija kategorija, Context context) throws ExecutionException, InterruptedException {
        new AsyncTask<String, Void, Void>() {

            @Override
            protected Void doInBackground(String... strings) {

                GoogleCredential credential;
                try {
                    InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                    credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                    credential.refreshToken();
                    String TOKEN = credential.getAccessToken();
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents:runQuery?access_token=";
                    URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
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
                            "               \"stringValue\":\"" + kategorija.getNaziv() + "\"\n" +
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
                    //int CODE = conn.getResponseCode();
                    InputStream odgovor = CONNECTION.getInputStream();
                    String result = "{\"documents\" : ";
                    result += streamToStringConversion(odgovor);
                    result += " }";
                    JSONObject jsonObject  = new JSONObject(result);
                    JSONArray dokumenti = jsonObject.getJSONArray("documents");
                    int brojacDokumenata = 0;
                    for( int i = 0; i < dokumenti.length(); i++ ){
                        JSONObject objekat = dokumenti.getJSONObject(i);
                        try {
                            JSONObject dokument = objekat.getJSONObject("document");
                            brojacDokumenata ++;
                        }
                        catch (JSONException e){

                        }
                    }
                    if( brojacDokumenata == 0 ){
                        POSTOJI_LI_KATEGORIJA = false;
                        dodajKat = true;
                    }
                    CONNECTION.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }


        }.execute();

    }

}


