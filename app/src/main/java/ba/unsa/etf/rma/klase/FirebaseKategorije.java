package ba.unsa.etf.rma.klase;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kategorije;

public class FirebaseKategorije {

    public static void dodajKategoriju(Kategorija kategorija, Context context) throws ExecutionException, InterruptedException {
        new AsyncTask<String, Void, Void>() {

            @Override
            protected Void doInBackground(String... strings) {
                //Provjeriti da li je vec u bazi.

                GoogleCredential credential;
                try{

                    InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                    credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                    credential.refreshToken();
                    String TOKEN = credential.getAccessToken();
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-e36af/databases/(default)/documents/Kategorije?documentId=" + kategorije.size() + "&access_token=";
                    java.net.URL urlOBJ = new URL( URL + URLEncoder.encode(TOKEN,"UTF-8"));
                    HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                    CONNECTION.setDoOutput(true);
                    CONNECTION.setRequestMethod("POST");
                    CONNECTION.setRequestProperty("Content-Type","application/json");
                    CONNECTION.setRequestProperty("Accept","application/json");
                    String noviDokument = "{ \"fields\": { \"naziv\": { \"stringValue\" : \"" + kategorija.getNaziv() + "\" }, \"idIkonice\" : { \"integerValue\" : \"" +
                            kategorija.getId() + "\" } } }";
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
                    kategorije.add( kategorije.size(), kategorija );
                    CONNECTION.disconnect();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
                return null;
            }

        }.execute();
    }

    public static void dajKategorije(Context context) throws ExecutionException, InterruptedException {
        new AsyncTask<String, Void, Void>() {

            @Override
            protected Void doInBackground(String... strings) {
                //Provjeriti da li je vec u bazi.
                GoogleCredential credential;
                try{
                    InputStream secretStream = context.getResources().openRawResource(R.raw.secret);
                    credential = GoogleCredential.fromStream(secretStream).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                    credential.refreshToken();
                    String TOKEN = credential.getAccessToken();
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-e36af/databases/(default)/documents/Kategorije?access_token=";
                    java.net.URL urlOBJ = new URL( URL + URLEncoder.encode(TOKEN,"UTF-8"));
                    HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                    InputStream inputStream = new BufferedInputStream(CONNECTION.getInputStream());
                    String result = streamToStringConversion(inputStream);
                    JSONObject jo = new JSONObject(result);
                    JSONArray dokumentovaniKvizovi;
                    try{
                        dokumentovaniKvizovi = jo.getJSONArray("documents");
                    }
                    catch (Exception e){
                        return null;
                    }
                    for( int i = 0; i < dokumentovaniKvizovi.length(); i++ ){
                        JSONObject dokument = dokumentovaniKvizovi.getJSONObject(i);
                        JSONObject field =  dokument.getJSONObject("fields");
                        JSONObject nazivOBJEKAT = field.getJSONObject("naziv");
                        String nazivString = nazivOBJEKAT.getString("stringValue");
                        JSONObject idIkoniceOBJEKAT = field.getJSONObject("idIkonice");
                        int idIkoniceINT = idIkoniceOBJEKAT.getInt("integerValue");
                        String idIkonice = String.valueOf(idIkoniceINT);
                        Kategorija novaKategorija = new Kategorija();
                        novaKategorija.setNaziv(nazivString);
                        novaKategorija.setId( idIkonice );
                        kategorije.add( novaKategorija );
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



        }.execute().get();

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
