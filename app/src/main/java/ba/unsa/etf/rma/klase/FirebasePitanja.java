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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.firebasePitanja;

public class FirebasePitanja {

    public static void dodajPitanje(Pitanje pitanje, Context context) throws ExecutionException, InterruptedException {
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
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-e36af/databases/(default)/documents/Pitanja?documentId=" + firebasePitanja.size() +"&access_token=";
                    java.net.URL urlOBJ = new URL( URL + URLEncoder.encode(TOKEN,"UTF-8"));
                    HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                    CONNECTION.setDoOutput(true);
                    CONNECTION.setRequestMethod("POST");
                    CONNECTION.setRequestProperty("Content-Type","application/json");
                    CONNECTION.setRequestProperty("Accept","application/json");
                    int indexTacnog = -1;
                    for( int i = 0; i < pitanje.getOdgovori().size(); i++ )
                        if( pitanje.getOdgovori().get(i).equals( pitanje.getTacan() ) ){
                            indexTacnog = i;
                            break;
                        }
                    String noviDokument = "{ \"fields\": { \"naziv\": { \"stringValue\" : \"" + pitanje.getNaziv() + "\" }, \"indexTacnog\" : { \"integerValue\" : \"" +
                            indexTacnog + "\" }, \"odgovori\": { \"arrayValue\" : { \"values\": [";
                    for( int i = 0; i < pitanje.getOdgovori().size(); i++ ){
                        String jsonPITANJE = "{ \"stringValue\" : \"";
                        jsonPITANJE += pitanje.getOdgovori().get(i);
                        jsonPITANJE += "\" }";
                        noviDokument += jsonPITANJE;
                        if( i < pitanje.getOdgovori().size() - 1   ) noviDokument += ",";
                    }
                    noviDokument += " ] } } } }";
                    System.out.println(noviDokument);
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

    public static void dajPitanja(Context context) throws ExecutionException, InterruptedException {
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
                    String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-e36af/databases/(default)/documents/Pitanja?access_token=";
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

    public static void dodajPitanja(ArrayList<Pitanje> pitanja, Context context) throws ExecutionException, InterruptedException {
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
                    for( int N = 0; N < pitanja.size(); N++ ) {
                        Pitanje pitanje = pitanja.get(N);
                        String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-e36af/databases/(default)/documents/Pitanja?documentId=" + firebasePitanja.size() + "&access_token=";
                        java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                        HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                        CONNECTION.setDoOutput(true);
                        CONNECTION.setRequestMethod("POST");
                        CONNECTION.setRequestProperty("Content-Type", "application/json");
                        CONNECTION.setRequestProperty("Accept", "application/json");
                        int indexTacnog = -1;
                        for (int i = 0; i < pitanje.getOdgovori().size(); i++)
                            if (pitanje.getOdgovori().get(i).equals(pitanje.getTacan())) {
                                indexTacnog = i;
                                break;
                            }
                        String noviDokument = "{ \"fields\": { \"naziv\": { \"stringValue\" : \"" + pitanje.getNaziv() + "\" }, \"indexTacnog\" : { \"integerValue\" : \"" +
                                indexTacnog + "\" }, \"odgovori\": { \"arrayValue\" : { \"values\": [";
                        for (int i = 0; i < pitanje.getOdgovori().size(); i++) {
                            String jsonPITANJE = "{ \"stringValue\" : \"";
                            jsonPITANJE += pitanje.getOdgovori().get(i);
                            jsonPITANJE += "\" }";
                            noviDokument += jsonPITANJE;
                            if (i < pitanje.getOdgovori().size() - 1) noviDokument += ",";
                        }
                        noviDokument += " ] } } } }";
                        System.out.println(noviDokument);
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
                        firebasePitanja.add( pitanje );
                        CONNECTION.disconnect();
                    }


                }
                catch (IOException e){
                    e.printStackTrace();
                }
                return null;
            }


        }.execute();

    }

}
