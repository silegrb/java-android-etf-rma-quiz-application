package ba.unsa.etf.rma.fragmenti;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;
import ba.unsa.etf.rma.klase.RangListaKlasa;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.RANG_LISTE;


public class PitanjeFrag extends Fragment {

    private TextView tekstPitanja;
    private ListView odgovoriPitanja;
    private Kviz trenutniKviz;
    private ArrayList<Pitanje> alPitanja = new ArrayList<>();
    private ArrayList<String> alOdgovori = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Pitanje trenutnoPitanje = null;
    private OnPitanjeFragmentListener callback;
    private int brTacnihOdgovora = 0;
    private int brPreostalihPitanja;
    private boolean pitanjeOdgovoreno = false;
    private Context CONTEXT;
    private FragmentManager fm;
    private  boolean alertReady = false;
    private  boolean kvizGotov = false;
    public static double POSTOTAK_KVIZA = 0;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.CONTEXT = context;
        fm = getFragmentManager();
        if (context instanceof PitanjeFrag.OnPitanjeFragmentListener) {
            callback = (PitanjeFrag.OnPitanjeFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGreenFragmentListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pitanje, container, false);
        tekstPitanja = (TextView)rootView.findViewById(R.id.tekstPitanja);
        odgovoriPitanja = (ListView) rootView.findViewById(R.id.odgovoriPitanja);
        trenutniKviz = (Kviz)getArguments().getSerializable("trenutniKviz");
        alPitanja.addAll( trenutniKviz.getPitanja() );
        brPreostalihPitanja = alPitanja.size();
        Collections.shuffle( alPitanja );
        adapter = new ArrayAdapter<String>( getActivity(), android.R.layout.simple_list_item_1, alOdgovori);
        odgovoriPitanja.setAdapter( adapter );
        adapter.notifyDataSetChanged();
        if( alPitanja.size() == 0 ) {
            RangLista fragmentRangLista = new RangLista();
            Bundle bundle = new Bundle();
            bundle.putSerializable( "trenutniKviz", trenutniKviz  );
            fragmentRangLista.setArguments(bundle);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace( R.id.pitanjePlace, fragmentRangLista);
            ft.commit();
        }
        else{
            trenutnoPitanje = alPitanja.get(0);
            tekstPitanja.setText( trenutnoPitanje.getNaziv() );
            alOdgovori.addAll( trenutnoPitanje.dajRandomOdgovore());
            adapter.notifyDataSetChanged();
        }

        odgovoriPitanja.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    if( !pitanjeOdgovoreno ) {

                        pitanjeOdgovoreno = true;
                        kvizGotov = false;

                        if (alPitanja.size() == 0) {
                            adapter.notifyDataSetChanged();
                            callback.messageFromGreenFragment(alPitanja.size(), alPitanja.size() - brPreostalihPitanja, brTacnihOdgovora);
                            RangLista fragmentRangLista = new RangLista();
                            Bundle bundle = new Bundle();
                            bundle.putSerializable( "trenutniKviz", trenutniKviz  );
                            fragmentRangLista.setArguments(bundle);
                            FragmentTransaction ft = fm.beginTransaction();
                            ft.replace( R.id.pitanjePlace, fragmentRangLista);
                            ft.commit();
                        } else {

                            int pozicijaTacnog = -1;
                            for( int i = 0; i < trenutnoPitanje.getOdgovori().size(); i++ )
                                if( trenutnoPitanje.getOdgovori().get(i).equals( trenutnoPitanje.getTacan() ) )
                                    pozicijaTacnog = i;
                            if( position != pozicijaTacnog )
                                view.setBackgroundResource( R.color.crvena );
                            else {
                                view.setBackgroundResource(R.color.zelena);
                                brTacnihOdgovora++;
                            }
                            //Provjera vidljivosti
                            if( (TextView)odgovoriPitanja.getChildAt( pozicijaTacnog - odgovoriPitanja.getFirstVisiblePosition() ) != null )
                                odgovoriPitanja.getChildAt(pozicijaTacnog - odgovoriPitanja.getFirstVisiblePosition()).setBackgroundResource( R.color.zelena );

                            odgovoriPitanja.setEnabled(false);
                            alPitanja.remove(trenutnoPitanje);
                            brPreostalihPitanja--;
                            (new Handler()).postDelayed(() -> {
                                for (int i = 0; i < parent.getChildCount(); i++)
                                    parent.getChildAt(i).setBackgroundColor(Color.WHITE);

                                if (alPitanja.size() == 0) {
                                   kvizGotov = true;

                                } else {
                                    trenutnoPitanje = alPitanja.get(0);
                                    tekstPitanja.setText(trenutnoPitanje.getNaziv());
                                    alOdgovori.clear();
                                    alOdgovori.addAll(trenutnoPitanje.dajRandomOdgovore());
                                    adapter.notifyDataSetChanged();
                                }
                                pitanjeOdgovoreno = false;
                                try {
                                    callback.messageFromGreenFragment(alPitanja.size() - 1, trenutniKviz.getPitanja().size() - brPreostalihPitanja, brTacnihOdgovora);
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                }
                                if( kvizGotov ){
                                    try {
                                        pokreniFormuZaRegistrovanjeURangListu();
                                    }
                                    catch (Exception e){
                                        //Ignored
                                    }
                                }
                                odgovoriPitanja.setEnabled(true);
                            }, 2000);

                        }
                    }
            }
        });

        return rootView;
    }

    public interface OnPitanjeFragmentListener {
        void messageFromGreenFragment(int brojPreostalih, int brojOdgovorenih, int brojTacnih);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    public class EditujRanglistu extends AsyncTask<String,Void,Void> {

        private Context context;
        private Kviz kviz;
        private RangListaKlasa rangListaKlasa;

        public EditujRanglistu(Context context, Kviz kviz, RangListaKlasa rangListaKlasa) {
            this.context = context;
            this.kviz = kviz;
            this.rangListaKlasa = rangListaKlasa;
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
                String URL = "https://firestore.googleapis.com/v1/projects/rma19sisicfaris31-97b17/databases/(default)/documents/Rangliste/" + rangListaKlasa.getNEPROMJENJIVI_ID() + "?access_token=";
                java.net.URL urlOBJ = new URL(URL + URLEncoder.encode(TOKEN, "UTF-8"));
                HttpURLConnection CONNECTION = (HttpURLConnection) urlOBJ.openConnection();
                CONNECTION.setDoOutput(true);
                CONNECTION.setRequestMethod("PATCH");
                CONNECTION.setRequestProperty("Content-Type", "application/json");
                CONNECTION.setRequestProperty("Accept", "application/json");
                String noviDokument = "{ \"fields\": { \"id\": {\"stringValue\" : \"" + rangListaKlasa.getNEPROMJENJIVI_ID() + "\"}, \"nazivKviza\": { \"stringValue\": \"" + kviz.getNaziv() + "\"}, \"lista\": {\"mapValue\": {\"fields\": {";
                int VELICINA_MAPE = rangListaKlasa.getMapa().size();
                int brojac = 0;
                for (Map.Entry<Integer, Pair<String, Double>> entry : rangListaKlasa.getMapa().entrySet()) {
                    Integer pozicijaPokusaja = entry.getKey();
                    Pair<String, Double> podaciOPokusaju = entry.getValue();
                    noviDokument += "\"" + pozicijaPokusaja + "\": {\"mapValue\": {\"fields\": {\"" + podaciOPokusaju.first + "\": {\"doubleValue\": " + String.valueOf(podaciOPokusaju.second) + "}}}}";
                    if (brojac < VELICINA_MAPE - 1) noviDokument += ",";
                    brojac++;
                }
                noviDokument += "} } } } }";
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
                CONNECTION.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            super.onPostExecute(result);
            RangLista fragmentRangLista = new RangLista();
            Bundle bundle = new Bundle();
            bundle.putSerializable( "trenutniKviz", trenutniKviz  );
            fragmentRangLista.setArguments(bundle);
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace( R.id.pitanjePlace, fragmentRangLista);
            ft.commit();
        }

    }

    public void pokreniFormuZaRegistrovanjeURangListu(){
        RangLista fragmentRangLista = new RangLista();
        Bundle bundle = new Bundle();
        bundle.putSerializable("trenutniKviz", trenutniKviz);
        fragmentRangLista.setArguments(bundle);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.pitanjePlace, fragmentRangLista);
        ft.commit();
        //OVDJE IDE ALERT DIALOG




        AlertDialog.Builder alertDialog = new AlertDialog.Builder(CONTEXT);
        alertDialog.setTitle("\"" + trenutniKviz.getNaziv() + "\" uspjesno zavrsen!\nDa li zelite registrovati vas rezultat?");
        alertDialog.setMessage("Ime igraca");
        final EditText input = new EditText(CONTEXT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input); // uncomment this line
        alertDialog.setPositiveButton("Potvrdi",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String text = input.getText().toString();
                        if (text.equals("")) {
                            Toast.makeText(CONTEXT,
                                    "Unesite ime igraca!", Toast.LENGTH_SHORT).show();
                        } else {
                            Pair<String, Double> ubacivanjePokusaja = new Pair<>(text, POSTOTAK_KVIZA);
                            for (int i = 0; i < RANG_LISTE.size(); i++)
                                if (RANG_LISTE.get(i).getNazivKviza().equals(trenutniKviz.getNaziv())) {
                                    RANG_LISTE.get(i).registrujKorisnika(ubacivanjePokusaja);
                                    EditujRanglistu edit = new EditujRanglistu(CONTEXT, trenutniKviz, RANG_LISTE.get(i));
                                    edit.execute();
                                }
                            dialog.cancel();
                        }
                    }
                });

        alertDialog.setNegativeButton("Ponisti",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        RangLista fragmentRangLista = new RangLista();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("trenutniKviz", trenutniKviz);
                        fragmentRangLista.setArguments(bundle);
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.replace(R.id.pitanjePlace, fragmentRangLista);
                        ft.commit();
                    }
                });

        AlertDialog alert = alertDialog.create();


        alertReady = false;
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                if (alertReady == false) {
                    Button button = alert.getButton(DialogInterface.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String text = input.getText().toString();
                            if (text.equals("")) {
                                Toast.makeText(CONTEXT,
                                        "Unesite ime igraca!", Toast.LENGTH_SHORT).show();
                            } else {
                                Pair<String, Double> ubacivanjePokusaja = new Pair<>(text, POSTOTAK_KVIZA);
                                for (int i = 0; i < RANG_LISTE.size(); i++)
                                    if (RANG_LISTE.get(i).getNazivKviza().equals(trenutniKviz.getNaziv())) {

                                        RANG_LISTE.get(i).registrujKorisnika(ubacivanjePokusaja);
                                        EditujRanglistu edit = new EditujRanglistu(CONTEXT, trenutniKviz, RANG_LISTE.get(i));
                                        edit.execute();

                                    }
                                dialog.cancel();
                            }
                        }
                    });
                    alertReady = true;
                }
            }
        });

        alert.show();
    }

}
