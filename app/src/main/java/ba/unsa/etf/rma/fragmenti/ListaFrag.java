package ba.unsa.etf.rma.fragmenti;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.AdapterZaListuKategorija;
import ba.unsa.etf.rma.klase.Kategorija;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.USPRAVAN_DISPLEJ;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kategorije;
import static ba.unsa.etf.rma.klase.FirebaseKvizovi.streamToStringConversion;

public class ListaFrag extends Fragment {

    private ListView listaKategorija;
    private AdapterZaListuKategorija adapterZaListuKategorija;
    private OnListaFragmentListener callback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lista, container, false);
        listaKategorija = (ListView) rootView.findViewById(R.id.listaKategorija);
        adapterZaListuKategorija = new AdapterZaListuKategorija(getContext(), kategorije);
        listaKategorija.setAdapter(adapterZaListuKategorija);
        if( !USPRAVAN_DISPLEJ )
            funkty();


        listaKategorija.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = parent.getItemAtPosition(position).toString();
                try {
                    String textBezRazmaka = text.replaceAll(" ", "_RAZMAK_");
                    String textBezKosihBezRazmaka = textBezRazmaka.replaceAll("/", "_KOSA_CRTA_");
                    callback.msg(textBezKosihBezRazmaka);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Toast.makeText(parent.getContext(), "Odabrano: " + text, Toast.LENGTH_SHORT).show();
            }
        });
        return rootView;
    }

    public void primiNotifikaciju() {
        adapterZaListuKategorija.notifyDataSetChanged();
    }



    public interface OnListaFragmentListener {
        void msg(String odabir) throws ExecutionException, InterruptedException;
        void slanjeObavijestiZaPocetakPreuzimanja();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ListaFrag.OnListaFragmentListener) {
            callback = (ListaFrag.OnListaFragmentListener) context;
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
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //Ovakvim pristupom da se u fragmentu pokupe kategorije, dolazi tokom izvrsavanja aplikacije do duplog kupljenja kategorija, tako
            //da se duplikati trebaju izbaciti. Naravno, u bazi se nalazi samo jedan primjerak svake...

            for( int i = 0; i < kategorije.size(); i++ ){
                for( int j = i + 1; j < kategorije.size(); j++ )
                    if( kategorije.get(i).getNaziv().equals( kategorije.get(j).getNaziv() ) ) {
                        kategorije.remove(kategorije.get(i));
                        j--;
                    }
            }
            try {
                callback.slanjeObavijestiZaPocetakPreuzimanja();
            }
            catch (Exception e){

            }
            adapterZaListuKategorija.notifyDataSetChanged();
        }
    }

    public void funkty(){
        //kategorije.clear();
        PokupiFirebaseKategorije pokupiFirebaseKategorije = new PokupiFirebaseKategorije(getContext());
        pokupiFirebaseKategorije.execute();
    }
}
