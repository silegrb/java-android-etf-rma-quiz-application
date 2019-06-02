package ba.unsa.etf.rma.fragmenti;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.FirebasePitanja;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.RangListaKlasa;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.RANG_LISTE;

public class RangLista extends Fragment {

    private ListView rangListaKviza;
    private ArrayList<String> rezultati = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Kviz trenutniKviz;
    private Context CONTEXT;
    private String idRangListe;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_rang_lista, container, false);
        rangListaKviza = (ListView) rootView.findViewById(R.id.rangListaKviza);
        trenutniKviz = (Kviz)getArguments().getSerializable("trenutniKviz");
        for( int i = 0; i < RANG_LISTE.size(); i++ )
            if( RANG_LISTE.get(i).getNazivKviza().equals( trenutniKviz.getNaziv() ) )
                idRangListe = RANG_LISTE.get(i).getNEPROMJENJIVI_ID();
        adapter = new ArrayAdapter<String>( getActivity(), android.R.layout.simple_list_item_1, rezultati);
        rangListaKviza.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        PreuzmiRanglistu preuzmiRanglistu = new PreuzmiRanglistu(CONTEXT,idRangListe);
        preuzmiRanglistu.execute();
        return rootView;
    }

    public class PreuzmiRanglistu extends AsyncTask<String, Void, Void> {

        private Context context;
        private String idRangListe;
        private RangListaKlasa rangListaKlasa = new RangListaKlasa();


        public PreuzmiRanglistu(Context context,String idRangListe) {

            this.context = context;
            this.idRangListe = idRangListe;
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
                String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Rangliste/"+idRangListe+"?access_token=";
                java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                InputStream inputStream = new BufferedInputStream(CONNECTION.getInputStream());
                String result = FirebasePitanja.streamToStringConversion(inputStream);
                JSONObject jo = new JSONObject(result);
                JSONObject fields;
                try{
                    fields = jo.getJSONObject("fields");
                }
                catch (JSONException e){
                    return null;
                }

                JSONObject naziv = fields.getJSONObject("nazivKviza");
                JSONObject id = fields.getJSONObject("id");
                rangListaKlasa.setNazivKviza( naziv.getString("stringValue") );
                rangListaKlasa.setNEPROMJENJIVI_ID( id.getString("stringValue") );
                Map<Integer,Pair<String,Double>> povratnaMapa = new TreeMap<>();
                try {
                    JSONObject lista = fields.getJSONObject("lista");
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
                    rangListaKlasa.setMapa(povratnaMapa);
                }
                catch (JSONException e){
                    //Ignored
                }
                CONNECTION.disconnect();
            } catch (IOException  e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Map<Integer,Pair<String,Double>> novaMapa = new TreeMap<Integer, Pair<String,Double>>();
            for(Map.Entry<Integer,Pair<String,Double>> entry : rangListaKlasa.getMapa().entrySet()) {
                Integer pozicijaPokusaja = entry.getKey();
                Pair<String, Double> podaciOPokusaju = entry.getValue();
                rezultati.add( String.valueOf(pozicijaPokusaja) + ". " + podaciOPokusaju.first + " (" + String.valueOf(podaciOPokusaju.second) + "%)" );
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.CONTEXT = context;
    }


}
