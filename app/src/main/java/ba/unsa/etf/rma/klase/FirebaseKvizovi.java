package ba.unsa.etf.rma.klase;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.firebasePitanja;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kategorije;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kvizovi;

public class FirebaseKvizovi extends AppCompatActivity {
    public static void dodajKviz(Kviz kviz, Context context) throws ExecutionException, InterruptedException {
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
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-e36af/databases/(default)/documents/Kvizovi?documentId=" + kvizovi.size() + "&access_token=";
                    URL urlOBJ = new URL( URL + URLEncoder.encode(TOKEN,"UTF-8"));
                    System.out.println(urlOBJ);
                    HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                    CONNECTION.setDoOutput(true);
                    CONNECTION.setRequestMethod("POST");
                    CONNECTION.setRequestProperty("Content-Type","application/json");
                    CONNECTION.setRequestProperty("Accept","application/json");
                    String noviDokument = "{ \"fields\": { \"naziv\": { \"stringValue\" : \"" + kviz.getNaziv() + "\" }, \"idKategorije\" : { \"stringValue\" : \"" +
                            kviz.getKategorija().getId() + "\" }, \"pitanja\": { \"arrayValue\" : { \"values\": [";
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

        }.execute();
    }
    public static void dajKvizove(Context context) throws ExecutionException, InterruptedException {
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
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-e36af/databases/(default)/documents/Kvizovi?access_token=";
                    java.net.URL urlOBJ = new URL( URL + URLEncoder.encode(TOKEN,"UTF-8"));
                    HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                    InputStream inputStream = new BufferedInputStream(CONNECTION.getInputStream());
                    String result = streamToStringConversion(inputStream);
                    JSONObject jo = new JSONObject(result);
                    JSONArray dokumentovaniKvizovi;
                    try{
                        dokumentovaniKvizovi  = jo.getJSONArray("documents");
                    }
                    catch (Exception e){
                        return null;
                    }
                    for( int i = 0; i < dokumentovaniKvizovi.length(); i++ ){
                        JSONObject dokument = dokumentovaniKvizovi.getJSONObject(i);
                        JSONObject field =  dokument.getJSONObject("fields");
                        JSONObject nazivOBJEKAT = field.getJSONObject("naziv");
                        String nazivString = nazivOBJEKAT.getString("stringValue");
                        JSONObject idKategorijeOBJEKAT = field.getJSONObject("idKategorije");
                        String idKategorijeString = idKategorijeOBJEKAT.getString("stringValue");
                        JSONObject pitanjaOBJECT = field.getJSONObject("pitanja");
                        JSONObject pitanjaARRAY = pitanjaOBJECT.getJSONObject("arrayValue");
                        ArrayList<String> pitanjaLista = new ArrayList<>();
                        try {
                            JSONArray pitanja = pitanjaARRAY.getJSONArray("values");
                            for (int j = 0; j < pitanja.length(); j++) {
                                JSONObject stringValue = pitanja.getJSONObject(i);
                                String string = stringValue.getString("stringValue");
                                pitanjaLista.add(string);
                            }
                        }
                        catch (Exception e){

                        }
                        ArrayList<Pitanje> pitanjaZaKviz = new ArrayList<>();
                        for( int k = 0; k < firebasePitanja.size(); k++ )
                            for( int l = 0; l < pitanjaLista.size(); l++ )
                                if( pitanjaLista.get(l).equals( firebasePitanja.get(k).getNaziv() ) )
                                    pitanjaZaKviz .add( firebasePitanja.get(k) );
                        Kviz noviKviz = new Kviz();
                        noviKviz.setNaziv( nazivString );
                        for( int m = 0; m < kategorije.size(); m++ )
                            if( kategorije.get(m).getId().equals( idKategorijeString ) )
                                noviKviz.setKategorija( kategorije.get(m) );
                        noviKviz.setPitanja( pitanjaZaKviz );
                        kvizovi.add( kvizovi.size(), noviKviz );
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
